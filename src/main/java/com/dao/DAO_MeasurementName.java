package com.dao;

import com.entities.MeasurementName;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;


public class DAO_MeasurementName {
    private final SessionFactory sessionFactory;

    public DAO_MeasurementName() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    public MeasurementName findMeasurementNameByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                            "FROM MeasurementName m WHERE lower(m.name) = :n",
                            MeasurementName.class)
                    .setParameter("n", name.trim().toLowerCase())
                    .uniqueResult();

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            if (session != null) session.close();
        }
    }



}
