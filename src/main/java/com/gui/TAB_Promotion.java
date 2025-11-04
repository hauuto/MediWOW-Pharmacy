package com.gui;

import com.utils.AppColors;
import com.enums.PromotionEnum;
import com.bus.BUS_Promotion;
import com.entities.Promotion;
import com.entities.PromotionAction;
import com.entities.PromotionCondition;
import com.entities.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Giao diện quản lý khuyến mãi
 * - SplitPane tổng: 60/40
 * - SplitPane bên trong: 40/60
 * - Bảng điều kiện & hành động có thể nhập trực tiếp
 * - Dưới cùng: 3 nút chính (Thêm mới, Cập nhật, Xóa)
 */
public class TAB_Promotion extends JPanel {

    private final BUS_Promotion busPromotion = new BUS_Promotion();

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

    // Giữ tham chiếu JTable danh sách và cache promotion để map row -> promotion
    private JTable table;
    private final List<Promotion> promotionCache = new ArrayList<>();

    // Biến để theo dõi chế độ (thêm mới hay cập nhật)
    private boolean isEditMode = false;
    private String currentEditingId = null;

    // Components cho filter
    private ButtonGroup statusFilterGroup;
    private ButtonGroup validFilterGroup;
    private JTextField txtSearch;
    private JComboBox<String> cbTypeSearch;
    private JComboBox<String> cbStatusSearch;

