package com.dao;

import com.entities.Lot;
import com.entities.Product;
import com.enums.LotStatus;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access Object for Lot entity
 * @author Tô Thanh Hậu
 */
public class DAO_Lot {
    private final SessionFactory sessionFactory;

    public DAO_Lot() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    /**
     * Add a new lot to the database
     * @param lot the lot to add
     * @return true if successful, false otherwise
     */
    public boolean add(Lot lot) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(lot);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing lot
     * @param lot the lot to update
     * @return true if successful, false otherwise
     */
    public boolean update(Lot lot) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(lot);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a lot by batch number
     * @param batchNumber the batch number of the lot to delete
     * @return true if successful, false otherwise
     */
    public boolean delete(String batchNumber) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Lot lot = session.get(Lot.class, batchNumber);
            if (lot != null) {
                session.remove(lot);
                transaction.commit();
                return true;
            }
            transaction.rollback();
            return false;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find a lot by batch number
     * @param batchNumber the batch number to search for
     * @return the lot if found, null otherwise
     */
    public Lot findByBatchNumber(String batchNumber) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Lot.class, batchNumber);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all lots
     * @return list of all lots
     */
    public List<Lot> getAll() {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Lot";
            Query<Lot> query = session.createQuery(hql, Lot.class);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Get all lots for a specific product
     * @param product the product to get lots for
     * @return list of lots for the product
     */
    public List<Lot> getLotsByProduct(Product product) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Lot l WHERE l.product = :product";
            Query<Lot> query = session.createQuery(hql, Lot.class);
            query.setParameter("product", product);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Get all lots by status
     * @param status the status to filter by
     * @return list of lots with the specified status
     */
    public List<Lot> getLotsByStatus(LotStatus status) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Lot l WHERE l.status = :status";
            Query<Lot> query = session.createQuery(hql, Lot.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Get all available lots for a product (AVAILABLE status and quantity > 0)
     * @param product the product to get available lots for
     * @return list of available lots
     */
    public List<Lot> getAvailableLotsByProduct(Product product) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Lot l WHERE l.product = :product AND l.status = :status AND l.quantity > 0";
            Query<Lot> query = session.createQuery(hql, Lot.class);
            query.setParameter("product", product);
            query.setParameter("status", LotStatus.AVAILABLE);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Get lots expiring before a certain date
     * @param expiryDate the date to check against
     * @return list of lots expiring before the date
     */
    public List<Lot> getLotsExpiringBefore(LocalDateTime expiryDate) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Lot l WHERE l.expiryDate < :expiryDate AND l.status = :status";
            Query<Lot> query = session.createQuery(hql, Lot.class);
            query.setParameter("expiryDate", expiryDate);
            query.setParameter("status", LotStatus.AVAILABLE);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Get the oldest available lot for a product (FIFO - First In First Out based on expiry date)
     * @param product the product to get the oldest lot for
     * @return the oldest available lot or null if none available
     */
    public Lot getOldestAvailableLot(Product product) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Lot l WHERE l.product = :product AND l.status = :status AND l.quantity > 0 ORDER BY l.expiryDate ASC";
            Query<Lot> query = session.createQuery(hql, Lot.class);
            query.setParameter("product", product);
            query.setParameter("status", LotStatus.AVAILABLE);
            query.setMaxResults(1);
            List<Lot> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update lot quantity
     * @param batchNumber the batch number of the lot
     * @param newQuantity the new quantity
     * @return true if successful, false otherwise
     */
    public boolean updateQuantity(String batchNumber, int newQuantity) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Lot lot = session.get(Lot.class, batchNumber);
            if (lot != null) {
                lot.setQuantity(newQuantity);
                session.merge(lot);
                transaction.commit();
                return true;
            }
            transaction.rollback();
            return false;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update lot status
     * @param batchNumber the batch number of the lot
     * @param newStatus the new status
     * @return true if successful, false otherwise
     */
    public boolean updateStatus(String batchNumber, LotStatus newStatus) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Lot lot = session.get(Lot.class, batchNumber);
            if (lot != null) {
                lot.setStatus(newStatus);
                session.merge(lot);
                transaction.commit();
                return true;
            }
            transaction.rollback();
            return false;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        }
    }
}
