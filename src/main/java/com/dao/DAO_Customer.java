package com.dao;

import com.entities.Customer;
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
    public boolean addCustomer(Customer customer) {
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
    public boolean updateCustomer(Customer customer) {
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
            Customer customer = session.get(Customer.class, id);
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
    public List<Customer> getAllCustomers() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Customer> query = session.createQuery(
                "FROM Customer ORDER BY creationDate DESC",
                Customer.class
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
    public Customer getCustomerById(String id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.get(Customer.class, id);
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
    public Customer getCustomerByPhoneNumber(String phoneNumber) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Customer> query = session.createQuery(
                "FROM Customer WHERE phoneNumber = :phoneNumber",
                Customer.class
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
    public List<Customer> searchCustomersByName(String name) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Customer> query = session.createQuery(
                "FROM Customer WHERE LOWER(name) LIKE :name ORDER BY name",
                Customer.class
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
                "SELECT COUNT(*) FROM Customer WHERE phoneNumber = :phoneNumber",
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
                "SELECT COUNT(*) FROM Customer WHERE phoneNumber = :phoneNumber AND id != :excludeId",
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

