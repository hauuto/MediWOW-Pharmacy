package com.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TAB_Product — SplitView: Danh sách (trái) + Chi tiết (phải)
 * - Bố cục, trải nghiệm bám TAB_Promotion
 * - Trường dữ liệu theo OOAD Product/UnitOfMeasure/Lot
 * - FEFO mặc định cho bảng Lô, cảnh báo cận HSD 30 ngày
 *
 * Gắn vào GUI_MainMenu bằng product.pProduct như hiện tại.
 */
public class TAB_Product {
    // ===== Root giữ nguyên để tương thích GUI_MainMenu =====
    public JPanel pProduct;

    // Toolbar
    private JTextField txtSearch;
    private JComboBox<ProductCategory> cboCateFilter;
    private JComboBox<DosageForm>      cboFormFilter;
    private JButton btnAdd, btnUpdate, btnDelete, btnRefresh, btnExport;

    // List (left)
    private JTable tblProducts;
    private ProductsTableModel productsModel;
    private List<ProductRow> allProducts = new ArrayList<>(); // mock storage

    // Detail (right)
    private JTextField txtId, txtBarcode, txtName, txtShort, txtManufacturer, txtActiveIng, txtStrength, txtBaseUom, txtImagePath, txtCreatedAt;
    private JTextArea  txaDesc;
    private JFormattedTextField fVat;
    private JComboBox<ProductCategory> cboCategory;
    private JComboBox<DosageForm>      cboForm;

    // Sub tables: UOM & LOT
    private JTable tblUoms, tblLots;
    private UomsTableModel uomsModel;
    private LotsTableModel lotsModel;
    private JButton btnAddUom, btnDelUom, btnAddLot, btnDelLot;
    private JCheckBox chkFefo;

    private final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TAB_Product(){
        buildUI();
        mockData();         // demo data
        applyFilter("");    // load list
    }

    // ================= Build UI =================
    private void buildUI(){
        pProduct = new JPanel(new BorderLayout(12, 12));
        pProduct.setBorder(new EmptyBorder(10, 10, 10, 10));

        pProduct.add(buildToolbar(), BorderLayout.NORTH);
        pProduct.add(buildSplitView(), BorderLayout.CENTER);
    }

