package com.gui;

import com.utils.AppColors;
import com.enums.PromotionEnum;
import com.bus.BUS_Promotion;
import com.entities.Promotion;
import com.entities.PromotionAction;
import com.entities.PromotionCondition;
import com.entities.Product;
import com.dao.DAO_Product;
import com.entities.UnitOfMeasure;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

/**
 * Giao diện quản lý khuyến mãi
 * - SplitPane tổng: 60/40
 * - SplitPane bên trong: 40/60
 * - Bảng điều kiện & hành động có thể nhập trực tiếp
 * - Dưới cùng: 2 nút chính (context-aware)
 */
public class TAB_Promotion extends JPanel {

    private final BUS_Promotion busPromotion = new BUS_Promotion();
    private final DAO_Product daoProduct = new DAO_Product();

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
    private final List<Promotion> allPromotions = new ArrayList<>(); // Store all promotions for pagination

    // Pagination fields
    private int currentPage = 0;
    private int itemsPerPage = 10; // Default 10 items per page
    private JLabel lblPageInfo;
    private JButton btnPrevPage;
    private JButton btnNextPage;
    private JComboBox<Integer> cbPageSize;

    // Các table editable trong phần điều kiện/hành động (lưu tham chiếu để enable/disable)
    private JTable condTable;
    private JTable actTable;

    // Biến để theo dõi chế độ
    private boolean isViewing = false;       // true khi đang xem 1 promotion đã chọn
    private boolean isAdding = false;        // true khi đang ở chế độ thêm mới
    private boolean isEditingFields = false; // true khi các trường đang bật để sửa
    private String currentEditingId = null;
    // Flag để yêu cầu nhấn lần 2 để xác nhận lưu khi thêm mới
    private boolean addConfirmPending = false;

    // Components cho filter
    private ButtonGroup statusFilterGroup;
    private ButtonGroup validFilterGroup;
    private JTextField txtSearch;
    private JComboBox<String> cbTypeSearch;
    private JComboBox<String> cbStatusSearch;

    // UI controls cần truy cập từ các method khác
    private JPanel detailCards; // CardLayout container
    private final String CARD_PLACEHOLDER = "placeholder";
    private final String CARD_DETAIL = "detail";

    private JButton bottomPrimaryButton;   // Cập nhật / Thêm mới
    private JButton bottomSecondaryButton; // Hủy

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

        // RIGHT: Chi tiết (card layout so we can show placeholder)
        JPanel mainRight = createMainDetailPanel();

        mainSplit.setLeftComponent(mainLeft);
        mainSplit.setRightComponent(mainRight);
        add(mainSplit, BorderLayout.CENTER);

        // Load dữ liệu Promotion vào bảng
        loadPromotions();

        // Start with placeholder visible
        showPlaceholder();
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
        txtSearch = new JTextField(20);
        JButton btnSearch = new JButton("Tìm");
        cbTypeSearch = new JComboBox<>(new String[]{"Tất cả", "Giảm giá", "Tặng phẩm"});
        cbStatusSearch = new JComboBox<>(new String[]{"Tất cả", "Kích hoạt", "Chưa áp dụng"});
        JButton btnRefresh = new JButton("Làm mới");

        // Thêm nút Thêm mới lên top theo yêu cầu
        JButton topAddButton = new JButton("Thêm mới");
        styleButton(topAddButton, new Color(40, 167, 69));

        styleButton(btnSearch, AppColors.PRIMARY);
        styleButton(btnRefresh, AppColors.PRIMARY);

