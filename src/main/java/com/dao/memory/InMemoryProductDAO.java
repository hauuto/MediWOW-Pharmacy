//// com/dao/memory/InMemoryProductDAO.java
//package com.dao.memory;
//
//import com.dao.ProductDAO;
//import com.entities.Lot;
//import com.entities.Product;
//import com.entities.UnitOfMeasure;
//import com.enums.DosageForm;
//import com.enums.LotStatus;
//import com.enums.ProductCategory;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
//public class InMemoryProductDAO implements ProductDAO {
//    private final Map<String, Product> byId = new ConcurrentHashMap<>();
//    private final Map<String, String> idByBarcode = new ConcurrentHashMap<>();
//
//    public InMemoryProductDAO() {
//        seed();
//    }
//
//    @Override public List<Product> findAll() {
//        return byId.values().stream()
//                .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
//                .collect(Collectors.toList());
//    }
//
//    @Override public Optional<Product> findById(String id) { return Optional.ofNullable(byId.get(id)); }
//
//    @Override public Optional<Product> findByBarcode(String barcode) {
//        if (barcode == null || barcode.isBlank()) return Optional.empty();
//        String id = idByBarcode.get(barcode);
//        return id != null ? findById(id) : Optional.empty();
//    }
//
//    @Override public List<Product> search(String keyword, ProductCategory category, DosageForm form, Boolean forSale) {
//        String q = keyword == null ? "" : keyword.trim().toLowerCase();
//        return findAll().stream()
//                .filter(p -> q.isEmpty() ||
//                        p.getId().toLowerCase().contains(q) ||
//                        (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(q)) ||
//                        (p.getName() != null && p.getName().toLowerCase().contains(q)) ||
//                        (p.getShortName() != null && p.getShortName().toLowerCase().contains(q)))
//                .filter(p -> category == null || p.getCategory() == category)
//                .filter(p -> form == null || p.getForm() == form)
//                .filter(p -> forSale == null || p.isForSale() == forSale)
//                .collect(Collectors.toList());
//    }
//
//    @Override public Product create(Product product) {
//        Objects.requireNonNull(product, "product");
//        if (product.getId() == null) throw new IllegalArgumentException("Product.id is required");
//        if (product.getBarcode() != null && idByBarcode.containsKey(product.getBarcode()))
//            throw new IllegalStateException("Barcode already exists: " + product.getBarcode());
//        byId.put(product.getId(), product);
//        if (product.getBarcode() != null && !product.getBarcode().isBlank()) {
//            idByBarcode.put(product.getBarcode(), product.getId());
//        }
//        return product;
//    }
//
//    @Override public Product update(Product product) {
//        Objects.requireNonNull(product, "product");
//        if (!byId.containsKey(product.getId()))
//            throw new NoSuchElementException("Product not found: " + product.getId());
//
//        // cập nhật index barcode
//        String oldBarcode = byId.get(product.getId()).getBarcode();
//        if (!Objects.equals(oldBarcode, product.getBarcode())) {
//            if (product.getBarcode() != null && idByBarcode.containsKey(product.getBarcode()))
//                throw new IllegalStateException("Barcode already exists: " + product.getBarcode());
//            if (oldBarcode != null) idByBarcode.remove(oldBarcode);
//            if (product.getBarcode() != null && !product.getBarcode().isBlank())
//                idByBarcode.put(product.getBarcode(), product.getId());
//        }
//
//        byId.put(product.getId(), product);
//        return product;
//    }
//
//    @Override public boolean markNotForSale(String productId) {
//        Product p = byId.get(productId);
//        if (p == null) return false;
//        p.setForSale(false);
//        return true;
//    }
//
//    @Override public boolean existsBarcode(String barcode, String excludeProductId) {
//        if (barcode == null || barcode.isBlank()) return false;
//        String id = idByBarcode.get(barcode);
//        return id != null && !id.equals(excludeProductId);
//    }
//
//    // ===== seed 3 products =====
//    private void seed() {
//        // P1
//        Product p1 = new Product("PRO2025-0001");
//        p1.setName("Paracetamol 500mg");
//        p1.setShortName("PARA500");
//        p1.setCategory(ProductCategory.OTC);
//        p1.setForm(DosageForm.TABLET);
//        p1.setManufacturer("ABC Pharma");
//        p1.setActiveIngredient("Paracetamol");
//        p1.setStrength("500 mg");
//        p1.setVat(5);
//        p1.setBaseUnitOfMeasure("Viên");
//        p1.setBarcode("8931234567890");
//        // UOM
//        p1.addUnitOfMeasure(new UnitOfMeasure("UOM-1", p1, "Hộp(10 vỉ x 10 viên)", 100));
//        // Lot
//        p1.addLot(new Lot("LOT-P1-A", p1, 500, 1500, LocalDateTime.now().plusMonths(18), LotStatus.AVAILABLE));
//
//        // P2
//        Product p2 = new Product("PRO2025-0002");
//        p2.setName("Vitamin C 1000mg");
//        p2.setShortName("VITC1000");
//        p2.setCategory(ProductCategory.SUPPLEMENT);
//        p2.setForm(DosageForm.TABLET);
//        p2.setManufacturer("HealthPlus");
//        p2.setActiveIngredient("Ascorbic Acid");
//        p2.setStrength("1000 mg");
//        p2.setVat(5);
//        p2.setBaseUnitOfMeasure("Viên");
//        p2.setBarcode("8930001112223");
//        p2.addUnitOfMeasure(new UnitOfMeasure("UOM-2", p2, "Lọ 100 viên", 100));
//        p2.addLot(new Lot("LOT-P2-A", p2, 200, 3000, LocalDateTime.now().plusMonths(12), LotStatus.AVAILABLE));
//
//        // P3
//        Product p3 = new Product("PRO2025-0003");
//        p3.setName("Nước muối sinh lý 0.9% 500ml");
//        p3.setShortName("SALINE500");
//        p3.setCategory(ProductCategory.OTC);
//        p3.setForm(DosageForm.SYRUP);
//        p3.setManufacturer("Medicare");
//        p3.setActiveIngredient("NaCl 0.9%");
//        p3.setStrength("500 ml");
//        p3.setVat(0);
//        p3.setBaseUnitOfMeasure("Chai");
//        p3.setBarcode("8934567890123");
//        p3.addUnitOfMeasure(new UnitOfMeasure("UOM-3", p3, "Thùng 12 chai", 12));
//        p3.addLot(new Lot("LOT-P3-A", p3, 120, 8000, LocalDateTime.now().plusMonths(8), LotStatus.AVAILABLE));
//
//        create(p1);
//        create(p2);
//        create(p3);
//    }
//}
