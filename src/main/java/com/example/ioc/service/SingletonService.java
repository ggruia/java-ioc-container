package com.example.ioc.service;

import com.example.ioc.annotations.Autowired;
import com.example.ioc.annotations.Component;

@Component
public class SingletonService {

    @Autowired
    private Repository repository;

    public void doSomething() {
        System.out.println("\n" + this);
        repository.sayHello();
    }
}
