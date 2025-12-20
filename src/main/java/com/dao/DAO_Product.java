package com.dao;

import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.interfaces.IProduct;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.Hibernate;

import java.util.List;

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

            Product product = session.createQuery(
                            "SELECT DISTINCT p FROM Product p " +
                                    "LEFT JOIN FETCH p.unitOfMeasureList u " +
                                    "LEFT JOIN FETCH u.measurement m " +
                                    "LEFT JOIN FETCH p.lotList l " +
                                    "WHERE p.id = :id",
                            Product.class)
                    .setParameter("id", id)
                    .uniqueResult();

            return product;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) session.close();
        }
    }


    @Override
    public Product getProductByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                            "FROM Product p WHERE p.barcode = :barcode", Product.class)
                    .setParameter("barcode", barcode.trim())
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public Lot getLotByBatchNumber(String batchNumber) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                            "FROM Lot l WHERE l.batchNumber = :batchNumber", Lot.class)
                    .setParameter("batchNumber", batchNumber)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) session.close();
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
            if (session != null) session.close();
        }
    }

    @Override
    public UnitOfMeasure getUnitOfMeasureById(String productId, String name) {
        if (productId == null || name == null || productId.trim().isEmpty() || name.trim().isEmpty())
            return null;

        Session session = null;
        try {
            session = sessionFactory.openSession();

            // 1) Find MeasurementName by text
            Integer measurementId = session.createQuery(
                            "SELECT m.id FROM MeasurementName m WHERE lower(m.name) = :n",
                            Integer.class)
                    .setParameter("n", name.trim().toLowerCase())
                    .uniqueResult();

            if (measurementId == null) {
                System.err.println("❌ No measurement found for name = " + name);
                return null;
            }

            // 2) Fetch UOM using new schema
            return session.createQuery(
                            "SELECT u FROM UnitOfMeasure u " +
                                    "LEFT JOIN FETCH u.measurement " +
                                    "WHERE u.product.id = :pid AND u.measurement.id = :mid",
                            UnitOfMeasure.class)
                    .setParameter("pid", productId)
                    .setParameter("mid", measurementId)
                    .uniqueResult();


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) session.close();
        }
    }


    /**
     * Get UOM by product + measurementId (new schema)
     */
    @Override
    public UnitOfMeasure getUnitOfMeasureById(String productId, Integer measurementId) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                            "SELECT u FROM UnitOfMeasure u " +
                                    "WHERE u.product.id = :pid AND u.measurement.id = :mid",
                            UnitOfMeasure.class)
                    .setParameter("pid", productId)
                    .setParameter("mid", measurementId)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public Lot getLotById(String id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                            "FROM Lot l WHERE l.id = :id", Lot.class)
                    .setParameter("id", id)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public List<UnitOfMeasure> getAllUnitOfMeasures() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM UnitOfMeasure u", UnitOfMeasure.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public boolean addProduct(Product s) {
        Transaction tx = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            session.persist(s);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public List<Product> getAllProducts() {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            // Query 1: fetch UOM + measurement
            List<Product> products = session.createQuery(
                    "SELECT DISTINCT p FROM Product p " +
                            "LEFT JOIN FETCH p.unitOfMeasureList u " +
                            "LEFT JOIN FETCH u.measurement",
                    Product.class
            ).list();

            // Query 2: subselect load lotList for all products
            if (!products.isEmpty()) {
                session.createQuery(
                                "SELECT DISTINCT p FROM Product p " +
                                        "LEFT JOIN FETCH p.lotList " +
                                        "WHERE p IN :prods",
                                Product.class
                        )
                        .setParameter("prods", products)
                        .list();
            }

            return products;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) session.close();
        }
    }



    @Override
    public boolean existsByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return false;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Long cnt = session.createQuery(
                            "SELECT COUNT(p) FROM Product p WHERE p.barcode = :b", Long.class)
                    .setParameter("b", barcode.trim())
                    .getSingleResult();
            return cnt > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public boolean existsByNameAndManufacturer(String name, String manufacturer) {
        if (name == null || name.trim().isEmpty() ||
                manufacturer == null || manufacturer.trim().isEmpty())
            return false;

        Session session = null;
        try {
            session = sessionFactory.openSession();
            Long cnt = session.createQuery(
                            "SELECT COUNT(p) FROM Product p " +
                                    "WHERE lower(p.name) = :n AND lower(p.manufacturer) = :m",
                            Long.class)
                    .setParameter("n", name.trim().toLowerCase())
                    .setParameter("m", manufacturer.trim().toLowerCase())
                    .getSingleResult();
            return cnt > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (session != null) session.close();
        }
    }

    @Override
    public boolean existsLotByBatchNumber(String batchNumber) {
        if (batchNumber == null || batchNumber.trim().isEmpty()) return false;

        Session session = null;
        try {
            session = sessionFactory.openSession();
            Long cnt = session.createQuery(
                            "SELECT COUNT(l) FROM Lot l WHERE l.batchNumber = :bn", Long.class)
                    .setParameter("bn", batchNumber.trim())
                    .getSingleResult();
            return cnt > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (session != null) session.close();
        }
    }
}
