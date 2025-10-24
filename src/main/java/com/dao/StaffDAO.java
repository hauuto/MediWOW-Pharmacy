package com.dao;

import com.entities.Staff;
import com.interfaces.IStaff;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

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

    @Override
    public boolean existsByUsername(String username) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Long> query = session.createQuery(
                "SELECT COUNT(s) FROM Staff s WHERE s.username = :username",
                Long.class
            );
            query.setParameter("username", username);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Long> query = session.createQuery(
                "SELECT COUNT(s) FROM Staff s WHERE s.email = :email",
                Long.class
            );
            query.setParameter("email", email);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Long> query = session.createQuery(
                "SELECT COUNT(s) FROM Staff s WHERE s.phoneNumber = :phoneNumber",
                Long.class
            );
            query.setParameter("phoneNumber", phoneNumber);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.trim().isEmpty()) {
            return false;
        }
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Long> query = session.createQuery(
                "SELECT COUNT(s) FROM Staff s WHERE s.licenseNumber = :licenseNumber",
                Long.class
            );
            query.setParameter("licenseNumber", licenseNumber);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
