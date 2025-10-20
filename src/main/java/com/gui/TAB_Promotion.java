package com.gui;

import com.utils.AppColors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;

/**
 * Giao diện quản lý khuyến mãi (bỏ phần Chi nhánh)
 * Có danh sách, bộ lọc, và nút thêm +Khuyến mãi
 * Thanh Khôi
 */
public class TAB_Promotion extends JPanel {

    private DefaultTableModel tableModel;

    // Instance fields for form components
    private JTextField txtCodeField;
    private JTextField txtNameField;
    private GUI_DatePicker dpStartDate;
    private GUI_DatePicker dpEndDate;
    private JComboBox<String> cbTypeField;
    private JComboBox<String> cbStatusField;
    private JTextArea txtDescField;

    public TAB_Promotion() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // -------------------- TOP: Thanh tìm kiếm + nút thêm --------------------
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JTextField search = new JTextField();
        search.setPreferredSize(new Dimension(300, 30));
        search.setToolTipText("Theo mã, tên chương trình");
        top.add(search, BorderLayout.CENTER);

        JButton addBtn = new JButton("+ Khuyến mãi");
        styleButton(addBtn, AppColors.PRIMARY, Color.WHITE);
        top.add(addBtn, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // -------------------- LEFT: Bộ lọc --------------------
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(6, 6, 6, 12));

        left.add(createFilterCard("Trạng thái", new String[]{"Tất cả", "Kích hoạt", "Chưa áp dụng"}));
        left.add(Box.createVerticalStrut(12));
        left.add(createFilterCard("Hiệu lực", new String[]{"Tất cả", "Còn hiệu lực", "Hết hiệu lực"}));
        left.add(Box.createVerticalGlue());

        add(left, BorderLayout.WEST);

        // -------------------- CENTER: Bảng dữ liệu --------------------
        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(new EmptyBorder(6, 6, 6, 6));

