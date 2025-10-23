package com.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * TAB_Product — giữ nguyên cấu trúc, chỉ chỉnh màu & in đậm để đồng bộ với TAB_Promotion.
 *
 * pProduct (BorderLayout)
 *  ├─ NORTH : Toolbar (CENTER: search | EAST: filters + Export)
 *  └─ CENTER: JSplitPane
 *       ├─ left : List (JTable)
 *       └─ right: DetailRoot (BorderLayout)
 *            ├─ CENTER: JScrollPane(DetailContent)
 *            └─ SOUTH : ActionBar [Chỉnh sửa | Ngừng bán/Kích hoạt] (hoặc [Lưu | Hủy] khi edit)
 *
 * Ghi chú:
 *  - Chỉ đổi: màu chữ, màu button, màu tiêu đề bảng/section và font đậm ở tiêu đề/thead/button.
 *  - Mock data & behavior giữ nguyên.
 */
public class TAB_Product {

    // ================= THEME (đổi màu/đậm để giống TAB_Promotion) =================
    private static final Color COL_TEXT             = new Color(33, 37, 41);   // chữ chính
    private static final Color COL_MUTED_TEXT       = new Color(88, 96, 105);  // chữ phụ/nhãn
    private static final Color COL_PRIMARY          = new Color(30, 136, 229); // blue-600 (#1E88E5)
    private static final Color COL_PRIMARY_HOVER    = new Color(25, 118, 210); // blue-700 (#1976D2)
    private static final Color COL_SECONDARY        = new Color(230, 234, 240);// xám nhạt cho nút phụ/outline
    private static final Color COL_TITLE            = new Color(25, 118, 210); // tiêu đề section/bảng (giống TAB_Promotion)
    private static final Color COL_TABLE_HEADER_BG  = new Color(240, 247, 255);// nền header bảng (nhạt theo primary)
    private static final Color COL_TABLE_HEADER_FG  = new Color(25, 118, 210); // chữ header bảng
    private static final Color COL_TITLE_BORDER     = new Color(225, 232, 244); // viền nhẹ cho container/title
    private static final Color COL_PILL_BORDER      = new Color(210, 210, 210);

    private static final Font  FONT_TITLE_LG        = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font  FONT_TITLE_MD        = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font  FONT_TABLE_HEADER    = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_BUTTON          = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font  FONT_LABEL           = new Font("Segoe UI", Font.PLAIN, 12);

    // ================= Root =================
    public JPanel pProduct;

    // ================= Toolbar =================
    private JTextField txtSearch;
    private JComboBox<Category> cboCategory;
    private JComboBox<DosageForm> cboForm;
    private JTextField txtActiveIngredientFilter;
    private JButton btnExport;

    // ================= List (left) =================
    private JTable tblProducts;
    private ProductTableModel productModel;
    private TableRowSorter<ProductTableModel> sorter;

    // ================= Detail (right) =================
    private JPanel pDetailRoot;
    private JPanel pDetailContent;
    private JScrollPane scrDetail;

    // Row 0
    private JLabel lblImage;
    private JButton btnChangeImage;

    // Row 0 right fields
    private JTextField txtId, txtBarcode;
    private JComboBox<ProductStatus> cboStatus;
    private JComboBox<Category> cboCatDetail;

    // Rows 1..
    private JTextField txtName, txtShortName, txtStrength, txtActiveIngredient, txtVat, txtBaseUom;
    private JComboBox<DosageForm> cboFormDetail;
    private JTextArea txaDescription;

    // Units
    private JTable tblUnits;
    private UnitsModel unitsModel;
    private JButton btnAddUnit, btnDelUnit;

    // Lots
    private JTable tblLots;
    private LotsModel lotsModel;
    private JButton btnAddLot, btnDelLot;

    // ActionBar
    private final JButton btnPrimary   = new JButton("Chỉnh sửa");
    private final JButton btnSecondary = new JButton("Ngừng bán");

    // ================= State =================
    private boolean editing = false;
    private Product current;
    private final List<Product> data = new ArrayList<>();

