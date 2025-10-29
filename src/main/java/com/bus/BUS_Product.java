package com.bus;

import com.dao.DAO_Product;
import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.interfaces.IProduct;

import java.util.List;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
/**
 * @author Nguyễn Thanh Khôi
 */
public class BUS_Product implements IProduct {

    private final DAO_Product dao;

    public BUS_Product() {
        this.dao = new DAO_Product();
    }

    @Override
    public Product getProductById(String id) {
        if (id == null || id.isBlank()) return null;
        return dao.getProductById(id);
    }

    @Override
    public Lot getLotByBatchNumber(String batchNumber) {
        if (batchNumber == null || batchNumber.isBlank()) return null;
        return dao.getLotByBatchNumber(batchNumber);
    }

    @Override
    public List<Lot> getAllLots() {
        return dao.getAllLots();
    }

    @Override
    public UnitOfMeasure getUnitOfMeasureById(String id) {
        if (id == null || id.isBlank()) return null;
        return dao.getUnitOfMeasureById(id);
    }

    @Override
    public List<UnitOfMeasure> getAllUnitOfMeasures() {
        return dao.getAllUnitOfMeasures();
    }

    @Override
    public boolean addProduct(Product p) {

        // ==== VALIDATION ====
        if (p == null) {
            System.err.println("❌ Product null");
            return false;
        }

        if (p.getName() == null || p.getName().isBlank()) {
            System.err.println("❌ Tên sản phẩm không được để trống");
            return false;
        }

        if (p.getCategory() == null) {
            System.err.println("❌ Danh mục chưa chọn");
            return false;
        }

        if (p.getForm() == null) {
            System.err.println("❌ Dạng bào chế chưa chọn");
            return false;
        }

        if (p.getVat() < 0) {
            System.err.println("❌ VAT không thể âm");
            return false;
        }

        // Nếu có UnitOfMeasure đi kèm → gán quan hệ 2 chiều
        for (UnitOfMeasure u : p.getUnitOfMeasureList()) {
            u.setProduct(p);
        }

        // Nếu có Lot đi kèm → gán quan hệ 2 chiều
        for (Lot lot : p.getLotList()) {
            lot.setProduct(p);
        }

        return dao.addProduct(p);
    }

    @Override
    public List<Product> getAllProducts() {
        return dao.getAllProducts();
    }

    public List<Product> searchProducts(String keyword, String categoryCode, String formCode) {
        final String kw = normalize(keyword);
        final boolean hasKw = kw != null && !kw.isEmpty();

        final String cat = isBlank(categoryCode) ? null : categoryCode.trim();
        final String form = isBlank(formCode)     ? null : formCode.trim();

        // Lấy toàn bộ product theo cách DAO đang dùng (đã JOIN FETCH UOM/Lot để sẵn)
        // -> lọc tại BUS theo yêu cầu search.
        List<Product> all = dao.getAllProducts(); // có thể trả null nếu lỗi
        if (all == null || all.isEmpty()) return java.util.Collections.emptyList();

        return all.stream()
                .filter(p -> {
                    if (cat  != null && !cat.equalsIgnoreCase(nz(p.getCategory().toString())))   return false;
                    if (form != null && !form.equalsIgnoreCase(nz(p.getForm().toString())))       return false;
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

    // ===== helpers =====
    private static String nz(String s) { return s == null ? "" : s; }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    /** Chuẩn hoá: trim + lowercase + bỏ dấu (để search tiếng Việt thân thiện). */
    private static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase(Locale.ROOT);
        t = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t;
    }
    private static boolean contains(String haystack, String normalizedNeedle) {
        if (haystack == null) return false;
        String h = normalize(haystack);
        return h.contains(normalizedNeedle);
    }
}
