package com.example.ioc.service;

import com.example.ioc.annotations.Component;

@Component
public class Repository {

    public void sayHello() {
        System.out.println("Hello World");
    }
}
