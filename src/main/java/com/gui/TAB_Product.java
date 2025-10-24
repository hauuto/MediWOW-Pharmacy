package com.gui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * TAB_Product — List (left) + Detail (right) + Toolbar + ActionBar
 * Cấu trúc:
 * pProduct (BorderLayout)
 *  ├─ NORTH: Toolbar (Export + bộ lọc + search kiểu TAB_Promotion)
 *  └─ CENTER: JSplitPane
 *        ├─ leftPanel: List (JTable)
 *        └─ rightPanel: DetailRoot (BorderLayout)
 *              ├─ CENTER: JScrollPane(DetailContent) ← vertically scrollable
 *              └─ SOUTH : ActionBar [Chỉnh sửa | Ngừng bán/Kích hoạt]  (hoặc [Lưu | Hủy] khi edit)
 *
 * Ghi chú:
 *  - Giữ mock data
 *  - Loại & Dạng dùng enum; Hoạt chất (filter) là String (JTextField)
 *  - Bỏ Nhà sản xuất
 *  - FEFO luôn ưu tiên (không có checkbox)
 *  - Đơn vị quy đổi & Lô/HSD: độ cao tối thiểu 3 hàng; Ẩn nút Thêm/Xóa ở view mode; hiện khi edit
 */
public class TAB_Product {

    // ========== Root ==========
    public JPanel pProduct;

    // ========== Toolbar ==========
    private JTextField txtSearch;
    private JComboBox<Category> cboCategory;
    private JComboBox<DosageForm> cboForm;
    private JTextField txtActiveIngredientFilter;
    private JButton btnExport;

    // ========== List (left) ==========
    private JTable tblProducts;
    private ProductTableModel productModel;
    private TableRowSorter<ProductTableModel> sorter;

    // ========== Detail (right) CORE ==========
    private JPanel pDetailRoot;
    private JPanel pDetailContent;   // GridBag
    private JScrollPane scrDetail;

    // Row 0 - left
    private JLabel lblImage;
    private JButton btnChangeImage;

    // Row 0 - right 4 fields
    private JTextField txtId, txtBarcode;
    private JComboBox<ProductStatus> cboStatus;     // disabled (not editable)
    private JComboBox<Category> cboCatDetail;       // disabled in view; editable in edit

    // Rows 1.. other fields (no Manufacturer)
    private JTextField txtName, txtShortName, txtStrength, txtActiveIngredient, txtVat, txtBaseUom;
    private JComboBox<DosageForm> cboFormDetail;
    private JTextArea txaDescription;

    // Đơn vị quy đổi
    private JTable tblUnits;
    private UnitsModel unitsModel;
    private JButton btnAddUnit, btnDelUnit;

    // Lô & hạn sử dụng
    private JTable tblLots;
    private LotsModel lotsModel;
    private JButton btnAddLot, btnDelLot;

    // ActionBar (luôn hiển thị)
    private final JButton btnPrimary = new JButton("Chỉnh sửa");     // or "Lưu" when in edit mode
    private final JButton btnSecondary = new JButton("Ngừng bán");   // or "Kích hoạt" / "Hủy" (edit mode)

    // ========== State ==========
    private boolean editing = false;
    private Product current; // sản phẩm đang xem
    private final List<Product> data = new ArrayList<>(); // mock

    // ========== Constants ==========
    private static final String DEFAULT_IMG = "src/main/resources/images/products/otc/otc1.png";
    private static final int IMG_W = 140, IMG_H = 140;

    public TAB_Product() {
        buildUI();
        loadMock();
        productModel.setRows(data);
        if (!data.isEmpty()) {
            tblProducts.setRowSelectionInterval(0, 0);
            loadDetail(getSelected());
        }
    }

