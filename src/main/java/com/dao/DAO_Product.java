package com.dao;

import com.entities.Lot;
import com.entities.MeasurementName;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.interfaces.IProduct;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.Hibernate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public boolean updateProduct(Product p) {
        Transaction tx = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();

            // Fetch the existing product with its UOMs and Lots
            Product existing = session.createQuery(
                    "SELECT DISTINCT p FROM Product p " +
                            "LEFT JOIN FETCH p.unitOfMeasureList u " +
                            "LEFT JOIN FETCH u.measurement " +
                            "LEFT JOIN FETCH p.lotList l " +
                            "WHERE p.id = :id",
                    Product.class)
                    .setParameter("id", p.getId())
                    .uniqueResult();

            if (existing == null) {
                return false;
            }

            // Update basic fields
            existing.setName(p.getName());
            existing.setShortName(p.getShortName());
            existing.setBarcode(p.getBarcode());
            existing.setCategory(p.getCategory());
            existing.setForm(p.getForm());
            existing.setActiveIngredient(p.getActiveIngredient());
            existing.setManufacturer(p.getManufacturer());
            existing.setStrength(p.getStrength());
            existing.setDescription(p.getDescription());
            existing.setVat(p.getVat());
            existing.setBaseUnitOfMeasure(p.getBaseUnitOfMeasure());

            // Handle UnitOfMeasure updates
            if (p.getUnitOfMeasureList() != null) {
                // Find UOMs that are no longer in the new list
                Set<UnitOfMeasure> toRemove = new HashSet<>();
                for (UnitOfMeasure existingUom : existing.getUnitOfMeasureList()) {
                    boolean found = false;
                    for (UnitOfMeasure newUom : p.getUnitOfMeasureList()) {
                        if (newUom.getMeasurement() != null &&
                            existingUom.getMeasurement() != null &&
                            newUom.getMeasurement().getId().equals(existingUom.getMeasurement().getId())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // Check if this UOM is referenced by PromotionAction or PromotionCondition
                        boolean isReferenced = isUnitOfMeasureReferenced(session, existingUom);
                        if (!isReferenced) {
                            toRemove.add(existingUom);
                        }
                        // If referenced, we keep it in the list (don't delete)
                    }
                }

                // Remove orphaned UOMs that are not referenced
                for (UnitOfMeasure uom : toRemove) {
                    existing.getUnitOfMeasureList().remove(uom);
                    session.remove(uom);
                }

                // Add or update UOMs
                for (UnitOfMeasure newUom : p.getUnitOfMeasureList()) {
                    if (newUom.getMeasurement() == null) continue;

                    UnitOfMeasure existingUom = null;
                    for (UnitOfMeasure u : existing.getUnitOfMeasureList()) {
                        if (u.getMeasurement() != null &&
                            u.getMeasurement().getId().equals(newUom.getMeasurement().getId())) {
                            existingUom = u;
                            break;
                        }
                    }

                    if (existingUom != null) {
                        // Update existing UOM
                        existingUom.setBaseUnitConversionRate(newUom.getBaseUnitConversionRate());
                        existingUom.setPrice(newUom.getPrice());
                    } else {
                        // Add new UOM - need to get managed MeasurementName
                        MeasurementName managedMeasurement = session.get(MeasurementName.class, newUom.getMeasurement().getId());
                        if (managedMeasurement != null) {
                            UnitOfMeasure uomToAdd = new UnitOfMeasure(existing, managedMeasurement, newUom.getPrice(), newUom.getBaseUnitConversionRate());
                            existing.getUnitOfMeasureList().add(uomToAdd);
                        }
                    }
                }
            }

            // Handle Lot updates
            if (p.getLotList() != null) {
                // Find Lots that are no longer in the new list
                Set<Lot> toRemoveLots = new HashSet<>();
                for (Lot existingLot : existing.getLotList()) {
                    boolean found = false;
                    for (Lot newLot : p.getLotList()) {
                        if (newLot.getBatchNumber() != null &&
                            existingLot.getBatchNumber() != null &&
                            newLot.getBatchNumber().equals(existingLot.getBatchNumber())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // Check if this Lot is referenced by LotAllocation
                        boolean isReferenced = isLotReferenced(session, existingLot);
                        if (!isReferenced) {
                            toRemoveLots.add(existingLot);
                        }
                        // If referenced, we keep it in the list (don't delete)
                    }
                }

                // Remove orphaned Lots that are not referenced
                for (Lot lot : toRemoveLots) {
                    existing.getLotList().remove(lot);
                    session.remove(lot);
                }

                // Add or update Lots
                for (Lot newLot : p.getLotList()) {
                    if (newLot.getBatchNumber() == null || newLot.getBatchNumber().isEmpty()) continue;

                    Lot existingLot = null;
                    for (Lot l : existing.getLotList()) {
                        if (l.getBatchNumber() != null &&
                            l.getBatchNumber().equals(newLot.getBatchNumber())) {
                            existingLot = l;
                            break;
                        }
                    }

                    if (existingLot != null) {
                        // Update existing Lot
                        existingLot.setQuantity(newLot.getQuantity());
                        existingLot.setRawPrice(newLot.getRawPrice());
                        existingLot.setExpiryDate(newLot.getExpiryDate());
                        existingLot.setStatus(newLot.getStatus());
                    } else {
                        // Add new Lot
                        Lot lotToAdd = new Lot(
                            newLot.getId(),
                            newLot.getBatchNumber(),
                            existing,
                            newLot.getQuantity(),
                            newLot.getRawPrice(),
                            newLot.getExpiryDate(),
                            newLot.getStatus()
                        );
                        existing.getLotList().add(lotToAdd);
                    }
                }
            }

            session.merge(existing);
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

    /**
     * Check if a UnitOfMeasure is referenced by PromotionAction, PromotionCondition, or InvoiceLine
     */
    private boolean isUnitOfMeasureReferenced(Session session, UnitOfMeasure uom) {
        if (uom == null || uom.getProduct() == null || uom.getMeasurement() == null) {
            return false;
        }

        String productId = uom.getProduct().getId();
        Integer measurementId = uom.getMeasurement().getId();

        // Check PromotionAction references
        Long actionCount = session.createQuery(
                "SELECT COUNT(pa) FROM PromotionAction pa " +
                "WHERE pa.productUOM.product.id = :productId " +
                "AND pa.productUOM.measurement.id = :measurementId",
                Long.class)
                .setParameter("productId", productId)
                .setParameter("measurementId", measurementId)
                .getSingleResult();

        if (actionCount > 0) {
            return true;
        }

        // Check PromotionCondition references
        Long conditionCount = session.createQuery(
                "SELECT COUNT(pc) FROM PromotionCondition pc " +
                "WHERE pc.productUOM.product.id = :productId " +
                "AND pc.productUOM.measurement.id = :measurementId",
                Long.class)
                .setParameter("productId", productId)
                .setParameter("measurementId", measurementId)
                .getSingleResult();

        if (conditionCount > 0) {
            return true;
        }

        // Check InvoiceLine references - InvoiceLine stores measurementId as unitOfMeasure column
        // The FK is (product, unitOfMeasure) -> UnitOfMeasure(product, measurementId)
        Long invoiceLineCount = session.createQuery(
                "SELECT COUNT(il) FROM InvoiceLine il " +
                "WHERE il.product.id = :productId " +
                "AND il.unitOfMeasure = :measurementId",
                Long.class)
                .setParameter("productId", productId)
                .setParameter("measurementId", String.valueOf(measurementId))
                .getSingleResult();

        return invoiceLineCount > 0;
    }

    /**
     * Check if a Lot is referenced by LotAllocation
     */
    private boolean isLotReferenced(Session session, Lot lot) {
        if (lot == null || lot.getId() == null) {
            return false;
        }

        // Check LotAllocation references
        Long allocationCount = session.createQuery(
                "SELECT COUNT(la) FROM LotAllocation la " +
                "WHERE la.lot.id = :lotId",
                Long.class)
                .setParameter("lotId", lot.getId())
                .getSingleResult();

        return allocationCount > 0;
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

    /**
     * Get all MeasurementName entities from database
     */
    public List<MeasurementName> getAllMeasurementNames() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery("FROM MeasurementName m ORDER BY m.name", MeasurementName.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        } finally {
            if (session != null) session.close();
        }
    }

    /**
     * Get or create MeasurementName by name
     */
    public MeasurementName getOrCreateMeasurementName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            // Try to find existing
            MeasurementName existing = session.createQuery(
                    "FROM MeasurementName m WHERE lower(m.name) = :n", MeasurementName.class)
                    .setParameter("n", name.trim().toLowerCase())
                    .uniqueResult();
            if (existing != null) return existing;

            // Create new
            tx = session.beginTransaction();
            MeasurementName newName = new MeasurementName(name.trim());
            session.persist(newName);
            tx.commit();
            return newName;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) session.close();
        }
    }
}
