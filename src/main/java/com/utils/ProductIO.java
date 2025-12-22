package com.utils;

import com.bus.BUS_Product;
import com.entities.Lot;
import com.entities.MeasurementName;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.enums.DosageForm;
import com.enums.LotStatus;
import com.enums.ProductCategory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Helper for Product Excel IO operations used by TAB_Product.
 *
 * Excel template (XLSX) structure:
 *  - Sheet 1: "Sản phẩm"  (one row per product)
 *  - Sheet 2: "UOM"       (one row per unit-of-measure record)
 *  - Sheet 3: "Lot"       (one row per lot record)
 */
public class ProductIO {
    public static final String SHEET_PRODUCTS = "Sản phẩm";
    public static final String SHEET_UOM = "UOM";
    public static final String SHEET_LOT = "Lot";

    private final BUS_Product bus = new BUS_Product();

    /** Add a product using business validations. */
    public boolean addProduct(Product p) {
        return bus.addProduct(p);
    }

    /**
     * Export products (full details) to an Excel file with 3 sheets.
     * @return number of products exported
     */
    public int exportProductsToExcel(File file) throws Exception {
        if (file == null) return 0;

        List<Product> products = bus.getAllProducts();
        if (products == null) products = Collections.emptyList();

        try (Workbook wb = new XSSFWorkbook()) {
            CreationHelper helper = wb.getCreationHelper();

            // Styles
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle textStyle = createTextStyle(wb);
            CellStyle intStyle = createIntStyle(wb);
            CellStyle decimalStyle = createDecimalStyle(wb);
            CellStyle dateStyle = createDateStyle(wb, helper);

            // ===== Sheet 1: Products =====
            Sheet sProd = wb.createSheet(SHEET_PRODUCTS);
            int r = 0;
            Row h = sProd.createRow(r++);
            String[] prodHeaders = new String[]{
                    "Mã", "Tên", "Tên viết tắt", "Mã vạch", "Loại", "Dạng", "Hoạt chất", "Nhà sản xuất", "Hàm lượng", "VAT", "ĐVT gốc", "Mô tả"
            };
            for (int c = 0; c < prodHeaders.length; c++) {
                Cell cell = h.createCell(c);
                cell.setCellValue(prodHeaders[c]);
                cell.setCellStyle(headerStyle);
            }

            for (Product p : products) {
                Row row = sProd.createRow(r++);
                int c = 0;
                putText(row, c++, safe(p.getId()), textStyle);
                putText(row, c++, safe(p.getName()), textStyle);
                putText(row, c++, safe(p.getShortName()), textStyle);
                putText(row, c++, safe(p.getBarcode()), textStyle);
                putText(row, c++, p.getCategory() != null ? p.getCategory().name() : "", textStyle);
                putText(row, c++, p.getForm() != null ? p.getForm().name() : "", textStyle);
                putText(row, c++, safe(p.getActiveIngredient()), textStyle);
                putText(row, c++, safe(p.getManufacturer()), textStyle);
                putText(row, c++, safe(p.getStrength()), textStyle);

                BigDecimal vat = (p.getVat() != null) ? p.getVat() : BigDecimal.ZERO;
                Cell vatCell = row.createCell(c++);
                vatCell.setCellValue(vat.doubleValue());
                vatCell.setCellStyle(decimalStyle);

                putText(row, c++, safe(p.getBaseUnitOfMeasure()), textStyle);
                putText(row, c++, safe(p.getDescription()), textStyle);
            }
            autosize(sProd, prodHeaders.length);

            // ===== Sheet 2: UOM =====
            Sheet sUom = wb.createSheet(SHEET_UOM);
            r = 0;
            Row hu = sUom.createRow(r++);
            String[] uomHeaders = new String[]{
                    "Mã vạch", "Tên sản phẩm", "MeasurementId", "Tên ĐV", "Quy đổi về ĐV gốc", "Giá"
            };
            for (int c = 0; c < uomHeaders.length; c++) {
                Cell cell = hu.createCell(c);
                cell.setCellValue(uomHeaders[c]);
                cell.setCellStyle(headerStyle);
            }

            for (Product p : products) {
                Set<UnitOfMeasure> uoms = p.getUnitOfMeasureSet();
                if (uoms == null) continue;
                for (UnitOfMeasure u : uoms) {
                    if (u == null) continue;
                    Row row = sUom.createRow(r++);
                    int c = 0;
                    putText(row, c++, safe(p.getBarcode()), textStyle);
                    putText(row, c++, safe(p.getName()), textStyle);

                    Integer measurementId = (u.getMeasurement() != null) ? u.getMeasurement().getId() : null;
                    Cell idCell = row.createCell(c++);
                    if (measurementId != null) {
                        idCell.setCellValue(measurementId);
                        idCell.setCellStyle(intStyle);
                    } else {
                        idCell.setBlank();
                        idCell.setCellStyle(textStyle);
                    }

                    putText(row, c++, (u.getMeasurement() != null) ? safe(u.getMeasurement().getName()) : "", textStyle);

                    BigDecimal rate = (u.getBaseUnitConversionRate() != null) ? u.getBaseUnitConversionRate() : BigDecimal.ONE;
                    Cell rateCell = row.createCell(c++);
                    rateCell.setCellValue(rate.doubleValue());
                    rateCell.setCellStyle(decimalStyle);

                    BigDecimal price = (u.getPrice() != null) ? u.getPrice() : BigDecimal.ZERO;
                    Cell priceCell = row.createCell(c++);
                    priceCell.setCellValue(price.doubleValue());
                    priceCell.setCellStyle(decimalStyle);
                }
            }
            autosize(sUom, uomHeaders.length);

            // ===== Sheet 3: Lot =====
            Sheet sLot = wb.createSheet(SHEET_LOT);
            r = 0;
            Row hl = sLot.createRow(r++);
            String[] lotHeaders = new String[]{
                    "Mã vạch", "Tên sản phẩm", "LotId", "Mã lô", "Số lượng", "Giá (ĐV gốc)", "HSD", "Tình trạng"
            };
            for (int c = 0; c < lotHeaders.length; c++) {
                Cell cell = hl.createCell(c);
                cell.setCellValue(lotHeaders[c]);
                cell.setCellStyle(headerStyle);
            }

            for (Product p : products) {
                Set<Lot> lots = p.getLotSet();
                if (lots == null) continue;
                for (Lot lot : lots) {
                    if (lot == null) continue;
                    Row row = sLot.createRow(r++);
                    int c = 0;
                    putText(row, c++, safe(p.getBarcode()), textStyle);
                    putText(row, c++, safe(p.getName()), textStyle);
                    putText(row, c++, safe(lot.getId()), textStyle);
                    putText(row, c++, safe(lot.getBatchNumber()), textStyle);

                    Cell qtyCell = row.createCell(c++);
                    qtyCell.setCellValue(lot.getQuantity());
                    qtyCell.setCellStyle(intStyle);

                    BigDecimal price = (lot.getRawPrice() != null) ? lot.getRawPrice() : BigDecimal.ZERO;
                    Cell priceCell = row.createCell(c++);
                    priceCell.setCellValue(price.doubleValue());
                    priceCell.setCellStyle(decimalStyle);

                    Cell dCell = row.createCell(c++);
                    LocalDateTime exp = lot.getExpiryDate();
                    if (exp != null) {
                        dCell.setCellValue(java.sql.Timestamp.valueOf(exp));
                        dCell.setCellStyle(dateStyle);
                    } else {
                        dCell.setBlank();
                        dCell.setCellStyle(textStyle);
                    }

                    putText(row, c++, lot.getStatus() != null ? lot.getStatus().name() : LotStatus.AVAILABLE.name(), textStyle);
                }
            }
            autosize(sLot, lotHeaders.length);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        return products.size();
    }

    /**
     * Import products (plus UOM/Lot) from an Excel file (3 sheets).
     * Returns number of products successfully imported.
     */
    public int importProductsFromExcel(File file) throws Exception {
        if (file == null || !file.exists()) throw new java.io.FileNotFoundException("File không tồn tại");

        Map<String, Product> byKey = new LinkedHashMap<>();
        Map<String, List<UomRow>> uomsByKey = new HashMap<>();
        Map<String, List<LotRow>> lotsByKey = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(file); Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sProd = sheetByNameOrFirst(wb, SHEET_PRODUCTS, 0);
            Sheet sUom = sheetByNameOrFirst(wb, SHEET_UOM, 1);
            Sheet sLot = sheetByNameOrFirst(wb, SHEET_LOT, 2);

            // ---- read products ----
            if (sProd != null) {
                int first = sProd.getFirstRowNum();
                int last = sProd.getLastRowNum();
                for (int r = first + 1; r <= last; r++) {
                    Row row = sProd.getRow(r);
                    if (row == null) continue;

                    String name = getString(row, 1);
                    String barcode = getString(row, 3);
                    if (isBlank(name) && isBlank(barcode)) continue;

                    Product p = new Product();
                    // ID (ignored for add; but keep if user wants it; BUS/DAO may generate)
                    String id = getString(row, 0);
                    if (!isBlank(id)) {
                        // Product has no setter for id; keep only for display; ignore.
                    }

                    p.setName(name);
                    p.setShortName(blankToNull(getString(row, 2)));
                    p.setBarcode(barcode);
                    p.setCategory(parseCategory(getString(row, 4)));
                    p.setForm(parseForm(getString(row, 5)));
                    p.setActiveIngredient(blankToNull(getString(row, 6)));
                    p.setManufacturer(blankToNull(getString(row, 7)));
                    p.setStrength(blankToNull(getString(row, 8)));

                    BigDecimal vat = getDecimal(row, 9);
                    if (vat == null) vat = BigDecimal.valueOf(5.0);
                    p.setVat(vat.doubleValue());

                    p.setBaseUnitOfMeasure(getString(row, 10));
                    p.setDescription(blankToNull(getString(row, 11)));

                    String key = productKey(barcode, name);
                    byKey.put(key, p);
                }
            }

            // ---- read UOM ----
            if (sUom != null) {
                int first = sUom.getFirstRowNum();
                int last = sUom.getLastRowNum();
                for (int r = first + 1; r <= last; r++) {
                    Row row = sUom.getRow(r);
                    if (row == null) continue;

                    String barcode = getString(row, 0);
                    String productName = getString(row, 1);
                    String uomName = getString(row, 3);
                    if (isBlank(uomName)) continue;

                    BigDecimal rate = getDecimal(row, 4);
                    if (rate == null) rate = BigDecimal.ONE;
                    if (rate.compareTo(BigDecimal.ONE) < 0) rate = BigDecimal.ONE;

                    BigDecimal price = getDecimal(row, 5);
                    if (price == null) price = BigDecimal.ZERO;

                    Integer measurementId = getInteger(row, 2);
                    String key = productKey(barcode, productName);

                    UomRow ur = new UomRow(measurementId, uomName, rate, price);
                    uomsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(ur);
                }
            }

            // ---- read Lot ----
            if (sLot != null) {
                int first = sLot.getFirstRowNum();
                int last = sLot.getLastRowNum();
                for (int r = first + 1; r <= last; r++) {
                    Row row = sLot.getRow(r);
                    if (row == null) continue;

                    String barcode = getString(row, 0);
                    String productName = getString(row, 1);
                    String lotId = getString(row, 2);
                    String batch = getString(row, 3);
                    if (isBlank(batch)) continue;

                    Integer qty = getInteger(row, 4);
                    if (qty == null) qty = 0;
                    if (qty < 0) qty = 0;

                    BigDecimal rawPrice = getDecimal(row, 5);
                    if (rawPrice == null) rawPrice = BigDecimal.ZERO;

                    LocalDateTime expiry = getDateTime(row, 6);

                    LotStatus st = parseLotStatus(getString(row, 7));

                    String key = productKey(barcode, productName);
                    LotRow lr = new LotRow(lotId, batch, qty, rawPrice, expiry, st);
                    lotsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(lr);
                }
            }
        }

        // ---- build + add products ----
        int imported = 0;
        for (Map.Entry<String, Product> e : byKey.entrySet()) {
            String key = e.getKey();
            Product p = e.getValue();

            // UOMs
            Set<UnitOfMeasure> uomSet = new HashSet<>();
            List<UomRow> uomRows = uomsByKey.getOrDefault(key, Collections.emptyList());
            Set<String> seenUom = new HashSet<>();
            for (UomRow ur : uomRows) {
                if (ur == null || isBlank(ur.name)) continue;
                String nrm = ur.name.trim().toLowerCase(Locale.ROOT);
                if (!seenUom.add(nrm)) continue;

                MeasurementName mn;
                if (ur.measurementId != null) {
                    // Try to resolve by id from cached list; fallback to create by name
                    mn = findMeasurementById(ur.measurementId);
                    if (mn == null) mn = bus.getOrCreateMeasurementName(ur.name);
                } else {
                    mn = bus.getOrCreateMeasurementName(ur.name);
                }

                BigDecimal rate = (ur.baseUnitRate != null) ? ur.baseUnitRate : BigDecimal.ONE;
                if (rate.compareTo(BigDecimal.ONE) < 0) rate = BigDecimal.ONE;
                UnitOfMeasure u = new UnitOfMeasure(p, mn, nzMoney(ur.price), rate);
                u.setProduct(p);
                uomSet.add(u);
            }
            p.setUnitOfMeasureSet(uomSet);

            // Lots
            Set<Lot> lotSet = new HashSet<>();
            List<LotRow> lotRows = lotsByKey.getOrDefault(key, Collections.emptyList());
            for (LotRow lr : lotRows) {
                if (lr == null || isBlank(lr.batchNumber)) continue;
                String idToUse = !isBlank(lr.id) ? lr.id : UUID.randomUUID().toString();
                Lot lot = new Lot(idToUse, lr.batchNumber.trim(), p, Math.max(0, lr.quantity), nzMoney(lr.rawPrice), lr.expiryDate, lr.status);
                lotSet.add(lot);
            }
            p.setLotSet(lotSet);

            try {
                // BUS requires at least 1 lot when adding
                if (bus.addProduct(p)) imported++;
            } catch (Exception ignore) {
                // Skip invalid/duplicate rows
            }
        }

        return imported;
    }

