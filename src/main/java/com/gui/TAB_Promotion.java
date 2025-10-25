package com.gui;

import com.utils.AppColors;
import com.enums.PromotionEnum;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;

/**
 * Giao diện quản lý khuyến mãi
 * - SplitPane tổng: 60/40
 * - SplitPane bên trong: 40/60
 * - Bảng điều kiện & hành động có thể nhập trực tiếp
 * - Dưới cùng: 3 nút chính (Thêm mới, Xóa, Xóa trắng)
 */
public class TAB_Promotion extends JPanel {

    private DefaultTableModel tableModel;
    private DefaultTableModel condModel;
    private DefaultTableModel actModel;

    private TableModelListener condListener;
    private TableModelListener actListener;

    private JTextField txtCodeField;
    private JTextField txtNameField;
    private DIALOG_DatePicker dpStartDate;
    private DIALOG_DatePicker dpEndDate;
    private JComboBox<String> cbTypeField;
    private JComboBox<String> cbStatusField;
    private JTextArea txtDescField;

    public TAB_Promotion() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 250, 250));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // -------------------- TOP: Thanh tìm kiếm --------------------
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "QUẢN LÝ KHUYẾN MÃI", 0, 0,
                new Font("Segoe UI", Font.BOLD, 16),
                AppColors.PRIMARY
        ));
        top.setBackground(new Color(245, 250, 250));

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        JTextField txtSearch = new JTextField(25);
        JButton btnSearch = new JButton("Tìm");
        JComboBox<String> cbType = new JComboBox<>(new String[]{"Tất cả", "Giảm giá", "Tặng phẩm"});
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Tất cả", "Kích hoạt", "Chưa áp dụng"});
        JButton btnRefresh = new JButton("Làm mới");

        styleButton(btnSearch, AppColors.PRIMARY, Color.WHITE);
        styleButton(btnRefresh, AppColors.PRIMARY, Color.WHITE);

        top.add(lblSearch);
        top.add(txtSearch);
        top.add(btnSearch);
        top.add(new JLabel("Hình thức:"));
        top.add(cbType);
        top.add(new JLabel("Trạng thái:"));
        top.add(cbStatus);
        top.add(btnRefresh);

        add(top, BorderLayout.NORTH);

        // -------------------- LEFT: Bộ lọc --------------------
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(6, 6, 6, 12));
        left.setBackground(new Color(245, 250, 250));

        left.add(createFilterCard("Trạng thái", new String[]{"Tất cả", "Kích hoạt", "Chưa áp dụng"}));
        left.add(Box.createVerticalStrut(12));
        left.add(createFilterCard("Hiệu lực", new String[]{"Tất cả", "Còn hiệu lực", "Hết hiệu lực"}));
        left.add(Box.createVerticalGlue());
        add(left, BorderLayout.WEST);

        // -------------------- CENTER: Danh sách & Chi tiết --------------------
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.6); // 60:40
        mainSplit.setDividerSize(6);
        mainSplit.setBackground(new Color(245, 250, 250));

        // LEFT: Danh sách
        JPanel mainLeft = new JPanel(new BorderLayout());
        mainLeft.setBackground(new Color(245, 250, 250));
        mainLeft.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Danh sách khuyến mãi",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 14),
                AppColors.PRIMARY
        ));

        String[] cols = {"Tên chương trình", "Từ ngày", "Đến ngày", "Hình thức", "Trạng thái"};
        tableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 230, 240)));
        mainLeft.add(scroll, BorderLayout.CENTER);

        // RIGHT: Chi tiết
        JPanel mainRight = createMainDetailPanel();

        mainSplit.setLeftComponent(mainLeft);
        mainSplit.setRightComponent(mainRight);
        add(mainSplit, BorderLayout.CENTER);
    }

    /** Panel chi tiết bên phải */
    private JPanel createMainDetailPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Thông tin khuyến mãi",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 14),
                AppColors.PRIMARY
        ));
        mainPanel.setBackground(new Color(240, 250, 250));

        condModel = new DefaultTableModel(
                new String[]{"Mục tiêu", "Toán tử", "Giá trị 1", "Giá trị 2", "Sản phẩm"}, 0);
        actModel = new DefaultTableModel(
                new String[]{"Loại hành động", "Mục tiêu", "Giá trị 1", "Giá trị 2", "Sản phẩm"}, 0);

        JPanel infoPanel = createInfoPanel();
        JPanel tablesPanel = createTablesPanel();

        // --- SplitPane trong: 40:60 ---
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        verticalSplit.setResizeWeight(0.4);
        verticalSplit.setDividerSize(6);
        verticalSplit.setTopComponent(infoPanel);
        verticalSplit.setBottomComponent(tablesPanel);
        verticalSplit.setBorder(null);

        // --- Nút chức năng ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(245, 250, 250));

        JButton btnAdd = new JButton("Thêm mới");
        styleButton(btnAdd, new Color(40, 167, 69), Color.WHITE);
        JButton btnDelete = new JButton("Xóa");
        styleButton(btnDelete, new Color(220, 53, 69), Color.WHITE);
        JButton btnClear = new JButton("Xóa trắng");
        styleButton(btnClear, AppColors.PRIMARY, Color.WHITE);

        btnAdd.addActionListener(e -> handleSaveFromMainPanel());
        btnClear.addActionListener(e -> handleClearMainPanel());

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);

        mainPanel.add(verticalSplit, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        return mainPanel;
    }

    /** Panel thông tin khuyến mãi */
    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new BorderLayout(0, 10));
        infoPanel.setBackground(new Color(245, 250, 250));
        infoPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel grid = new JPanel(new GridLayout(3, 2, 15, 10));
        grid.setOpaque(false);

        txtCodeField = new JTextField();
        txtNameField = new JTextField();
        dpStartDate = new DIALOG_DatePicker(new Date());
        dpEndDate = new DIALOG_DatePicker(new Date());
        cbTypeField = new JComboBox<>(new String[]{"Giảm giá hóa đơn", "Tặng sản phẩm", "Giảm giá sản phẩm"});
        cbStatusField = new JComboBox<>(new String[]{"Kích hoạt", "Chưa áp dụng"});

        grid.add(labeled("Mã chương trình:", txtCodeField));
        grid.add(labeled("Hình thức:", cbTypeField));
        grid.add(labeled("Tên chương trình:", txtNameField));
        grid.add(labeled("Trạng thái:", cbStatusField));
        grid.add(labeled("Từ ngày:", dpStartDate));
        grid.add(labeled("Đến ngày:", dpEndDate));

        txtDescField = new JTextArea(3, 20);
        txtDescField.setLineWrap(true);
        txtDescField.setWrapStyleWord(true);
        txtDescField.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        JScrollPane descScroll = new JScrollPane(txtDescField);
        descScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "Mô tả", 0, 0, new Font("Segoe UI", Font.BOLD, 12), AppColors.PRIMARY
        ));

        infoPanel.add(grid, BorderLayout.NORTH);
        infoPanel.add(descScroll, BorderLayout.CENTER);
        return infoPanel;
    }

    private JPanel labeled(String text, Component c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setPreferredSize(new Dimension(110, 25));
        p.add(l, BorderLayout.WEST);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private JPanel createTablesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));

        panel.add(createEditableSection("Điều kiện áp dụng", condModel));
        panel.add(Box.createVerticalStrut(12));
        panel.add(createEditableSection("Hành động khuyến mãi", actModel));

        return panel;
    }

    private JPanel createEditableSection(String title, DefaultTableModel model) {
        JPanel section = new JPanel(new BorderLayout(5, 5));
        section.setBackground(new Color(250, 252, 252));
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                title, 0, 0, new Font("Segoe UI", Font.BOLD, 13), AppColors.PRIMARY
        ));

        JTable table = createEditableTable(model);

        // Set combo box cell editors for enum columns
        if (model == condModel) {
            // condModel columns: 0: Mục tiêu (Target), 1: Toán tử (Comp)
            try {
                javax.swing.table.TableColumnModel cm = table.getColumnModel();
                cm.getColumn(0).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.Target.values())));
                cm.getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.Comp.values())));
            } catch (Exception ignored) {}
        } else if (model == actModel) {
            // actModel columns: 0: Loại hành động (ActionType), 1: Mục tiêu (Target)
            try {
                javax.swing.table.TableColumnModel cm = table.getColumnModel();
                cm.getColumn(0).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.ActionType.values())));
                cm.getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.Target.values())));
            } catch (Exception ignored) {}
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        scroll.getViewport().setBackground(Color.WHITE);

        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    /** Bảng editable tự thêm dòng */
    private JTable createEditableTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) { return true; }
        };
        styleTable(table);

        table.putClientProperty("terminateEditOnFocusLost", true);
        table.setForeground(Color.BLACK);
        table.setSelectionForeground(Color.BLACK); // Màu chữ khi cell được chọn

        TableModelListener listener = e -> {
            if (model.getRowCount() == 0) return; // Tránh lỗi khi bảng rỗng
            int last = model.getRowCount() - 1;
            boolean filled = false;
            for (int c = 0; c < model.getColumnCount(); c++) {
                Object val = model.getValueAt(last, c);
                if (val != null && !val.toString().trim().isEmpty()) { filled = true; break; }
            }
            if (filled) model.addRow(new Object[model.getColumnCount()]);
        };
        model.addTableModelListener(listener);

        // Lưu listener để có thể remove sau
        if (model == condModel) condListener = listener;
        else if (model == actModel) actListener = listener;

        if (model.getRowCount() == 0)
            model.addRow(new Object[model.getColumnCount()]);
        return table;
    }

    private void handleSaveFromMainPanel() {
        String name = txtNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên chương trình!");
            return;
        }
        if (dpEndDate.getDate().before(dpStartDate.getDate())) {
            JOptionPane.showMessageDialog(this, "Ngày kết thúc không thể trước ngày bắt đầu!");
            return;
        }

        tableModel.addRow(new Object[]{
                name, dpStartDate.getFormattedDate(), dpEndDate.getFormattedDate(),
                cbTypeField.getSelectedItem(), cbStatusField.getSelectedItem()
        });
        JOptionPane.showMessageDialog(this, "✅ Đã lưu chương trình khuyến mãi!");
        handleClearMainPanel();
    }

    private void handleClearMainPanel() {
        txtCodeField.setText("");
        txtNameField.setText("");
        txtDescField.setText("");
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.MONTH, 6);
        dpStartDate.setDate(today);
        dpEndDate.setDate(cal.getTime());
        cbTypeField.setSelectedIndex(0);
        cbStatusField.setSelectedIndex(0);

        // Clear và thêm lại dòng trống - tạm remove listener để tránh lỗi
        if (condListener != null) condModel.removeTableModelListener(condListener);
        condModel.setRowCount(0);
        condModel.addRow(new Object[condModel.getColumnCount()]);
        if (condListener != null) condModel.addTableModelListener(condListener);

        if (actListener != null) actModel.removeTableModelListener(actListener);
        actModel.setRowCount(0);
        actModel.addRow(new Object[actModel.getColumnCount()]);
        if (actListener != null) actModel.addTableModelListener(actListener);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setShowGrid(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(230, 245, 255));
        table.setSelectionForeground(Color.BLACK); // Màu chữ khi dòng được chọn
        table.setBackground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setBackground(AppColors.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(120, 35));
    }

    private JPanel createFilterCard(String title, String[] options) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                new EmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(new Color(245, 250, 250));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(AppColors.PRIMARY);
        card.add(t);
        card.add(Box.createVerticalStrut(8));
        ButtonGroup g = new ButtonGroup();
        for (String o : options) {
            JRadioButton r = new JRadioButton(o);
            r.setBackground(new Color(245, 250, 250));
            r.setFocusPainted(false);
            g.add(r);
            card.add(r);
            card.add(Box.createVerticalStrut(4));
        }
        return card;
    }
}
