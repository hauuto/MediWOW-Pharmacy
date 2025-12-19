// File: /MediWOW-Pharmacy/src/main/java/com/dao/DAO_Product.java
package com.dao;

import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.interfaces.IProduct;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;   // ⇦ NEW
import org.hibernate.Hibernate;
import java.util.List;

public class DAO_Product implements IProduct {
    private final SessionFactory sessionFactory;

    public DAO_Product() { this.sessionFactory = HibernateUtil.getSessionFactory(); }

    // <-- add

    @Override
    public Product getProductById(String id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            // 1) FETCH UOM
            Product product = session.createQuery(
                            "SELECT DISTINCT p FROM Product p " +
                                    "LEFT JOIN FETCH p.unitOfMeasureList " +
                                    "WHERE p.id = :id", Product.class)
                    .setParameter("id", id)
                    .uniqueResult();

            if (product != null) {
                // 2) FETCH LOT (trong cùng persistence context)
                session.createQuery(
                                "SELECT DISTINCT p FROM Product p " +
                                        "LEFT JOIN FETCH p.lotList " +
                                        "WHERE p.id = :id", Product.class)
                        .setParameter("id", id)
                        .uniqueResult();

                // 3) Defensive: đảm bảo cả 2 collection đã init trước khi đóng session
                Hibernate.initialize(product.getUnitOfMeasureList());
                Hibernate.initialize(product.getLotList());
            }
            return product;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    @Override
    public Product getProductByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM Product p WHERE p.barcode = :barcode", Product.class)
                    .setParameter("barcode", barcode.trim())
                    .uniqueResult();
        } catch (Exception e) { e.printStackTrace(); return null; }
        finally { if (session != null && session.isOpen()) session.close(); }
    }

    @Override
    public Lot getLotByBatchNumber(String batchNumber) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM Lot l WHERE l.batchNumber = :batchNumber", Lot.class)
                    .setParameter("batchNumber", batchNumber).uniqueResult();
        } catch (Exception e) { e.printStackTrace(); return null; }
        finally { if (session != null && session.isOpen()) session.close(); }
    }

    @Override
    public List<Lot> getAllLots() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM Lot", Lot.class).list();
        } catch (Exception e) { e.printStackTrace(); return null; }
        finally { if (session != null && session.isOpen()) session.close(); }
    }

    @Override
    public UnitOfMeasure getUnitOfMeasureById(String productId, String name) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM UnitOfMeasure u WHERE u.product.id = :productId AND u.name = :name", UnitOfMeasure.class)
                    .setParameter("productId", productId)
                    .setParameter("name", name)
                    .uniqueResult();
        } catch (Exception e) { e.printStackTrace(); return null; }
        finally { if (session != null && session.isOpen()) session.close(); }
    }

    @Override
    public Lot getLotById(String id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM Lot l WHERE l.id = :id", Lot.class)
                    .setParameter("id", id).uniqueResult();
        } catch (Exception e) { e.printStackTrace(); return null; }
        finally { if (session != null && session.isOpen()) session.close(); }
    }

    @Override
    public List<UnitOfMeasure> getAllUnitOfMeasures() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM UnitOfMeasure", UnitOfMeasure.class).list();
        } catch (Exception e) { e.printStackTrace(); return null; }
        finally { if (session != null && session.isOpen()) session.close(); }
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
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    @Override
    public List<Product> getAllProducts() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            List<Product> products = session.createQuery(
                    "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.unitOfMeasureList",
                    Product.class
            ).list();

            if (!products.isEmpty()) {
                session.createQuery(
                        "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.lotList WHERE p IN :products",
                        Product.class
                ).setParameter("products", products).list();
            }
            return products;
        } catch (Exception e) { e.printStackTrace(); return null; }
        finally { if (session != null && session.isOpen()) session.close(); }
    }

    // ============ existsBy* (duplicate checks) ============
    @Override
    public boolean existsByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return false;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Long cnt = session.createQuery("SELECT COUNT(p) FROM Product p WHERE p.barcode = :b", Long.class)
                    .setParameter("b", barcode.trim()).getSingleResult();
            return cnt > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
        finally { if (session != null && session.isOpen()) session.close(); }
    }

    @Override
    public boolean existsByNameAndManufacturer(String name, String manufacturer) {
        if (name == null || name.trim().isEmpty() || manufacturer == null || manufacturer.trim().isEmpty()) return false;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Long cnt = session.createQuery(
                            "SELECT COUNT(p) FROM Product p " +
                                    "WHERE lower(p.name) = :n AND lower(p.manufacturer) = :m", Long.class)
                    .setParameter("n", name.trim().toLowerCase())
                    .setParameter("m", manufacturer.trim().toLowerCase())
                    .getSingleResult();
            return cnt > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
        finally { if (session != null && session.isOpen()) session.close(); }
    }

    @Override
    public boolean existsLotByBatchNumber(String batchNumber) {
        if (batchNumber == null || batchNumber.trim().isEmpty()) return false;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Long cnt = session.createQuery("SELECT COUNT(l) FROM Lot l WHERE l.batchNumber = :bn", Long.class)
                    .setParameter("bn", batchNumber.trim()).getSingleResult();
            return cnt > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
        finally { if (session != null && session.isOpen()) session.close(); }
    }

    /**
     * Update the quantity of a specific lot and change status if quantity becomes 0
     * @param lotId The ID of the lot
     * @param quantityToDeduct The quantity to deduct from the lot
     * @return true if successful, false otherwise
     */
    public boolean deductLotQuantity(String lotId, int quantityToDeduct) {
        if (lotId == null || lotId.trim().isEmpty() || quantityToDeduct <= 0) return false;
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            Lot lot = session.get(Lot.class, lotId);
            if (lot != null) {
                int newQuantity = lot.getQuantity() - quantityToDeduct;
                if (newQuantity < 0) {
                    transaction.rollback();
                    return false; // Cannot deduct more than available
                }

                lot.setQuantity(newQuantity);

                // Update status based on quantity
//                if (newQuantity == 0) {
//                    lot.setStatus(com.enums.LotStatus.OUT_OF_STOCK);
//                }

                session.merge(lot);
                transaction.commit();
                return true;
            }

            transaction.rollback();
            return false;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    /**
     * Update the quantity of a specific lot to a new value
     * @param lotId The ID of the lot
     * @param newQuantity The new quantity value
     * @return true if successful, false otherwise
     */
    public boolean updateLotQuantity(String lotId, int newQuantity) {
        if (lotId == null || lotId.trim().isEmpty() || newQuantity < 0) return false;
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            Lot lot = session.get(Lot.class, lotId);
            if (lot != null) {
                lot.setQuantity(newQuantity);

                // Update status based on quantity
//                if (newQuantity == 0) {
//                    lot.setStatus(com.enums.LotStatus.OUT_OF_STOCK);
//                } else if (lot.getStatus() == com.enums.LotStatus.OUT_OF_STOCK) {
//                    lot.setStatus(com.enums.LotStatus.AVAILABLE);
//                }

                session.merge(lot);
                transaction.commit();
                return true;
            }

            transaction.rollback();
            return false;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }
}
