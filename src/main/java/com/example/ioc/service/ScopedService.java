package com.example.ioc.service;

import com.example.ioc.annotations.Autowired;
import com.example.ioc.annotations.Component;
import com.example.ioc.annotations.Scope;

@Component(Scope.SCOPED)
public class ScopedService {

    @Autowired
    private Repository repository;

    public void doSomething() {
        System.out.println("\n" + this);
        repository.sayHello();
    }
}