    // region ===== UI Build =====
    private void buildUI() {
        pProduct = new JPanel(new BorderLayout(10, 10));
        pProduct.setBorder(new EmptyBorder(10, 10, 10, 10));
        pProduct.add(buildToolbar(), BorderLayout.NORTH);
        pProduct.add(buildSplit(), BorderLayout.CENTER);
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 4));

        // left/center: search field (giữ phong cách như TAB_Promotion)
        txtSearch = new JTextField();
        txtSearch.setColumns(22);
        txtSearch.addActionListener(e -> applyFilter());
        JButton btnSearch = new JButton("Tìm");
        btnSearch.addActionListener(e -> applyFilter());

        JPanel searchWrap = new JPanel(new BorderLayout(6, 0));
        searchWrap.add(txtSearch, BorderLayout.CENTER);
        searchWrap.add(btnSearch, BorderLayout.EAST);
        bar.add(searchWrap, BorderLayout.CENTER);

        // right: filters & export
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.add(pill("Loại", (cboCategory = new JComboBox<>(Category.values()))));
        right.add(pill("Dạng", (cboForm = new JComboBox<>(DosageForm.values()))));
        // Hoạt chất: String placeholder
        txtActiveIngredientFilter = new JTextField(12);
        right.add(pill("Hoạt chất", txtActiveIngredientFilter));
        btnExport = new JButton("Xuất Excel");
        right.add(btnExport);
        bar.add(right, BorderLayout.EAST);

        // filter listeners
        cboCategory.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) applyFilter(); });
        cboForm.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) applyFilter(); });
        txtActiveIngredientFilter.addActionListener(e -> applyFilter());

        return bar;
    }

    private JSplitPane buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildListPanel(),
                buildDetailRoot());
        split.setContinuousLayout(true);
        split.setResizeWeight(0.48);
        split.setBorder(null);
        return split;
    }

    private JComponent buildListPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new CompoundBorder(new LineBorder(new Color(220,220,220)),
                new EmptyBorder(10,10,10,10)));
        JLabel title = new JLabel("Danh sách sản phẩm");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        p.add(title, BorderLayout.NORTH);

        productModel = new ProductTableModel();
        tblProducts = new JTable(productModel);
        tblProducts.setRowHeight(26);
        tblProducts.setAutoCreateRowSorter(true);

        sorter = new TableRowSorter<>(productModel);
        tblProducts.setRowSorter(sorter);

        tblProducts.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Product sel = getSelected();
                if (sel != null) loadDetail(sel);
            }
        });

        p.add(new JScrollPane(tblProducts), BorderLayout.CENTER);
        return p;
    }

    private JComponent buildDetailRoot() {
        pDetailRoot = new JPanel(new BorderLayout(8, 8));

        // Action bar (South) — tạo trước để tránh NPE khi setEditMode chỉnh text nút
        pDetailRoot.add(buildActionBar(), BorderLayout.SOUTH);

        // CENTER: scrollable content
        pDetailContent = buildDetailContent();
        scrDetail = new JScrollPane(pDetailContent);
        scrDetail.getVerticalScrollBar().setUnitIncrement(16);
        pDetailRoot.add(scrDetail, BorderLayout.CENTER);

        setEditMode(false);
        return pDetailRoot;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnPrimary.addActionListener(e -> {
            if (!editing) enterEdit();
            else saveAndExitEdit();
        });
        btnSecondary.addActionListener(e -> {
            if (!editing) toggleStatus();
            else cancelEdit();
        });
        bar.add(btnPrimary);
        bar.add(btnSecondary);
        return bar;
    }

    private JPanel buildDetailContent() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(new CompoundBorder(new LineBorder(new Color(220,220,220)),
                new EmptyBorder(10,10,10,10)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel title = new JLabel("I. Chi tiết sản phẩm");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        root.add(title, gbc);

        // ===== Row 0: 2 cột =====
        // Left: image + button
        JPanel imgWrap = new JPanel(new BorderLayout(0, 6));
        lblImage = new JLabel(scaleIcon(DEFAULT_IMG, IMG_W, IMG_H));
        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        lblImage.setVerticalAlignment(SwingConstants.CENTER);
        btnChangeImage = new JButton("Đổi ảnh…");
        btnChangeImage.addActionListener(e -> changeImage());
        imgWrap.add(lblImage, BorderLayout.CENTER);
        imgWrap.add(btnChangeImage, BorderLayout.SOUTH);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
        root.add(imgWrap, gbc);

        // Right: 4 field Mã, Mã vạch, Tình trạng, Loại
        JPanel right4 = new JPanel(new GridLayout(4, 2, 6, 6));
        right4.add(new JLabel("Mã:"));        txtId = roText();       right4.add(txtId);
        right4.add(new JLabel("Mã vạch:"));   txtBarcode = roText();  right4.add(txtBarcode);
        right4.add(new JLabel("Tình trạng:"));
        cboStatus = new JComboBox<>(ProductStatus.values()); cboStatus.setEnabled(false); right4.add(cboStatus);
        right4.add(new JLabel("Loại:"));
        cboCatDetail = new JComboBox<>(Category.values());    cboCatDetail.setEnabled(false); right4.add(cboCatDetail);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.7;
        root.add(right4, gbc);

        // ===== Row 1..: các field khác (không có Nhà SX) =====
        JPanel row1 = new JPanel(new GridLayout(1, 4, 6, 6));
        row1.add(new JLabel("Tên:"));         txtName = roText();       row1.add(txtName);
        row1.add(new JLabel("Tên viết tắt:")); txtShortName = roText(); row1.add(txtShortName);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        root.add(row1, gbc);

        JPanel row2 = new JPanel(new GridLayout(1, 4, 6, 6));
        row2.add(new JLabel("Dạng:"));
        cboFormDetail = new JComboBox<>(DosageForm.values()); cboFormDetail.setEnabled(false); row2.add(cboFormDetail);
        row2.add(new JLabel("Hàm lượng:"));   txtStrength = roText();   row2.add(txtStrength);
        gbc.gridy = 3; root.add(row2, gbc);

        JPanel row3 = new JPanel(new GridLayout(1, 4, 6, 6));
        row3.add(new JLabel("Hoạt chất:"));   txtActiveIngredient = roText(); row3.add(txtActiveIngredient);
        row3.add(new JLabel("VAT (%):"));     txtVat = roText();        row3.add(txtVat);
        gbc.gridy = 4; root.add(row3, gbc);

        JPanel row4 = new JPanel(new GridLayout(1, 4, 6, 6));
        row4.add(new JLabel("Đơn vị cơ bản:")); txtBaseUom = roText(); row4.add(txtBaseUom);
        row4.add(new JLabel("")); row4.add(new JLabel("")); // filler
        gbc.gridy = 5; root.add(row4, gbc);

        JPanel row5 = new JPanel(new BorderLayout(6, 6));
        row5.add(new JLabel("Mô tả:"), BorderLayout.WEST);
        txaDescription = new JTextArea(3, 30);
        txaDescription.setLineWrap(true); txaDescription.setWrapStyleWord(true); txaDescription.setEditable(false);
        row5.add(new JScrollPane(txaDescription), BorderLayout.CENTER);
        gbc.gridy = 6; root.add(row5, gbc);

        // --- Đơn vị quy đổi ---
        JPanel unitsWrap = new JPanel(new BorderLayout(6, 6));
        unitsWrap.setBorder(new TitledBorder("2. Đơn vị quy đổi"));
        unitsModel = new UnitsModel();
        tblUnits = new JTable(unitsModel);
        tblUnits.setRowHeight(26);
        ensureMinVisibleRows(tblUnits, 3);
        unitsWrap.add(new JScrollPane(tblUnits), BorderLayout.CENTER);
        JPanel unitsBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        btnAddUnit = new JButton("+ Thêm");
        btnDelUnit = new JButton("Xóa");
        btnAddUnit.addActionListener(e -> { unitsModel.addRow(new UnitRow("", 1)); selectLastRowAndScroll(tblUnits); });
        btnDelUnit.addActionListener(e -> deleteSelected(tblUnits, unitsModel));
        unitsBtns.add(btnAddUnit); unitsBtns.add(btnDelUnit);
        unitsWrap.add(unitsBtns, BorderLayout.SOUTH);
        gbc.gridy = 7; root.add(unitsWrap, gbc);

        // --- Lô & HSD ---
        JPanel lotsWrap = new JPanel(new BorderLayout(6, 6));
        lotsWrap.setBorder(new TitledBorder("3. Lô & hạn sử dụng"));
        lotsModel = new LotsModel();
        tblLots = new JTable(lotsModel);
        tblLots.setRowHeight(26);
        ensureMinVisibleRows(tblLots, 3);
        lotsWrap.add(new JScrollPane(tblLots), BorderLayout.CENTER);
        JPanel lotsBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        btnAddLot = new JButton("+ Thêm");
        btnDelLot = new JButton("Xóa");
        btnAddLot.addActionListener(e -> { lotsModel.addRow(new LotRow("NEW", 0, 0.0, LocalDate.now().plusMonths(6), "AVAILABLE")); selectLastRowAndScroll(tblLots); });
        btnDelLot.addActionListener(e -> deleteSelected(tblLots, lotsModel));
        lotsBtns.add(btnAddLot); lotsBtns.add(btnDelLot);
        lotsWrap.add(lotsBtns, BorderLayout.SOUTH);
        gbc.gridy = 8; root.add(lotsWrap, gbc);

        return root;
    }
    // endregion

    // region ===== Actions & State =====
    private void enterEdit() {
        setEditMode(true);
    }

    private void saveAndExitEdit() {
        // Demo: lưu ngược dữ liệu detail vào 'current' (mock only; bạn sẽ map sang entity thực tế)
        if (current != null) {
            current.barcode = txtBarcode.getText().trim();
            current.category = (Category) cboCatDetail.getSelectedItem();
            current.name = txtName.getText().trim();
            current.shortName = txtShortName.getText().trim();
            current.form = (DosageForm) cboFormDetail.getSelectedItem();
            current.strength = txtStrength.getText().trim();
            current.activeIngredient = txtActiveIngredient.getText().trim();
            current.vat = parseDouble(txtVat.getText());
            current.baseUom = txtBaseUom.getText().trim();
            current.description = txaDescription.getText();
            // units & lots from table models
            current.units.clear();
            for (UnitRow r : unitsModel.rows) if (!r.name.isBlank()) current.units.add(new UnitRow(r.name.trim(), (int)r.rate));
            current.lots.clear();
            current.lots.addAll(lotsModel.rows);
            // refresh list row view
            productModel.fireTableDataChanged();
        }
        setEditMode(false);
    }

    private void cancelEdit() {
        // revert UI from current product
        if (current != null) loadDetail(current);
        setEditMode(false);
    }

    private void toggleStatus() {
        if (current == null) return;
        if (current.status == ProductStatus.DANG_BAN) current.status = ProductStatus.NGUNG_BAN;
        else current.status = ProductStatus.DANG_BAN;
        cboStatus.setSelectedItem(current.status);
        updateSecondaryButton();
        productModel.fireTableDataChanged();
    }

    private void setEditMode(boolean on) {
        this.editing = on;
        // fields editable, trừ Mã & Tình trạng
        txtId.setEditable(false);
        cboStatus.setEnabled(false);

        txtBarcode.setEditable(on);
        cboCatDetail.setEnabled(on);
        txtName.setEditable(on);
        txtShortName.setEditable(on);
        cboFormDetail.setEnabled(on);
        txtStrength.setEditable(on);
        txtActiveIngredient.setEditable(on);
        txtVat.setEditable(on);
        txtBaseUom.setEditable(on);
        txaDescription.setEditable(on);
        btnChangeImage.setEnabled(on);

        // Units & Lots buttons
        btnAddUnit.setVisible(on);
        btnDelUnit.setVisible(on);
        btnAddLot.setVisible(on);
        btnDelLot.setVisible(on);

        // Table editability
        unitsModel.setEditable(on);
        lotsModel.setEditable(on);

        // ActionBar labels
        btnPrimary.setText(on ? "Lưu" : "Chỉnh sửa");
        btnSecondary.setText(on ? "Hủy" : secondaryLabelForStatus());
    }

    private void updateSecondaryButton() {
        if (!editing) btnSecondary.setText(secondaryLabelForStatus());
    }

    private String secondaryLabelForStatus() {
        ProductStatus st = (ProductStatus) cboStatus.getSelectedItem();
        return (st == ProductStatus.DANG_BAN) ? "Ngừng bán" : "Kích hoạt";
    }
    // endregion

    // region ===== Helpers =====
    private JTextField roText() {
        JTextField t = new JTextField();
        t.setEditable(false);
        return t;
    }

    private JPanel pill(String label, Component comp) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        wrap.setBorder(new CompoundBorder(new LineBorder(new Color(210,210,210)), new EmptyBorder(2,8,2,8)));
        wrap.add(new JLabel(label + ":"));
        wrap.add(comp);
        return wrap;
    }

    private void applyFilter() {
        if (sorter == null) return;
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        Category cat = (Category) cboCategory.getSelectedItem();
        DosageForm form = (DosageForm) cboForm.getSelectedItem();
        String ai = txtActiveIngredientFilter.getText() == null ? "" : txtActiveIngredientFilter.getText().trim().toLowerCase(Locale.ROOT);

        List<RowFilter<Object,Object>> filters = new ArrayList<>();
        if (!q.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(q), 0, 1)); // id or name
        }
        if (cat != null && cat != Category.ALL) {
            filters.add(RowFilter.regexFilter("^" + cat.name() + "$", 2)); // category col
        }
        if (form != null && form != DosageForm.ALL) {
            filters.add(RowFilter.regexFilter("^" + form.name() + "$", 3)); // form col
        }
        if (!ai.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(ai), 4)); // active ingredient col
        }
        if (filters.isEmpty()) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.andFilter(filters));
    }

    private void loadDetail(Product p) {
        current = p;
        if (p == null) return;

        // general
        lblImage.setIcon(scaleIcon(p.imagePath != null ? p.imagePath : DEFAULT_IMG, IMG_W, IMG_H));
        txtId.setText(p.id);
        txtBarcode.setText(nullSafe(p.barcode));
        cboStatus.setSelectedItem(p.status);
        cboCatDetail.setSelectedItem(p.category);

        // more fields
        txtName.setText(nullSafe(p.name));
        txtShortName.setText(nullSafe(p.shortName));
        cboFormDetail.setSelectedItem(p.form);
        txtStrength.setText(nullSafe(p.strength));
        txtActiveIngredient.setText(nullSafe(p.activeIngredient));
        txtVat.setText(p.vat == null ? "" : p.vat.toString());
        txtBaseUom.setText(nullSafe(p.baseUom));
        txaDescription.setText(nullSafe(p.description));

        // tables
        unitsModel.setRows(new ArrayList<>(p.units));
        lotsModel.setRows(new ArrayList<>(p.lots));

        setEditMode(false);
        updateSecondaryButton();
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    private Product getSelected() {
        int view = tblProducts.getSelectedRow();
        if (view < 0) return null;
        int modelIndex = tblProducts.convertRowIndexToModel(view);
        return productModel.getAt(modelIndex);
    }

    private void changeImage() {
        JFileChooser fc = new JFileChooser();
        int rs = fc.showOpenDialog(pProduct);
        if (rs == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f != null && f.exists()) {
                if (current != null) current.imagePath = f.getAbsolutePath();
                lblImage.setIcon(scaleIcon(f.getAbsolutePath(), IMG_W, IMG_H));
            }
        }
    }

    private static void ensureMinVisibleRows(JTable t, int minRows) {
        int rh = t.getRowHeight();
        int hh = t.getTableHeader() != null ? t.getTableHeader().getPreferredSize().height : 22;
        t.setPreferredScrollableViewportSize(new Dimension(t.getPreferredScrollableViewportSize().width, hh + rh * minRows + 4));
    }

    private static void selectLastRowAndScroll(JTable t) {
        int last = t.getRowCount() - 1;
        if (last >= 0) {
            t.setRowSelectionInterval(last, last);
            t.scrollRectToVisible(t.getCellRect(last, 0, true));
        }
    }

    private static void deleteSelected(JTable t, AbstractTableModel m) {
        int view = t.getSelectedRow();
        if (view < 0) return;
        int modelRow = t.convertRowIndexToModel(view);
        if (m instanceof UnitsModel um) {
            um.removeRow(modelRow);
        } else if (m instanceof LotsModel lm) {
            lm.removeRow(modelRow);
        }
    }

    private static Double parseDouble(String s) {
        try { return s == null || s.isBlank() ? null : Double.parseDouble(s.trim()); }
        catch (Exception ex) { return null; }
    }

    private ImageIcon scaleIcon(String path, int w, int h) {
        try {
            Image img = null;
            // 1) absolute/relative file
            File f = new File(path);
            if (f.exists()) {
                img = ImageIO.read(f);
            } else {
                // 2) try classpath from "src/main/resources/..."
                String cp = toResourcePath(path);
                var url = getClass().getResource(cp);
                if (url != null) img = ImageIO.read(url);
            }
            if (img == null) {
                // fallback transparent
                img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            }
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            Image tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(tmp);
        }
    }

    private static String toResourcePath(String fileLike) {
        // convert "src/main/resources/images/..." -> "/images/..."
        String norm = fileLike.replace('\\', '/');
        int idx = norm.indexOf("/resources/");
        if (idx >= 0) {
            return norm.substring(idx + "/resources".length());
        }
        if (!norm.startsWith("/")) return "/" + norm;
        return norm;
    }
    // endregion

    // region ===== Mock data =====
    private void loadMock() {
        // Sản phẩm A
        Product a = new Product("PRD-0001", "8801234567890", Category.OTC);
        a.name = "Paracetamol 500mg";
        a.shortName = "Para500";
        a.form = DosageForm.TABLET;
        a.strength = "500 mg";
        a.activeIngredient = "Paracetamol";
        a.vat = 8.0;
        a.baseUom = "VIÊN";
        a.description = "Giảm đau, hạ sốt";
        a.imagePath = DEFAULT_IMG;
        a.units.addAll(List.of(new UnitRow("VỈ (10 viên)", 10), new UnitRow("HỘP (10 vỉ)", 100)));
        a.lots.addAll(List.of(
                new LotRow("A01", 120, 1200.0, LocalDate.now().plusMonths(8), "AVAILABLE"),
                new LotRow("A02", 90, 1250.0, LocalDate.now().plusMonths(5), "AVAILABLE")
        ));

        // Sản phẩm B
        Product b = new Product("PRD-0002", "8930001234567", Category.SUPPLEMENT);
        b.name = "Vitamin C 1000mg";
        b.shortName = "VITC1000";
        b.form = DosageForm.LOZENGE;
        b.strength = "1000 mg";
        b.activeIngredient = "Ascorbic acid";
        b.vat = 5.0;
        b.baseUom = "VIÊN";
        b.description = "Tăng sức đề kháng";
        b.imagePath = DEFAULT_IMG;
        b.units.addAll(List.of(new UnitRow("LỌ (100 viên)", 100)));
        b.lots.addAll(List.of(
                new LotRow("B01", 30, 3500.0, LocalDate.now().plusMonths(2), "AVAILABLE"),
                new LotRow("B02", 0, 3600.0, LocalDate.now().minusDays(5), "EXPIRED")
        ));
        b.status = ProductStatus.NGUNG_BAN;

        // Sản phẩm C
        Product c = new Product("PRD-0003", "8939998887776", Category.ETC);
        c.name = "Amoxicillin 500mg";
        c.shortName = "Amox500";
        c.form = DosageForm.CAPSULE;
        c.strength = "500 mg";
        c.activeIngredient = "Amoxicillin";
        c.vat = 8.0;
        c.baseUom = "VIÊN";
        c.description = "Kháng sinh";
        c.imagePath = DEFAULT_IMG;
        c.units.addAll(List.of(new UnitRow("VỈ (10 viên)", 10), new UnitRow("HỘP (10 vỉ)", 100)));
        c.lots.add(new LotRow("C01", 25, 4200.0, LocalDate.now().plusMonths(1), "AVAILABLE"));

        data.addAll(List.of(a, b, c));
    }
    // endregion

    // region ===== Table models & DTOs =====
    private static class ProductTableModel extends AbstractTableModel {
        private final String[] cols = {"Mã","Tên","Loại","Dạng","Hoạt chất","VAT(%)","Tình trạng"};
        private List<Product> rows = new ArrayList<>();

        void setRows(List<Product> list) { rows = list; fireTableDataChanged(); }
        Product getAt(int r) { return rows.get(r); }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Product p = rows.get(r);
            return switch (c) {
                case 0 -> p.id;
                case 1 -> p.name;
                case 2 -> p.category.name();
                case 3 -> p.form.name();
                case 4 -> p.activeIngredient;
                case 5 -> p.vat;
                case 6 -> p.status == ProductStatus.DANG_BAN ? "Đang bán" : "Ngừng bán";
                default -> "";
            };
        }
    }

    private static class UnitsModel extends AbstractTableModel {
        private final String[] cols = {"Đơn vị", "Quy đổi (so với đơn vị cơ bản)"};
        private final Class<?>[] types = {String.class, Integer.class};
        private boolean editable = false;
        private List<UnitRow> rows = new ArrayList<>();

        public void setEditable(boolean e) { editable = e; fireTableDataChanged(); }
        public void setRows(List<UnitRow> list) { rows = list; fireTableDataChanged(); }
        public void addRow(UnitRow r) { rows.add(r); fireTableRowsInserted(rows.size()-1, rows.size()-1); }
        public void removeRow(int idx) { if (idx>=0 && idx<rows.size()) { rows.remove(idx); fireTableRowsDeleted(idx, idx); } }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int columnIndex) { return types[columnIndex]; }
        @Override public boolean isCellEditable(int r, int c) { return editable; }

        @Override
        public Object getValueAt(int r, int c) {
            UnitRow u = rows.get(r);
            return c==0 ? u.name : (int)u.rate;
        }
        @Override
        public void setValueAt(Object aValue, int r, int c) {
            if (!editable) return;
            UnitRow u = rows.get(r);
            if (c==0) u.name = Objects.toString(aValue, "");
            else u.rate = toInt(aValue);
        }
        private int toInt(Object v){ try { return v==null?1: Integer.parseInt(v.toString()); } catch(Exception e){ return 1; } }
    }

    private static class LotsModel extends AbstractTableModel {
        private final String[] cols = {"Lô", "Số lượng", "Giá cơ sở", "HSD", "Trạng thái"};
        private final Class<?>[] types = {String.class, Integer.class, Double.class, String.class, String.class};
        private boolean editable = false;
        private List<LotRow> rows = new ArrayList<>();
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        public void setEditable(boolean e) { editable = e; fireTableDataChanged(); }
        public void setRows(List<LotRow> list) { rows = list; fireTableDataChanged(); }
        public void addRow(LotRow r) { rows.add(r); fireTableRowsInserted(rows.size()-1, rows.size()-1); }
        public void removeRow(int idx) { if (idx>=0 && idx<rows.size()) { rows.remove(idx); fireTableRowsDeleted(idx, idx); } }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int columnIndex) { return types[columnIndex]; }
        @Override public boolean isCellEditable(int r, int c) { return editable; }

        @Override
        public Object getValueAt(int r, int c) {
            LotRow l = rows.get(r);
            return switch (c) {
                case 0 -> l.batch;
                case 1 -> l.qty;
                case 2 -> l.basePrice;
                case 3 -> l.expiry != null ? l.expiry.format(fmt) : "";
                case 4 -> l.status;
                default -> "";
            };
        }
        @Override
        public void setValueAt(Object aValue, int r, int c) {
            if (!editable) return;
            LotRow l = rows.get(r);
            switch (c) {
                case 0 -> l.batch = Objects.toString(aValue, "");
                case 1 -> l.qty = toInt(aValue);
                case 2 -> l.basePrice = toDouble(aValue);
                case 3 -> {
                    try { l.expiry = LocalDate.parse(Objects.toString(aValue,""), fmt); }
                    catch (Exception ignored) { l.expiry = null; }
                }
                case 4 -> l.status = Objects.toString(aValue, "AVAILABLE");
            }
        }
        private int toInt(Object v){ try { return v==null?0: Integer.parseInt(v.toString()); } catch(Exception e){ return 0; } }
        private double toDouble(Object v){ try { return v==null?0.0: Double.parseDouble(v.toString()); } catch(Exception e){ return 0.0; } }
    }

    // Lightweight DTOs for mock UI (độc lập với entity thật)
    private static class Product {
        String id, barcode;
        Category category;
        DosageForm form = DosageForm.TABLET;
        String name, shortName, strength, activeIngredient, baseUom, description, imagePath;
        Double vat;
        ProductStatus status = ProductStatus.DANG_BAN;
        final List<UnitRow> units = new ArrayList<>();
        final List<LotRow> lots = new ArrayList<>();

        Product(String id, String barcode, Category cat) {
            this.id = id; this.barcode = barcode; this.category = cat;
        }
    }
    private static class UnitRow {
        String name; double rate;
        UnitRow(String n, double r){ this.name=n; this.rate=r; }
    }
    private static class LotRow {
        String batch; int qty; double basePrice; LocalDate expiry; String status;
        LotRow(String b, int q, double price, LocalDate ex, String st){ batch=b; qty=q; basePrice=price; expiry=ex; status=st; }
    }

    // Enums
    private enum ProductStatus { DANG_BAN, NGUNG_BAN; @Override public String toString(){ return this==DANG_BAN? "Đang bán":"Ngừng bán"; } }
    private enum Category {
        ALL("Tất cả"), SUPPLEMENT("SUPPLEMENT"), OTC("OTC"), ETC("ETC");
        final String label; Category(String l){ label=l; }
        @Override public String toString(){ return label; }
    }
    private enum DosageForm {
        ALL("Tất cả"), TABLET("TABLET"), CAPSULE("CAPSULE"), POWDER("POWDER"),
        LOZENGE("LOZENGE"), SYRUP("SYRUP"), DROP("DROP"), MOUTHWASH("MOUTHWASH");
        final String label; DosageForm(String l){ label=l; }
        @Override public String toString(){ return label; }
    }
    // endregion
}
