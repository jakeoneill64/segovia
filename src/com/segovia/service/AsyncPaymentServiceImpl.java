package com.segovia.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasongoodwin.monads.Try;
import com.segovia.model.BasicPaymentResponse;
import com.segovia.model.DetailedPaymentResponse;
import com.segovia.model.PaymentRequest;
import com.segovia.model.Session;
import com.segovia.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@DependsOn("FSPaymentRepository")
public class AsyncPaymentServiceImpl implements AsyncPaymentService{

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final ScheduledExecutorService scheduledExecutorService;

    private Session currentSession;

    private final Logger logger = LoggerFactory.getLogger(AsyncPaymentServiceImpl.class);

    @Value("${payment-provider.session-timeout}")
    private long providerSessionTtl;
    @Value("${payment-provider.url}")
    private String providerUri;
    @Value("${credentials.account-id}")
    private String accountId;
    @Value("${credentials.key}")
    private String key;

    //this allows us to use exponential smoothing function for polling and allows us to
    //stop polling after we have received a callback.
    private final Map<PaymentRequest, Long> pollIntervalByPaymentRequest = new HashMap<>();
    //auxiliary data struct for reference lookups
    private final Map<String, PaymentRequest> paymentRequestByConversationId = new HashMap<>();



    @Override
    public void process(PaymentRequest request){

        Try<BasicPaymentResponse> paymentAttempt = Try.ofFailable(() -> submitPayment(request));

        logger.info("submitting payment with reference ".concat(request.reference()));

        Try<DetailedPaymentResponse> statusResponseAttempt = paymentAttempt
            .map((basicPaymentResponse) ->

                                Optional.ofNullable(basicPaymentResponse.getConversationID()).isPresent() ?

                                Try.ofFailable(
                                    () -> fetchPaymentStatus(basicPaymentResponse.getConversationID())
                                )
                                .getUnchecked() :

                                new DetailedPaymentResponse(
                                    request.reference(),
                                    basicPaymentResponse.getStatus(),
                                    Optional.ofNullable(basicPaymentResponse.getMessage()).orElse("")
                                )


            );

        if(!statusResponseAttempt.isSuccess()) {
            insertUnknownPaymentResponse(request.reference());
            return;
        }

        DetailedPaymentResponse statusResponse = statusResponseAttempt.getUnchecked();

        paymentRequestByConversationId.put(statusResponse.getConversationID(), request);

        if(statusResponse.getStatus() == 100) {
            pollIntervalByPaymentRequest.put(request, 2L);
            scheduleStatusPoll(statusResponse.getConversationID(), request);
        }else {
            paymentRepository.insert(statusResponse);
        }

    }

    @Override
    public void onResponseReceived(DetailedPaymentResponse detailedResponse) {
        pollIntervalByPaymentRequest.remove(paymentRequestByConversationId.get(detailedResponse.getConversationID()));
        paymentRepository.insert(detailedResponse);
    }



    private Optional<Session> getOrFetchSession() {

        if (currentSession == null || currentSession.validUntil() < System.currentTimeMillis()) {

            Try<Session> authAttempt = Try.ofFailable(this::authenticate);
            authAttempt.onFailure((e) -> logger.error(e.getMessage()));
            if (authAttempt.isSuccess()) {
                currentSession = authAttempt.getUnchecked();
                return Optional.of(authAttempt.getUnchecked());
            }
            else {
                logger.error("could not authenticate with the payment provider");
                return Optional.empty();
            }

        }

        return Optional.of(currentSession);
    }


    private Session authenticate(){

        HttpHeaders headers = new HttpHeaders();

        headers.set("Accept", "application/json");
        headers.set("Api-Key", key);
        headers.set("Content-Type", "application/json");

        String body = "{\"account\":\"%s\"}".formatted(accountId);
        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        String responseBody = restTemplate
                                .exchange(
                                    providerUri.concat("/auth"),
                                    HttpMethod.POST,
                                    httpEntity,
                                    String.class
                                )
                                .getBody();

        //if the creds are not valid, there will be no token field - this will throw.
        String token = Try.ofFailable(() -> objectMapper.readTree(responseBody))
                .map(jsonNode -> jsonNode.get("token").asText())
                .orElseThrow(RuntimeException::new);


        return new Session(token,System.currentTimeMillis() + providerSessionTtl);

    }


    private DetailedPaymentResponse fetchPaymentStatus(String conversationId){

        Session session = getOrFetchSession().orElseThrow(() -> new RuntimeException("authentication failed"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer ".concat(session.token()));
        HttpEntity<PaymentRequest> httpEntity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                    providerUri.concat("/status/%s".formatted(conversationId)),
                    HttpMethod.GET,
                    httpEntity,
                    DetailedPaymentResponse.class
                )
                .getBody();
    }



    private BasicPaymentResponse submitPayment(PaymentRequest paymentRequest){

        Session session = getOrFetchSession().orElseThrow(() -> new RuntimeException("authentication failed"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer ".concat(session.token()));
        HttpEntity<PaymentRequest> httpEntity = new HttpEntity<>(paymentRequest, headers);

        return restTemplate.exchange(providerUri.concat("/pay"), HttpMethod.POST, httpEntity, BasicPaymentResponse.class).getBody();
    }



    private void scheduleStatusPoll(String conversationId, PaymentRequest request){

        long currentPollPeriod = Optional.ofNullable(pollIntervalByPaymentRequest.get(request)).orElse(1L);

        Runnable pollAction = () -> {

            if(!pollIntervalByPaymentRequest.containsKey(request))
                return;

            Try<DetailedPaymentResponse> statusResponseAttempt = Try.ofFailable(
                    () -> fetchPaymentStatus(conversationId)
            );

            if(!statusResponseAttempt.isSuccess()) {
                insertUnknownPaymentResponse(request.reference());
                return;
            }

            DetailedPaymentResponse detailedResponse = statusResponseAttempt.getUnchecked();

            if(detailedResponse.getStatus() == 100) { // reschedule

                long doubledInterval = Optional.ofNullable(pollIntervalByPaymentRequest.get(request))
                        .map(period -> period * 2)
                        .orElse(2L);

                //timeout after 24 hours
                if(doubledInterval >= 86_400_000L) {
                    pollIntervalByPaymentRequest.remove(request);
                    insertUnknownPaymentResponse(request.reference());
                }

                scheduleStatusPoll(conversationId, request);

                pollIntervalByPaymentRequest.put(
                    request,
                    Optional.ofNullable(pollIntervalByPaymentRequest.get(request))
                                            .map(period -> period * 2)
                                            .orElse(2L)
                );

            }else {
                paymentRepository.insert(detailedResponse);
                pollIntervalByPaymentRequest.remove(request);
            }

        };

        scheduledExecutorService.schedule(pollAction, currentPollPeriod, TimeUnit.SECONDS);

    }


    private void insertUnknownPaymentResponse(String reference){
        paymentRepository.insert(
                new DetailedPaymentResponse(
                        reference,
                         -1,
                        "there was a problem whilst making the request"
                )
        );

    }

    @PreDestroy
    private void flushPendingRequests(){
        pollIntervalByPaymentRequest
            .keySet()
            .stream()
            .map(PaymentRequest::reference)
            .forEach(this::insertUnknownPaymentResponse);
    }


}
