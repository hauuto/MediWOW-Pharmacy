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
import org.hibernate.query.Query;

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
                                    "LEFT JOIN FETCH p.unitOfMeasureSet u " +
                                    "LEFT JOIN FETCH u.measurement m " +
                                    "LEFT JOIN FETCH p.lotSet l " +
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

            // KIỂM TRA BARCODE TRÙNG TRƯỚC KHI INSERT
            if (s.getBarcode() != null && !s.getBarcode().trim().isEmpty()) {
                Long count = session.createQuery(
                                "SELECT COUNT(p) FROM Product p WHERE p.barcode = :barcode", Long.class)
                        .setParameter("barcode", s.getBarcode().trim())
                        .uniqueResult();

                if (count != null && count > 0) {
                    System.err.println("❌ Barcode đã tồn tại: " + s.getBarcode());
                    throw new IllegalArgumentException("Mã vạch '" + s.getBarcode() + "' đã tồn tại trong hệ thống. Vui lòng sử dụng mã vạch khác.");
                }
            }

            // Tách Lots và UnitOfMeasures ra khỏi Product trước khi persist
            Set<Lot> lots = new HashSet<>(s.getLotSet());
            Set<UnitOfMeasure> uoms = new HashSet<>(s.getUnitOfMeasureSet());

            // Clear để tránh cascade insert
            s.getLotSet().clear();
            s.getUnitOfMeasureSet().clear();

            // Đảm bảo creationDate được set (vì native SQL không kích hoạt @CreationTimestamp)
            if (s.getCreationDate() == null) {
                java.lang.reflect.Field creationDateField = Product.class.getDeclaredField("creationDate");
                creationDateField.setAccessible(true);
                creationDateField.set(s, java.time.LocalDateTime.now());
            }

            // BƯỚC 1: Insert Product trực tiếp bằng native SQL
            String insertProductSQL = "INSERT INTO Product (barcode, category, form, name, shortName, manufacturer, activeIngredient, vat, strength, description, baseUnitOfMeasure, creationDate, updateDate, image) " +
                    "VALUES (:barcode, :category, :form, :name, :shortName, :manufacturer, :activeIngredient, :vat, :strength, :description, :baseUnitOfMeasure, :creationDate, :updateDate, :image)";

            session.createNativeQuery(insertProductSQL)
                    .setParameter("barcode", s.getBarcode())
                    .setParameter("category", s.getCategory().name())
                    .setParameter("form", s.getForm().name())
                    .setParameter("name", s.getName())
                    .setParameter("shortName", s.getShortName())
                    .setParameter("manufacturer", s.getManufacturer())
                    .setParameter("activeIngredient", s.getActiveIngredient())
                    .setParameter("vat", s.getVat())
                    .setParameter("strength", s.getStrength())
                    .setParameter("description", s.getDescription())
                    .setParameter("baseUnitOfMeasure", s.getBaseUnitOfMeasure())
                    .setParameter("creationDate", s.getCreationDate())
                    .setParameter("updateDate", s.getUpdateDate())
                    .setParameter("image", s.getImage())
                    .executeUpdate();

            session.flush();

            // Lấy ID thực do trigger sinh ra (ID mới nhất được tạo)
            String getLastIdSQL = "SELECT TOP 1 id FROM Product ORDER BY creationDate DESC, id DESC";
            String realProductId = (String) session.createNativeQuery(getLastIdSQL).getSingleResult();

            // Load lại Product từ database để có managed entity
            // Điều này tránh Hibernate cố gắng insert lại Product khi persist UOM
            Product managedProduct = session.get(Product.class, realProductId);
            if (managedProduct == null) {
                throw new IllegalStateException("Không thể load Product vừa insert với ID: " + realProductId);
            }

            // BƯỚC 2: Insert UnitOfMeasure với Product managed entity
            for (UnitOfMeasure uom : uoms) {
                // Get managed MeasurementName entity from database
                MeasurementName managedMeasurement = session.get(MeasurementName.class, uom.getMeasurement().getId());
                if (managedMeasurement != null) {
                    // Create new UOM với managed entities (managedProduct thay vì s)
                    UnitOfMeasure uomToAdd = new UnitOfMeasure(managedProduct, managedMeasurement, uom.getPrice(), uom.getBaseUnitConversionRate());
                    session.persist(uomToAdd);
                }
            }

            session.flush();

            // BƯỚC 3: Insert Lot với Product ID thực
            for (Lot lot : lots) {
                // Insert Lot bằng native SQL để trigger tự sinh ID
                String insertLotSQL = "INSERT INTO Lot (batchNumber, product, quantity, rawPrice, expiryDate, status) " +
                        "VALUES (:batchNumber, :product, :quantity, :rawPrice, :expiryDate, :status)";

                session.createNativeQuery(insertLotSQL)
                        .setParameter("batchNumber", lot.getBatchNumber())
                        .setParameter("product", realProductId)
                        .setParameter("quantity", lot.getQuantity())
                        .setParameter("rawPrice", lot.getRawPrice())
                        .setParameter("expiryDate", lot.getExpiryDate())
                        .setParameter("status", lot.getStatus().name())
                        .executeUpdate();
            }

            session.flush();

            // Lấy lại các Lot vừa insert để có ID thực
            String getLotsSQL = "SELECT * FROM Lot WHERE product = :productId";
            List<Lot> insertedLots = session.createNativeQuery(getLotsSQL, Lot.class)
                    .setParameter("productId", realProductId)
                    .getResultList();

            // Cập nhật ID thực vào object Product gốc (cho UI sử dụng)
            java.lang.reflect.Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(s, realProductId);

            // Gán lại vào Product object
            s.getLotSet().addAll(insertedLots);
            s.getUnitOfMeasureSet().addAll(uoms);

            tx.commit();
            return true;
        } catch (IllegalArgumentException e) {
            if (tx != null) tx.rollback();
            // Ném lại exception để UI xử lý
            throw e;
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
                                    "LEFT JOIN FETCH p.unitOfMeasureSet u " +
                                    "LEFT JOIN FETCH u.measurement " +
                                    "LEFT JOIN FETCH p.lotSet l " +
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
            existing.setImage(p.getImage()); // Cập nhật image path

            // Handle UnitOfMeasure updates
            if (p.getUnitOfMeasureSet() != null) {
                // Find UOMs that are no longer in the new list
                Set<UnitOfMeasure> toRemove = new HashSet<>();
                for (UnitOfMeasure existingUom : existing.getUnitOfMeasureSet()) {
                    boolean found = false;
                    for (UnitOfMeasure newUom : p.getUnitOfMeasureSet()) {
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
                    existing.getUnitOfMeasureSet().remove(uom);
                    session.remove(uom);
                }

                // Add or update UOMs
                for (UnitOfMeasure newUom : p.getUnitOfMeasureSet()) {
                    if (newUom.getMeasurement() == null) continue;

                    UnitOfMeasure existingUom = null;
                    for (UnitOfMeasure u : existing.getUnitOfMeasureSet()) {
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
                            existing.getUnitOfMeasureSet().add(uomToAdd);
                        }
                    }
                }
            }

            // Handle Lot updates
            if (p.getLotSet() != null) {
                // Find Lots that are no longer in the new list
                Set<Lot> toRemoveLots = new HashSet<>();
                for (Lot existingLot : existing.getLotSet()) {
                    boolean found = false;
                    for (Lot newLot : p.getLotSet()) {
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
                    existing.getLotSet().remove(lot);
                    session.remove(lot);
                }

                // Add or update Lots
                for (Lot newLot : p.getLotSet()) {
                    if (newLot.getBatchNumber() == null || newLot.getBatchNumber().isEmpty()) continue;

                    Lot existingLot = null;
                    for (Lot l : existing.getLotSet()) {
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
                        existing.getLotSet().add(lotToAdd);
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

        // Check InvoiceLine references - InvoiceLine has unitOfMeasure which contains product and measurementId
        Long invoiceLineCount = session.createQuery(
                        "SELECT COUNT(il) FROM InvoiceLine il " +
                                "WHERE il.unitOfMeasure.product.id = :productId " +
                                "AND il.unitOfMeasure.measurement.id = :measurementId",
                        Long.class)
                .setParameter("productId", productId)
                .setParameter("measurementId", measurementId)
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
                            "LEFT JOIN FETCH p.unitOfMeasureSet u " +
                            "LEFT JOIN FETCH u.measurement",
                    Product.class
            ).list();

            // Query 2: subselect load lotSet for all products
            if (!products.isEmpty()) {
                session.createQuery(
                                "SELECT DISTINCT p FROM Product p " +
                                        "LEFT JOIN FETCH p.lotSet " +
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

    /**
     * Search top 5 products by name (contains, case-insensitive) or barcode (contains).
     * Used by global/omni-search in the GUI.
     */
    public List<Product> searchTop5ByNameOrBarcode(String keyword) {
        if (keyword == null) return java.util.Collections.emptyList();
        String kw = keyword.trim();
        if (kw.isEmpty()) return java.util.Collections.emptyList();

        Session session = null;
        try {
            session = sessionFactory.openSession();
            String jpql = "FROM Product p WHERE lower(p.name) LIKE :kw OR p.barcode LIKE :bar ORDER BY p.name";
            Query<Product> q = session.createQuery(jpql, Product.class);
            q.setParameter("kw", "%" + kw.toLowerCase() + "%");
            q.setParameter("bar", "%" + kw + "%");
            q.setMaxResults(5);
            return q.list();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            if (session != null && session.isOpen()) session.close();
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

    public MeasurementName findMeasurementNameByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                            "FROM MeasurementName m WHERE lower(m.name) = :n",
                            MeasurementName.class)
                    .setParameter("n", name.trim().toLowerCase())
                    .uniqueResult();

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            if (session != null) session.close();
        }
    }

}