    // ================= Const =================
    private static final String DEFAULT_IMG = "src/main/resources/images/products/etc/etc1.jpg";
    private static final int    IMG_W = 140, IMG_H = 140;

    public TAB_Product() {
        buildUI();
        loadMock();
        productModel.setRows(data);
        if (!data.isEmpty()) {
            tblProducts.setRowSelectionInterval(0, 0);
            loadDetail(getSelected());
        }
    }

    // ================= UI BUILD =================
    private void buildUI() {
        pProduct = new JPanel(new BorderLayout(10, 10));
        pProduct.setBorder(new EmptyBorder(10, 10, 10, 10));
        pProduct.add(buildToolbar(), BorderLayout.NORTH);
        pProduct.add(buildSplit(),   BorderLayout.CENTER);
        pProduct.setBackground(Color.WHITE);
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 4));
        bar.setBackground(Color.WHITE);

        // CENTER: Search (giữ như TAB_Promotion)
        txtSearch = new JTextField();
        txtSearch.setColumns(22);
        txtSearch.addActionListener(e -> applyFilter());
        JButton btnSearch = new JButton("Tìm");
        btnSearch.addActionListener(e -> applyFilter());
        stylePrimaryButton(btnSearch); // màu & đậm giống TAB_Promotion

        JPanel searchWrap = new JPanel(new BorderLayout(6, 0));
        searchWrap.add(txtSearch, BorderLayout.CENTER);
        searchWrap.add(btnSearch, BorderLayout.EAST);
        searchWrap.setOpaque(false);
        bar.add(searchWrap, BorderLayout.CENTER);

        // EAST: filters + Export
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        right.add(pill("Loại", (cboCategory = new JComboBox<>(Category.values()))));
        right.add(pill("Dạng", (cboForm = new JComboBox<>(DosageForm.values()))));
        txtActiveIngredientFilter = new JTextField(12);
        right.add(pill("Hoạt chất", txtActiveIngredientFilter));
        btnExport = new JButton("Xuất Excel");
        styleSecondaryButton(btnExport);
        right.add(btnExport);
        bar.add(right, BorderLayout.EAST);

        // listeners
        cboCategory.addItemListener(e -> { if (e.getStateChange()==ItemEvent.SELECTED) applyFilter(); });
        cboForm.addItemListener(e -> { if (e.getStateChange()==ItemEvent.SELECTED) applyFilter(); });
        txtActiveIngredientFilter.addActionListener(e -> applyFilter());

        return bar;
    }

    private JSplitPane buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildListPanel(), buildDetailRoot());
        split.setContinuousLayout(true);
        split.setResizeWeight(0.48);
        split.setBorder(null);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.48));
        return split;
    }

    private JComponent buildListPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(COL_TITLE_BORDER), new EmptyBorder(10, 10, 10, 10)));

        JLabel title = new JLabel("Danh sách sản phẩm");
        title.setFont(FONT_TITLE_LG);         // in đậm
        title.setForeground(COL_TITLE);       // màu tiêu đề bảng
        p.add(title, BorderLayout.NORTH);

        productModel = new ProductTableModel();
        tblProducts = new JTable(productModel);
        tblProducts.setRowHeight(26);
        tblProducts.setAutoCreateRowSorter(true);
        tblProducts.setForeground(COL_TEXT);
        tblProducts.setGridColor(new Color(235, 238, 245));
        tblProducts.setShowGrid(true);

        // Header style (màu & đậm)
        JTableHeader header = tblProducts.getTableHeader();
        header.setBackground(COL_TABLE_HEADER_BG);
        header.setForeground(COL_TABLE_HEADER_FG);
        header.setFont(FONT_TABLE_HEADER);
        header.setBorder(new MatteBorder(0, 0, 1, 0, COL_TITLE_BORDER));

        sorter = new TableRowSorter<>(productModel);
        tblProducts.setRowSorter(sorter);

        p.add(new JScrollPane(tblProducts), BorderLayout.CENTER);
        return p;
    }

    private JComponent buildDetailRoot() {
        pDetailRoot = new JPanel(new BorderLayout(8, 8));
        pDetailRoot.setBackground(Color.WHITE);
        pDetailRoot.setBorder(new CompoundBorder(new LineBorder(COL_TITLE_BORDER), new EmptyBorder(10, 10, 10, 10)));

        // SOUTH: ActionBar (style nút)
        pDetailRoot.add(buildActionBar(), BorderLayout.SOUTH);

        // CENTER: Scroll content
        pDetailContent = buildDetailContent();
        scrDetail = new JScrollPane(pDetailContent);
        scrDetail.getVerticalScrollBar().setUnitIncrement(16);
        pDetailRoot.add(scrDetail, BorderLayout.CENTER);

        setEditMode(false);
        return pDetailRoot;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bar.setOpaque(true);
        bar.setBackground(Color.WHITE);
        bar.setBorder(new MatteBorder(1, 0, 0, 0, COL_TITLE_BORDER));

        stylePrimaryButton(btnPrimary);     // đậm & màu primary
        styleTertiaryButton(btnSecondary);  // nút phụ (outline / nền xám nhạt)

        btnPrimary.addActionListener(e -> { if (!editing) enterEdit(); else saveAndExitEdit(); });
        btnSecondary.addActionListener(e -> { if (!editing) toggleStatus(); else cancelEdit(); });

        bar.add(btnPrimary);
        bar.add(btnSecondary);
        return bar;
    }

    private JPanel buildDetailContent() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Title (in đậm + màu)
        JLabel title = new JLabel("I. Chi tiết sản phẩm");
        title.setFont(FONT_TITLE_MD);
        title.setForeground(COL_TITLE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        root.add(title, gbc);

        // ===== Row 0: 2 cột =====
        // Left: image + button
        JPanel imgWrap = new JPanel(new BorderLayout(0, 6));
        imgWrap.setOpaque(false);
        lblImage = new JLabel(scaleIcon(DEFAULT_IMG, IMG_W, IMG_H));
        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        lblImage.setVerticalAlignment(SwingConstants.CENTER);
        btnChangeImage = new JButton("Đổi ảnh…");
        styleSecondaryButton(btnChangeImage);
        btnChangeImage.addActionListener(e -> changeImage());
        imgWrap.add(lblImage, BorderLayout.CENTER);
        imgWrap.add(btnChangeImage, BorderLayout.SOUTH);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
        root.add(imgWrap, gbc);

        // Right: 4 field Mã, Mã vạch, Tình trạng, Loại
        JPanel right4 = new JPanel(new GridLayout(4, 2, 6, 6));
        right4.setOpaque(false);
        right4.add(label("Mã:"));        txtId = roText();       right4.add(txtId);
        right4.add(label("Mã vạch:"));   txtBarcode = roText();  right4.add(txtBarcode);
        right4.add(label("Tình trạng:")); cboStatus = new JComboBox<>(ProductStatus.values()); cboStatus.setEnabled(false); right4.add(cboStatus);
        right4.add(label("Loại:"));      cboCatDetail = new JComboBox<>(Category.values());    cboCatDetail.setEnabled(false); right4.add(cboCatDetail);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.7;
        root.add(right4, gbc);

        // ===== Row 1..: các field khác =====
        JPanel row1 = new JPanel(new GridLayout(1, 4, 6, 6));
        row1.setOpaque(false);
        row1.add(label("Tên:"));           txtName = roText();       row1.add(txtName);
        row1.add(label("Tên viết tắt:"));  txtShortName = roText();  row1.add(txtShortName);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        root.add(row1, gbc);

        JPanel row2 = new JPanel(new GridLayout(1, 4, 6, 6));
        row2.setOpaque(false);
        row2.add(label("Dạng:"));          cboFormDetail = new JComboBox<>(DosageForm.values()); cboFormDetail.setEnabled(false); row2.add(cboFormDetail);
        row2.add(label("Hàm lượng:"));     txtStrength = roText();   row2.add(txtStrength);
        gbc.gridy = 3; root.add(row2, gbc);

        JPanel row3 = new JPanel(new GridLayout(1, 4, 6, 6));
        row3.setOpaque(false);
        row3.add(label("Hoạt chất:"));     txtActiveIngredient = roText(); row3.add(txtActiveIngredient);
        row3.add(label("VAT (%):"));       txtVat = roText();        row3.add(txtVat);
        gbc.gridy = 4; root.add(row3, gbc);

        JPanel row4 = new JPanel(new GridLayout(1, 4, 6, 6));
        row4.setOpaque(false);
        row4.add(label("Đơn vị cơ bản:")); txtBaseUom = roText();    row4.add(txtBaseUom);
        row4.add(new JLabel("")); row4.add(new JLabel(""));
        gbc.gridy = 5; root.add(row4, gbc);

        JPanel row5 = new JPanel(new BorderLayout(6, 6));
        row5.setOpaque(false);
        row5.add(label("Mô tả:"), BorderLayout.WEST);
        txaDescription = new JTextArea(3, 30);
        txaDescription.setLineWrap(true); txaDescription.setWrapStyleWord(true); txaDescription.setEditable(false);
        txaDescription.setForeground(COL_TEXT);
        row5.add(new JScrollPane(txaDescription), BorderLayout.CENTER);
        gbc.gridy = 6; root.add(row5, gbc);

        // ===== 2) Đơn vị quy đổi (title màu/đậm) =====
        JPanel unitsWrap = new JPanel(new BorderLayout(6, 6));
        unitsWrap.setOpaque(false);
        unitsWrap.setBorder(makeTitledBorder("2. Đơn vị quy đổi"));
        unitsModel = new UnitsModel();
        tblUnits = new JTable(unitsModel);
        decorateTable(tblUnits);
        ensureMinVisibleRows(tblUnits, 3);
        unitsWrap.add(new JScrollPane(tblUnits), BorderLayout.CENTER);
        JPanel unitsBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        unitsBtns.setOpaque(false);
        btnAddUnit = new JButton("+ Thêm");
        btnDelUnit = new JButton("Xóa");
        styleSecondaryButton(btnAddUnit);
        styleSecondaryButton(btnDelUnit);
        btnAddUnit.addActionListener(e -> { unitsModel.addRow(new UnitRow("", 1)); selectLastRowAndScroll(tblUnits); });
        btnDelUnit.addActionListener(e -> deleteSelected(tblUnits, unitsModel));
        unitsBtns.add(btnAddUnit); unitsBtns.add(btnDelUnit);
        unitsWrap.add(unitsBtns, BorderLayout.SOUTH);
        gbc.gridy = 7; root.add(unitsWrap, gbc);

        // ===== 3) Lô & HSD (title màu/đậm) =====
        JPanel lotsWrap = new JPanel(new BorderLayout(6, 6));
        lotsWrap.setOpaque(false);
        lotsWrap.setBorder(makeTitledBorder("3. Lô & hạn sử dụng (ưu tiên FEFO)"));
        lotsModel = new LotsModel();
        tblLots = new JTable(lotsModel);
        decorateTable(tblLots);
        ensureMinVisibleRows(tblLots, 3);
        lotsWrap.add(new JScrollPane(tblLots), BorderLayout.CENTER);
        JPanel lotsBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        lotsBtns.setOpaque(false);
        btnAddLot = new JButton("+ Thêm");
        btnDelLot = new JButton("Xóa");
        styleSecondaryButton(btnAddLot);
        styleSecondaryButton(btnDelLot);
        btnAddLot.addActionListener(e -> { lotsModel.addRow(new LotRow("NEW", 0, 0.0, LocalDate.now().plusMonths(6), "AVAILABLE")); selectLastRowAndScroll(tblLots); });
        btnDelLot.addActionListener(e -> deleteSelected(tblLots, lotsModel));
        lotsBtns.add(btnAddLot); lotsBtns.add(btnDelLot);
        lotsWrap.add(lotsBtns, BorderLayout.SOUTH);
        gbc.gridy = 8; root.add(lotsWrap, gbc);

        return root;
    }

    // ================= Actions & State =================
    private void enterEdit() { setEditMode(true); }

    private void saveAndExitEdit() {
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

            current.units.clear();
            current.units.addAll(unitsModel.rows);
            current.lots.clear();
            current.lots.addAll(lotsModel.rows);

            productModel.fireTableDataChanged();
        }
        setEditMode(false);
        JOptionPane.showMessageDialog(pProduct, "Đã lưu (mock).", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelEdit() {
        if (current != null) loadDetail(current);
        setEditMode(false);
    }

    private void toggleStatus() {
        if (current == null) return;
        current.status = (current.status == ProductStatus.DANG_BAN) ? ProductStatus.NGUNG_BAN : ProductStatus.DANG_BAN;
        cboStatus.setSelectedItem(current.status);
        if (!editing) btnSecondary.setText(secondaryLabelForStatus());
        productModel.fireTableDataChanged();
    }

    private void setEditMode(boolean on) {
        this.editing = on;
        // Fields
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

        // Units/Lots buttons
        btnAddUnit.setVisible(on);
        btnDelUnit.setVisible(on);
        btnAddLot.setVisible(on);
        btnDelLot.setVisible(on);

        unitsModel.setEditable(on);
        lotsModel.setEditable(on);

        // Buttons text
        btnPrimary.setText(on ? "Lưu" : "Chỉnh sửa");
        btnSecondary.setText(on ? "Hủy" : secondaryLabelForStatus());
    }

    private String secondaryLabelForStatus() {
        ProductStatus st = (ProductStatus) cboStatus.getSelectedItem();
        return (st == ProductStatus.DANG_BAN) ? "Ngừng bán" : "Kích hoạt";
    }

    private void applyFilter() {
        if (sorter == null) return;
        String q   = optLower(txtSearch.getText());
        Category c = (Category)   cboCategory.getSelectedItem();
        DosageForm f = (DosageForm) cboForm.getSelectedItem();
        String ai = optLower(txtActiveIngredientFilter.getText());

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends ProductTableModel, ? extends Integer> entry) {
                Product p = productModel.getAt(entry.getIdentifier());
                if (p == null) return false;
                boolean byQ  = q.isEmpty() ||
                        contains(p.id, q) || contains(p.name, q) || contains(p.barcode, q) || contains(p.shortName, q);
                boolean byC  = (c == null || c == Category.ALL) || p.category == c;
                boolean byF  = (f == null || f == DosageForm.ALL) || p.form == f;
                boolean byAI = ai.isEmpty() || contains(p.activeIngredient, ai);
                return byQ && byC && byF && byAI;
            }
        });
    }

    private static String optLower(String s){ return s==null? "": s.trim().toLowerCase(Locale.ROOT); }
    private static boolean contains(String hay, String needle){ return hay!=null && hay.toLowerCase(Locale.ROOT).contains(needle); }

    private void loadDetail(Product p) {
        current = p;
        if (p == null) return;

        lblImage.setIcon(scaleIcon(p.imagePath != null ? p.imagePath : DEFAULT_IMG, IMG_W, IMG_H));
        txtId.setText(p.id);
        txtBarcode.setText(ns(p.barcode));
        cboStatus.setSelectedItem(p.status);
        cboCatDetail.setSelectedItem(p.category);

        txtName.setText(ns(p.name));
        txtShortName.setText(ns(p.shortName));
        cboFormDetail.setSelectedItem(p.form);
        txtStrength.setText(ns(p.strength));
        txtActiveIngredient.setText(ns(p.activeIngredient));
        txtVat.setText(p.vat == null ? "" : String.valueOf(p.vat));
        txtBaseUom.setText(ns(p.baseUom));
        txaDescription.setText(ns(p.description));

        unitsModel.setRows(new ArrayList<>(p.units));
        lotsModel.setRows(new ArrayList<>(p.lots));

        setEditMode(false);
        btnSecondary.setText(secondaryLabelForStatus());
    }

    private Product getSelected() {
        int view = tblProducts.getSelectedRow();
        if (view < 0) return null;
        int modelIdx = tblProducts.convertRowIndexToModel(view);
        return productModel.getAt(modelIdx);
    }

    // ================= Helpers: UI =================
    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(COL_MUTED_TEXT);
        l.setFont(FONT_LABEL);
        return l;
    }

    private JTextField roText() {
        JTextField t = new JTextField();
        t.setEditable(false);
        t.setForeground(COL_TEXT);
        return t;
    }

    private JPanel pill(String label, Component comp) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        wrap.setOpaque(false);
        wrap.setBorder(new CompoundBorder(new LineBorder(COL_PILL_BORDER), new EmptyBorder(2, 8, 2, 8)));
        JLabel lb = new JLabel(label + ":");
        lb.setForeground(COL_MUTED_TEXT);
        lb.setFont(FONT_LABEL);
        wrap.add(lb);
        wrap.add(comp);
        if (comp instanceof JComponent jc) jc.setFont(FONT_LABEL);
        return wrap;
    }

    private void stylePrimaryButton(JButton b) {
        b.setFont(FONT_BUTTON); b.setForeground(Color.WHITE);
        b.setBackground(COL_PRIMARY); b.setOpaque(true); b.setFocusPainted(false);
        b.setBorder(new LineBorder(COL_PRIMARY, 1, true));
        b.addChangeListener(e -> {
            if (b.getModel().isRollover()) b.setBackground(COL_PRIMARY_HOVER);
            else b.setBackground(COL_PRIMARY);
        });
    }

    private void styleSecondaryButton(JButton b) {
        b.setFont(FONT_BUTTON); b.setForeground(COL_TEXT);
        b.setBackground(COL_SECONDARY); b.setOpaque(true); b.setFocusPainted(false);
        b.setBorder(new LineBorder(new Color(210, 214, 220), 1, true));
    }

    private void styleTertiaryButton(JButton b) {
        b.setFont(FONT_BUTTON);
        b.setForeground(COL_PRIMARY);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(COL_PRIMARY, 1, true));
        b.setOpaque(true);
        b.addChangeListener(e -> {
            if (b.getModel().isRollover()) b.setBackground(COL_TABLE_HEADER_BG);
            else b.setBackground(Color.WHITE);
        });
    }

    private void decorateTable(JTable t) {
        t.setForeground(COL_TEXT);
        t.setGridColor(new Color(235, 238, 245));
        JTableHeader h = t.getTableHeader();
        h.setBackground(COL_TABLE_HEADER_BG);
        h.setForeground(COL_TABLE_HEADER_FG);
        h.setFont(FONT_TABLE_HEADER);
        h.setBorder(new MatteBorder(0, 0, 1, 0, COL_TITLE_BORDER));
    }

    private TitledBorder makeTitledBorder(String title) {
        TitledBorder tb = new TitledBorder(new LineBorder(COL_TITLE_BORDER), title);
        tb.setTitleFont(FONT_TITLE_MD);
        tb.setTitleColor(COL_TITLE);
        return tb;
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
        t.setPreferredScrollableViewportSize(new Dimension(450, hh + rh * minRows + 6));
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
        if (m instanceof UnitsModel um) um.removeRow(modelRow);
        else if (m instanceof LotsModel lm) lm.removeRow(modelRow);
    }

    private ImageIcon scaleIcon(String path, int w, int h) {
        try {
            Image img = null;
            File f = new File(path);
            if (f.exists()) img = ImageIO.read(f);
            if (img == null) img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            Image fallback = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(fallback);
        }
    }

    private static String ns(String s){ return s==null? "": s; }
    private static Double parseDouble(String s){ try { return (s==null||s.isBlank())? null : Double.parseDouble(s.trim()); } catch(Exception e){ return null; } }

    // ================= Mock Data =================
    private void loadMock() {
        Product a = new Product("PRD-0001", "8801234567890", Category.OTC);
        a.name="Paracetamol 500mg"; a.shortName="Para500"; a.form=DosageForm.TABLET; a.strength="500 mg";
        a.activeIngredient="Paracetamol"; a.vat=8.0; a.baseUom="VIÊN"; a.description="Giảm đau, hạ sốt"; a.imagePath=DEFAULT_IMG;
        a.units.addAll(List.of(new UnitRow("VỈ (10 viên)", 10), new UnitRow("HỘP (10 vỉ)", 100)));
        a.lots.addAll(List.of(new LotRow("A01", 120, 1200.0, LocalDate.now().plusMonths(8), "AVAILABLE"),
                new LotRow("A02",  90, 1250.0, LocalDate.now().plusMonths(5), "AVAILABLE")));

        Product b = new Product("PRD-0002", "8930001234567", Category.SUPPLEMENT);
        b.name="Vitamin C 1000mg"; b.shortName="VITC1000"; b.form=DosageForm.LOZENGE; b.strength="1000 mg";
        b.activeIngredient="Ascorbic acid"; b.vat=5.0; b.baseUom="VIÊN"; b.description="Tăng sức đề kháng"; b.imagePath=DEFAULT_IMG;
        b.units.addAll(List.of(new UnitRow("LỌ (100 viên)", 100)));
        b.lots.addAll(List.of(new LotRow("B01", 30, 3500.0, LocalDate.now().plusMonths(2), "AVAILABLE")));
        b.status = ProductStatus.NGUNG_BAN;

        Product c = new Product("PRD-0003", "8939998887776", Category.ETC);
        c.name="Amoxicillin 500mg"; c.shortName="Amox500"; c.form=DosageForm.CAPSULE; c.strength="500 mg";
        c.activeIngredient="Amoxicillin"; c.vat=8.0; c.baseUom="VIÊN"; c.description="Kháng sinh"; c.imagePath=DEFAULT_IMG;
        c.units.addAll(List.of(new UnitRow("VỈ (10 viên)", 10), new UnitRow("HỘP (10 vỉ)", 100)));
        c.lots.add(new LotRow("C01", 25, 4200.0, LocalDate.now().plusMonths(1), "AVAILABLE"));

        data.addAll(List.of(a,b,c));
    }

    // ================= Table Models & DTO =================
    private static class ProductTableModel extends AbstractTableModel {
        private final String[] cols = {"Mã","Tên","Loại","Dạng","Hoạt chất","VAT(%)","Tình trạng"};
        private List<Product> rows = new ArrayList<>();
        void setRows(List<Product> list){ rows = list; fireTableDataChanged(); }
        Product getAt(int r){ return rows.get(r); }

        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Product p = rows.get(r);
            return switch (c) {
                case 0 -> p.id;
                case 1 -> p.name;
                case 2 -> p.category.name();
                case 3 -> p.form.name();
                case 4 -> p.activeIngredient;
                case 5 -> p.vat;
                case 6 -> (p.status == ProductStatus.DANG_BAN) ? "Đang bán" : "Ngừng bán";
                default -> "";
            };
        }
    }

    private static class UnitsModel extends AbstractTableModel {
        private final String[] cols = {"Đơn vị", "Quy đổi (so với đơn vị cơ bản)"};
        private final Class<?>[] types = {String.class, Integer.class};
        private boolean editable = false;
        private List<UnitRow> rows = new ArrayList<>();
        public void setEditable(boolean e){ editable=e; fireTableDataChanged(); }
        public void setRows(List<UnitRow> list){ rows = list; fireTableDataChanged(); }
        public void addRow(UnitRow r){ rows.add(r); fireTableRowsInserted(rows.size()-1, rows.size()-1); }
        public void removeRow(int idx){ if (idx>=0 && idx<rows.size()){ rows.remove(idx); fireTableRowsDeleted(idx, idx);} }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Class<?> getColumnClass(int c){ return types[c]; }
        @Override public boolean isCellEditable(int r, int c){ return editable; }
        @Override public Object getValueAt(int r, int c){ UnitRow u = rows.get(r); return c==0? u.name : (int)u.rate; }
        @Override public void setValueAt(Object v, int r, int c){
            if (!editable) return;
            UnitRow u = rows.get(r);
            if (c==0) u.name = Objects.toString(v, "");
            else {
                try { u.rate = Integer.parseInt(Objects.toString(v,"1")); }
                catch (Exception ignore){ u.rate = 1; }
            }
        }
    }

    private static class LotsModel extends AbstractTableModel {
        private final String[] cols = {"Lô", "Số lượng", "Giá cơ sở", "HSD", "Trạng thái"};
        private final Class<?>[] types = {String.class, Integer.class, Double.class, String.class, String.class};
        private boolean editable = false;
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private List<LotRow> rows = new ArrayList<>();
        public void setEditable(boolean e){ editable=e; fireTableDataChanged(); }
        public void setRows(List<LotRow> list){ rows = list; fireTableDataChanged(); }
        public void addRow(LotRow r){ rows.add(r); fireTableRowsInserted(rows.size()-1, rows.size()-1); }
        public void removeRow(int idx){ if (idx>=0 && idx<rows.size()){ rows.remove(idx); fireTableRowsDeleted(idx, idx);} }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Class<?> getColumnClass(int c){ return types[c]; }
        @Override public boolean isCellEditable(int r, int c){ return editable; }
        @Override public Object getValueAt(int r, int c) {
            LotRow l = rows.get(r);
            return switch (c) {
                case 0 -> l.batch;
                case 1 -> l.qty;
                case 2 -> l.basePrice;
                case 3 -> l.expiry!=null? l.expiry.format(fmt) : "";
                case 4 -> l.status;
                default -> "";
            };
        }
        @Override public void setValueAt(Object v, int r, int c){
            if (!editable) return;
            LotRow l = rows.get(r);
            switch (c) {
                case 0 -> l.batch = Objects.toString(v,"");
                case 1 -> { try { l.qty = Integer.parseInt(Objects.toString(v,"0")); } catch(Exception ignore){ l.qty=0; } }
                case 2 -> { try { l.basePrice = Double.parseDouble(Objects.toString(v,"0")); } catch(Exception ignore){ l.basePrice=0; } }
                case 3 -> { try { l.expiry = LocalDate.parse(Objects.toString(v,""), fmt); } catch(Exception ignore){ l.expiry=null; } }
                case 4 -> l.status = Objects.toString(v,"AVAILABLE");
            }
        }
    }

    // DTOs
    private static class Product {
        String id, barcode, name, shortName, activeIngredient, strength, baseUom, description, imagePath;
        Category category; DosageForm form = DosageForm.TABLET;
        Double vat; ProductStatus status = ProductStatus.DANG_BAN;
        final List<UnitRow> units = new ArrayList<>();
        final List<LotRow>  lots  = new ArrayList<>();
        Product(String id, String barcode, Category cat){ this.id=id; this.barcode=barcode; this.category=cat; }
    }
    private static class UnitRow {
        String name; double rate;
        UnitRow(String n, double r){ name=n; rate=r; }
    }
    private static class LotRow {
        String batch; int qty; double basePrice; LocalDate expiry; String status;
        LotRow(String b, int q, double p, LocalDate e, String s){ batch=b; qty=q; basePrice=p; expiry=e; status=s; }
    }

    // Enums
    private enum ProductStatus { DANG_BAN, NGUNG_BAN; @Override public String toString(){ return this==DANG_BAN? "Đang bán":"Ngừng bán"; } }
    private enum Category {
        ALL("Tất cả"), SUPPLEMENT("SUPPLEMENT"), OTC("OTC"), ETC("ETC");
        final String label; Category(String l){ label=l; } @Override public String toString(){ return label; }
    }
    private enum DosageForm {
        ALL("Tất cả"), TABLET("TABLET"), CAPSULE("CAPSULE"), POWDER("POWDER"),
        LOZENGE("LOZENGE"), SYRUP("SYRUP"), DROP("DROP"), MOUTHWASH("MOUTHWASH");
        final String label; DosageForm(String l){ label=l; } @Override public String toString(){ return label; }
    }
}
