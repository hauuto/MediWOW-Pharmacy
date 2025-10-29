// File: /MediWOW-Pharmacy/src/main/java/com/bus/BUS_Product.java
package com.bus;

import com.dao.DAO_Product;
import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.interfaces.IProduct;

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
    @Override public List<Lot> getAllLots() { return dao.getAllLots(); }
    @Override public UnitOfMeasure getUnitOfMeasureById(String id) { return (id == null || id.isBlank()) ? null : dao.getUnitOfMeasureById(id); }
    @Override public List<UnitOfMeasure> getAllUnitOfMeasures() { return dao.getAllUnitOfMeasures(); }

    @Override
    public boolean addProduct(Product p) {
        validateProduct(p);
        checkDuplicates(p);

        // Gán quan hệ 2 chiều (phòng trường hợp caller chưa set)
        if (p.getUnitOfMeasureList() != null)
            for (UnitOfMeasure u : p.getUnitOfMeasureList()) if (u != null) u.setProduct(p);
        if (p.getLotList() != null)
            for (Lot l : p.getLotList()) if (l != null) l.setProduct(p);

        return dao.addProduct(p);
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

        if (p.getVat() < 0 || p.getVat() > 100)
            throw new IllegalArgumentException("VAT phải nằm trong khoảng 0–100%");

        // Lot: khi thêm mới, yêu cầu >= 1 dòng (theo UI validate)
        if (p.getLotList() == null || p.getLotList().isEmpty())
            throw new IllegalArgumentException("Sản phẩm mới cần có ít nhất 1 lô hàng");
        for (Lot lot : p.getLotList()) {
            if (lot == null) continue;
            if (lot.getBatchNumber() == null || lot.getBatchNumber().trim().isEmpty())
                throw new IllegalArgumentException("Mã lô không được để trống");
            if (lot.getQuantity() < 0) throw new IllegalArgumentException("Số lượng lô phải ≥ 0");
            if (lot.getRawPrice() < 0) throw new IllegalArgumentException("Giá lô phải ≥ 0");
            if (lot.getExpiryDate() == null) throw new IllegalArgumentException("Lô phải có hạn sử dụng");
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
        if (p.getLotList() != null) {
            for (Lot lot : p.getLotList()) {
                if (lot != null && lot.getBatchNumber() != null && !lot.getBatchNumber().trim().isEmpty()) {
                    if (existsLotByBatchNumber(lot.getBatchNumber()))
                        throw new IllegalArgumentException("Mã lô '" + lot.getBatchNumber() + "' đã tồn tại");
                }
            }
        }
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
}
