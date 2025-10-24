package com.dao;

import com.entities.Staff;
import com.interfaces.IStaff;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

/**
 * @author Tô Thanh Hậu
 */

public class StaffDAO implements IStaff {
    private final SessionFactory sessionFactory;

    public StaffDAO() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @Override
    public boolean addStaff(Staff s) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(s);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Staff> getAllStaffs() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM Staff", Staff.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