    private JComponent buildToolbar(){
        JPanel bar = new JPanel(new BorderLayout(8, 4));

        txtSearch = new JTextField();
        txtSearch.setFont(txtSearch.getFont().deriveFont(16f));
        txtSearch.setToolTipText("Tìm: mã / mã vạch / tên / hoạt chất / NSX");
        JButton btnFind = new JButton("Tìm");
        btnFind.addActionListener(e -> applyFilter(txtSearch.getText()));

        JPanel searchWrap = new JPanel(new BorderLayout(4, 0));
        searchWrap.add(txtSearch, BorderLayout.CENTER);
        searchWrap.add(btnFind, BorderLayout.EAST);

        // right controls
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        cboCateFilter = new JComboBox<>(ProductCategory.values());
        cboFormFilter = new JComboBox<>(DosageForm.values());
        cboCateFilter.insertItemAt(null, 0);
        cboFormFilter.insertItemAt(null, 0);
        cboCateFilter.setSelectedIndex(0);
        cboFormFilter.setSelectedIndex(0);
        Action filterAction = new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ applyFilter(txtSearch.getText()); }};
        cboCateFilter.addActionListener(filterAction);
        cboFormFilter.addActionListener(filterAction);

        right.add(pill("Loại", cboCateFilter));
        right.add(pill("Dạng", cboFormFilter));

        btnAdd = new JButton("Thêm");
        btnUpdate = new JButton("Cập nhật");
        btnDelete = new JButton("Xoá");
        btnRefresh= new JButton("Làm mới");
        btnExport = new JButton("Xuất Excel");

        right.add(btnAdd); right.add(btnUpdate); right.add(btnDelete);
        right.add(btnRefresh); right.add(btnExport);

        bar.add(searchWrap, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);

        // actions (mock)
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            cboCateFilter.setSelectedIndex(0);
            cboFormFilter.setSelectedIndex(0);
            applyFilter("");
        });

        return bar;
    }

    private JPanel pill(String label, JComponent comp){
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        wrap.setBorder(new CompoundBorder(new LineBorder(new Color(210,210,210)), new EmptyBorder(2,8,2,8)));
        wrap.add(new JLabel(label + ":"));
        wrap.add(comp);
        return wrap;
    }

    private JSplitPane buildSplitView(){
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildListPanel(), buildDetailPanel());
        split.setContinuousLayout(true);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        return split;
    }

    private JPanel buildListPanel(){
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new CompoundBorder(new LineBorder(new Color(220,220,220)),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel title = new JLabel("Danh sách sản phẩm");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        p.add(title, BorderLayout.NORTH);

        productsModel = new ProductsTableModel();
        tblProducts = new JTable(productsModel);
        tblProducts.setAutoCreateRowSorter(true);
        tblProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblProducts.setRowHeight(26);

        tblProducts.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblProducts.getSelectedRow() >= 0) {
                ProductRow pr = getSelectedProduct();
                loadDetail(pr);
            }
        });

        // column widths
        int[] widths = {100, 120, 220, 110, 110, 70, 110, 140, 120};
        for (int i=0;i<widths.length && i<tblProducts.getColumnCount();i++){
            tblProducts.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        p.add(new JScrollPane(tblProducts), BorderLayout.CENTER);
        return p;
    }

    private ProductRow getSelectedProduct(){
        int view = tblProducts.getSelectedRow();
        if (view < 0) return null;
        int modelRow = tblProducts.convertRowIndexToModel(view);
        return productsModel.getRowAt(modelRow);
    }

    private JComponent buildDetailPanel(){
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new CompoundBorder(new LineBorder(new Color(220,220,220)),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel title = new JLabel("Chi tiết sản phẩm");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(6));

        // ===== Thông tin chung =====
        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(new TitledBorder("Thông tin chung"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,6,4,6);
        g.fill = GridBagConstraints.HORIZONTAL;

        txtId = tf(); txtId.setEditable(false);
        txtBarcode = tf(); txtName = tf(); txtShort = tf();
        txtManufacturer = tf(); txtActiveIng = tf(); txtStrength = tf(); txtBaseUom = tf();
        txtImagePath = tf(); txtCreatedAt = tf(); txtCreatedAt.setEditable(false);
        txaDesc = new JTextArea(2, 30); txaDesc.setLineWrap(true); txaDesc.setWrapStyleWord(true);
        fVat = new JFormattedTextField(java.text.NumberFormat.getNumberInstance());
        fVat.setColumns(10);

        cboCategory = new JComboBox<>(ProductCategory.values());
        cboForm     = new JComboBox<>(DosageForm.values());

        // Row 1: ID - Barcode
        add(info, g, 0,0, new JLabel("Mã *")); add(info, g, 1,0, txtId);
        add(info, g, 2,0, new JLabel("Mã vạch")); add(info, g, 3,0, txtBarcode);

        // Row 2: Name
        add(info, g, 0,1, new JLabel("Tên *")); addSpan(info, g, 1,1, 3, txtName);

        // Row 3: Short - BaseUom
        add(info, g, 0,2, new JLabel("Tên tắt")); add(info, g, 1,2, txtShort);
        add(info, g, 2,2, new JLabel("ĐVT cơ sở")); add(info, g, 3,2, txtBaseUom);

        // Row 4: Category - Form
        add(info, g, 0,3, new JLabel("Loại *")); add(info, g, 1,3, cboCategory);
        add(info, g, 2,3, new JLabel("Dạng *")); add(info, g, 3,3, cboForm);

        // Row 5: VAT - Strength
        add(info, g, 0,4, new JLabel("VAT (%)")); add(info, g, 1,4, fVat);
        add(info, g, 2,4, new JLabel("Hàm lượng")); add(info, g, 3,4, txtStrength);

        // Row 6: Manufacturer - ActiveIngredient
        add(info, g, 0,5, new JLabel("Nhà SX")); add(info, g, 1,5, txtManufacturer);
        add(info, g, 2,5, new JLabel("Hoạt chất")); add(info, g, 3,5, txtActiveIng);

        // Row 7: Desc
        add(info, g, 0,6, new JLabel("Mô tả")); addSpan(info, g, 1,6, 3, new JScrollPane(txaDesc));

        // Row 8: Image - CreatedAt
        add(info, g, 0,7, new JLabel("Ảnh")); add(info, g, 1,7, txtImagePath);
        add(info, g, 2,7, new JLabel("Ngày tạo")); add(info, g, 3,7, txtCreatedAt);

        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(info);
        root.add(Box.createVerticalStrut(6));

        // ===== UOM =====
        uomsModel = new UomsTableModel();
        tblUoms = new JTable(uomsModel);
        tblUoms.setRowHeight(26);
        JPanel uWrap = new JPanel(new BorderLayout(6,6));
        uWrap.setBorder(new TitledBorder("Đơn vị quy đổi"));
        uWrap.add(new JScrollPane(tblUoms), BorderLayout.CENTER);
        JPanel uBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        btnAddUom = new JButton("+ Thêm"); btnDelUom = new JButton("Xoá");
        uBtns.add(btnAddUom); uBtns.add(btnDelUom);
        uWrap.add(uBtns, BorderLayout.SOUTH);
        btnAddUom.addActionListener(e -> { uomsModel.addRow(new UomRow(genId("UOM"), "", 1d)); selectLastRow(tblUoms); });
        btnDelUom.addActionListener(e -> deleteSelected(tblUoms, uomsModel));
        uWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(uWrap);
        root.add(Box.createVerticalStrut(6));

        // ===== LOTS =====
        lotsModel = new LotsTableModel();
        tblLots = new JTable(lotsModel){
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col){
                Component c = super.prepareRenderer(r, row, col);
                int mRow = convertRowIndexToModel(row);
                LotRow lr = lotsModel.getRowAt(mRow);
                c.setForeground(Color.BLACK);
                c.setBackground(Color.WHITE);
                if (lr.status == LotStatus.EXPIRED) c.setBackground(new Color(0xFFCDD2));          // đỏ nhạt
                else if (lr.status == LotStatus.FAULTY) c.setBackground(new Color(0xF8BBD0));     // hồng
                else if (lr.expiryDate != null && !lr.expiryDate.isAfter(LocalDate.now().plusDays(30)))
                    c.setBackground(new Color(0xE1BEE7)); // tím: cận HSD 30 ngày
                return c;
            }
        };
        tblLots.setRowHeight(26);
        // FEFO: sort theo HSD asc
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(lotsModel);
        tblLots.setRowSorter(sorter);
        chkFefo = new JCheckBox("Ưu tiên FEFO (HSD gần nhất lên trước)", true);
        chkFefo.addActionListener(e -> {
            if (chkFefo.isSelected()){
                sorter.setSortKeys(List.of(new RowSorter.SortKey(3, SortOrder.ASCENDING)));
            } else sorter.setSortKeys(null);
        });
        chkFefo.doClick(); chkFefo.doClick(); // ensure sort applied once

        JPanel lWrap = new JPanel(new BorderLayout(6,6));
        lWrap.setBorder(new TitledBorder("Lô & hạn sử dụng"));
        lWrap.add(new JScrollPane(tblLots), BorderLayout.CENTER);
        JPanel lSouth = new JPanel(new BorderLayout());
        lSouth.add(chkFefo, BorderLayout.WEST);
        JPanel lBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        btnAddLot = new JButton("+ Thêm lô"); btnDelLot = new JButton("Xoá lô");
        lBtns.add(btnAddLot); lBtns.add(btnDelLot);
        lSouth.add(lBtns, BorderLayout.EAST);
        lWrap.add(lSouth, BorderLayout.SOUTH);
        btnAddLot.addActionListener(e -> {
            lotsModel.addRow(new LotRow(genId("LOT"), 0, 0d, LocalDate.now().plusMonths(12), LotStatus.AVAILABLE));
            selectLastRow(tblLots);
        });
        btnDelLot.addActionListener(e -> deleteSelected(tblLots, lotsModel));

        lWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(lWrap);

        return root;
    }

    private JTextField tf(){ JTextField t = new JTextField(); t.setFont(t.getFont().deriveFont(14f)); return t; }
    private void add(JPanel p, GridBagConstraints g, int x, int y, JComponent c){ g.gridx=x; g.gridy=y; g.gridwidth=1; g.weightx = (x%2==1)?1:0; p.add(c, g); }
    private void addSpan(JPanel p, GridBagConstraints g, int x, int y, int span, JComponent c){ g.gridx=x; g.gridy=y; g.gridwidth=span; g.weightx=1; p.add(c, g); }
    private void selectLastRow(JTable t){ int last=t.getRowCount()-1; if (last>=0) t.setRowSelectionInterval(last, last); }
    private void deleteSelected(JTable t, AbstractTableModel m){
        int viewRow = t.getSelectedRow(); if (viewRow<0) return;
        int modelRow = t.convertRowIndexToModel(viewRow);
        if (m instanceof UomsTableModel um) um.removeRow(modelRow);
        if (m instanceof LotsTableModel lm) lm.removeRow(modelRow);
    }
    private String genId(String prefix){ return prefix + "-" + UUID.randomUUID().toString().substring(0,8).toUpperCase(); }

    // ================= Filtering & Binding =================
    private void applyFilter(String q){
        String query = q==null? "": q.trim().toLowerCase();
        ProductCategory cate = (ProductCategory)cboCateFilter.getSelectedItem();
        DosageForm form = (DosageForm)cboFormFilter.getSelectedItem();

        List<ProductRow> filtered = allProducts.stream().filter(p -> {
            boolean ok = true;
            if (cate != null) ok &= p.category == cate;
            if (form != null) ok &= p.form == form;
            if (!query.isEmpty()){
                ok &= (contains(p.id, query) || contains(p.barcode, query) || contains(p.name, query)
                        || contains(p.activeIngredient, query) || contains(p.manufacturer, query));
            }
            return ok;
        }).collect(Collectors.toList());

        productsModel.setData(filtered);
        if (!filtered.isEmpty()){
            tblProducts.setRowSelectionInterval(0,0);
            loadDetail(filtered.get(0));
        } else {
            clearDetail();
        }
    }
    private boolean contains(String s, String q){ return s!=null && s.toLowerCase().contains(q); }

    private void loadDetail(ProductRow p){
        if (p==null) return;
        txtId.setText(p.id); txtBarcode.setText(p.barcode);
        txtName.setText(p.name); txtShort.setText(p.shortName);
        txtManufacturer.setText(p.manufacturer); txtActiveIng.setText(p.activeIngredient);
        fVat.setValue(p.vat); txtStrength.setText(p.strength);
        txaDesc.setText(p.description); txtBaseUom.setText(p.baseUom);
        txtImagePath.setText(p.imagePath); txtCreatedAt.setText(p.createdAt!=null? p.createdAt.format(DMY):"");
        cboCategory.setSelectedItem(p.category); cboForm.setSelectedItem(p.form);

        uomsModel.setData(new ArrayList<>(p.uoms));
        lotsModel.setData(new ArrayList<>(p.lots));
    }

    private void clearDetail(){
        for (JTextField tf : List.of(txtId, txtBarcode, txtName, txtShort, txtManufacturer, txtActiveIng, txtStrength, txtBaseUom, txtImagePath, txtCreatedAt)) tf.setText("");
        txaDesc.setText(""); fVat.setValue(null);
        cboCategory.setSelectedIndex(0); cboForm.setSelectedIndex(0);
        uomsModel.setData(new ArrayList<>()); lotsModel.setData(new ArrayList<>());
    }

    // ================== Mock data (demo) ==================
    private void mockData(){
        ProductRow a = new ProductRow("PRD-0001","8938505970012", ProductCategory.OTC, DosageForm.TABLET,
                "Paracetamol 500mg","PARA500","Imexpharm","Paracetamol",5.0,"500mg",
                "Giảm đau, hạ sốt", "viên", "", LocalDate.now().minusDays(3));
        a.uoms.add(new UomRow("UOM-VIEN","Viên",1));
        a.uoms.add(new UomRow("UOM-HOP","Hộp 10 vỉ x 10 viên",100));
        a.lots.add(new LotRow("L2309A", 120, 900.0, LocalDate.now().plusDays(20), LotStatus.AVAILABLE));
        a.lots.add(new LotRow("L2312B", 60,  920.0, LocalDate.now().plusMonths(6),  LotStatus.AVAILABLE));

        ProductRow b = new ProductRow("PRD-0002","8938505970029", ProductCategory.SUPPLEMENT, DosageForm.SYRUP,
                "Vitamin C Syrup","VITC-SYR","DHG Pharma","Ascorbic acid",0.0,"100mg/5ml",
                "Bổ sung Vitamin C", "ml", "", LocalDate.now().minusDays(10));
        b.uoms.add(new UomRow("UOM-CHAI","Chai 120ml",120));
        b.lots.add(new LotRow("VC-2401", 30, 15000.0, LocalDate.now().plusDays(5), LotStatus.AVAILABLE));
        b.lots.add(new LotRow("VC-2402", 10, 14800.0, LocalDate.now().minusDays(1), LotStatus.EXPIRED));

        allProducts = List.of(a, b);
    }

    // ================== TableModels & Rows ==================
    // ——— Entities (enum) từ OOAD Product ———
    public enum ProductCategory { SUPPLEMENT, OTC, ETC }   // OOAD & SQL (category)
    public enum DosageForm {
        TABLET, CAPSULE, POWDER, LOZENGE, SYRUP, DROP, MOUTHWASH   // OOAD  :contentReference[oaicite:16]{index=16}
    }
    public enum LotStatus { AVAILABLE, EXPIRED, FAULTY }           // OOAD & SQL

    // Row structs (UI-only; thay bằng entity/DAO khi tích hợp DB)
    static class ProductRow {
        String id, barcode, name, shortName, manufacturer, activeIngredient, strength, description, baseUom, imagePath;
        Double vat;
        ProductCategory category; DosageForm form;
        LocalDate createdAt;
        List<UomRow> uoms = new ArrayList<>();
        List<LotRow> lots = new ArrayList<>();
        ProductRow(String id, String barcode, ProductCategory cat, DosageForm form, String name, String shortName,
                   String manufacturer, String activeIngredient, Double vat, String strength, String desc,
                   String baseUom, String imagePath, LocalDate createdAt){
            this.id=id; this.barcode=barcode; this.category=cat; this.form=form; this.name=name; this.shortName=shortName;
            this.manufacturer=manufacturer; this.activeIngredient=activeIngredient; this.vat=vat; this.strength=strength;
            this.description=desc; this.baseUom=baseUom; this.imagePath=imagePath; this.createdAt=createdAt;
        }
    }
    static class UomRow { String id, name; double baseConv; UomRow(String id, String name, double b){ this.id=id; this.name=name; this.baseConv=b; } }
    static class LotRow { String batch; int qty; double basePrice; LocalDate expiryDate; LotStatus status;
        LotRow(String b, int q, double p, LocalDate d, LotStatus s){ batch=b; qty=q; basePrice=p; expiryDate=d; status=s; } }

    static class ProductsTableModel extends AbstractTableModel {
        private final String[] cols = {"Mã","Mã vạch","Tên","Loại","Dạng","VAT","ĐVT cơ sở","Nhà SX","Ngày tạo"};
        private List<ProductRow> rows = new ArrayList<>();
        public void setData(List<ProductRow> data){ rows=data; fireTableDataChanged(); }
        public ProductRow getRowAt(int r){ return rows.get(r); }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r, int c){
            ProductRow p = rows.get(r);
            return switch (c){
                case 0 -> p.id;
                case 1 -> p.barcode;
                case 2 -> p.name;
                case 3 -> p.category;
                case 4 -> p.form;
                case 5 -> p.vat;
                case 6 -> p.baseUom;
                case 7 -> p.manufacturer;
                case 8 -> p.createdAt!=null? p.createdAt.format(DateTimeFormatter.ofPattern("dd/MM")) : "";
                default -> "";
            };
        }
        @Override public Class<?> getColumnClass(int c){
            return switch (c){ case 5 -> Double.class; default -> String.class; };
        }
        @Override public boolean isCellEditable(int r, int c){ return false; }
    }

    static class UomsTableModel extends AbstractTableModel {
        private final String[] cols = {"Mã đơn vị","Tên đơn vị","Hệ số quy đổi"};
        private List<UomRow> rows = new ArrayList<>();
        public void setData(List<UomRow> list){ rows=list; fireTableDataChanged(); }
        public void addRow(UomRow r){ rows.add(r); fireTableRowsInserted(rows.size()-1, rows.size()-1); }
        public void removeRow(int idx){ rows.remove(idx); fireTableRowsDeleted(idx, idx); }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Class<?> getColumnClass(int c){ return c==2? Double.class : String.class; }
        @Override public boolean isCellEditable(int r, int c){ return true; }
        @Override public Object getValueAt(int r, int c){
            UomRow u = rows.get(r);
            return switch (c){ case 0 -> u.id; case 1 -> u.name; case 2 -> u.baseConv; default -> null; };
        }
        @Override public void setValueAt(Object v, int r, int c){
            UomRow u = rows.get(r);
            switch (c){
                case 0 -> u.id = Objects.toString(v,"");
                case 1 -> u.name = Objects.toString(v,"");
                case 2 -> u.baseConv = v instanceof Number n? n.doubleValue() : u.baseConv;
            }
            fireTableCellUpdated(r,c);
        }
    }

    static class LotsTableModel extends AbstractTableModel {
        private final String[] cols = {"Số lô","Số lượng","Giá vốn (đvt cơ sở)","HSD","Trạng thái"};
        private List<LotRow> rows = new ArrayList<>();
        public void setData(List<LotRow> list){ rows=list; fireTableDataChanged(); }
        public void addRow(LotRow r){ rows.add(r); fireTableRowsInserted(rows.size()-1, rows.size()-1); }
        public void removeRow(int idx){ rows.remove(idx); fireTableRowsDeleted(idx, idx); }
        public LotRow getRowAt(int i){ return rows.get(i); }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Class<?> getColumnClass(int c){ return switch (c){ case 1 -> Integer.class; case 2 -> Double.class; case 3 -> String.class; default -> String.class; }; }
        @Override public boolean isCellEditable(int r, int c){ return true; }
        @Override public Object getValueAt(int r, int c){
            LotRow l = rows.get(r);
            return switch (c){
                case 0 -> l.batch;
                case 1 -> l.qty;
                case 2 -> l.basePrice;
                case 3 -> l.expiryDate!=null? l.expiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
                case 4 -> l.status.name();
                default -> null;
            };
        }
        @Override public void setValueAt(Object v, int r, int c){
            LotRow l = rows.get(r);
            switch (c){
                case 0 -> l.batch = Objects.toString(v,"");
                case 1 -> l.qty = (v instanceof Number n) ? n.intValue() : l.qty;
                case 2 -> l.basePrice = (v instanceof Number n2)? n2.doubleValue() : l.basePrice;
                case 3 -> {
                    try {
                        l.expiryDate = LocalDate.parse(Objects.toString(v,""), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } catch (Exception ignored){}
                }
                case 4 -> {
                    try { l.status = LotStatus.valueOf(Objects.toString(v,"AVAILABLE")); } catch(Exception ignored){}
                }
            }
            fireTableCellUpdated(r,c);
        }
    }
}