package com.segovia.service;

import com.segovia.model.ApiCredentials;
import com.segovia.model.PaymentRequest;
import com.segovia.model.PaymentResponse;
import com.segovia.model.PaymentProviderSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncPaymentServiceImpl implements AsyncPaymentService{

    private final RestTemplate restTemplate;
    private final Map<ApiCredentials, PaymentProviderSession> sessionByApiCredentials = new HashMap<>();

    @Value("${payment-provider.session-timeout}")
    private long providerSessionTtl;
    @Value("${payment-provider.uri}")
    private String providerUri;

    private AsyncPaymentServiceImpl(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    public CompletableFuture<PaymentResponse> process(PaymentRequest request, ApiCredentials credentials){

        //check our auth status
        PaymentProviderSession currentSession = Optional.ofNullable(sessionByApiCredentials.get(credentials))
                .filter(session -> System.currentTimeMillis() - session.timeIssuedEpochMillis() < providerSessionTtl)
                .orElseGet(() -> {


                    PaymentProviderSession session = authenticate(credentials);
                    sessionByApiCredentials.put(credentials, session);
                    return session;

                }
                );

        //add remove to sessionCache.


        postPayment(request, currentSession);
        return null;


    }

//    POST /auth
//    Api-Key: XXXXXX
//
//    {"account": "my-account-id"}

    //{"token": "my-session-token"}

    private PaymentProviderSession authenticate(ApiCredentials credentials){

        return new PaymentProviderSession(
                        restTemplate
                            .postForEntity(providerUri, credentials, String.class)
                            .getBody(),
                        System.currentTimeMillis()
        );

    }




    private PaymentResponse postPayment(PaymentRequest paymentRequest, PaymentProviderSession session){
        return restTemplate
            .postForEntity("url", paymentRequest, PaymentResponse.class)
            .getBody();
    }


}