        String[] cols = {"Tên chương trình", "Từ ngày", "Đến ngày", "Hình thức", "Trạng thái"};
        tableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(210, 210, 210)));
        main.add(scroll, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);

        // -------------------- SỰ KIỆN: Nút thêm khuyến mãi --------------------
        addBtn.addActionListener(e -> openAddPromotionDialog());
    }

    /**
     * Hiển thị dialog thêm mới chương trình khuyến mãi
     */
    private void openAddPromotionDialog() {
        JDialog dialog = new JDialog((Frame) null, "Thêm chương trình khuyến mãi", true);
        dialog.setSize(950, 600);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(AppColors.BACKGROUND);

        // ---------------- LEFT PANEL: Thông tin ----------------
        JPanel infoPanel = createInfoPanel();

        // ---------------- RIGHT PANEL: Điều kiện + Hành động ----------------
        DefaultTableModel condModel = new DefaultTableModel(new String[]{"Mục tiêu", "Toán tử", "Giá trị 1", "Giá trị 2", "Sản phẩm"}, 0);
        DefaultTableModel actModel = new DefaultTableModel(new String[]{"Loại hành động", "Mục tiêu", "Giá trị 1", "Giá trị 2", "Sản phẩm"}, 0);

        JPanel rightPanel = createRightPanel(dialog, condModel, actModel);

        // ----------- SPLIT LAYOUT (Left - Right) -----------
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, infoPanel, rightPanel);
        split.setResizeWeight(0.45);
        split.setDividerSize(6);
        split.setBorder(null);

        // ----------- Bottom Buttons -----------
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottom.setBackground(AppColors.BACKGROUND);
        JButton btnCancel = new JButton("Hủy");
        JButton btnSave = new JButton("Lưu chương trình");
        styleButton(btnSave, AppColors.PRIMARY, Color.WHITE);
        styleButton(btnCancel, AppColors.DARK, Color.WHITE);
        bottom.add(btnCancel);
        bottom.add(btnSave);

        dialog.add(split, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);

        // -------------------- SỰ KIỆN --------------------
        setupButtonActions(dialog, infoPanel, condModel, actModel, btnSave, btnCancel);

        dialog.setVisible(true);
    }

    // ============ TÁCH SỰ KIỆN RA =================

    private void setupButtonActions(
            JDialog dialog, JPanel infoPanel,
            DefaultTableModel condModel, DefaultTableModel actModel,
            JButton btnSave, JButton btnCancel
    ) {
        btnSave.addActionListener(e -> handleSavePromotion(dialog, infoPanel, condModel, actModel));
        btnCancel.addActionListener(e -> handleCancelDialog(dialog));
    }

    private void handleSavePromotion(JDialog dialog, JPanel infoPanel,
                                     DefaultTableModel condModel, DefaultTableModel actModel) {
        // Sử dụng instance fields thay vì lấy từ component index
        String name = txtNameField.getText().trim();
        String start = dpStartDate.getFormattedDate();
        String end = dpEndDate.getFormattedDate();
        String type = (String) cbTypeField.getSelectedItem();
        String status = (String) cbStatusField.getSelectedItem();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên chương trình!");
            return;
        }

        // Kiểm tra ngày kết thúc không được trước ngày bắt đầu
        if (dpEndDate.getDate().before(dpStartDate.getDate())) {
            JOptionPane.showMessageDialog(dialog, "Ngày kết thúc không thể trư��c ngày bắt đầu!");
            return;
        }

        tableModel.addRow(new Object[]{name, start, end, type, status});

        JOptionPane.showMessageDialog(dialog,
                "✅ Đã lưu chương trình khuyến mãi!\n" +
                        "Điều kiện: " + condModel.getRowCount() +
                        " | Hành động: " + actModel.getRowCount());
        dialog.dispose();
    }

    private void handleCancelDialog(JDialog dialog) {
        dialog.dispose();
    }

    // ============ UI THÀNH PHẦN =================

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin khuyến mãi"));
        infoPanel.setBackground(AppColors.LIGHT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Khởi tạo các fields
        txtCodeField = new JTextField(20);
        txtNameField = new JTextField(20);

        // Tạo DatePicker cho ngày bắt đầu và kết thúc
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.MONTH, 6); // Mặc định kết thúc sau 6 tháng
        Date sixMonthsLater = cal.getTime();

        dpStartDate = new GUI_DatePicker(today);
        dpEndDate = new GUI_DatePicker(sixMonthsLater);

        // Thiết lập ràng buộc ban đầu
        dpEndDate.setMinDate(today); // Ngày kết thúc không được trước ngày bắt đầu
        dpStartDate.setMaxDate(sixMonthsLater); // Ngày bắt đầu không được sau ngày kết thúc

        // Listener cho ngày bắt đầu: Khi thay đổi, cập nhật minDate của ngày kết thúc
        dpStartDate.addPropertyChangeListener("date", evt -> {
            Date startDate = (Date) evt.getNewValue();
            dpEndDate.setMinDate(startDate);

            // Nếu ngày kết thúc hiện tại < ngày bắt đầu mới, tự động điều chỉnh
            if (dpEndDate.getDate().before(startDate)) {
                dpEndDate.setDate(startDate);
            }
        });

        // Listener cho ngày kết thúc: Khi thay đổi, cập nhật maxDate của ngày bắt đầu
        dpEndDate.addPropertyChangeListener("date", evt -> {
            Date endDate = (Date) evt.getNewValue();
            dpStartDate.setMaxDate(endDate);

            // Nếu ngày bắt đầu hiện tại > ngày kết thúc mới, tự động điều chỉnh
            if (dpStartDate.getDate().after(endDate)) {
                dpStartDate.setDate(endDate);
            }
        });

        cbTypeField = new JComboBox<>(new String[]{"Giảm giá hóa đơn", "Tặng sản phẩm", "Giảm giá sản phẩm"});
        cbStatusField = new JComboBox<>(new String[]{"Kích hoạt", "Chưa áp dụng"});
        txtDescField = new JTextArea(3, 20);
        txtDescField.setLineWrap(true);
        txtDescField.setWrapStyleWord(true);

        addRow(infoPanel, gbc, row++, "Mã chương trình:", txtCodeField);
        addRow(infoPanel, gbc, row++, "Tên chương trình:", txtNameField);
        addRow(infoPanel, gbc, row++, "Từ ngày:", dpStartDate);
        addRow(infoPanel, gbc, row++, "Đến ngày:", dpEndDate);
        addRow(infoPanel, gbc, row++, "Hình thức:", cbTypeField);
        addRow(infoPanel, gbc, row++, "Trạng thái:", cbStatusField);
        addRow(infoPanel, gbc, row++, "Mô tả:", new JScrollPane(txtDescField));

        return infoPanel;
    }

    private JPanel createRightPanel(JDialog dialog, DefaultTableModel condModel, DefaultTableModel actModel) {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        JPanel condPanel = createModernPanel("Điều kiện áp dụng");
        JTable condTable = createTable(condModel);
        JScrollPane condScroll = new JScrollPane(condTable);
        JButton btnAddCond = createAddButton("+ Thêm điều kiện");
        btnAddCond.addActionListener(e -> handleAddCondition(dialog, condModel));
        condPanel.add(condScroll, BorderLayout.CENTER);
        condPanel.add(btnAddCond, BorderLayout.SOUTH);

        JPanel actPanel = createModernPanel("Hành động khuyến mãi");
        JTable actTable = createTable(actModel);
        JScrollPane actScroll = new JScrollPane(actTable);
        JButton btnAddAction = createAddButton("+ Thêm hành động");
        btnAddAction.addActionListener(e -> handleAddAction(dialog, actModel));
        actPanel.add(actScroll, BorderLayout.CENTER);
        actPanel.add(btnAddAction, BorderLayout.SOUTH);

        rightPanel.add(condPanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(actPanel);
        return rightPanel;
    }

    private void handleAddCondition(JDialog parent, DefaultTableModel model) {
        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));
        panel.add(new JLabel("Mục tiêu:"));
        JComboBox<String> cbTarget = new JComboBox<>(new String[]{"PRODUCT_ID", "PRODUCT_QTY", "ORDER_SUBTOTAL"});
        panel.add(cbTarget);
        panel.add(new JLabel("Toán tử:"));
        JComboBox<String> cbComp = new JComboBox<>(new String[]{"GREATER", "LESS", "EQUAL", "BETWEEN"});
        panel.add(cbComp);
        panel.add(new JLabel("Giá trị 1:"));
        JTextField v1 = new JTextField();
        panel.add(v1);
        panel.add(new JLabel("Giá trị 2:"));
        JTextField v2 = new JTextField();
        panel.add(v2);
        panel.add(new JLabel("Sản phẩm (nếu có):"));
        JTextField prod = new JTextField();
        panel.add(prod);

        int r = JOptionPane.showConfirmDialog(parent, panel, "Thêm điều kiện", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            model.addRow(new Object[]{cbTarget.getSelectedItem(), cbComp.getSelectedItem(), v1.getText(), v2.getText(), prod.getText()});
        }
    }

    private void handleAddAction(JDialog parent, DefaultTableModel model) {
        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));
        panel.add(new JLabel("Loại hành động:"));
        JComboBox<String> cbType = new JComboBox<>(new String[]{"PERCENT_DISCOUNT", "FIXED_DISCOUNT", "PRODUCT_GIFT"});
        panel.add(cbType);
        panel.add(new JLabel("Mục tiêu:"));
        JComboBox<String> cbTarget = new JComboBox<>(new String[]{"PRODUCT", "ORDER_SUBTOTAL"});
        panel.add(cbTarget);
        panel.add(new JLabel("Giá trị 1:"));
        JTextField v1 = new JTextField();
        panel.add(v1);
        panel.add(new JLabel("Giá trị 2:"));
        JTextField v2 = new JTextField();
        panel.add(v2);
        panel.add(new JLabel("Sản phẩm (nếu có):"));
        JTextField prod = new JTextField();
        panel.add(prod);

        int r = JOptionPane.showConfirmDialog(parent, panel, "Thêm hành động", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            model.addRow(new Object[]{cbType.getSelectedItem(), cbTarget.getSelectedItem(), v1.getText(), v2.getText(), prod.getText()});
        }
    }

    // ================== HỖ TRỢ GIAO DIỆN ==================

    private JPanel createModernPanel(String title) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.setBackground(AppColors.LIGHT);
        return p;
    }

    private JTable createTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(26);
        t.setShowGrid(true);
        t.setGridColor(AppColors.SECONDARY);
        t.setBackground(Color.WHITE);
        t.getTableHeader().setBackground(AppColors.PRIMARY);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        return t;
    }

    private JButton createAddButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, AppColors.SECONDARY, Color.WHITE);
        return b;
    }

    private void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setPreferredSize(new Dimension(150, 35));
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    private JPanel createFilterCard(String title, String[] options) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)
        ));
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 14f));
        card.add(t);
        card.add(Box.createVerticalStrut(8));

        ButtonGroup g = new ButtonGroup();
        for (String option : options) {
            JRadioButton r = new JRadioButton(option);
            r.setFocusPainted(false);
            g.add(r);
            card.add(r);
            card.add(Box.createVerticalStrut(6));
        }
        return card;
    }
}
