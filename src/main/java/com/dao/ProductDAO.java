package com.dao;

import com.entities.Product;
import com.entities.Staff;
import com.interfaces.IProduct;
import com.interfaces.IStaff;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

/**
 * @author Nguyễn Thanh Khôi
 */

public class ProductDAO implements IProduct {
    private final SessionFactory sessionFactory;

    public ProductDAO() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @Override
    public boolean addProduct(Product s) {
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
    public List<Product> getAllProducts() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM Product ", Product.class).list();
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
