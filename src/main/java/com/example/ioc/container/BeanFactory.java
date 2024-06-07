package com.example.ioc.container;

import com.example.ioc.annotations.*;
import com.example.ioc.dependency.DependencyNode;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public class BeanFactory {
    private final Map<String, Object> singletonBeans = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<String, Object>> scopedBeans = ThreadLocal.withInitial(HashMap::new);
    private final Map<String, DependencyNode> dependencyGraph = new HashMap<>();

    public void initialize() {
        List<DependencyNode> sortedNodes = topologicalSort(dependencyGraph.values());

        for (DependencyNode node : sortedNodes) {
            getBean(node.getBeanName());
        }
    }

    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        DependencyNode node = new DependencyNode(beanName, beanDefinition);
        dependencyGraph.put(beanName, node);

        // analyze dependencies and update the graph
        for (Field field : beanDefinition.getBeanClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                String dependencyName = field.getType().getSimpleName();
                DependencyNode dependencyNode = dependencyGraph.get(dependencyName);
                if (dependencyNode == null) {
                    throw new RuntimeException("Unresolved dependency: " + dependencyName);
                }
                node.addDependency(dependencyNode);
                dependencyNode.addDependent(node);
            }
        }
    }

    public void registerSingleton(String beanName, Class<?> beanClass) {
        BeanDefinition beanDefinition = new BeanDefinition(beanClass, Scope.SINGLETON, () -> createBeanInstance(beanClass));
        registerBeanDefinition(beanName, beanDefinition);
    }

    public void registerTransient(String beanName, Class<?> beanClass) {
        BeanDefinition beanDefinition = new BeanDefinition(beanClass, Scope.TRANSIENT, () -> createBeanInstance(beanClass));
        registerBeanDefinition(beanName, beanDefinition);
    }

    public void registerScoped(String beanName, Class<?> beanClass) {
        BeanDefinition beanDefinition = new BeanDefinition(beanClass, Scope.SCOPED, () -> createBeanInstance(beanClass));
        registerBeanDefinition(beanName, beanDefinition);
    }

    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = dependencyGraph.get(beanName).getBeanDefinition();
        if (beanDefinition == null) {
            throw new RuntimeException("No bean with name " + beanName + " found");
        }

        return getBean(beanDefinition);
    }

    public <T> T getBean(Class<T> beanType) {
        Optional<String> beanName = dependencyGraph.entrySet().stream()
                .filter(entry -> beanType.isAssignableFrom(entry.getValue().getBeanDefinition().getBeanClass()))
                .map(Map.Entry::getKey)
                .findFirst();

        if (beanName.isPresent()) {
            return beanType.cast(getBean(beanName.get()));
        } else {
            throw new RuntimeException("No bean of type " + beanType.getName() + " found");
        }
    }

    private Object getBean(BeanDefinition beanDefinition) {
        return switch (beanDefinition.getScope()) {
            case SINGLETON -> singletonBeans.computeIfAbsent(beanDefinition.getBeanClass().getSimpleName(),
                    k -> beanDefinition.getBeanSupplier().get());
            case TRANSIENT -> beanDefinition.getBeanSupplier().get();
            case SCOPED -> {
                Map<String, Object> scopedMap = scopedBeans.get();
                yield scopedMap.computeIfAbsent(beanDefinition.getBeanClass().getSimpleName(),
                        k -> beanDefinition.getBeanSupplier().get());
            }
        };
    }

    // instantiate bean and inject dependencies
    private Object createBeanInstance(Class<?> beanClass) {
        try {
            Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.isAnnotationPresent(Autowired.class) || constructor.getParameterCount() != 0) {
                    // constructor injection
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    Object[] parameters = new Object[parameterTypes.length];
                    for (int i = 0; i < parameterTypes.length; i++) {
                        String dependentBeanName = parameterTypes[i].getSimpleName();
                        parameters[i] = getBean(dependentBeanName);
                    }
                    constructor.setAccessible(true);
                    return constructor.newInstance(parameters);
                }
            }

            Object beanInstance = beanClass.getDeclaredConstructor().newInstance();

            // field injection
            for (Field field : beanClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Class<?> fieldType = field.getType();
                    String dependentBeanName = fieldType.getSimpleName();
                    Object dependentBean = getBean(dependentBeanName);
                    field.setAccessible(true);
                    field.set(beanInstance, dependentBean);
                }
            }

            for (Method method : beanClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    try {
                        method.setAccessible(true);
                        method.invoke(beanInstance);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to execute @PostConstruct method for class: " + beanClass.getName(), e);
                    }
                }
            }

            return beanInstance;
        } catch (InstantiationException e) {
            throw new RuntimeException("Failed to instantiate bean of class: " + beanClass.getName(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access during bean instantiation of class: " + beanClass.getName(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Constructor threw an exception for class: " + beanClass.getName(), e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No suitable constructor found for class: " + beanClass.getName(), e);
        }
    }

    public void destroyBeans() {
        List<DependencyNode> sortedNodes = topologicalSort(dependencyGraph.values());
        Collections.reverse(sortedNodes);

        // destroy beans in reverse sorted order
        for (DependencyNode node : sortedNodes) {
            Object beanInstance = getBean(node.getBeanName());
            // @PreDestroy
            for (Method method : beanInstance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    try {
                        method.setAccessible(true);
                        method.invoke(beanInstance);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to execute @PreDestroy method for class: " + beanInstance.getClass().getName(), e);
                    }
                }
            }

            BeanDefinition beanDefinition = node.getBeanDefinition();
            if (beanDefinition.getScope() == Scope.SINGLETON) {
                singletonBeans.remove(beanDefinition.getBeanClass().getSimpleName());
            } else if (beanDefinition.getScope() == Scope.SCOPED) {
                scopedBeans.get().remove(beanDefinition.getBeanClass().getSimpleName());
            }
        }
    }

    public void scanConfigClass(Class<?> configClass) {
        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                // register a bean definition
                String beanName = method.getName(); // use method name as bean name
                BeanDefinition beanDefinition = new BeanDefinition(method.getReturnType(), Scope.SINGLETON, () -> {
                    try {
                        return method.invoke(configClass.getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create bean from configuration class", e);
                    }
                });
                registerBeanDefinition(beanName, beanDefinition);
            }
        }
    }

    public void processConfigClass(Class<?> configClass) {
        Object configInstance;
        try {
            configInstance = configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate configuration class: " + configClass.getName(), e);
        }

        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                String beanName = method.getName();
                BeanDefinition beanDefinition = new BeanDefinition(method.getReturnType(), Scope.SINGLETON, () -> {
                    try {
                        return method.invoke(configInstance);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create bean from configuration class", e);
                    }
                });
                registerBeanDefinition(beanName, beanDefinition);
            }
        }
    }

    // scan for components and register them as beans
    public void scanComponents(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> components = reflections.getTypesAnnotatedWith(Component.class);
        for (Class<?> component : components) {
            Component componentAnnotation = component.getAnnotation(Component.class);
            Scope scope = componentAnnotation.value();
            String beanName = component.getSimpleName();

            switch (scope) {
                case SINGLETON -> registerSingleton(beanName, component);
                case TRANSIENT -> registerTransient(beanName, component);
                case SCOPED -> registerScoped(beanName, component);
            }
        }
    }

    public void scanConfigurations(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> configClasses = reflections.getTypesAnnotatedWith(Configuration.class);

        for (Class<?> configClass : configClasses) {
            processConfigClass(configClass);
        }
    }

    private List<DependencyNode> topologicalSort(Collection<DependencyNode> nodes) {
        List<DependencyNode> sorted = new ArrayList<>();
        Map<DependencyNode, Integer> inDegree = new HashMap<>();

        // initialize in-degree count for each node
        for (DependencyNode node : nodes) {
            inDegree.put(node, node.getDependencies().size());
            for (DependencyNode dependent : node.getDependents()) {
                inDegree.putIfAbsent(dependent, 0);
            }
        }

        // nodes with no incoming edges
        Queue<DependencyNode> queue = new LinkedList<>();
        for (Map.Entry<DependencyNode, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        // process nodes with no incoming edges
        while (!queue.isEmpty()) {
            DependencyNode node = queue.poll();
            sorted.add(node);

            // decrease the in-degree of each dependent
            for (DependencyNode dependent : node.getDependents()) {
                int count = inDegree.get(dependent) - 1;
                inDegree.put(dependent, count);

                // if in-degree becomes zero, add to queue
                if (count == 0) {
                    queue.add(dependent);
                }
            }
        }

        // check graph for cycles
        if (sorted.size() != inDegree.size()) {
            throw new RuntimeException("Cycle detected in the dependency graph, cannot perform topological sort");
        }

        return sorted;
    }
}
