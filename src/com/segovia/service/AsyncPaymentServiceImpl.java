package com.segovia.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segovia.model.ApiCredentials;
import com.segovia.model.BasicPaymentResponse;
import com.segovia.model.PaymentRequest;
import com.segovia.model.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;

@Service
public class AsyncPaymentServiceImpl implements AsyncPaymentService{

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;


    @Value("${payment-provider.session-timeout}")
    private long providerSessionTtl;
    @Value("${payment-provider.uri}")
    private String providerUri;

    private AsyncPaymentServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper){
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void process(PaymentRequest request, ApiCredentials credentials){

        //check our auth status
        Session currentSession = Optional.ofNullable(sessionByApiCredentials.get(credentials))
                .filter(session -> System.currentTimeMillis() > session.validUntil())
                .orElseGet(() -> {


                    Session session = authenticate(credentials);
                    sessionByApiCredentials.put(credentials, session);
                    return session;

                }
                );



        submitPayment(request, currentSession);


    }


    private Session authenticate(ApiCredentials credentials){

        return new Session(
                        restTemplate
                            .postForEntity(providerUri, credentials, String.class)
                            .getBody(),
                        System.currentTimeMillis() + providerSessionTtl
        );

    }



    private BasicPaymentResponse submitPayment(PaymentRequest paymentRequest, Session session){
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer ".concat(session.token()));
        HttpEntity<PaymentRequest> httpEntity = new HttpEntity<>(paymentRequest, headers);
        return restTemplate.exchange(providerUri, HttpMethod.POST, httpEntity, BasicPaymentResponse.class).getBody();
    }


}
