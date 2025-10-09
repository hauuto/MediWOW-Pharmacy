package com.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import com.enums.PromotionEnum.*;
import com.entities.Promotion;
import com.entities.PromotionCondition;
import com.entities.PromotionAction;

/** Trang trung tâm: Khuyến mãi — Danh sách & Chi tiết (SplitView) */
public class TAB_Promotion extends JPanel {

    // ====== UI: trái (JTable) ======
    private JTable tblPromotions;
    private PromotionsTableModel promotionsModel;
    private final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM");

    // ====== UI: phải (form + 2 table) ======
    private JTextField txtId, txtName, txtStart, txtEnd;
    private JTextArea txaDesc;
    private JToggleButton tglStatus;

    private JTable tblConds, tblActs;
    private ConditionsModel condsModel;
    private ActionsModel actsModel;

    // --- new: search controls ---
    private JTextField txtSearch;      // search input
    private JButton btnSearch;         // search button

    private final JButton btnAddCond = new JButton("+ Thêm điều kiện");
    private final JButton btnDelCond = new JButton("Xóa điều kiện");
    private final JButton btnAddAct = new JButton("+ Thêm hành động");
    private final JButton btnDelAct = new JButton("Xóa hành động");

    // dữ liệu mẫu
    private final ArrayList<Promotion> data = new ArrayList<>();

