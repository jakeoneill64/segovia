package com.segovia.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class ContextConfiguration {

    @Bean
    RestTemplate getRestTemplate(){
        return new RestTemplate();
    }

    @Bean
    ObjectMapper getObjectMapper(){return new ObjectMapper();}

    @Bean
    ScheduledExecutorService getScheduledExecutorService(){return new ScheduledThreadPoolExecutor(4);}



}
