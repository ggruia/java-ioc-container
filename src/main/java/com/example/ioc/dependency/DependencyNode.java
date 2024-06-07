package com.example.ioc.dependency;

import com.example.ioc.container.BeanDefinition;

import java.util.HashSet;
import java.util.Set;

public class DependencyNode {
    private final String beanName;
    private final BeanDefinition beanDefinition;
    private final Set<DependencyNode> dependencies = new HashSet<>();
    private final Set<DependencyNode> dependents = new HashSet<>();

    public DependencyNode(String beanName, BeanDefinition beanDefinition) {
        this.beanName = beanName;
        this.beanDefinition = beanDefinition;
    }

    public String getBeanName() {
        return beanName;
    }

    public BeanDefinition getBeanDefinition() {
        return beanDefinition;
    }

    public Set<DependencyNode> getDependencies() {
        return dependencies;
    }

    public Set<DependencyNode> getDependents() {
        return dependents;
    }

    public void addDependency(DependencyNode dependency) {
        dependencies.add(dependency);
    }

    public void addDependent(DependencyNode dependent) {
        dependents.add(dependent);
    }
}