        // Sự kiện tìm kiếm
        btnSearch.addActionListener(e -> handleSearch());
        txtSearch.addActionListener(e -> handleSearch());
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            cbTypeSearch.setSelectedIndex(0);
            cbStatusSearch.setSelectedIndex(0);
            resetFilters();
            loadPromotions();
            // reset UI
            handleCancelAll();
        });

        topAddButton.addActionListener(e -> startAddMode());

        top.add(lblSearch);
        top.add(txtSearch);
        top.add(btnSearch);
        top.add(btnRefresh);
        top.add(topAddButton);

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
        styleTable(table, false);

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

        // -------------------- PAGINATION CONTROLS --------------------
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        paginationPanel.setBackground(new Color(245, 250, 250));
        paginationPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        btnPrevPage = new JButton("« Trang trước");
        btnNextPage = new JButton("Trang tiếp »");
        cbPageSize = new JComboBox<>(new Integer[]{5, 10, 20, 50});

        lblPageInfo = new JLabel();
        lblPageInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPageInfo.setForeground(new Color(100, 100, 100));

        // Style buttons
        styleButton(btnPrevPage, AppColors.PRIMARY);
        styleButton(btnNextPage, AppColors.PRIMARY);

        // Sự kiện cho pagination
        btnPrevPage.addActionListener(e -> changePage(currentPage - 1));
        btnNextPage.addActionListener(e -> changePage(currentPage + 1));
        cbPageSize.addActionListener(e -> {
            itemsPerPage = (int) cbPageSize.getSelectedItem();
            currentPage = 0; // Reset về trang 1
            loadPromotions();
        });

        paginationPanel.add(btnPrevPage);
        paginationPanel.add(btnNextPage);
        paginationPanel.add(cbPageSize);
        paginationPanel.add(lblPageInfo);

        mainLeft.add(paginationPanel, BorderLayout.SOUTH);

        return mainLeft;
    }

    /** Panel chi tiết bên phải - chuyển sang CardLayout để hỗ trợ placeholder */
    private JPanel createMainDetailPanel() {
        detailCards = new JPanel(new CardLayout());

        // Placeholder card
        JPanel placeholderCard = new JPanel(new BorderLayout());
        placeholderCard.setBackground(new Color(245, 250, 250));
        JLabel placeholderLabelLocal = new JLabel("Bấm vào 1 khuyến mãi bất kỳ để xem chi tiết", SwingConstants.CENTER);
        placeholderLabelLocal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        placeholderLabelLocal.setForeground(new Color(180, 180, 180));
        placeholderCard.add(placeholderLabelLocal, BorderLayout.CENTER);

        // Real detail card
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
                new String[]{"Mục tiêu", "Sản phẩm", "Đơn vị", "Toán tử", "Giá trị"}, 0);
        actModel = new DefaultTableModel(
                new String[]{"Loại hành động", "Mục tiêu", "Giá trị", "Sản phẩm", "Đơn vị"}, 0);

        JPanel infoPanel = createInfoPanel();
        JPanel tablesPanel = createTablesPanel();

        // --- SplitPane trong: 40:60 ---
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        verticalSplit.setResizeWeight(0.4);
        verticalSplit.setDividerSize(6);
        verticalSplit.setTopComponent(infoPanel);
        verticalSplit.setBottomComponent(tablesPanel);
        verticalSplit.setBorder(null);

        // --- Nút chức năng (hai nút) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(245, 250, 250));

        bottomPrimaryButton = new JButton("Cập nhật");
        styleButton(bottomPrimaryButton, new Color(255, 193, 7));
        bottomSecondaryButton = new JButton("Hủy");
        styleButton(bottomSecondaryButton, AppColors.PRIMARY);

        // Default listeners will be replaced dynamically in flows
        bottomPrimaryButton.addActionListener(e -> {
            // If adding mode -> confirm add
            if (isAdding) {
                // Two-step: first click arms confirmation, second click will perform confirmation dialog
                if (!addConfirmPending) {
                    addConfirmPending = true;
                    bottomPrimaryButton.setText("Xác nhận");
                    styleButton(bottomPrimaryButton, new Color(255, 87, 34));
                    return;
                }

                // second click -> show confirmation dialog and save
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Bạn có chắc muốn thêm khuyến mãi này?",
                        "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    handleAdd();
                } else {
                    // user cancelled -> revert the confirm pending state
                    addConfirmPending = false;
                    bottomPrimaryButton.setText("Thêm mới");
                    styleButton(bottomPrimaryButton, new Color(40, 167, 69));
                }
            } else if (isViewing) {
                // Update flow: if fields not editable -> enable editing; else perform update (handleUpdate already asks confirm)
                if (!isEditingFields) {
                    setFormEditable(true);
                    isEditingFields = true;
                    // keep label as "Cập nhật" (user will press again to save)
                } else {
                    handleUpdate();
                }
            }
        });

        bottomSecondaryButton.addActionListener(e -> {
            // Cancel for both add and update flows
            handleCancelAll();
        });

        buttonPanel.add(bottomPrimaryButton);
        buttonPanel.add(bottomSecondaryButton);

        mainPanel.add(verticalSplit, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Compose cards
        detailCards.add(placeholderCard, CARD_PLACEHOLDER);
        detailCards.add(mainPanel, CARD_DETAIL);

        // Initially show placeholder
        return detailCards;
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

        panel.add(createEditableSection("Điều kiện áp dụng", condModel, true));
        panel.add(Box.createVerticalStrut(12));
        panel.add(createEditableSection("Hành động khuyến mãi", actModel, false));

        return panel;
    }

    private JPanel createEditableSection(String title, DefaultTableModel model, boolean isCondition) {
        JPanel section = new JPanel(new BorderLayout(5, 5));
        section.setBackground(new Color(250, 252, 252));
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 240)),
                title, 0, 0, new Font("Segoe UI", Font.BOLD, 13), AppColors.PRIMARY
        ));

        JTable table = createEditableTable(model, isCondition);

        // Keep references to editable tables for enable/disable
        if (model == condModel) condTable = table;
        else if (model == actModel) actTable = table;

        // Set combo box cell editors cho các cột enum
        if (isCondition) {
            try {
                javax.swing.table.TableColumnModel cm = table.getColumnModel();
                // cột 0: Target, cột 3: Comparator (đã đổi vị trí)
                cm.getColumn(0).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.Target.values())));
                cm.getColumn(3).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.Comp.values())));

                // Thêm validation cho cột Giá trị (cột 4) - không cho số âm và chữ cái
                cm.getColumn(4).setCellEditor(new DefaultCellEditor(new JTextField()) {
                    @Override
                    public boolean stopCellEditing() {
                        String value = ((JTextField) getComponent()).getText().trim();
                        if (!value.isEmpty()) {
                            try {
                                double val = Double.parseDouble(value);
                                if (val < 0) {
                                    JOptionPane.showMessageDialog(table, "Giá trị không được là số âm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                                    return false;
                                }
                            } catch (NumberFormatException e) {
                                JOptionPane.showMessageDialog(table, "Giá trị phải là số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                        }
                        return super.stopCellEditing();
                    }
                });
            } catch (Exception ignored) {}
        } else {
            try {
                javax.swing.table.TableColumnModel cm = table.getColumnModel();
                // cột 0: ActionType, cột 1: Target (đã đổi vị trí từ 3 -> 1)
                cm.getColumn(0).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.ActionType.values())));
                cm.getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(PromotionEnum.Target.values())));

                // Thêm validation cho cột Giá trị (cột 2) - không cho số âm và chữ cái
                cm.getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()) {
                    @Override
                    public boolean stopCellEditing() {
                        String value = ((JTextField) getComponent()).getText().trim();
                        if (!value.isEmpty()) {
                            try {
                                double val = Double.parseDouble(value);
                                if (val < 0) {
                                    JOptionPane.showMessageDialog(table, "Giá trị không được là số âm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                                    return false;
                                }
                            } catch (NumberFormatException e) {
                                JOptionPane.showMessageDialog(table, "Giá trị phải là số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                        }
                        return super.stopCellEditing();
                    }
                });
            } catch (Exception ignored) {}
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 230, 240)));
        scroll.getViewport().setBackground(Color.WHITE);

        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    /** Bảng editable tự thêm dòng và click 1 lần vào cột "Sản phẩm" để chọn sản phẩm */
    private JTable createEditableTable(DefaultTableModel model, boolean isCondition) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Kiểm tra chế độ editing - nếu đang ở chế độ xem (không phải adding/editing), disable tất cả
                if (isViewing && !isEditingFields) {
                    return false;
                }

                // Kiểm tra logic ẩn/hiện dựa trên các trường khác
                if (isCondition) {
                    // Với điều kiện: nếu Target = ORDER_SUBTOTAL -> disable cột Sản phẩm (1) và Đơn vị (2)
                    Object targetObj = model.getValueAt(row, 0);
                    if (targetObj != null && (column == 1 || column == 2)) {
                        PromotionEnum.Target target = (targetObj instanceof PromotionEnum.Target)
                            ? (PromotionEnum.Target) targetObj
                            : PromotionEnum.Target.valueOf(targetObj.toString());
                        if (target == PromotionEnum.Target.ORDER_SUBTOTAL) {
                            return false; // Không cho edit Sản phẩm và Đơn vị
                        }
                    }
                    // Chỉ cho phép chỉnh sửa sản phẩm khi Target = PRODUCT
                    if (column == 1) { // Cột sản phẩm
                        if (targetObj == null) return false;
                        PromotionEnum.Target target = (targetObj instanceof PromotionEnum.Target)
                            ? (PromotionEnum.Target) targetObj
                            : PromotionEnum.Target.valueOf(targetObj.toString());
                        return target == PromotionEnum.Target.PRODUCT;
                    }
                } else {
                    // Với hành động: Chỉ dựa vào Target, không quan tâm ActionType
                    // NEW ORDER: 0=ActionType, 1=Target, 2=Value, 3=Product, 4=UOM
                    Object targetObj = model.getValueAt(row, 1);

                    // Chỉ cho phép chỉnh sửa sản phẩm khi Target = PRODUCT
                    if (column == 3) { // Cột sản phẩm
                        if (targetObj == null) return false;
                        PromotionEnum.Target target = (targetObj instanceof PromotionEnum.Target)
                            ? (PromotionEnum.Target) targetObj
                            : PromotionEnum.Target.valueOf(targetObj.toString());
                        return target == PromotionEnum.Target.PRODUCT;
                    }

                    // Chỉ cho phép chỉnh sửa đơn vị khi Target = PRODUCT
                    if (column == 4) { // Cột đơn vị
                        if (targetObj == null) return false;
                        PromotionEnum.Target target = (targetObj instanceof PromotionEnum.Target)
                            ? (PromotionEnum.Target) targetObj
                            : PromotionEnum.Target.valueOf(targetObj.toString());
                        return target == PromotionEnum.Target.PRODUCT;
                    }
                }
                return true;
            }
        };

        styleTable(table, true); // Hiển thị viền cho bảng điều kiện và hành động

        // VÔ HIỆU HÓA KÉO THẢ CỘT
        table.getTableHeader().setReorderingAllowed(false);

        table.putClientProperty("terminateEditOnFocusLost", true);
        table.setForeground(Color.BLACK);
        table.setSelectionForeground(Color.BLACK); // Màu chữ khi cell được chọn

        // Yêu cầu 4: Các ô bị disable nên có màu xám
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                // Ép measurementName về String để tránh lazy
                if (value instanceof com.entities.MeasurementName m) {
                    value = m.getName();
                }

                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );

                if (!table.isCellEditable(row, column)) {
                    c.setBackground(new Color(240, 240, 240));
                    c.setForeground(new Color(150, 150, 150));
                } else if (isSelected) {
                    c.setBackground(new Color(200, 230, 255));
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });


        TableModelListener listener = e -> {
            // Yêu cầu 5: Chỉ thêm hàng mới khi hàng cuối được điền đầy đủ
            if (model.getRowCount() == 0) {
                model.addRow(new Object[model.getColumnCount()]);
                return;
            }

            int last = model.getRowCount() - 1;
            boolean isLastRowComplete = true;

            // Kiểm tra xem hàng cuối có được điền đầy đủ không
            for (int c = 0; c < model.getColumnCount(); c++) {
                // Bỏ qua các cột bị disable
                if (!table.isCellEditable(last, c)) continue;

                Object val = model.getValueAt(last, c);
                if (val == null || val.toString().trim().isEmpty()) {
                    isLastRowComplete = false;
                    break;
                }
            }

            // Chỉ thêm hàng mới nếu hàng cuối đã hoàn thành
            if (isLastRowComplete) {
                model.addRow(new Object[model.getColumnCount()]);
            }

            // Validation logic khi thay đổi
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row >= 0 && col >= 0) {
                if (!isCondition) {
                    // Action table: kiểm tra ActionType
                    // NEW ORDER: 0=ActionType, 1=Target, 2=Value, 3=Product, 4=UOM
                    if (col == 0) { // ActionType changed
                        Object typeObj = model.getValueAt(row, 0);
                        if (typeObj != null) {
                            PromotionEnum.ActionType actionType = (typeObj instanceof PromotionEnum.ActionType)
                                ? (PromotionEnum.ActionType) typeObj
                                : PromotionEnum.ActionType.valueOf(typeObj.toString());

                            if (actionType == PromotionEnum.ActionType.PRODUCT_GIFT) {
                                // Tự động set Target = PRODUCT
                                model.setValueAt(PromotionEnum.Target.PRODUCT, row, 1);
                            } else if (actionType == PromotionEnum.ActionType.PERCENT_DISCOUNT ||
                                       actionType == PromotionEnum.ActionType.FIXED_DISCOUNT) {
                                // Xóa Sản phẩm và Đơn vị
                                model.setValueAt("", row, 3);
                                model.setValueAt("", row, 4);
                            }
                        }
                    }
                }

                // Cả 2 bảng: kiểm tra Target
                int targetCol = isCondition ? 0 : 1;
                if (col == targetCol) {
                    Object targetObj = model.getValueAt(row, targetCol);
                    if (targetObj != null) {
                        PromotionEnum.Target target = (targetObj instanceof PromotionEnum.Target)
                            ? (PromotionEnum.Target) targetObj
                            : PromotionEnum.Target.valueOf(targetObj.toString());

                        if (target == PromotionEnum.Target.ORDER_SUBTOTAL) {
                            // Xóa Sản phẩm và Đơn vị
                            int prodCol = isCondition ? 1 : 3;
                            int uomCol = isCondition ? 2 : 4;
                            model.setValueAt("", row, prodCol);
                            model.setValueAt("", row, uomCol);
                        }
                    }
                }

                // Yêu cầu 3: Khi thay đổi sản phẩm, xóa đơn vị
                int prodCol = isCondition ? 1 : 3;
                int uomCol = isCondition ? 2 : 4;
                if (col == prodCol) {
                    model.setValueAt("", row, uomCol);
                }

                // Refresh table to update disabled cell rendering
                table.repaint();
            }
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
                if (row >= 0) {
                    // For action table: NEW ORDER is 0=ActionType, 1=Target, 2=Value, 3=Product, 4=UOM
                    // For condition table: ORDER is 0=Target, 1=Product, 2=UOM, 3=Comparator, 4=Value
                    int prodCol = isCondition ? 1 : 3;
                    int uomCol = isCondition ? 2 : 4;

                    if (col == prodCol) { // cột "Sản phẩm"
                        // Kiểm tra xem có được phép edit không
                        if (!table.isCellEditable(row, col)) {
                            JOptionPane.showMessageDialog(table,
                                "Vui lòng chọn Mục tiêu là 'Sản phẩm' trước!",
                                "Thông báo",
                                JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        Window owner = SwingUtilities.getWindowAncestor(table);
                        DIALOG_ProductPicker picker = new DIALOG_ProductPicker(owner);
                        picker.setVisible(true);
                        Product selected = picker.getSelectedProduct();
                        if (selected != null) {
                            // Yêu cầu 1: Điền tên sản phẩm vào thay vì mã sản phẩm
                            model.setValueAt(selected.getName() + " (" + selected.getId() + ")", row, col);
                            // Lưu ID thực tế vào client property của table để dùng khi lưu
                            table.putClientProperty("product_" + row + "_" + col, selected.getId());
                        }
                    } else if (col == uomCol) { // cột "Đơn vị" (UOM)
                        // Kiểm tra xem có được phép edit không
                        if (!table.isCellEditable(row, col)) {
                            return; // Bị disable
                        }

                        Object prodIdObj = model.getValueAt(row, prodCol);
                        if (prodIdObj == null || prodIdObj.toString().trim().isEmpty()) {
                            JOptionPane.showMessageDialog(table, "Vui lòng chọn sản phẩm trước khi chọn đơn vị tính.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        // Extract product ID from the stored property
                        String productId = (String) table.getClientProperty("product_" + row + "_" + prodCol);
                        if (productId == null) {
                            JOptionPane.showMessageDialog(table, "Vui lòng chọn lại sản phẩm.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        Product p = daoProduct.getProductById(productId);
                        if (p == null || p.getUnitOfMeasureList() == null || p.getUnitOfMeasureList().isEmpty()) {
                            JOptionPane.showMessageDialog(table, "Không tìm thấy đơn vị tính cho sản phẩm đã chọn.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        // Yêu cầu 3: Combobox cho đơn vị
                        List<String> names = new ArrayList<>();
                        for (UnitOfMeasure u : p.getUnitOfMeasureList()) {
                            if (u != null && u.getMeasurement()!= null) names.add(u.getMeasurement().getName());
                        }
                        if (names.isEmpty()) {
                            JOptionPane.showMessageDialog(table, "Sản phẩm chưa có đơn vị tính.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        Object selected = JOptionPane.showInputDialog(table, "Chọn đơn vị tính:", "Đơn vị tính", JOptionPane.PLAIN_MESSAGE, null, names.toArray(), names.get(0));
                        if (selected != null) {
                            model.setValueAt(selected.toString(), row, col);
                        }
                    }
                }
            }
        });

        // Thêm KeyListener để xử lý phím Delete cho việc xóa dòng
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        // Xác nhận xóa
                        int confirm = JOptionPane.showConfirmDialog(table,
                                "Bạn có chắc muốn xóa dòng này?",
                                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            // Xóa dòng khỏi model
                            model.removeRow(row);
                            // Thêm dòng trống mới nếu còn thiếu
                            if (model.getRowCount() == 0) {
                                model.addRow(new Object[model.getColumnCount()]);
                            }
                        }
                    }
                }
            }
        });

        return table;
    }

    /** Bắt đầu chế độ thêm mới khi bấm nút Thêm mới trên top */
    private void startAddMode() {
        isAdding = true;
        isViewing = false;
        isEditingFields = true; // allow editing fields immediately
        currentEditingId = null;
        addConfirmPending = false;

        // Clear form and enable editing
        clearFormFields();
        setFormEditable(true);

        // Show detail card
        showDetailCard();

        // Update bottom buttons
        bottomPrimaryButton.setText("Thêm mới");
        styleButton(bottomPrimaryButton, new Color(40, 167, 69));
        bottomSecondaryButton.setText("Hủy");
        styleButton(bottomSecondaryButton, AppColors.PRIMARY);
    }

    /** Xử lý thêm mới (thực tế lưu vào DB) */
    private void handleAdd() {
        // reset pending flag regardless of outcome
        addConfirmPending = false;
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
                // After save, go back to placeholder
                handleCancelAll();
            } else {
                JOptionPane.showMessageDialog(this,
                    "❌ Không thể thêm khuyến mãi. Vui lòng kiểm tra lại thông tin hoặc xem console!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            System.err.println("Lỗi khi thêm khuyến mãi: " + ex.toString());
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

        // If fields are not in edit mode, enable them and return (first click)
        if (!isEditingFields) {
            setFormEditable(true);
            isEditingFields = true;
            return;
        }

        // When fields already editable, validate and confirm then save
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
                // after update, go back to placeholder
                handleCancelAll();
            } else {
                JOptionPane.showMessageDialog(this,
                    "❌ Không thể cập nhật khuyến mãi!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            System.err.println("Lỗi khi cập nhật khuyến mãi: " + ex.toString());
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

        boolean isActive = cbStatusField.getSelectedIndex() == 0;

        Promotion promotion;
        if (isNew) {
            // When adding new promotions, do not generate or set IDs here — DB trigger will assign IDs.
            promotion = new Promotion();
            promotion.setName(name);
            promotion.setEffectiveDate(startDate);
            promotion.setEndDate(endDate);
            promotion.setIsActive(isActive);
            promotion.setDescription(description);
        } else {
            String promoId = currentEditingId;
            promotion = new Promotion(name, startDate, endDate, isActive, description);
        }

        // Thu thập điều kiện từ bảng condModel
        // THỨ TỰ CỘT: Mục tiêu(0), Sản phẩm(1), Đơn vị(2), Toán tử(3), Giá trị(4)
        List<PromotionCondition> conditions = new ArrayList<>();
        for (int i = 0; i < condModel.getRowCount(); i++) {
            Object targetObj = condModel.getValueAt(i, 0);   // Mục tiêu
            Object prodIdObj = condModel.getValueAt(i, 1);   // Sản phẩm
            Object uomNameObj = condModel.getValueAt(i, 2);  // Đơn vị
            Object compObj = condModel.getValueAt(i, 3);     // Toán tử
            Object val1Obj = condModel.getValueAt(i, 4);     // Giá trị

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

            BigDecimal val1BD = parseBigDecimal(val1Obj);

            UnitOfMeasure uom = null;
            if (target == PromotionEnum.Target.PRODUCT) {
                // Extract product ID from client property
                String productId = (String) condTable.getClientProperty("product_" + i + "_1");
                if (productId != null && uomNameObj != null && !uomNameObj.toString().trim().isEmpty()) {
                    String uomName = uomNameObj.toString().trim();
                    uom = daoProduct.getUnitOfMeasureById(productId, uomName);
                }
            }

            PromotionCondition cond = new PromotionCondition(
                target, comp, PromotionEnum.ConditionType.PRODUCT_QTY, val1BD, uom
            );

            conditions.add(cond);
        }

        // Thu thập hành động từ bảng actModel
        // THỨ TỰ CỘT MỚI: Loại hành động(0), Mục tiêu(1), Giá trị(2), Sản phẩm(3), Đơn vị(4)
        List<PromotionAction> actions = new ArrayList<>();
        for (int i = 0; i < actModel.getRowCount(); i++) {
            Object typeObj = actModel.getValueAt(i, 0);      // Loại hành động
            Object targetObj = actModel.getValueAt(i, 1);    // Mục tiêu
            Object val1Obj = actModel.getValueAt(i, 2);      // Giá trị
            Object prodIdObj = actModel.getValueAt(i, 3);    // Sản phẩm
            Object uomNameObj = actModel.getValueAt(i, 4);   // Đơn vị

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

            BigDecimal val1BD = parseBigDecimal(val1Obj);

            UnitOfMeasure uom = null;
            if (target == PromotionEnum.Target.PRODUCT) {
                // Lưu UnitOfMeasure cho tất cả các loại action khi target là PRODUCT
                // Extract product ID from client property
                String productId = (String) actTable.getClientProperty("product_" + i + "_3");
                if (productId != null && uomNameObj != null && !uomNameObj.toString().trim().isEmpty()) {
                    String uomName = uomNameObj.toString().trim();
                    uom = daoProduct.getUnitOfMeasureById(productId, uomName);
                }
            }

            PromotionAction action = new PromotionAction(
                type, target, val1BD, uom, i
            );

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

    /** Helper để parse BigDecimal từ Object (money/percent). */
    private java.math.BigDecimal parseBigDecimal(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof java.math.BigDecimal) {
                return ((java.math.BigDecimal) obj).setScale(2, java.math.RoundingMode.HALF_UP);
            }
            if (obj instanceof Number) {
                // Avoid constructing from double binary fraction by going through string when possible
                String s = String.valueOf(obj);
                return new java.math.BigDecimal(s).setScale(2, java.math.RoundingMode.HALF_UP);
            }
            String str = obj.toString().trim();
            if (str.isEmpty()) return null;

            // allow inputs like "1.234.567" or "1,234,567" or "1.234,56"
            str = str.replace("Đ", "").replace("₫", "").trim();
            // if comma is decimal separator (no dot), convert to dot
            if (str.contains(",") && !str.contains(".")) {
                str = str.replace(",", ".");
            }
            // remove thousands separators (either . or , when followed by 3 digits)
            str = str.replaceAll("(?<=\\d)[,\\.](?=\\d{3}(\\D|$))", "");

            return new java.math.BigDecimal(str).setScale(2, java.math.RoundingMode.HALF_UP);
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
            allPromotions.clear();
            allPromotions.addAll(promotions); // Store all promotions for pagination
            // Chỉ lấy các phần tử trong khoảng currentPage * itemsPerPage
            for (int i = currentPage * itemsPerPage; i < (currentPage + 1) * itemsPerPage && i < promotions.size(); i++) {
                Promotion p = promotions.get(i);
                promotionCache.add(p);
                addPromotionToTable(p);
            }
        }

        // Cập nhật thông tin phân trang
        updatePaginationInfo();
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

        // cancel any pending add confirmation
        addConfirmPending = false;

        isViewing = true;
        isAdding = false;
        isEditingFields = false;
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

        // Load conditions - THỨ TỰ: Mục tiêu(0), Sản phẩm(1), Đơn vị(2), Toán tử(3), Giá trị(4)
        if (condListener != null) condModel.removeTableModelListener(condListener);
        condModel.setRowCount(0);

        if (promo.getConditions() != null) {
            int rowIdx = 0;
            for (PromotionCondition cond : promo.getConditions()) {
                String productDisplay = "";
                String productId = null;

                if (cond.getProductUOM() != null && cond.getProductUOM().getProduct() != null) {
                    Product p = cond.getProductUOM().getProduct();
                    productId = p.getId();
                    productDisplay = p.getName() + " (" + productId + ")";
                }

                condModel.addRow(new Object[]{
                    cond.getTarget(),  // 0: Mục tiêu
                    productDisplay,  // 1: Sản phẩm (tên + mã)
                    cond.getProductUOM() != null ? cond.getProductUOM().getMeasurement() : "",  // 2: Đơn vị
                    cond.getComparator(),  // 3: Toán tử
                    cond.getValue()  // 4: Giá trị
                });

                // Store product ID in client property
                if (productId != null) {
                    condTable.putClientProperty("product_" + rowIdx + "_1", productId);
                }
                rowIdx++;
            }
        }
        condModel.addRow(new Object[condModel.getColumnCount()]);
        if (condListener != null) condModel.addTableModelListener(condListener);

        // Load actions - THỨ TỰ MỚI: Loại hành động(0), Mục tiêu(1), Giá trị(2), Sản phẩm(3), Đơn vị(4)
        if (actListener != null) actModel.removeTableModelListener(actListener);
        actModel.setRowCount(0);

        if (promo.getActions() != null) {
            int rowIdx = 0;
            for (PromotionAction act : promo.getActions()) {
                String productDisplay = "";
                String productId = null;

                if (act.getProductUOM() != null && act.getProductUOM().getProduct() != null) {
                    Product p = act.getProductUOM().getProduct();
                    productId = p.getId();
                    productDisplay = p.getName() + " (" + productId + ")";
                }

                actModel.addRow(new Object[]{
                    act.getType(),  // 0: Loại hành động
                    act.getTarget(),  // 1: Mục tiêu
                    act.getValue(),  // 2: Giá trị
                    productDisplay,  // 3: Sản phẩm (tên + mã)
                    act.getProductUOM() != null ? act.getProductUOM().getMeasurement() : ""  // 4: Đơn vị
                });

                // Store product ID in client property
                if (productId != null) {
                    actTable.putClientProperty("product_" + rowIdx + "_3", productId);
                }
                rowIdx++;
            }
        }
        actModel.addRow(new Object[actModel.getColumnCount()]);
        if (actListener != null) actModel.addTableModelListener(actListener);

        // show detail card and set buttons to update/cancel
        showDetailCard();
        bottomPrimaryButton.setText("Cập nhật");
        styleButton(bottomPrimaryButton, new Color(255, 193, 7));
        bottomSecondaryButton.setText("Hủy");
        styleButton(bottomSecondaryButton, AppColors.PRIMARY);

        // Disable editing until user clicks cập nhật
        setFormEditable(false);
    }

    /** Xóa trắng form */
    private void handleClearMainPanel() {
        isViewing = false;
        isAdding = false;
        isEditingFields = false;
        currentEditingId = null;

        clearFormFields();

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

    /** Cancel current mode and show placeholder */
    private void handleCancelAll() {
        // Reset states
        isAdding = false;
        isViewing = false;
        isEditingFields = false;
        currentEditingId = null;
        addConfirmPending = false;

        // Clear and disable form
        handleClearMainPanel();
        setFormEditable(false);

        // Show placeholder card
        showPlaceholder();

        // Reset bottom buttons text (not visible in placeholder but keep default)
        bottomPrimaryButton.setText("Cập nhật");
        styleButton(bottomPrimaryButton, new Color(255, 193, 7));
        bottomSecondaryButton.setText("Hủy");
        styleButton(bottomSecondaryButton, AppColors.PRIMARY);
    }

    /** Enable or disable form fields and editable tables */
    private void setFormEditable(boolean editable) {
        txtNameField.setEditable(editable);
        txtDescField.setEditable(editable);
        dpStartDate.setEnabled(editable);
        dpEndDate.setEnabled(editable);
        cbTypeField.setEnabled(editable);
        cbStatusField.setEnabled(editable);

        // Repaint tables to update the cell rendering based on new editable state
        if (condTable != null) {
            condTable.repaint();
        }
        if (actTable != null) {
            actTable.repaint();
        }
    }

    private void clearFormFields() {
        txtCodeField.setText("");
        txtNameField.setText("");
        txtDescField.setText("");

        // Clear conditions/actions data (but keep 1 empty row)
        if (condListener != null) condModel.removeTableModelListener(condListener);
        condModel.setRowCount(0);
        condModel.addRow(new Object[condModel.getColumnCount()]);
        if (condListener != null) condModel.addTableModelListener(condListener);

        if (actListener != null) actModel.removeTableModelListener(actListener);
        actModel.setRowCount(0);
        actModel.addRow(new Object[actModel.getColumnCount()]);
        if (actListener != null) actModel.addTableModelListener(actListener);
    }

    private void showPlaceholder() {
        CardLayout cl = (CardLayout) detailCards.getLayout();
        cl.show(detailCards, CARD_PLACEHOLDER);
    }

    private void showDetailCard() {
        CardLayout cl = (CardLayout) detailCards.getLayout();
        cl.show(detailCards, CARD_DETAIL);
    }

    /** Style button */
    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 35));
    }


    private void styleTable(JTable table, boolean showGrid) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(AppColors.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(200, 230, 255));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(220, 230, 240));
        table.setShowGrid(showGrid); // Hiển thị viền nếu showGrid = true
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

    /** Thay đổi trang hiển thị */
    private void changePage(int newPage) {
        // Kiểm tra giới hạn trang
        if (newPage < 0 || newPage >= Math.ceil((double) allPromotions.size() / itemsPerPage)) {
            return;
        }
        currentPage = newPage;
        loadPromotions();
    }

    /** Cập nhật thông tin phân trang */
    private void updatePaginationInfo() {
        int totalItems = allPromotions.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        lblPageInfo.setText("Trang " + (currentPage + 1) + " / " + totalPages + " (Tổng: " + totalItems + " khuyến mãi)");

        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
    }
}
