package com.dao;

import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Data Access Object for UnitOfMeasure entity
 * @author Tô Thanh Hậu
 */
public class DAO_UnitOfMeasure {
    private final SessionFactory sessionFactory;

    public DAO_UnitOfMeasure() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    /**
     * Add a new unit of measure to the database
     * @param uom the unit of measure to add
     * @return true if successful, false otherwise
     */
    public boolean add(UnitOfMeasure uom) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(uom);
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
     * Update an existing unit of measure
     * @param uom the unit of measure to update
     * @return true if successful, false otherwise
     */
    public boolean update(UnitOfMeasure uom) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(uom);
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
     * Delete a unit of measure by ID
     * @param id the ID of the unit of measure to delete
     * @return true if successful, false otherwise
     */
    public boolean delete(String id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            UnitOfMeasure uom = session.get(UnitOfMeasure.class, id);
            if (uom != null) {
                session.remove(uom);
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
     * Find a unit of measure by ID
     * @param id the ID to search for
     * @return the unit of measure if found, null otherwise
     */
    public UnitOfMeasure findById(String id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(UnitOfMeasure.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all units of measure
     * @return list of all units of measure
     */
    public List<UnitOfMeasure> getAll() {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM UnitOfMeasure";
            Query<UnitOfMeasure> query = session.createQuery(hql, UnitOfMeasure.class);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Get all units of measure for a specific product
     * @param product the product to get units for
     * @return list of units of measure for the product
     */
    public List<UnitOfMeasure> getByProduct(Product product) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM UnitOfMeasure u WHERE u.product = :product";
            Query<UnitOfMeasure> query = session.createQuery(hql, UnitOfMeasure.class);
            query.setParameter("product", product);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Get units of measure by product ID
     * @param productId the product ID to search for
     * @return list of units of measure for the product
     */
    public List<UnitOfMeasure> getByProductId(String productId) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM UnitOfMeasure u WHERE u.product.id = :productId";
            Query<UnitOfMeasure> query = session.createQuery(hql, UnitOfMeasure.class);
            query.setParameter("productId", productId);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Find unit of measure by name for a specific product
     * @param product the product
     * @param name the unit name to search for
     * @return the unit of measure if found, null otherwise
     */
    public UnitOfMeasure findByProductAndName(Product product, String name) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM UnitOfMeasure u WHERE u.product = :product AND u.name = :name";
            Query<UnitOfMeasure> query = session.createQuery(hql, UnitOfMeasure.class);
            query.setParameter("product", product);
            query.setParameter("name", name);
            List<UnitOfMeasure> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the base unit of measure for a product (conversion rate = 1)
     * @param product the product
     * @return the base unit of measure or null if not found
     */
    public UnitOfMeasure getBaseUnit(Product product) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM UnitOfMeasure u WHERE u.product = :product AND u.baseUnitConversionRate = 1.0";
            Query<UnitOfMeasure> query = session.createQuery(hql, UnitOfMeasure.class);
            query.setParameter("product", product);
            List<UnitOfMeasure> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete all units of measure for a specific product
     * @param product the product
     * @return true if successful, false otherwise
     */
    public boolean deleteByProduct(Product product) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            String hql = "DELETE FROM UnitOfMeasure u WHERE u.product = :product";
            session.createQuery(hql)
                    .setParameter("product", product)
                    .executeUpdate();
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
}
