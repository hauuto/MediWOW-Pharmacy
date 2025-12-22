package com.utils;

import com.bus.BUS_Product;
import com.entities.Product;
import com.enums.ProductCategory;
import com.enums.DosageForm;

import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for simple Product IO operations used by TAB_Product.
 * - addProduct: delegate to BUS layer with domain validations & duplicate checks
 * - exportProductsTableToCSV: dump current JTable model to CSV (UTF-8)
 * - importProductsFromCSV: basic CSV import (UTF-8) with header row
 */
public class ProductIO {
    private final BUS_Product bus = new BUS_Product();

    /** Add a product using business validations. */
    public boolean addProduct(Product p) {
        return bus.addProduct(p);
    }

    /** Export a JTable model (left product list) to CSV; returns rows exported (excluding header). */
    public int exportProductsTableToCSV(DefaultTableModel model, File file) throws Exception {
        if (model == null || file == null) return 0;
        StringBuilder sb = new StringBuilder(1024);
        // Header
        for (int c = 0; c < model.getColumnCount(); c++) {
            sb.append(escapeCsv(String.valueOf(model.getColumnName(c))));
            if (c < model.getColumnCount() - 1) sb.append(',');
        }
        sb.append('\n');
        // Rows
        for (int r = 0; r < model.getRowCount(); r++) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                Object val = model.getValueAt(r, c);
                sb.append(escapeCsv(val == null ? "" : String.valueOf(val)));
                if (c < model.getColumnCount() - 1) sb.append(',');
            }
            sb.append('\n');
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
        return model.getRowCount();
    }

    /** Import products from a CSV file. Returns number of products successfully imported. */
    public int importProductsFromCSV(File file) throws Exception {
        if (file == null || !file.exists()) throw new FileNotFoundException("File không tồn tại");
        int count = 0; List<String[]> rows = readCsv(file);
        // Expect header with at least: name, barcode, category, form, activeIngredient, manufacturer, strength, description, vat
        int start = 0;
        if (!rows.isEmpty()) start = 1; // skip header
        for (int i = start; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (r.length < 9) continue;
            Product p = new Product();
            p.setName(n(r, 0));
            p.setBarcode(n(r, 1));
            p.setCategory(parseCategory(n(r, 2)));
            p.setForm(parseForm(n(r, 3)));
            p.setActiveIngredient(n(r, 4));
            p.setManufacturer(n(r, 5));
            p.setStrength(n(r, 6));
            p.setDescription(n(r, 7));
            double vat = 5.0; try { vat = Double.parseDouble(n(r, 8)); } catch (Exception ignore) {}
            p.setVat(vat);
            try {
                if (bus.addProduct(p)) count++;
            } catch (Exception ignore) {
                // skip invalid rows
            }
        }
        return count;
    }

    private List<String[]> readCsv(File file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line; boolean inQuotes = false; StringBuilder cur = new StringBuilder(); List<String> cols = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);
                    if (ch == '"') {
                        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                        else { inQuotes = !inQuotes; }
                    } else if (ch == ',' && !inQuotes) {
                        cols.add(cur.toString()); cur.setLength(0);
                    } else {
                        cur.append(ch);
                    }
                }
                if (inQuotes) { cur.append('\n'); continue; }
                cols.add(cur.toString()); cur.setLength(0);
                rows.add(cols.toArray(new String[0])); cols.clear();
            }
        }
        return rows;
    }

    private ProductCategory parseCategory(String s) {
        if (s == null) return ProductCategory.OTC;
        String l = s.trim().toLowerCase();
        if (l.contains("không kê đơn") || l.equals("otc")) return ProductCategory.OTC;
        if (l.contains("kê đơn") || l.equals("etc")) return ProductCategory.ETC;
        if (l.contains("chức năng") || l.equals("supplement")) return ProductCategory.SUPPLEMENT;
        return ProductCategory.OTC;
    }
    private DosageForm parseForm(String s) {
        if (s == null) return DosageForm.SOLID;
        String l = s.trim().toLowerCase();
        if (l.contains("viên") || l.contains("bột") || l.contains("kẹo") || l.equals("solid")) return DosageForm.SOLID;
        if (l.contains("si rô") || l.contains("siro") || l.contains("nhỏ giọt") || l.contains("súc miệng") || l.equals("liquid_dosage")) return DosageForm.LIQUID_DOSAGE;
        return DosageForm.SOLID;
    }

    private String n(String[] arr, int i) { return (i >= 0 && i < arr.length && arr[i] != null) ? arr[i].trim() : ""; }

    private String escapeCsv(String s) {
        boolean needQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String escaped = s.replace("\"", "\"\"");
        return needQuotes ? ("\"" + escaped + "\"") : escaped;
    }
}
