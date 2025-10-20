package com.dao;

import com.entities.Product;
import com.enums.DosageForm;
import com.enums.ProductCategory;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProductDAO {
    private static final ProductDAO INSTANCE = new ProductDAO();
    public static ProductDAO getInstance() { return INSTANCE; }

    private final List<Product> store = new ArrayList<>();
    private final AtomicInteger seq = new AtomicInteger(0);
    private final String year = String.valueOf(LocalDate.now().getYear());

    private ProductDAO() { seed(); }

    private void seed() {
        if (!store.isEmpty()) return;
        create(new Product(null, "8938505970012", ProductCategory.OTC, DosageForm.TABLET,
                "Paracetamol 500mg", "PARA500", "Imexpharm", "Paracetamol", 5.0, "500mg",
                "Giảm đau, hạ sốt", "Viên"));
        create(new Product(null, "8938505970029", ProductCategory.SUPPLEMENT, DosageForm.TABLET,
                "Vitamin C 500mg", "VITC500", "Traphaco", "Vitamin C", 5.0, "500mg",
                "Bổ sung vitamin C", "Viên"));
        create(new Product(null, "8938505970036", ProductCategory.OTC, DosageForm.MOUTHWASH,
                "Nước muối sinh lý 0.9%", "SALINE", "Bidiphar", "NaCl", 5.0, "0.9%",
                "Súc họng, vệ sinh mũi/họng", "Chai"));
    }

    private String nextId() {
        int n = seq.incrementAndGet();
        return String.format("PRD%s-%04d", year, n);
    }

    // ---- CRUD như OOP cơ bản ----
    public synchronized Product create(Product p) {
        p.setId(nextId());
        store.add(p);
        return p;
    }

    public synchronized boolean update(Product p) {
        if (p == null || p.getId() == null) return false;
        int idx = indexOf(p.getId());
        if (idx < 0) return false;
        store.set(idx, p);
        return true;
    }

    public synchronized boolean setActive(String id, boolean active) {
        int idx = indexOf(id);
        if (idx < 0) return false;
        store.get(idx).setActive(active);
        return true;
    }

    public synchronized Optional<Product> findById(String id) {
        return store.stream().filter(x -> Objects.equals(x.getId(), id)).findFirst();
    }

    public synchronized List<Product> findAll() {
        return new ArrayList<>(store);
    }

    public synchronized List<Product> search(String keyword,
                                             ProductCategory category,
                                             DosageForm form,
                                             Boolean active) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        return store.stream().filter(p -> {
            boolean okKw = kw.isEmpty()
                    || (p.getId() != null && p.getId().toLowerCase().contains(kw))
                    || (p.getName() != null && p.getName().toLowerCase().contains(kw))
                    || (p.getManufacturer() != null && p.getManufacturer().toLowerCase().contains(kw))
                    || (p.getShortName() != null && p.getShortName().toLowerCase().contains(kw));
            boolean okCat = (category == null) || category == p.getCategory();
            boolean okForm = (form == null) || form == p.getForm();
            boolean okActive = (active == null) || active == p.isActive();
            return okKw && okCat && okForm && okActive;
        }).collect(Collectors.toList());
    }

    private int indexOf(String id) {
        for (int i = 0; i < store.size(); i++)
            if (Objects.equals(store.get(i).getId(), id)) return i;
        return -1;
    }
}
