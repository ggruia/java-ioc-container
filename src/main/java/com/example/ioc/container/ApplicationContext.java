package com.example.ioc.container;

public class ApplicationContext {
    private static final ApplicationContext INSTANCE = new ApplicationContext();

    private static final BeanFactory beanFactory = new BeanFactory();

    private ApplicationContext() {}

    public static ApplicationContext getInstance() {
        return INSTANCE;
    }

    static {
        beanFactory.scanConfigurations("com.example.ioc");
        beanFactory.scanComponents("com.example.ioc");
        try {
            beanFactory.initialize();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialize ApplicationContext: " + e.getMessage());
        }
    }

    public Object getBean(String beanName) {
        return beanFactory.getBean(beanName);
    }

    public <T> T getBean(Class<T> beanType) {
        return beanFactory.getBean(beanType);
    }

    public static void close() {
        beanFactory.destroyBeans();
    }
}
