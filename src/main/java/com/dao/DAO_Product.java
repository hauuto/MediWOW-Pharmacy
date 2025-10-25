package com.dao;

import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.interfaces.IProduct;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

/**
 * @author Nguyễn Thanh Khôi
 */

public class DAO_Product implements IProduct {
    private final SessionFactory sessionFactory;

    public DAO_Product() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @Override
    public Product getProductById(String id) {
        return null;
    }

    @Override
    public Lot getLotByBatchNumber(String batchNumber) {
        return null;
    }

    @Override
    public List<Lot> getAllLots() {
        return List.of();
    }

    @Override
    public UnitOfMeasure getUnitOfMeasureById(String id) {
        return null;
    }

    @Override
    public List<UnitOfMeasure> getAllUnitOfMeasures() {
        return List.of();
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
