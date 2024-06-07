package com.example.ioc.container;

import com.example.ioc.annotations.Scope;

import java.util.function.Supplier;

public class BeanDefinition {
    private final Class<?> beanClass;
    private final Supplier<?> beanSupplier;
    private final Scope scope;

    public BeanDefinition(Class<?> beanClass, Scope scope, Supplier<?> beanSupplier) {
        this.beanClass = beanClass;
        this.beanSupplier = beanSupplier;
        this.scope = scope;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public Scope getScope() {
        return scope;
    }

    public Supplier<?> getBeanSupplier() {
        return beanSupplier;
    }
}
