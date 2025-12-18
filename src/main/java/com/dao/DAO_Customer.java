package com.dao;

import com.entities.PrescribedCustomer;
import com.interfaces.ICustomer;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

/**
 * @author Tô Thanh Hậu
 */
public class DAO_Customer implements ICustomer {
    private final SessionFactory sessionFactory;

    public DAO_Customer() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @Override
    public boolean addCustomer(PrescribedCustomer customer) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(customer);
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
    public boolean updateCustomer(PrescribedCustomer customer) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.merge(customer);
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
    public boolean deleteCustomer(String id) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            PrescribedCustomer customer = session.get(PrescribedCustomer.class, id);
            if (customer != null) {
                session.remove(customer);
                transaction.commit();
                return true;
            }
            return false;
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
    public List<PrescribedCustomer> getAllCustomers() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<PrescribedCustomer> query = session.createQuery(
                "FROM PrescribedCustomer ORDER BY creationDate DESC",
                PrescribedCustomer.class
            );
            return query.list();
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
    public PrescribedCustomer getCustomerById(String id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.get(PrescribedCustomer.class, id);
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
    public PrescribedCustomer getCustomerByPhoneNumber(String phoneNumber) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<PrescribedCustomer> query = session.createQuery(
                "FROM PrescribedCustomer WHERE phoneNumber = :phoneNumber",
                PrescribedCustomer.class
            );
            query.setParameter("phoneNumber", phoneNumber);
            return query.uniqueResult();
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
    public List<PrescribedCustomer> searchCustomersByName(String name) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<PrescribedCustomer> query = session.createQuery(
                "FROM PrescribedCustomer WHERE LOWER(name) LIKE :name ORDER BY name",
                PrescribedCustomer.class
            );
            query.setParameter("name", "%" + name.toLowerCase() + "%");
            return query.list();
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
    public boolean existsByPhoneNumber(String phoneNumber) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM PrescribedCustomer WHERE phoneNumber = :phoneNumber",
                Long.class
            );
            query.setParameter("phoneNumber", phoneNumber);
            Long count = query.uniqueResult();
            return count != null && count > 0;
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
    public boolean existsByPhoneNumberExcludingId(String phoneNumber, String excludeId) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM PrescribedCustomer WHERE phoneNumber = :phoneNumber AND id != :excludeId",
                Long.class
            );
            query.setParameter("phoneNumber", phoneNumber);
            query.setParameter("excludeId", excludeId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
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

