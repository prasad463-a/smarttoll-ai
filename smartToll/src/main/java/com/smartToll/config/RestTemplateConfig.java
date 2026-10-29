package com.smartToll.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(25000); // 25s connect timeout for cloud wakeups
        factory.setReadTimeout(75000);    // 75s read timeout to safely allow AI cold-start inference
        return new RestTemplate(factory);
    }
}
