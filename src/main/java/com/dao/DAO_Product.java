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
        Session session = null;
        try {
            session = sessionFactory.openSession();
            // Fetch product with unitOfMeasureList first
            Product product = session.createQuery(
                "SELECT DISTINCT p FROM Product p " +
                "LEFT JOIN FETCH p.unitOfMeasureList " +
                "WHERE p.id = :id",
                Product.class
            ).setParameter("id", id).uniqueResult();

            // Then fetch lotList for the same product
            if (product != null) {
                session.createQuery(
                    "SELECT DISTINCT p FROM Product p " +
                    "LEFT JOIN FETCH p.lotList " +
                    "WHERE p.id = :id",
                    Product.class
                ).setParameter("id", id).uniqueResult();
            }

            return product;
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
    public Lot getLotByBatchNumber(String batchNumber) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                "FROM Lot l WHERE l.batchNumber = :batchNumber",
                Lot.class
            ).setParameter("batchNumber", batchNumber).uniqueResult();
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
    public List<Lot> getAllLots() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM Lot", Lot.class).list();
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
    public UnitOfMeasure getUnitOfMeasureById(String id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                "FROM UnitOfMeasure u WHERE u.id = :id",
                UnitOfMeasure.class
            ).setParameter("id", id).uniqueResult();
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
    public List<UnitOfMeasure> getAllUnitOfMeasures() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM UnitOfMeasure", UnitOfMeasure.class).list();
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
            // Fetch products with unitOfMeasureList first
            List<Product> products = session.createQuery(
                "SELECT DISTINCT p FROM Product p " +
                "LEFT JOIN FETCH p.unitOfMeasureList",
                Product.class
            ).list();

            // Then fetch lotList for the same products
            if (!products.isEmpty()) {
                session.createQuery(
                    "SELECT DISTINCT p FROM Product p " +
                    "LEFT JOIN FETCH p.lotList " +
                    "WHERE p IN :products",
                    Product.class
                ).setParameter("products", products).list();
            }

            return products;
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