    public TAB_Promotion() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 250, 250));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // -------------------- TOP: Thanh tìm kiếm --------------------
        JPanel top = createSearchPanel();
        add(top, BorderLayout.NORTH);

        // -------------------- LEFT: Bộ lọc --------------------
        JPanel left = createFilterPanel();
        add(left, BorderLayout.WEST);

        // -------------------- CENTER: Danh sách & Chi tiết --------------------
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.6); // 60:40
        mainSplit.setDividerSize(6);
        mainSplit.setBackground(new Color(245, 250, 250));

        // LEFT: Danh sách
        JPanel mainLeft = createListPanel();

        // RIGHT: Chi tiết
        JPanel mainRight = createMainDetailPanel();

        mainSplit.setLeftComponent(mainLeft);
        mainSplit.setRightComponent(mainRight);
        add(mainSplit, BorderLayout.CENTER);

        // Load dữ liệu Promotion vào bảng
        loadPromotions();
    }

    /** Tạo panel tìm kiếm */
    private JPanel createSearchPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 240)),
                "QUẢN LÝ KHUYẾN MÃI", 0, 0,
                new Font("Segoe UI", Font.BOLD, 16),
                AppColors.PRIMARY
        ));
        top.setBackground(new Color(245, 250, 250));

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        txtSearch = new JTextField(25);
        JButton btnSearch = new JButton("Tìm");
        cbTypeSearch = new JComboBox<>(new String[]{"Tất cả", "Giảm giá", "Tặng phẩm"});
        cbStatusSearch = new JComboBox<>(new String[]{"Tất cả", "Kích hoạt", "Chưa áp dụng"});
        JButton btnRefresh = new JButton("Làm mới");

        styleButton(btnSearch, AppColors.PRIMARY, Color.WHITE);
        styleButton(btnRefresh, AppColors.PRIMARY, Color.WHITE);

        // Sự kiện tìm kiếm
        btnSearch.addActionListener(e -> handleSearch());
        txtSearch.addActionListener(e -> handleSearch());
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            cbTypeSearch.setSelectedIndex(0);
            cbStatusSearch.setSelectedIndex(0);
            resetFilters();
            loadPromotions();
        });

        top.add(lblSearch);
        top.add(txtSearch);
        top.add(btnSearch);
        top.add(btnRefresh);

        return top;
    }

    /** Tạo panel filter bên trái */
    private JPanel createFilterPanel() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(6, 6, 6, 12));
        left.setBackground(new Color(245, 250, 250));

        JPanel statusCard = createFilterCard("Trạng thái",
            new String[]{"Tất cả", "Kích hoạt", "Chưa áp dụng"}, true);
        JPanel validCard = createFilterCard("Hiệu lực",
            new String[]{"Tất cả", "Còn hiệu lực", "Hết hiệu lực"}, false);

        left.add(statusCard);
        left.add(Box.createVerticalStrut(12));
        left.add(validCard);
        left.add(Box.createVerticalGlue());

        return left;
    }

    /** Tạo panel danh sách khuyến mãi */
    private JPanel createListPanel() {
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
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        // Chọn dòng -> hiển thị chi tiết
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < promotionCache.size()) {
                    showPromotionDetails(promotionCache.get(row));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 230, 240)));
        mainLeft.add(scroll, BorderLayout.CENTER);

        return mainLeft;
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
        JButton btnUpdate = new JButton("Cập nhật");
        styleButton(btnUpdate, new Color(255, 193, 7), Color.WHITE);
        JButton btnClear = new JButton("Xóa trắng");
        styleButton(btnClear, AppColors.PRIMARY, Color.WHITE);

        btnAdd.addActionListener(e -> handleAdd());
        btnUpdate.addActionListener(e -> handleUpdate());
        btnClear.addActionListener(e -> handleClearMainPanel());

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
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
        txtCodeField.setEditable(false); // Không cho sửa mã
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

        // Set combo box cell editors cho các cột enum
        if (model == condModel) {
            try {
                javax.swing.table.TableColumnModel cm = table.getColumnModel();
                // cột 0: Target, cột 1: Comparator
                cm.getColumn(0).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.Target.values())));
                cm.getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.Comp.values())));
            } catch (Exception ignored) {}
        } else if (model == actModel) {
            try {
                javax.swing.table.TableColumnModel cm = table.getColumnModel();
                // cột 0: ActionType, cột 1: Target
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

    /** Bảng editable tự thêm dòng và click 1 lần vào cột "Sản phẩm" để chọn sản phẩm */
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

        // ✅ Click 1 lần vào cột "Sản phẩm" để chọn sản phẩm
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4) { // cột "Sản phẩm"
                    Window owner = SwingUtilities.getWindowAncestor(table);
                    DIALOG_ProductPicker picker = new DIALOG_ProductPicker(owner);
                    picker.setVisible(true);
                    Product selected = picker.getSelectedProduct();
                    if (selected != null) {
                        // Điền mã sản phẩm (id) vào ô
                        model.setValueAt(selected.getId(), row, col);
                    }
                }
            }
        });

        return table;
    }

    /** Xử lý thêm mới */
    private void handleAdd() {
        // Validation
        String name = txtNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên chương trình!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (dpEndDate.getDate().before(dpStartDate.getDate())) {
            JOptionPane.showMessageDialog(this, "Ngày kết thúc không thể trước ngày bắt đầu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Promotion promotion = buildPromotionFromForm(true);
            if (promotion == null) return;

            // Lưu vào database
            boolean success = busPromotion.addPromotion(promotion);

            if (success) {
                JOptionPane.showMessageDialog(this,
                    "✅ Đã thêm chương trình khuyến mãi thành công!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                handleClearMainPanel();
                loadPromotions(); // Reload danh sách
            } else {
                JOptionPane.showMessageDialog(this,
                    "❌ Không thể thêm khuyến mãi. Vui lòng kiểm tra lại thông tin hoặc xem console!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "❌ Lỗi khi thêm khuyến mãi: " + ex.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Xử lý cập nhật */
    private void handleUpdate() {
        if (currentEditingId == null || currentEditingId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn một khuyến mãi từ danh sách để cập nhật!",
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validation
        String name = txtNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên chương trình!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (dpEndDate.getDate().before(dpStartDate.getDate())) {
            JOptionPane.showMessageDialog(this, "Ngày kết thúc không thể trước ngày bắt đầu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Promotion promotion = buildPromotionFromForm(false);
            if (promotion == null) return;

            promotion.setId(currentEditingId); // Giữ nguyên ID

            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn cập nhật khuyến mãi này?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) return;

            boolean success = busPromotion.updatePromotion(promotion);

            if (success) {
                JOptionPane.showMessageDialog(this,
                    "✅ Đã cập nhật khuyến mãi thành công!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                handleClearMainPanel();
                loadPromotions();
            } else {
                JOptionPane.showMessageDialog(this,
                    "❌ Không thể cập nhật khuyến mãi!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "❌ Lỗi khi cập nhật: " + ex.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }


    /** Xây dựng object Promotion từ form */
    private Promotion buildPromotionFromForm(boolean isNew) {
        String name = txtNameField.getText().trim();
        String description = txtDescField.getText().trim();

        // Convert Date to LocalDate
        LocalDate startDate = dpStartDate.getDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = dpEndDate.getDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();

        String promoId = isNew ? generatePromotionId() : currentEditingId;

        boolean isActive = cbStatusField.getSelectedIndex() == 0;

        Promotion promotion = new Promotion(promoId, name, startDate, endDate, isActive, description);

        // Thu thập điều kiện từ bảng condModel
        java.util.Set<PromotionCondition> conditions = new java.util.HashSet<>();
        int condCounter = 1;
        for (int i = 0; i < condModel.getRowCount(); i++) {
            Object targetObj = condModel.getValueAt(i, 0);
            Object compObj = condModel.getValueAt(i, 1);
            Object val1Obj = condModel.getValueAt(i, 2);

            // Bỏ qua dòng trống
            if (targetObj == null && compObj == null && val1Obj == null) continue;

            if (targetObj == null || compObj == null || val1Obj == null) {
                JOptionPane.showMessageDialog(this,
                    "Điều kiện dòng " + (i+1) + " chưa đầy đủ thông tin!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            PromotionEnum.Target target = (targetObj instanceof PromotionEnum.Target)
                ? (PromotionEnum.Target) targetObj
                : PromotionEnum.Target.valueOf(targetObj.toString());

            PromotionEnum.Comp comp = (compObj instanceof PromotionEnum.Comp)
                ? (PromotionEnum.Comp) compObj
                : PromotionEnum.Comp.valueOf(compObj.toString());

            Double val1 = parseDouble(val1Obj);
            Double val2 = parseDouble(condModel.getValueAt(i, 3));

            Object prodIdObj = condModel.getValueAt(i, 4);
            Product product = null;
            if (prodIdObj != null && !prodIdObj.toString().trim().isEmpty()) {
                String productId = prodIdObj.toString().trim();
                product = new Product(productId, null, null, null, null, null, null, null, 0, null, null, null, null, null, null);
            }

            PromotionCondition cond = new PromotionCondition(
                target, comp, PromotionEnum.ConditionType.PRODUCT_QTY, val1, val2, product
            );
            // Generate ID cho condition
            String condId = promoId.replace("PRM-", "PRMC-") + "-" + String.format("%02d", condCounter++);
            cond.setId(condId);

            conditions.add(cond);
        }

        // Thu thập hành động từ bảng actModel
        java.util.Set<PromotionAction> actions = new java.util.HashSet<>();
        int actCounter = 1;
        for (int i = 0; i < actModel.getRowCount(); i++) {
            Object typeObj = actModel.getValueAt(i, 0);
            Object targetObj = actModel.getValueAt(i, 1);
            Object val1Obj = actModel.getValueAt(i, 2);

            // Bỏ qua dòng trống
            if (typeObj == null && targetObj == null && val1Obj == null) continue;

            if (typeObj == null || targetObj == null || val1Obj == null) {
                JOptionPane.showMessageDialog(this,
                    "Hành động dòng " + (i+1) + " chưa đầy đủ thông tin!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            PromotionEnum.ActionType type = (typeObj instanceof PromotionEnum.ActionType)
                ? (PromotionEnum.ActionType) typeObj
                : PromotionEnum.ActionType.valueOf(typeObj.toString());

            PromotionEnum.Target target = (targetObj instanceof PromotionEnum.Target)
                ? (PromotionEnum.Target) targetObj
                : PromotionEnum.Target.valueOf(targetObj.toString());

            Double val1 = parseDouble(val1Obj);
            Double val2 = parseDouble(actModel.getValueAt(i, 3));

            Object prodIdObj = actModel.getValueAt(i, 4);
            Product product = null;
            if (prodIdObj != null && !prodIdObj.toString().trim().isEmpty()) {
                String productId = prodIdObj.toString().trim();
                product = new Product(productId, null, null, null, null, null, null, null, 0, null, null, null, null, null, null);
            }

            PromotionAction action = new PromotionAction(
                type, target, val1, val2, product, i
            );
            // Generate ID cho action
            String actId = promoId.replace("PRM-", "PRMA-") + "-" + String.format("%02d", actCounter++);
            action.setId(actId);

            actions.add(action);
        }

        // Kiểm tra phải có ít nhất 1 điều kiện và 1 hành động
        if (conditions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Phải có ít nhất 1 điều kiện áp dụng!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (actions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Phải có ít nhất 1 hành động khuyến mãi!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        promotion.setConditions(conditions);
        promotion.setActions(actions);

        return promotion;
    }

    /** Generate ID cho Promotion theo format PRM-YYYYMMDD-XXXX */
    private String generatePromotionId() {
        List<Promotion> allPromotions = busPromotion.getAllPromotions();
        int maxNum = 0;

        String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PRM-" + today + "-";

        if (allPromotions != null) {
            for (Promotion p : allPromotions) {
                if (p.getId() != null && p.getId().startsWith(prefix)) {
                    try {
                        String numPart = p.getId().substring(prefix.length());
                        int num = Integer.parseInt(numPart);
                        if (num > maxNum) maxNum = num;
                    } catch (Exception ignored) {}
                }
            }
        }

        return prefix + String.format("%04d", maxNum + 1);
    }

    /** Helper để parse Double từ Object */
    private Double parseDouble(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Double) return (Double) obj;
            if (obj instanceof Number) return ((Number) obj).doubleValue();
            String str = obj.toString().trim();
            if (str.isEmpty()) return null;
            return Double.parseDouble(str);
        } catch (Exception e) {
            return null;
        }
    }

    /** Xử lý tìm kiếm */
    private void handleSearch() {
        String keyword = txtSearch.getText().trim();

        if (keyword.isEmpty()) {
            loadPromotions();
            return;
        }

        tableModel.setRowCount(0);
        promotionCache.clear();

        List<Promotion> promotions = busPromotion.searchPromotions(keyword);
        if (promotions != null) {
            for (Promotion p : promotions) {
                promotionCache.add(p);
                addPromotionToTable(p);
            }
        }
    }

    /** Reset các filter */
    private void resetFilters() {
        if (statusFilterGroup != null) {
            for (java.util.Enumeration<AbstractButton> buttons = statusFilterGroup.getElements();
                 buttons.hasMoreElements();) {
                AbstractButton button = buttons.nextElement();
                if (button.getText().equals("Tất cả")) {
                    button.setSelected(true);
                    break;
                }
            }
        }
        if (validFilterGroup != null) {
            for (java.util.Enumeration<AbstractButton> buttons = validFilterGroup.getElements();
                 buttons.hasMoreElements();) {
                AbstractButton button = buttons.nextElement();
                if (button.getText().equals("Tất cả")) {
                    button.setSelected(true);
                    break;
                }
            }
        }
    }

    /** Xử lý filter */
    private void handleFilter() {
        Boolean isActive = null;
        Boolean isValid = null;

        // Lấy giá trị từ status filter
        if (statusFilterGroup != null) {
            String statusSelected = getSelectedRadioButtonText(statusFilterGroup);
            if ("Kích hoạt".equals(statusSelected)) {
                isActive = true;
            } else if ("Chưa áp dụng".equals(statusSelected)) {
                isActive = false;
            }
        }

        // Lấy giá trị từ validity filter
        if (validFilterGroup != null) {
            String validSelected = getSelectedRadioButtonText(validFilterGroup);
            if ("Còn hiệu lực".equals(validSelected)) {
                isValid = true;
            } else if ("Hết hiệu lực".equals(validSelected)) {
                isValid = false;
            }
        }

        tableModel.setRowCount(0);
        promotionCache.clear();

        List<Promotion> promotions = busPromotion.filterPromotions(isActive, isValid);
        if (promotions != null) {
            for (Promotion p : promotions) {
                promotionCache.add(p);
                addPromotionToTable(p);
            }
        }
    }

    /** Lấy text của radio button được chọn */
    private String getSelectedRadioButtonText(ButtonGroup group) {
        for (java.util.Enumeration<AbstractButton> buttons = group.getElements();
             buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                return button.getText();
            }
        }
        return null;
    }

    /** Load danh sách promotion từ database */
    private void loadPromotions() {
        tableModel.setRowCount(0);
        promotionCache.clear();

        List<Promotion> promotions = busPromotion.getAllPromotions();
        if (promotions != null) {
            for (Promotion p : promotions) {
                promotionCache.add(p);
                addPromotionToTable(p);
            }
        }
    }

    /** Thêm promotion vào bảng */
    private void addPromotionToTable(Promotion p) {
        String status = p.getIsActive() ? "Kích hoạt" : "Chưa áp dụng";
        String type = "Khuyến mãi"; // Có thể tùy chỉnh dựa trên actions

        tableModel.addRow(new Object[]{
            p.getName(),
            p.getEffectiveDate(),
            p.getEndDate(),
            type,
            status
        });
    }

    /** Hiển thị chi tiết promotion khi chọn dòng */
    private void showPromotionDetails(Promotion promo) {
        if (promo == null) return;

        isEditMode = true;
        currentEditingId = promo.getId();

        // Điền thông tin cơ bản
        txtCodeField.setText(promo.getId());
        txtNameField.setText(promo.getName());
        txtDescField.setText(promo.getDescription() != null ? promo.getDescription() : "");

        // Convert LocalDate to Date
        Date startDate = Date.from(promo.getEffectiveDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(promo.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        dpStartDate.setDate(startDate);
        dpEndDate.setDate(endDate);

        cbStatusField.setSelectedIndex(promo.getIsActive() ? 0 : 1);

        // Load conditions
        condModel.removeTableModelListener(condListener);
        condModel.setRowCount(0);

        if (promo.getConditions() != null) {
            for (PromotionCondition cond : promo.getConditions()) {
                condModel.addRow(new Object[]{
                    cond.getTarget(),
                    cond.getComparator(),
                    cond.getPrimaryValue(),
                    cond.getSecondaryValue(),
                    cond.getProduct() != null ? cond.getProduct().getId() : ""
                });
            }
        }
        condModel.addRow(new Object[condModel.getColumnCount()]);
        condModel.addTableModelListener(condListener);

        // Load actions
        actModel.removeTableModelListener(actListener);
        actModel.setRowCount(0);

        if (promo.getActions() != null) {
            for (PromotionAction act : promo.getActions()) {
                actModel.addRow(new Object[]{
                    act.getType(),
                    act.getTarget(),
                    act.getPrimaryValue(),
                    act.getSecondaryValue(),
                    act.getProduct() != null ? act.getProduct().getId() : ""
                });
            }
        }
        actModel.addRow(new Object[actModel.getColumnCount()]);
        actModel.addTableModelListener(actListener);
    }

    /** Xóa trắng form */
    private void handleClearMainPanel() {
        isEditMode = false;
        currentEditingId = null;

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

        // Clear conditions
        if (condListener != null) condModel.removeTableModelListener(condListener);
        condModel.setRowCount(0);
        condModel.addRow(new Object[condModel.getColumnCount()]);
        if (condListener != null) condModel.addTableModelListener(condListener);

        // Clear actions
        if (actListener != null) actModel.removeTableModelListener(actListener);
        actModel.setRowCount(0);
        actModel.addRow(new Object[actModel.getColumnCount()]);
        if (actListener != null) actModel.addTableModelListener(actListener);

        // Clear selection
        table.clearSelection();
    }

    /** Style button */
    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 35));
    }

    /** Style table */
    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(AppColors.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(200, 230, 255));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(220, 230, 240));
    }

    /** Tạo filter card */
    private JPanel createFilterCard(String title, String[] options, boolean isStatusFilter) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 230, 240)),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(AppColors.PRIMARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(8));

        ButtonGroup group = new ButtonGroup();
        if (isStatusFilter) {
            statusFilterGroup = group;
        } else {
            validFilterGroup = group;
        }

        for (String opt : options) {
            JRadioButton radio = new JRadioButton(opt);
            radio.setBackground(Color.WHITE);
            radio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (opt.equals("Tất cả")) radio.setSelected(true);

            // Thêm listener để filter khi chọn
            radio.addActionListener(e -> handleFilter());

            group.add(radio);
            card.add(radio);
            card.add(Box.createVerticalStrut(4));
        }

        return card;
    }
}
