package com.example.ioc;

import com.example.ioc.annotations.Bean;
import com.example.ioc.annotations.Configuration;
import com.example.ioc.service.User;

@Configuration
public class Config {

    @Bean
    public User user() {
        return new User();
    }
}
