package com.example.ioc;

import com.example.ioc.container.ApplicationContext;
import com.example.ioc.service.ScopedService;
import com.example.ioc.service.SingletonService;
import com.example.ioc.service.TransientService;
import com.example.ioc.service.User;

public class Application {
    public static void main(String[] args) {
        try {
            Thread t1 = new Thread(Application::test);
            Thread t2 = new Thread(Application::test);
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            ApplicationContext.close();
        }
    }

    public static void test() {
        ApplicationContext applicationContext = ApplicationContext.getInstance();

        SingletonService singletonService1 = (SingletonService) applicationContext.getBean("SingletonService");
        SingletonService singletonService2 = applicationContext.getBean(SingletonService.class);

        ScopedService scopedService1 = (ScopedService) applicationContext.getBean("ScopedService");
        ScopedService scopedService2 = applicationContext.getBean(ScopedService.class);

        TransientService transientService1 = (TransientService) applicationContext.getBean("TransientService");
        TransientService transientService2 = applicationContext.getBean(TransientService.class);

        singletonService1.doSomething();
        singletonService2.doSomething();

        scopedService1.doSomething();
        scopedService2.doSomething();

        transientService1.doSomething();
        transientService2.doSomething();

        User user1 = (User) applicationContext.getBean("user");
        User user2 = applicationContext.getBean(User.class);

        user1.sayHi();
        user2.sayHi();
    }
}
