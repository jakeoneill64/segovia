package com.segovia.meta;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ContextConfiguration {

    @Bean
    RestTemplate getRestTemplate(){
        return new RestTemplate();
    }



}
