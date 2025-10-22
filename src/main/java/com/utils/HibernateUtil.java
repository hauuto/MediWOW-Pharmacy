package com.utils;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Hibernate Utility class with a convenient method to get Session Factory object.
 * @author Tô Thanh Hậu
 */
public class HibernateUtil {
    private static SessionFactory sessionFactory;

    static {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");

            sessionFactory = configuration.buildSessionFactory();

            System.out.println("✅ Hibernate SessionFactory created successfully!");

            // Add shutdown hook to close SessionFactory when application exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                shutdown();
            }));

        } catch (Throwable ex) {
            System.err.println("❌ Initial SessionFactory creation failed: " + ex);
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            throw new IllegalStateException("SessionFactory is not initialized or has been closed");
        }
        return sessionFactory;
    }

    public static void shutdown() {
        // Close caches and connection pools
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            System.out.println("✅ Hibernate SessionFactory closed.");
        }
    }

    public static boolean isSessionFactoryInitialized() {
        return sessionFactory != null && !sessionFactory.isClosed();
    }
}