    public TAB_Promotion() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildSplitView(), BorderLayout.CENTER);

        loadSampleData(); // tạo vài KM mẫu

        promotionsModel.setData(data);
        if (!data.isEmpty()) {
            tblPromotions.setRowSelectionInterval(0, 0);
            loadDetail(getSelectedPromotion());
        }

        SwingUtilities.invokeLater(() -> {
            Container p = (Container) getComponent(1);
            if (p instanceof JSplitPane sp) sp.setDividerLocation(0.6);
        });
    }

    /*
    Thanh công cụ trên cùng: nút + bộ lọc
    @return JPanel chứa các nút và bộ lọc
    @Thanh Khôi
     */
    private JComponent buildToolbar() {
        // Use BorderLayout so the search box can expand in the center
        JPanel bar = new JPanel(new BorderLayout(8, 4));

        // --- Search field (larger font, wider, expandable) ---
        txtSearch = new JTextField();
        txtSearch.setColumns(24); // preferred column count
        txtSearch.setFont(txtSearch.getFont().deriveFont(Font.PLAIN, 16f)); // larger text
        // give a sensible preferred width; BorderLayout.CENTER will allow expansion
        txtSearch.setPreferredSize(new Dimension(420, txtSearch.getPreferredSize().height));
        txtSearch.addActionListener(e -> applyFilter());

        btnSearch = new JButton("Tìm");
        btnSearch.setFont(btnSearch.getFont().deriveFont(Font.PLAIN, 14f));
        btnSearch.addActionListener(e -> applyFilter());

        // Put search input + button into a small wrapper on the left/center
        JPanel searchWrap = new JPanel(new BorderLayout(4, 0));
        searchWrap.add(txtSearch, BorderLayout.CENTER);
        searchWrap.add(btnSearch, BorderLayout.EAST);
        bar.add(searchWrap, BorderLayout.CENTER);

        // Right-side controls (kept in a flow layout)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.add(pillCombo("Trạng thái", new String[]{"Tất cả","Đang áp dụng","Sắp tới","Hết hạn"}));
        right.add(pillCombo("Loại hành động", new String[]{"Tất cả","PERCENT_DISCOUNT","FIXED_DISCOUNT","PRODUCT_GIFT"}));
        right.add(pillCombo("Mục tiêu", new String[]{"Tất cả","ORDER_SUBTOTAL","PRODUCT","PRODUCT_QTY"}));
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    /*
    Pill-style combo box
    Bấm vào để xổ ra combo box chứa các lựa chọn trong mảng @items
    @return JPanel chứa label + combo box
    @Thanh Khôi
     */
    private JPanel pillCombo(String label, String[] items){
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        wrap.setBorder(new CompoundBorder(new LineBorder(new Color(210,210,210)), new EmptyBorder(2,8,2,8)));
        wrap.add(new JLabel(label + ":"));
        wrap.add(new JComboBox<>(items));
        return wrap;
    }

    /*
    Split-Pane View: trái (danh sách) + phải (chi tiết)
    Sử dụng JSplitPane để có thể thay đổi kích thước 2 bên
    Đồng thời chia nội dung 2 bên rõ ràng
    @return JSplitPane chứa 2 JPanel
    @Thanh Khôi
     */
    private JSplitPane buildSplitView() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildListPanel(), buildDetailPanel());
        split.setContinuousLayout(true);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        return split;
    }

    /*
    JPanel bên trái: chứa JTable danh sách khuyến mãi
    @return JPanel chứa JTable
    @Thanh Khôi
     */
    private JPanel buildListPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new CompoundBorder(new LineBorder(new Color(220,220,220)),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel title = new JLabel("Danh sách khuyến mãi");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        p.add(title, BorderLayout.NORTH);

        promotionsModel = new PromotionsTableModel();
        tblPromotions = new JTable(promotionsModel);
        tblPromotions.setAutoCreateRowSorter(true);
        tblPromotions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPromotions.setRowHeight(26);

        // chiều rộng cột giống mockup
        int[] widths = {80, 220, 160, 240, 220, 110};
        for (int i=0;i<widths.length;i++) {
            tblPromotions.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        tblPromotions.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblPromotions.getSelectedRow() >= 0) {
                loadDetail(getSelectedPromotion());
            }
        });

        p.add(new JScrollPane(tblPromotions), BorderLayout.CENTER);
        return p;
    }


    /*
    Jpanel bên phải: chứa form chi tiết + 2 bảng điều kiện & hành động
    @return JPanel chứa form và 2 bảng
    @Thanh Khôi
     */
    private JComponent buildDetailPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel title = new JLabel("Chi tiết khuyến mãi");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(6));

        // --- Thông tin chung ---
        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(new TitledBorder("Thông tin chung"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Khởi tạo field
        txtId = new JTextField();
        txtName = new JTextField();
        txaDesc = new JTextArea(2, 30);
        txaDesc.setLineWrap(true);
        txaDesc.setWrapStyleWord(true);
        JScrollPane taDescScroll = new JScrollPane(txaDesc);
        txtStart = new JTextField();
        txtEnd = new JTextField();
        tglStatus = new JToggleButton("Đang tắt");

        // Dòng 1: Mã - Hiệu lực từ
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0;
        info.add(new JLabel("Mã *"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 1.0;
        info.add(txtId, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        info.add(new JLabel("Hiệu lực từ"), gbc);
        gbc.gridx = 3; gbc.weightx = 1.0;
        info.add(txtStart, gbc);

        // Dòng 2: Tên
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
        info.add(new JLabel("Tên *"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        info.add(txtName, gbc);

        // Dòng 3: Mô tả
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        info.add(new JLabel("Mô tả"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        info.add(taDescScroll, gbc);

        // Dòng 4: Đến ngày - Trạng thái
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        info.add(new JLabel("Đến ngày"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 1.0;
        info.add(txtEnd, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        info.add(new JLabel("Trạng thái"), gbc);
        gbc.gridx = 3; gbc.weightx = 1.0;
        tglStatus.addItemListener(e -> tglStatus.setText(tglStatus.isSelected() ? "Đang áp dụng" : "Đang tắt"));
        info.add(tglStatus, gbc);

        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(info);
        root.add(Box.createVerticalStrut(6));

        // --- Điều kiện áp dụng ---
        condsModel = new ConditionsModel();
        tblConds = new JTable(condsModel);
        configConditionTable(tblConds);

        JPanel condWrap = new JPanel(new BorderLayout(6, 6));
        condWrap.setBorder(new TitledBorder("Điều kiện áp dụng"));
        condWrap.add(new JScrollPane(tblConds), BorderLayout.CENTER);

        JPanel condBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        condBtns.add(btnAddCond);
        condBtns.add(btnDelCond);
        condWrap.add(condBtns, BorderLayout.SOUTH);

        btnAddCond.addActionListener(e -> {
            condsModel.addRow(new PromotionCondition(Target.ORDER_SUBTOTAL, Comp.GREATER_EQUAL, 0.0, null, ""));
            selectLastRow(tblConds);
        });
        btnDelCond.addActionListener(e -> deleteSelected(tblConds, condsModel));

        condWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(condWrap);
        root.add(Box.createVerticalStrut(6));

        // --- Hành động khuyến mãi ---
        actsModel = new ActionsModel();
        tblActs = new JTable(actsModel);
        configActionTable(tblActs);

        JPanel actWrap = new JPanel(new BorderLayout(6, 6));
        actWrap.setBorder(new TitledBorder("Hành động khuyến mãi"));
        actWrap.add(new JScrollPane(tblActs), BorderLayout.CENTER);

        JPanel actBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        actBtns.add(btnAddAct);
        actBtns.add(btnDelAct);
        actWrap.add(actBtns, BorderLayout.SOUTH);

        btnAddAct.addActionListener(e -> {
            actsModel.addRow(new PromotionAction(ActionType.PERCENT_DISCOUNT, Target.ORDER_SUBTOTAL, 0.0, "", actsModel.getRowCount() + 1));
            selectLastRow(tblActs);
        });
        btnDelAct.addActionListener(e -> deleteSelected(tblActs, actsModel));

        actWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(actWrap);

        return root;
    }

    // ====== Binding chi tiết ======
    private void loadDetail(Promotion p){
        if (p==null) return;
        txtId.setText(p.getId());
        txtName.setText(p.getName());
        txtStart.setText(p.getStart()!=null ? p.getStart().format(DMY) : "");
        txtEnd.setText(p.getEnd()!=null ? p.getEnd().format(DMY) : "");
        txaDesc.setText(p.getDescription()!=null ? p.getDescription() : "");
        tglStatus.setSelected(p.getStatus()==Status.DANG_AP_DUNG);
        tglStatus.setText(tglStatus.isSelected() ? "Đang áp dụng" : "Đang tắt");

        condsModel.setData(p.getConditions());
        actsModel.setData(p.getActions());
    }

    // ====== Table models ======
    static class PromotionsTableModel extends AbstractTableModel {
        private final String[] cols = {"Mã","Tên khuyến mãi","Thời gian","Điều kiện tóm tắt","Hành động tóm tắt","Trạng thái"};
        private List<Promotion> rows = new ArrayList<>();

        public void setData(List<Promotion> data){ this.rows = data; fireTableDataChanged(); }
        public Promotion getPromotionAt(int r){ return rows.get(r); }

        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Promotion p = rows.get(r);
            return switch (c) {
                case 0 -> p.getId();
                case 1 -> p.getName();
                case 2 -> fmtRange(p.getStart(), p.getEnd());
                case 3 -> summarizeConditions(p.getConditions());
                case 4 -> summarizeActions(p.getActions());
                    case 5 -> switch (p.getStatus()) {
                    case DANG_AP_DUNG -> "Đang áp dụng";
                    case SAP_TOI -> "Sắp tới";
                    default -> "Hết hạn";
                };
                default -> "";
            };
        }

        private static String fmtRange(LocalDate s, LocalDate e){
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");
            return (s!=null? s.format(f):"") + "–" + (e!=null? e.format(f):"");
        }
        private static String summarizeConditions(List<PromotionCondition> cs){
            if (cs==null || cs.isEmpty()) return "";
            List<String> parts = new ArrayList<>();
            for (PromotionCondition c: cs){
                String left = c.getTarget().name();
                String right = c.getProduct()!=null && !c.getProduct().isBlank() ? c.getProduct() : "";
                String val  = c.getComparator() + " " + toNice(c.getPrimaryValue())
                        + (c.getComparator()==Comp.BETWEEN ? " & " + toNice(c.getSecondaryValue()) : "");
                parts.add((right.isBlank()? left : left+"="+right) + " " + val);
            }
            return String.join("; ", parts);
        }
        private static String summarizeActions(List<PromotionAction> as){
            if (as==null || as.isEmpty()) return "";
            List<String> parts = new ArrayList<>();
            for (PromotionAction a: as){
                String v = a.getType()==ActionType.PERCENT_DISCOUNT? ((int)Math.round(a.getValue()))+"%" :
                        a.getType()==ActionType.FIXED_DISCOUNT? toNice(a.getValue()) :
                                a.getType()==ActionType.PRODUCT_GIFT? (toNice(a.getValue())+" "+a.getProductOrGift()) : toNice(a.getValue());
                parts.add(a.getType().name()+": "+v);
            }
            return String.join("; ", parts);
        }
        private static String toNice(Double d){ if (d==null) return ""; long x=Math.round(d); return String.format("%,d", x); }
    }

    static class ConditionsModel extends AbstractTableModel {
        private final String[] cols = {"Mục tiêu","So sánh","Giá trị 1","Giá trị 2","Sản phẩm"};
        private List<PromotionCondition> rows = new ArrayList<>();

        public void setData(List<PromotionCondition> list){ rows=list; fireTableDataChanged(); }
        public void addRow(PromotionCondition r){ rows.add(r); fireTableRowsInserted(rows.size()-1, rows.size()-1); }
        public void removeRow(int idx){ rows.remove(idx); fireTableRowsDeleted(idx, idx); }

        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Class<?> getColumnClass(int c){
            return switch (c){ case 0 -> Target.class; case 1 -> Comp.class; case 2,3 -> Double.class; default -> String.class; };
        }
        @Override public boolean isCellEditable(int r, int c){ return true; }

        @Override
        public Object getValueAt(int r, int c) {
            PromotionCondition m = rows.get(r);
            return switch (c) {
                case 0 -> m.getTarget();
                case 1 -> m.getComparator();
                case 2 -> m.getPrimaryValue();
                case 3 -> m.getSecondaryValue();
                case 4 -> m.getProduct();
                default -> null;
            };
        }

    }

    static class ActionsModel extends AbstractTableModel {
        private final String[] cols = {"Loại","Mục tiêu","Giá trị","Sản phẩm/Quà tặng","Thứ tự"};
        private List<PromotionAction> rows = new ArrayList<>();

        public void setData(List<PromotionAction> list){ rows=list; fireTableDataChanged(); }
        public void addRow(PromotionAction r){ rows.add(r); fireTableRowsInserted(rows.size()-1, rows.size()-1); }
        public void removeRow(int idx){ rows.remove(idx); fireTableRowsDeleted(idx, idx); }

        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Class<?> getColumnClass(int c){
            return switch (c){ case 0 -> ActionType.class; case 1 -> Target.class; case 2 -> Double.class; case 3 -> String.class; default -> Integer.class; };
        }
        @Override public boolean isCellEditable(int r, int c){ return true; }

        @Override
        public Object getValueAt(int r, int c) {
            PromotionAction m = rows.get(r);
            return switch (c) {
                case 0 -> m.getType();
                case 1 -> m.getTarget();
                case 2 -> m.getValue();
                case 3 -> m.getProductOrGift();
                case 4 -> m.getOrder();
                default -> null;
            };
        }
    }

    // ====== Mockup Data ======
    private void loadSampleData() {
        Promotion p1 = new Promotion("KM10","Giảm 10% đơn trên 500k",
                LocalDate.of(2025,10,1), LocalDate.of(2025,12,31), Status.DANG_AP_DUNG, "");
        p1.getConditions().add(new PromotionCondition(Target.ORDER_SUBTOTAL, Comp.GREATER_EQUAL, 500_000d, null, ""));
        p1.getActions().add(new PromotionAction(ActionType.PERCENT_DISCOUNT, Target.ORDER_SUBTOTAL, 10d, "", 1));
        data.add(p1);

        Promotion p2 = new Promotion("KM50K","Trừ 50k cho hóa đơn 300k",
                LocalDate.of(2025,9,1), LocalDate.of(2025,11,30), Status.SAP_TOI, "");
        p2.getConditions().add(new PromotionCondition(Target.ORDER_SUBTOTAL, Comp.GREATER_EQUAL, 300_000d, null, ""));
        p2.getActions().add(new PromotionAction(ActionType.FIXED_DISCOUNT, Target.ORDER_SUBTOTAL, 50_000d, "", 1));
        data.add(p2);

        Promotion p3 = new Promotion("QTTANG","Mua 2 tặng 1 Vitamin C",
                LocalDate.of(2025,10,1), LocalDate.of(2025,10,30), Status.HET_HAN, "");
        p3.getConditions().add(new PromotionCondition(Target.PRODUCT_QTY, Comp.GREATER_EQUAL, 2d, null, "Vitamin C (VITC)"));
        p3.getActions().add(new PromotionAction(ActionType.PRODUCT_GIFT, Target.PRODUCT, 1d, "Vitamin C (VITC)", 1));
        data.add(p3);

        Promotion p4 = new Promotion("KM15RX","Đơn có toa: giảm 15%",
                LocalDate.of(2025,8,1), LocalDate.of(2025,12,31), Status.DANG_AP_DUNG, "");
        p4.getConditions().add(new PromotionCondition(Target.PRODUCT_QTY, Comp.GREATER_EQUAL, 1d, null, "Thuốc kê toa (any)"));
        p4.getActions().add(new PromotionAction(ActionType.PERCENT_DISCOUNT, Target.ORDER_SUBTOTAL, 15d, "", 1));
        data.add(p4);

        Promotion p5 = new Promotion("KM5","Giảm 5% mọi đơn",
                LocalDate.of(2025,10,1), LocalDate.of(2025,10,31), Status.HET_HAN, "");
        p5.getActions().add(new PromotionAction(ActionType.PERCENT_DISCOUNT, Target.ORDER_SUBTOTAL, 5d, "", 1));
        data.add(p5);

        Promotion p6 = new Promotion("KM30","Giảm 30% mỹ phẩm",
                LocalDate.of(2025,10,1), LocalDate.of(2025,10,31), Status.DANG_AP_DUNG, "");
        p6.getConditions().add(new PromotionCondition(Target.PRODUCT, Comp.EQUAL, null, null, "CATEGORY=SUPPLEMENT"));
        p6.getActions().add(new PromotionAction(ActionType.PERCENT_DISCOUNT, Target.PRODUCT, 30d, "Nhóm mỹ phẩm", 1));
        data.add(p6);
    }


    //============ Hàm hỗ trợ ============

    private Promotion getSelectedPromotion() {
        int viewRow = tblPromotions.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = tblPromotions.convertRowIndexToModel(viewRow);
        return promotionsModel.getPromotionAt(modelRow);
    }

    private void configConditionTable(JTable table){
        table.setRowHeight(26);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(130); // target
        cm.getColumn(1).setPreferredWidth(90);  // comparator
        cm.getColumn(2).setPreferredWidth(110); // value1
        cm.getColumn(3).setPreferredWidth(110); // value2
        cm.getColumn(4).setPreferredWidth(200); // product
        // editors
        cm.getColumn(0).setCellEditor(new DefaultCellEditor(new JComboBox<>(Target.values())));
        cm.getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(Comp.values())));
    }


    private void configActionTable(JTable table){
        table.setRowHeight(26);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(160); // type
        cm.getColumn(1).setPreferredWidth(130); // target
        cm.getColumn(2).setPreferredWidth(90);  // value
        cm.getColumn(3).setPreferredWidth(200); // product/gift
        cm.getColumn(4).setPreferredWidth(70);  // order
        cm.getColumn(0).setCellEditor(new DefaultCellEditor(new JComboBox<>(ActionType.values())));
        cm.getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(Target.values())));
    }

    private void selectLastRow(JTable t){
        int last = t.getRowCount()-1;
        if (last>=0) t.setRowSelectionInterval(last, last);
    }
    private void deleteSelected(JTable t, AbstractTableModel m){
        int row = t.getSelectedRow();
        if (row<0) return;
        int modelRow = t.convertRowIndexToModel(row);
        if (m instanceof ConditionsModel cm) cm.removeRow(modelRow);
        if (m instanceof ActionsModel am) am.removeRow(modelRow);
    }

    // new: apply filter wrapper
    private void applyFilter() {
        String q = txtSearch.getText() != null ? txtSearch.getText().trim() : "";
        filterPromotions(q);
    }

    // new: filter logic - searches id, name, description (case-insensitive)
    private void filterPromotions(String q) {
        if (q == null || q.isEmpty()) {
            promotionsModel.setData(data);
            if (!data.isEmpty()) {
                tblPromotions.setRowSelectionInterval(0, 0);
                loadDetail(getSelectedPromotion());
            }
            return;
        }
        String ql = q.toLowerCase(Locale.ROOT);
        List<Promotion> filtered = new ArrayList<>();
        for (Promotion p : data) {
            boolean matches = false;
            if (p.getId() != null && p.getId().toLowerCase(Locale.ROOT).contains(ql)) matches = true;
            if (!matches && p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(ql)) matches = true;
            if (!matches && p.getDescription() != null && p.getDescription().toLowerCase(Locale.ROOT).contains(ql)) matches = true;
            if (matches) filtered.add(p);
        }
        promotionsModel.setData(filtered);
        if (!filtered.isEmpty()) {
            // select first found item and load detail
            tblPromotions.setRowSelectionInterval(0, 0);
            loadDetail(getSelectedPromotion());
        } else {
            tblPromotions.clearSelection();
            // optionally clear detail view; loadDetail(null) is safe (it returns early)
            loadDetail(null);
        }
    }
}
