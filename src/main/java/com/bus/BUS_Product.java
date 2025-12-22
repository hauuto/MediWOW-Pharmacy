// File: /MediWOW-Pharmacy/src/main/java/com/bus/BUS_Product.java
package com.bus;

import com.dao.DAO_Product;
import com.entities.Lot;
import com.entities.MeasurementName;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.interfaces.IProduct;

import java.math.BigDecimal;
import java.util.List;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;

public class BUS_Product implements IProduct {

    private final DAO_Product dao;

    public BUS_Product() { this.dao = new DAO_Product(); }

    // ===== CRUD/Query =====
    @Override public Product getProductById(String id) { return (id == null || id.isBlank()) ? null : dao.getProductById(id); }
    @Override public Product getProductByBarcode(String barcode) { return dao.getProductByBarcode(barcode); }
    @Override public Lot getLotByBatchNumber(String batchNumber) { return (batchNumber == null || batchNumber.isBlank()) ? null : dao.getLotByBatchNumber(batchNumber); }

    @Override
    public UnitOfMeasure getUnitOfMeasureById(String productId, Integer measurementId) {
        if (productId == null || productId.trim().isEmpty() || measurementId == null) return null;
        return dao.getUnitOfMeasureById(productId, measurementId);
    }

    @Override
    public Lot getLotById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        return dao.getLotById(id);
    }

    @Override public List<Lot> getAllLots() { return dao.getAllLots(); }

    @Override
    public UnitOfMeasure getUnitOfMeasureById(String productId, String name) {
        if (productId == null || productId.trim().isEmpty() || name == null || name.trim().isEmpty()) return null;
        return dao.getUnitOfMeasureById(productId, name);
    }

    @Override public List<UnitOfMeasure> getAllUnitOfMeasures() { return dao.getAllUnitOfMeasures(); }

    @Override
    public boolean addProduct(Product p) {
        validateProduct(p);
        checkDuplicates(p);

        // Gán quan hệ 2 chiều (phòng trường hợp caller chưa set)
        if (p.getUnitOfMeasureSet() != null)
            for (UnitOfMeasure u : p.getUnitOfMeasureSet()) if (u != null) u.setProduct(p);
        if (p.getLotSet() != null)
            for (Lot l : p.getLotSet()) if (l != null) l.setProduct(p);

        return dao.addProduct(p);
    }

    @Override
    public boolean updateProduct(Product p) {
        if (p == null || p.getId() == null || p.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông tin sản phẩm cần cập nhật");
        }

        // Kiểm tra sản phẩm có tồn tại không
        Product existingProduct = dao.getProductById(p.getId());
        if (existingProduct == null) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại trong hệ thống");
        }

        // Validate thông tin
        validateProductForUpdate(p);

        // Kiểm tra trùng lặp (loại trừ chính sản phẩm đang cập nhật)
        checkDuplicatesForUpdate(p);

        // Gán quan hệ 2 chiều
        if (p.getUnitOfMeasureSet() != null)
            for (UnitOfMeasure u : p.getUnitOfMeasureSet()) if (u != null) u.setProduct(p);
        if (p.getLotSet() != null)
            for (Lot l : p.getLotSet()) if (l != null) l.setProduct(p);

        return dao.updateProduct(p);
    }

    @Override public List<Product> getAllProducts() { return dao.getAllProducts(); }

    // ===== Search (giữ nguyên) =====
    public List<Product> searchProducts(String keyword, String categoryCode, String formCode) {
        final String kw = normalize(keyword);
        final boolean hasKw = kw != null && !kw.isEmpty();
        final String cat = isBlank(categoryCode) ? null : categoryCode.trim();
        final String form = isBlank(formCode) ? null : formCode.trim();

        List<Product> all = dao.getAllProducts();
        if (all == null || all.isEmpty()) return java.util.Collections.emptyList();

        return all.stream()
                .filter(p -> {
                    if (cat  != null && !cat.equalsIgnoreCase(nz(p.getCategory().toString()))) return false;
                    if (form != null && !form.equalsIgnoreCase(nz(p.getForm().toString())))     return false;
                    if (!hasKw) return true;
                    return contains(nz(p.getId()), kw)
                            || contains(nz(p.getName()), kw)
                            || contains(nz(p.getShortName()), kw)
                            || contains(nz(p.getBarcode()), kw)
                            || contains(nz(p.getActiveIngredient()), kw)
                            || contains(nz(p.getManufacturer()), kw);
                })
                .sorted(Comparator.comparing(p -> nz(p.getName()).toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    // ===== Duplicate checks wrappers =====
    @Override public boolean existsByBarcode(String barcode) { return dao.existsByBarcode(barcode); }
    @Override public boolean existsByNameAndManufacturer(String name, String manufacturer) { return dao.existsByNameAndManufacturer(name, manufacturer); }
    @Override public boolean existsLotByBatchNumber(String batchNumber) { return dao.existsLotByBatchNumber(batchNumber); }

    // ===== Validation & duplicate logic =====
    private void validateProduct(Product p) {
        if (p == null) throw new IllegalArgumentException("Không có thông tin sản phẩm để thêm");

        if (p.getName() == null || p.getName().trim().isEmpty())
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");

        if (p.getCategory() == null)
            throw new IllegalArgumentException("Vui lòng chọn Loại sản phẩm");

        if (p.getForm() == null)
            throw new IllegalArgumentException("Vui lòng chọn Dạng bào chế");

        if (p.getBaseUnitOfMeasure() == null || p.getBaseUnitOfMeasure().trim().isEmpty())
            throw new IllegalArgumentException("Đơn vị tính gốc không được để trống");

        java.math.BigDecimal vat = (p.getVat() != null) ? p.getVat() : java.math.BigDecimal.ZERO;
        if (vat.compareTo(java.math.BigDecimal.ZERO) < 0 || vat.compareTo(java.math.BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("VAT phải nằm trong khoảng 0–100%");

        // Validate UOMs: Tên ĐV và Quy đổi về ĐV gốc không được null
        if (p.getUnitOfMeasureSet() != null) {
            for (UnitOfMeasure u : p.getUnitOfMeasureSet()) {
                if (u == null) continue;
                if (u.getMeasurement() == null || u.getMeasurement().getName() == null || u.getMeasurement().getName().trim().isEmpty())
                    throw new IllegalArgumentException("Đơn vị đo lường (Tên ĐV) không được để trống");
                if (u.getBaseUnitConversionRate() == null || u.getBaseUnitConversionRate().compareTo(java.math.BigDecimal.ONE) < 0)
                    throw new IllegalArgumentException("Quy đổi về ĐV gốc phải là số nguyên dương");
            }
        }

        // Lot: khi thêm mới, yêu cầu >= 1 dòng (theo UI validate)
        if (p.getLotSet() == null || p.getLotSet().isEmpty())
            throw new IllegalArgumentException("Sản phẩm mới cần có ít nhất 1 lô hàng");
        for (Lot lot : p.getLotSet()) {
            if (lot == null) continue;
            if (lot.getBatchNumber() == null || lot.getBatchNumber().trim().isEmpty())
                throw new IllegalArgumentException("Mã lô không được để trống");
            if (lot.getExpiryDate() == null)
                throw new IllegalArgumentException("Hạn sử dụng của lô không được để trống");
            if (lot.getQuantity() < 0) throw new IllegalArgumentException("Số lượng lô phải ≥ 0");

            java.math.BigDecimal rawPrice = lot.getRawPrice();
            if (rawPrice != null && rawPrice.compareTo(java.math.BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("Giá lô phải ≥ 0");
        }
    }

    /**
     * Validate product information for update (less strict than add)
     */
    private void validateProductForUpdate(Product p) {
        if (p == null) throw new IllegalArgumentException("Không có thông tin sản phẩm để cập nhật");

        if (p.getName() == null || p.getName().trim().isEmpty())
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");

        if (p.getCategory() == null)
            throw new IllegalArgumentException("Vui lòng chọn Loại sản phẩm");

        if (p.getForm() == null)
            throw new IllegalArgumentException("Vui lòng chọn Dạng bào chế");

        if (p.getBaseUnitOfMeasure() == null || p.getBaseUnitOfMeasure().trim().isEmpty())
            throw new IllegalArgumentException("Đơn vị tính gốc không được để trống");

        BigDecimal vat = p.getVat();
        if (vat.compareTo(BigDecimal.ZERO) < 0 || vat.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("VAT phải nằm trong khoảng 0–100%");
        }

        // Validate UOMs for update as well
        if (p.getUnitOfMeasureSet() != null) {
            for (UnitOfMeasure u : p.getUnitOfMeasureSet()) {
                if (u == null) continue;
                if (u.getMeasurement() == null || u.getMeasurement().getName() == null || u.getMeasurement().getName().trim().isEmpty())
                    throw new IllegalArgumentException("Đơn vị đo lường (Tên ĐV) không được để trống");
                if (u.getBaseUnitConversionRate() == null || u.getBaseUnitConversionRate().compareTo(java.math.BigDecimal.ONE) < 0)
                    throw new IllegalArgumentException("Quy đổi về ĐV gốc phải là số nguyên dương");
            }
        }

        // Validate Lots: Lô và Hạn sử dụng không được null
        if (p.getLotSet() != null) {
            for (Lot lot : p.getLotSet()) {
                if (lot == null) continue;
                if (lot.getBatchNumber() == null || lot.getBatchNumber().trim().isEmpty())
                    throw new IllegalArgumentException("Mã lô không được để trống");
                if (lot.getExpiryDate() == null)
                    throw new IllegalArgumentException("Hạn sử dụng của lô không được để trống");
                if (lot.getQuantity() < 0) throw new IllegalArgumentException("Số lượng lô phải ≥ 0");
                java.math.BigDecimal rawPrice = lot.getRawPrice();
                if (rawPrice != null && rawPrice.compareTo(java.math.BigDecimal.ZERO) < 0)
                    throw new IllegalArgumentException("Giá lô phải ≥ 0");
            }
        }
    }

    private void checkDuplicates(Product p) {
        if (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) {
            if (existsByBarcode(p.getBarcode()))
                throw new IllegalArgumentException("Mã vạch '" + p.getBarcode() + "' đã tồn tại trong hệ thống");
        }
        if (p.getName() != null && p.getManufacturer() != null
                && !p.getName().trim().isEmpty() && !p.getManufacturer().trim().isEmpty()) {
            if (existsByNameAndManufacturer(p.getName(), p.getManufacturer()))
                throw new IllegalArgumentException("Sản phẩm '" + p.getName() + "' của hãng '" + p.getManufacturer() + "' đã tồn tại");
        }
        if (p.getLotSet() != null) {
            for (Lot lot : p.getLotSet()) {
                if (lot != null && lot.getBatchNumber() != null && !lot.getBatchNumber().trim().isEmpty()) {
                    if (existsLotByBatchNumber(lot.getBatchNumber()))
                        throw new IllegalArgumentException("Mã lô '" + lot.getBatchNumber() + "' đã tồn tại");
                }
            }
        }
    }


    private void checkDuplicatesForUpdate(Product p) {
        if (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) {
            if (existsByBarcodeExcludingId(p.getBarcode(), p.getId()))
                throw new IllegalArgumentException("Mã vạch '" + p.getBarcode() + "' đã tồn tại trong hệ thống");
        }
        if (p.getName() != null && p.getManufacturer() != null
                && !p.getName().trim().isEmpty() && !p.getManufacturer().trim().isEmpty()) {
            if (existsByNameAndManufacturerExcludingId(p.getName(), p.getManufacturer(), p.getId()))
                throw new IllegalArgumentException("Sản phẩm '" + p.getName() + "' của hãng '" + p.getManufacturer() + "' đã tồn tại");
        }
    }

    /**
     * Check if barcode exists excluding a specific product id
     */
    public boolean existsByBarcodeExcludingId(String barcode, String excludeId) {
        if (barcode == null || barcode.trim().isEmpty()) return false;
        Product existing = dao.getProductByBarcode(barcode);
        return existing != null && !existing.getId().equals(excludeId);
    }

    /**
     * Check if product name and manufacturer combination exists excluding a specific product id
     */
    public boolean existsByNameAndManufacturerExcludingId(String name, String manufacturer, String excludeId) {
        if (name == null || name.trim().isEmpty() || manufacturer == null || manufacturer.trim().isEmpty()) return false;
        List<Product> all = dao.getAllProducts();
        if (all == null) return false;
        return all.stream().anyMatch(p ->
                !p.getId().equals(excludeId) &&
                        p.getName().trim().equalsIgnoreCase(name.trim()) &&
                        p.getManufacturer() != null && p.getManufacturer().trim().equalsIgnoreCase(manufacturer.trim())
        );
    }

    // ===== helpers =====
    private static String nz(String s) { return s == null ? "" : s; }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase(Locale.ROOT);
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t;
    }
    private static boolean contains(String haystack, String normalizedNeedle) {
        if (haystack == null) return false;
        String h = normalize(haystack);
        return h.contains(normalizedNeedle);
    }

    public Product getProductByIdWithChildren(String id) {
        return (id == null || id.isBlank()) ? null : dao.getProductById(id);
    }

    /**
     * Search top 5 products by name or barcode for omni-search.
     */
    public List<Product> searchTop5ByNameOrBarcode(String keyword) {
        try {
            return dao.searchTop5ByNameOrBarcode(keyword);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Search top 5 lots by batch number for omni-search.
     */
    public List<Lot> searchTop5LotsByBatchNumber(String keyword) {
        try {
            return dao.searchTop5LotsByBatchNumber(keyword);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Deduct a specific quantity from a lot
     * @param lotId The ID of the lot
     * @param quantityToDeduct The quantity to deduct
     * @return true if successful, false otherwise
     */
    public boolean deductLotQuantity(String lotId, int quantityToDeduct) {
        if (lotId == null || lotId.trim().isEmpty()) {
            throw new IllegalArgumentException("Lot ID cannot be null or empty");
        }
        if (quantityToDeduct <= 0) {
            throw new IllegalArgumentException("Quantity to deduct must be positive");
        }
        return dao.deductLotQuantity(lotId, quantityToDeduct);
    }

    /**
     * Update the quantity of a specific lot to a new value
     * @param lotId The ID of the lot
     * @param newQuantity The new quantity value
     * @return true if successful, false otherwise
     */
    public boolean updateLotQuantity(String lotId, int newQuantity) {
        if (lotId == null || lotId.trim().isEmpty()) {
            throw new IllegalArgumentException("Lot ID cannot be null or empty");
        }
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        return dao.updateLotQuantity(lotId, newQuantity);
    }

    /**
     * Add a specific quantity to a lot (for exchange returns)
     * @param lotId The ID of the lot
     * @param quantityToAdd The quantity to add
     * @return true if successful, false otherwise
     */
    public boolean addLotQuantity(String lotId, int quantityToAdd) {
        if (lotId == null || lotId.trim().isEmpty()) {
            throw new IllegalArgumentException("Lot ID cannot be null or empty");
        }
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }
        return dao.addLotQuantity(lotId, quantityToAdd);
    }

    /* Get all MeasurementName entities from database
     */
    public List<MeasurementName> getAllMeasurementNames() {
        return dao.getAllMeasurementNames();
    }

    /**
     * Get or create MeasurementName by name
     */
    public MeasurementName getOrCreateMeasurementName(String name) {
        return dao.getOrCreateMeasurementName(name);
    }

    /**
     * Validate product data for UI layer.
     * UI should call this and simply display the thrown message (IllegalArgumentException#getMessage).
     *
     * @param p     product to validate
     * @param isNew true when adding new product, false when updating
     */
    public void validateForUi(Product p, boolean isNew) {
        if (p == null) throw new IllegalArgumentException("Không có thông tin sản phẩm để " + (isNew ? "thêm" : "cập nhật"));

        // Barcode format: digits only, 8–20 chars (UI previously checked this manually)
        if (p.getBarcode() == null || p.getBarcode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vạch không được để trống");
        }
        String bc = p.getBarcode().trim();
        if (!bc.matches("\\d{8,20}")) {
            throw new IllegalArgumentException("Mã vạch chỉ gồm 8–20 chữ số.");
        }

        if (isNew) {
            validateProduct(p);
            checkDuplicates(p);
        } else {
            // update requires id to exist; keep same wording as updateProduct
            if (p.getId() == null || p.getId().trim().isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy thông tin sản phẩm cần cập nhật");
            }
            validateProductForUpdate(p);
            checkDuplicatesForUpdate(p);
        }
    }
}