    /**
     * Export an empty Excel template (with headers + example rows) for easier import.
     *
     * Sheets:
     *  - Sản phẩm: required fields example (barcode/name/category/form/base UOM)
     *  - UOM: map by product barcode or name
     *  - Lot: at least 1 lot is required for add-product validation
     */
    public void exportExcelTemplate(File file) throws Exception {
        if (file == null) return;

        try (Workbook wb = new XSSFWorkbook()) {
            CreationHelper helper = wb.getCreationHelper();

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle textStyle = createTextStyle(wb);
            CellStyle intStyle = createIntStyle(wb);
            CellStyle decimalStyle = createDecimalStyle(wb);
            CellStyle dateStyle = createDateStyle(wb, helper);

            // ===== Sheet 1: Products =====
            Sheet sProd = wb.createSheet(SHEET_PRODUCTS);
            int r = 0;
            Row h = sProd.createRow(r++);
            String[] prodHeaders = new String[]{
                    "Mã", "Tên", "Tên viết tắt", "Mã vạch", "Loại", "Dạng", "Hoạt chất", "Nhà sản xuất", "Hàm lượng", "VAT", "ĐVT gốc", "Mô tả"
            };
            for (int c = 0; c < prodHeaders.length; c++) {
                Cell cell = h.createCell(c);
                cell.setCellValue(prodHeaders[c]);
                cell.setCellStyle(headerStyle);
            }

            // Example row (keep 'Mã' empty, because addProduct usually generates it)
            Row ex1 = sProd.createRow(r++);
            int c = 0;
            putText(ex1, c++, "", textStyle); // Mã
            putText(ex1, c++, "Paracetamol 500mg", textStyle);
            putText(ex1, c++, "Para 500", textStyle);
            putText(ex1, c++, "8938505974192", textStyle); // digits only
            putText(ex1, c++, "OTC", textStyle); // OTC/ETC/SUPPLEMENT
            putText(ex1, c++, "SOLID", textStyle); // SOLID/LIQUID_DOSAGE
            putText(ex1, c++, "Paracetamol", textStyle);
            putText(ex1, c++, "Example Pharma", textStyle);
            putText(ex1, c++, "500mg", textStyle);
            Cell vatCell = ex1.createCell(c++);
            vatCell.setCellValue(5.0);
            vatCell.setCellStyle(decimalStyle);
            putText(ex1, c++, "Viên", textStyle); // base UOM name
            putText(ex1, c++, "Dòng ví dụ - có thể xóa", textStyle);

            autosize(sProd, prodHeaders.length);

            // ===== Sheet 2: UOM =====
            Sheet sUom = wb.createSheet(SHEET_UOM);
            r = 0;
            Row hu = sUom.createRow(r++);
            String[] uomHeaders = new String[]{
                    "Mã vạch", "Tên sản phẩm", "MeasurementId", "Tên ĐV", "Quy đổi về ĐV gốc", "Giá"
            };
            for (int i = 0; i < uomHeaders.length; i++) {
                Cell cell = hu.createCell(i);
                cell.setCellValue(uomHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Example UOM rows for the example product
            Row u1 = sUom.createRow(r++);
            c = 0;
            putText(u1, c++, "8938505974192", textStyle);
            putText(u1, c++, "Paracetamol 500mg", textStyle);
            Cell midCell = u1.createCell(c++);
            midCell.setBlank();
            midCell.setCellStyle(textStyle);
            putText(u1, c++, "Viên", textStyle);
            Cell rateCell = u1.createCell(c++);
            rateCell.setCellValue(1);
            rateCell.setCellStyle(intStyle);
            Cell priceCell = u1.createCell(c++);
            priceCell.setCellValue(0);
            priceCell.setCellStyle(decimalStyle);

            Row u2 = sUom.createRow(r++);
            c = 0;
            putText(u2, c++, "8938505974192", textStyle);
            putText(u2, c++, "Paracetamol 500mg", textStyle);
            Cell mid2 = u2.createCell(c++);
            mid2.setBlank();
            mid2.setCellStyle(textStyle);
            putText(u2, c++, "Vỉ", textStyle);
            Cell rate2 = u2.createCell(c++);
            rate2.setCellValue(10);
            rate2.setCellStyle(intStyle);
            Cell price2 = u2.createCell(c++);
            price2.setCellValue(0);
            price2.setCellStyle(decimalStyle);

            autosize(sUom, uomHeaders.length);

            // ===== Sheet 3: Lot =====
            Sheet sLot = wb.createSheet(SHEET_LOT);
            r = 0;
            Row hl = sLot.createRow(r++);
            String[] lotHeaders = new String[]{
                    "Mã vạch", "Tên sản phẩm", "LotId", "Mã lô", "Số lượng", "Giá (ĐV gốc)", "HSD", "Tình trạng"
            };
            for (int i = 0; i < lotHeaders.length; i++) {
                Cell cell = hl.createCell(i);
                cell.setCellValue(lotHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Example lot row (at least 1 lot is required by BUS when adding)
            Row l1 = sLot.createRow(r++);
            c = 0;
            putText(l1, c++, "8938505974192", textStyle);
            putText(l1, c++, "Paracetamol 500mg", textStyle);
            putText(l1, c++, "", textStyle); // LotId can be empty, system will generate
            putText(l1, c++, "LOT-001", textStyle);
            Cell qty = l1.createCell(c++);
            qty.setCellValue(100);
            qty.setCellStyle(intStyle);
            Cell raw = l1.createCell(c++);
            raw.setCellValue(1500.0);
            raw.setCellStyle(decimalStyle);
            Cell hsd = l1.createCell(c++);
            // put a date value in Excel
            LocalDate exampleDate = LocalDate.now().plusMonths(12);
            hsd.setCellValue(java.sql.Date.valueOf(exampleDate));
            hsd.setCellStyle(dateStyle);
            putText(l1, c++, "AVAILABLE", textStyle); // AVAILABLE/EXPIRED/FAULTY

            autosize(sLot, lotHeaders.length);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    /**
     * Legacy: keep CSV methods for backward compatibility if some other code still calls them.
     * TAB_Product will be updated to use Excel.
     */
    @Deprecated
    public int exportProductsTableToCSV(DefaultTableModel model, File file) throws Exception {
        // Keep old behavior by delegating to Excel export of full data.
        return exportProductsToExcel(file);
    }

    @Deprecated
    public int importProductsFromCSV(File file) throws Exception {
        return importProductsFromExcel(file);
    }

    // ===================== Excel helpers =====================

    private static void autosize(Sheet s, int cols) {
        for (int c = 0; c < cols; c++) {
            try { s.autoSizeColumn(c); } catch (Exception ignore) {}
        }
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        st.setFont(f);
        st.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setBorderBottom(BorderStyle.THIN);
        st.setBorderTop(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
        return st;
    }

    private static CellStyle createTextStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        st.setWrapText(true);
        return st;
    }

    private static CellStyle createIntStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        DataFormat fmt = wb.createDataFormat();
        st.setDataFormat(fmt.getFormat("0"));
        return st;
    }

    private static CellStyle createDecimalStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        DataFormat fmt = wb.createDataFormat();
        st.setDataFormat(fmt.getFormat("0.00"));
        return st;
    }

    private static CellStyle createDateStyle(Workbook wb, CreationHelper helper) {
        CellStyle st = wb.createCellStyle();
        st.setDataFormat(helper.createDataFormat().getFormat("dd/MM/yyyy"));
        return st;
    }

    private static void putText(Row row, int col, String val, CellStyle st) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val == null ? "" : val);
        if (st != null) cell.setCellStyle(st);
    }

    private static String safe(Object v) { return v == null ? "" : String.valueOf(v).trim(); }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String blankToNull(String s) {
        String t = (s == null) ? "" : s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String productKey(String barcode, String name) {
        String bc = (barcode == null) ? "" : barcode.trim();
        if (!bc.isEmpty()) return "bc:" + bc;
        String nm = (name == null) ? "" : name.trim().toLowerCase(Locale.ROOT);
        return "name:" + nm;
    }

    private static Sheet sheetByNameOrFirst(Workbook wb, String name, int fallbackIndex) {
        if (wb == null) return null;
        Sheet s = wb.getSheet(name);
        if (s != null) return s;
        if (fallbackIndex >= 0 && fallbackIndex < wb.getNumberOfSheets()) return wb.getSheetAt(fallbackIndex);
        return null;
    }

    private static String getString(Row row, int col) {
        if (row == null) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        try {
            CellType t = cell.getCellType();
            if (t == CellType.STRING) return cell.getStringCellValue().trim();
            if (t == CellType.NUMERIC) {
                double d = cell.getNumericCellValue();
                // keep integer-looking numbers without ".0" (useful for barcode)
                if (Math.floor(d) == d) return String.valueOf((long) d);
                return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            }
            if (t == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
            if (t == CellType.FORMULA) {
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception ignore) {
                    try {
                        double d = cell.getNumericCellValue();
                        if (Math.floor(d) == d) return String.valueOf((long) d);
                        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
                    } catch (Exception ignore2) {
                        return "";
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return "";
    }

    private static Integer getInteger(Row row, int col) {
        String s = getString(row, col);
        if (isBlank(s)) return null;
        try {
            s = s.replaceAll("\\s", "");
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal getDecimal(Row row, int col) {
        if (row == null) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
            }
        } catch (Exception ignore) {}
        String s = getString(row, col);
        if (isBlank(s)) return null;
        try {
            String t = s;
            if (t.contains(",") && !t.contains(".")) t = t.replace(",", ".");
            t = t.replaceAll("(?<=\\d)[,\\.](?=\\d{3}(\\D|$))", "");
            return new BigDecimal(t).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime getDateTime(Row row, int col) {
        if (row == null) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date d = cell.getDateCellValue();
                Instant ins = d.toInstant();
                return LocalDateTime.ofInstant(ins, ZoneId.systemDefault());
            }
        } catch (Exception ignore) {}

        String s = getString(row, col);
        if (isBlank(s)) return null;
        // Accept dd/MM/yyyy and dd/MM/yy
        String[] ps = {"dd/MM/yy", "d/M/yy", "dd/MM/yyyy", "d/M/yyyy"};
        for (String p : ps) {
            try {
                LocalDate ld = LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern(p));
                return ld.atStartOfDay();
            } catch (DateTimeParseException ignore) {}
        }
        return null;
    }

    private ProductCategory parseCategory(String s) {
        if (s == null) return ProductCategory.OTC;
        String l = s.trim().toLowerCase(Locale.ROOT);
        if (l.contains("không kê đơn") || l.equals("otc")) return ProductCategory.OTC;
        if (l.contains("kê đơn") || l.equals("etc")) return ProductCategory.ETC;
        if (l.contains("chức năng") || l.equals("supplement")) return ProductCategory.SUPPLEMENT;
        // Accept enum names
        try { return ProductCategory.valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (Exception ignore) {}
        return ProductCategory.OTC;
    }

    private DosageForm parseForm(String s) {
        if (s == null) return DosageForm.SOLID;
        String l = s.trim().toLowerCase(Locale.ROOT);
        if (l.contains("viên") || l.contains("bột") || l.contains("kẹo") || l.equals("solid")) return DosageForm.SOLID;
        if (l.contains("si rô") || l.contains("siro") || l.contains("nhỏ giọt") || l.contains("súc miệng") || l.equals("liquid_dosage")) return DosageForm.LIQUID_DOSAGE;
        try { return DosageForm.valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (Exception ignore) {}
        return DosageForm.SOLID;
    }

    private static LotStatus parseLotStatus(String s) {
        if (s == null) return LotStatus.AVAILABLE;
        String l = s.trim().toLowerCase(Locale.ROOT);
        if (l.contains("hết hạn") || l.equals("expired")) return LotStatus.EXPIRED;
        if (l.contains("lỗi") || l.equals("faulty")) return LotStatus.FAULTY;
        try { return LotStatus.valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (Exception ignore) {}
        return LotStatus.AVAILABLE;
    }

    private MeasurementName findMeasurementById(Integer id) {
        if (id == null) return null;
        try {
            List<MeasurementName> all = bus.getAllMeasurementNames();
            if (all == null) return null;
            for (MeasurementName mn : all) {
                if (mn != null && id.equals(mn.getId())) return mn;
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static BigDecimal nzMoney(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    // ===== internal row models =====
    private record UomRow(Integer measurementId, String name, BigDecimal baseUnitRate, BigDecimal price) {}

    private record LotRow(String id, String batchNumber, int quantity, BigDecimal rawPrice, LocalDateTime expiryDate, LotStatus status) {}
}
