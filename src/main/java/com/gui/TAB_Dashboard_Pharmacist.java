package com.gui;

import com.bus.BUS_Product;
import com.bus.BUS_Promotion;
import com.bus.BUS_Shift;
import com.entities.*;
import com.enums.LotStatus;
import com.enums.PromotionEnum;
import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * Dashboard cho Nhân viên (Dược sĩ)
 * Tập trung vào vận hành và xử lý tức thời:
 * - Cảnh báo thuốc sắp hết hàng
 * - Cảnh báo thuốc sắp hết hạn
 * - Tra cứu khuyến mãi đang áp dụng
 *
 * @author Tô Thanh Hậu
 */
public class TAB_Dashboard_Pharmacist extends JPanel {
    private final BUS_Product busProduct;
    private final BUS_Promotion busPromotion;
    private final BUS_Shift busShift;

    // Current staff and shift
    private Staff currentStaff;
    private Shift currentShift;

    // Tables
    private JTable tblLowStock;
    private JTable tblExpiringSoon;
    private JTable tblActivePromotions;

    private DefaultTableModel lowStockModel;
    private DefaultTableModel expiringSoonModel;
    private DefaultTableModel promotionModel;

    // Labels for statistics
    private JLabel lblLowStockCount;
    private JLabel lblExpiringCount;
    private JLabel lblActivePromotionCount;

    // Shift management labels
    private JLabel lblShiftId;
    private JLabel lblCurrentCash;

    private JButton btnRefresh;
    private JButton btnCloseShift;

    // Constants
    private static final int LOW_STOCK_THRESHOLD = 100; // Định mức tồn kho thấp
    private static final int EXPIRY_WARNING_DAYS = 90; // Cảnh báo thuốc còn 90 ngày hết hạn
    private static final int EXPIRY_DANGER_DAYS = 30; // Cảnh báo nguy hiểm còn 30 ngày

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public TAB_Dashboard_Pharmacist() {
        this(null);
    }

    public TAB_Dashboard_Pharmacist(Staff staff) {
        this.currentStaff = staff;
        this.busProduct = new BUS_Product();
        this.busPromotion = new BUS_Promotion();
        this.busShift = new BUS_Shift();
        initComponents();
        loadData();
        loadShiftData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(AppColors.WHITE);

        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        mainPanel.setBackground(AppColors.WHITE);

        mainPanel.add(createExpiringSoonPanel());
        mainPanel.add(createLowStockPanel());
        mainPanel.add(createPromotionPanel());

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setBackground(AppColors.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Top section: Title + Right Section (Shift Widget + Close Shift)
        JPanel topSection = new JPanel(new BorderLayout(10, 0));
        topSection.setBackground(AppColors.WHITE);

        // Title
        JLabel lblTitle = new JLabel("Dashboard Dược Sĩ - Vận Hành");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(AppColors.PRIMARY);

        // Right section: Shift Widget + Close Shift Button
        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightSection.setBackground(AppColors.WHITE);

        // Shift Info Widget
        JPanel shiftWidget = createShiftWidget();
        rightSection.add(shiftWidget);

        // Close Shift Button
        btnCloseShift = new JButton("Đóng ca");
        btnCloseShift.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCloseShift.setBackground(AppColors.DANGER);
        btnCloseShift.setForeground(Color.WHITE);
        btnCloseShift.setFocusPainted(false);
        btnCloseShift.setBorderPainted(false);
        btnCloseShift.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCloseShift.setPreferredSize(new Dimension(120, 40));
        btnCloseShift.addActionListener(e -> handleShiftButtonClick());
        rightSection.add(btnCloseShift);

        topSection.add(lblTitle, BorderLayout.WEST);
        topSection.add(rightSection, BorderLayout.EAST);

        // Bottom section: Date + Refresh button
        JPanel bottomSection = new JPanel(new BorderLayout());
        bottomSection.setBackground(AppColors.WHITE);

        JLabel lblDate = new JLabel("Ngày: " + LocalDate.now().format(dateFormatter));
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDate.setForeground(AppColors.DARK);

        btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setBackground(AppColors.SECONDARY);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setPreferredSize(new Dimension(150, 40));
        btnRefresh.addActionListener(e -> {
            loadData();
            loadShiftData();
        });

        bottomSection.add(lblDate, BorderLayout.WEST);
        bottomSection.add(btnRefresh, BorderLayout.EAST);

        headerPanel.add(topSection, BorderLayout.NORTH);
        headerPanel.add(bottomSection, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createShiftWidget() {
        JPanel widget = new JPanel();
        widget.setLayout(new BoxLayout(widget, BoxLayout.Y_AXIS));
        widget.setBackground(new Color(240, 248, 255)); // Light blue background
        widget.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppColors.SECONDARY, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));

        // Shift ID
        JPanel shiftIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        shiftIdPanel.setBackground(new Color(240, 248, 255));
        JLabel lblShiftIdLabel = new JLabel("Mã Ca:");
        lblShiftIdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblShiftIdLabel.setForeground(AppColors.DARK);
        lblShiftId = new JLabel("---");
        lblShiftId.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblShiftId.setForeground(AppColors.PRIMARY);
        shiftIdPanel.add(lblShiftIdLabel);
        shiftIdPanel.add(lblShiftId);

        // Current Cash
        JPanel cashPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        cashPanel.setBackground(new Color(240, 248, 255));
        JLabel lblCashLabel = new JLabel("Tiền mặt:");
        lblCashLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCashLabel.setForeground(AppColors.DARK);
        lblCurrentCash = new JLabel("0 ₫");
        lblCurrentCash.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCurrentCash.setForeground(AppColors.SUCCESS);
        cashPanel.add(lblCashLabel);
        cashPanel.add(lblCurrentCash);

        widget.add(shiftIdPanel);
        widget.add(cashPanel);

        return widget;
    }

    private void loadShiftData() {
        // Get current shift status
        if (currentStaff != null) {
            currentShift = busShift.getCurrentOpenShiftForStaff(currentStaff);
        }

        // Set common button properties
        btnCloseShift.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCloseShift.setForeground(Color.WHITE);
        btnCloseShift.setBorderPainted(false);
        btnCloseShift.setFocusPainted(false);
        btnCloseShift.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCloseShift.setPreferredSize(new Dimension(120, 40));

        if (currentShift != null) {
            // Shift is open - show shift info and "Đóng ca"
            lblShiftId.setText(currentShift.getId());
            BigDecimal currentCash = busShift.calculateSystemCashForShift(currentShift);
            lblCurrentCash.setText(currencyFormat.format(currentCash));

            btnCloseShift.setText("Đóng ca");
            btnCloseShift.setToolTipText("Nhấn để đóng ca làm việc");
            btnCloseShift.setBackground(new Color(220, 53, 69)); // Red color for close
            btnCloseShift.setEnabled(true);
        } else {
            // No open shift - show "Mở ca"
            lblShiftId.setText("Chưa mở ca");
            lblCurrentCash.setText("---");

            btnCloseShift.setText("Mở ca");
            btnCloseShift.setToolTipText("Nhấn để mở ca làm việc");
            btnCloseShift.setBackground(new Color(40, 167, 69)); // Green color for open
            btnCloseShift.setEnabled(true);
        }
    }

    private void handleShiftButtonClick() {
        if (currentShift != null) {
            // Close shift
            DIALOG_CloseShift closeShiftDialog = new DIALOG_CloseShift(
                (Frame) SwingUtilities.getWindowAncestor(this),
                currentShift,
                currentStaff
            );
            closeShiftDialog.setVisible(true);

            // Update button and shift info if shift was closed
            if (closeShiftDialog.isConfirmed()) {
                currentShift = null;
                loadShiftData();

                JOptionPane.showMessageDialog(this,
                    "Ca làm việc đã được đóng thành công!",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            // Open shift
            DIALOG_OpenShift openShiftDialog = new DIALOG_OpenShift(
                (Frame) SwingUtilities.getWindowAncestor(this),
                currentStaff
            );
            openShiftDialog.setVisible(true);

            // Update button and shift info if shift was opened
            if (openShiftDialog.getOpenedShift() != null) {
                currentShift = openShiftDialog.getOpenedShift();
                loadShiftData();

                JOptionPane.showMessageDialog(this,
                    "Ca làm việc đã được mở thành công!",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private JPanel createLowStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppColors.WHITE);

        JLabel lblTitle = new JLabel("CẢNH BÁO: Thuốc Sắp Hết Hàng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.DANGER);

        lblLowStockCount = new JLabel("0 sản phẩm");
        lblLowStockCount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLowStockCount.setForeground(AppColors.DANGER);

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(lblLowStockCount, BorderLayout.EAST);

        // Table
        String[] columns = {"Mã SP", "Tên sản phẩm", "Tồn kho", "Đơn vị", "Trạng thái"};
        lowStockModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblLowStock = createStyledTable(lowStockModel);
        tblLowStock.getColumnModel().getColumn(2).setCellRenderer(new LowStockCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblLowStock);
        scrollPane.setPreferredSize(new Dimension(0, 180));
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createExpiringSoonPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppColors.WHITE);

        JLabel lblTitle = new JLabel("CẢNH BÁO: Thuốc Sắp Hết Hạn");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.WARNING);

        lblExpiringCount = new JLabel("0 lô hàng");
        lblExpiringCount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblExpiringCount.setForeground(AppColors.WARNING);

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(lblExpiringCount, BorderLayout.EAST);

        // Table with Action column
        String[] columns = {"Mã lô", "Tên sản phẩm", "Số lượng", "Hạn sử dụng", "Còn lại", "Mức độ", "Thao tác"};
        expiringSoonModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only Action column is editable
            }
        };

        tblExpiringSoon = createStyledTable(expiringSoonModel);

        // Apply conditional row coloring
        tblExpiringSoon.setDefaultRenderer(Object.class, new ExpiringRowRenderer());

        // Add button renderer and editor for Action column
        tblExpiringSoon.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        tblExpiringSoon.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Adjust column widths
        tblExpiringSoon.getColumnModel().getColumn(0).setPreferredWidth(100); // Mã lô
        tblExpiringSoon.getColumnModel().getColumn(1).setPreferredWidth(200); // Tên sản phẩm
        tblExpiringSoon.getColumnModel().getColumn(2).setPreferredWidth(80);  // Số lượng
        tblExpiringSoon.getColumnModel().getColumn(3).setPreferredWidth(100); // Hạn sử dụng
        tblExpiringSoon.getColumnModel().getColumn(4).setPreferredWidth(80);  // Còn lại
        tblExpiringSoon.getColumnModel().getColumn(5).setPreferredWidth(100); // Mức độ
        tblExpiringSoon.getColumnModel().getColumn(6).setPreferredWidth(100); // Thao tác

        JScrollPane scrollPane = new JScrollPane(tblExpiringSoon);
        scrollPane.setPreferredSize(new Dimension(0, 180));
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPromotionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppColors.WHITE);

        JLabel lblTitle = new JLabel("Khuyến Mãi Đang Áp Dụng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.SUCCESS);

        lblActivePromotionCount = new JLabel("0 chương trình");
        lblActivePromotionCount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblActivePromotionCount.setForeground(AppColors.SUCCESS);

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(lblActivePromotionCount, BorderLayout.EAST);

        // Table - Replace Description with Condition
        String[] columns = {"Mã KM", "Tên khuyến mãi", "Ngày bắt đầu", "Ngày kết thúc", "Điều kiện áp dụng"};
        promotionModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblActivePromotions = createStyledTable(promotionModel);

        // Adjust column widths
        tblActivePromotions.getColumnModel().getColumn(0).setPreferredWidth(80);  // Mã KM
        tblActivePromotions.getColumnModel().getColumn(1).setPreferredWidth(200); // Tên KM
        tblActivePromotions.getColumnModel().getColumn(2).setPreferredWidth(100); // Ngày bắt đầu
        tblActivePromotions.getColumnModel().getColumn(3).setPreferredWidth(100); // Ngày kết thúc
        tblActivePromotions.getColumnModel().getColumn(4).setPreferredWidth(300); // Điều kiện

        JScrollPane scrollPane = new JScrollPane(tblActivePromotions);
        scrollPane.setPreferredSize(new Dimension(0, 180));
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(AppColors.SECONDARY.getRed(), AppColors.SECONDARY.getGreen(), AppColors.SECONDARY.getBlue(), 50));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(AppColors.BACKGROUND);
        table.setShowGrid(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(AppColors.SECONDARY);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 35));

        return table;
    }

    private void loadData() {
        try {
            loadExpiringSoonLots();
            loadLowStockProducts();
            loadActivePromotions();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Lỗi khi tải dữ liệu: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadLowStockProducts() {
        lowStockModel.setRowCount(0);

        List<Product> allProducts = busProduct.getAllProducts();
        if (allProducts == null) return;

        List<Product> lowStockProducts = new ArrayList<>();

        for (Product product : allProducts) {
            int totalStock = 0;
            if (product.getLotList() != null) {
                for (Lot lot : product.getLotList()) {
                    if (lot.getStatus() == LotStatus.AVAILABLE) {
                        totalStock += lot.getQuantity();
                    }
                }
            }

            if (totalStock <= LOW_STOCK_THRESHOLD && totalStock > 0) {
                lowStockProducts.add(product);
            }
        }

        // Sort by stock quantity (lowest first)
        lowStockProducts.sort((p1, p2) -> {
            int stock1 = p1.getLotList().stream()
                .filter(l -> l.getStatus() == LotStatus.AVAILABLE)
                .mapToInt(Lot::getQuantity).sum();
            int stock2 = p2.getLotList().stream()
                .filter(l -> l.getStatus() == LotStatus.AVAILABLE)
                .mapToInt(Lot::getQuantity).sum();
            return Integer.compare(stock1, stock2);
        });

        for (Product product : lowStockProducts) {
            int totalStock = product.getLotList().stream()
                .filter(l -> l.getStatus() == LotStatus.AVAILABLE)
                .mapToInt(Lot::getQuantity).sum();

            String status = totalStock == 0 ? "HẾT HÀNG" : "SẮP HẾT";

            lowStockModel.addRow(new Object[]{
                product.getId(),
                product.getName(),
                totalStock,
                product.getBaseUnitOfMeasure(),
                status
            });
        }

        lblLowStockCount.setText(lowStockProducts.size() + " sản phẩm");
    }

    private void loadExpiringSoonLots() {
        expiringSoonModel.setRowCount(0);

        List<Lot> allLots = busProduct.getAllLots();
        if (allLots == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningDate = now.plusDays(EXPIRY_WARNING_DAYS);

        List<Lot> expiringSoonLots = allLots.stream()
            .filter(lot -> lot.getStatus() == LotStatus.AVAILABLE)
            .filter(lot -> lot.getQuantity() > 0)
            .filter(lot -> lot.getExpiryDate() != null)
            .filter(lot -> lot.getExpiryDate().isBefore(warningDate))
            .sorted(Comparator.comparing(Lot::getExpiryDate))
            .collect(Collectors.toList());

        for (Lot lot : expiringSoonLots) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(now.toLocalDate(), lot.getExpiryDate().toLocalDate());

            String level;
            if (daysUntilExpiry <= EXPIRY_DANGER_DAYS) {
                level = "NGUY HIỂM";
            } else if (daysUntilExpiry <= 60) {
                level = "CAO";
            } else {
                level = "TRUNG BÌNH";
            }

            expiringSoonModel.addRow(new Object[]{
                lot.getBatchNumber(),
                lot.getProduct() != null ? lot.getProduct().getName() : "N/A",
                lot.getQuantity(),
                lot.getExpiryDate().format(dateFormatter),
                daysUntilExpiry + " ngày",
                level,
                "Copy ID" // Button label
            });
        }

        lblExpiringCount.setText(expiringSoonLots.size() + " lô hàng");
    }

    private void loadActivePromotions() {
        promotionModel.setRowCount(0);

        // Filter: active = true, valid = true
        List<Promotion> activePromotions = busPromotion.filterPromotions(true, true);

        if (activePromotions == null) {
            activePromotions = List.of();
        }

        for (Promotion promotion : activePromotions) {
            // Format promotion conditions for display
            String conditionText = formatPromotionConditions(promotion);

            promotionModel.addRow(new Object[]{
                promotion.getId(),
                promotion.getName(),
                promotion.getEffectiveDate() != null ? promotion.getEffectiveDate().format(dateFormatter) : "N/A",
                promotion.getEndDate() != null ? promotion.getEndDate().format(dateFormatter) : "N/A",
                conditionText
            });
        }

        lblActivePromotionCount.setText(activePromotions.size() + " chương trình");
    }

    /**
     * Format promotion conditions into readable text for staff
     */
    private String formatPromotionConditions(Promotion promotion) {
        if (promotion.getConditions() == null || promotion.getConditions().isEmpty()) {
            return "Không có điều kiện";
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (PromotionCondition condition : promotion.getConditions()) {
            if (count > 0) sb.append("; ");

            // Format based on condition type and target
            if (condition.getTarget() == PromotionEnum.Target.ORDER_SUBTOTAL) {
                sb.append("Hóa đơn ");
                sb.append(formatComparator(condition.getComparator()));
                sb.append(" ");
                sb.append(formatCurrency(condition.getPrimaryValue()));

                if (condition.getComparator() == PromotionEnum.Comp.BETWEEN && condition.getSecondaryValue() != null) {
                    sb.append(" - ");
                    sb.append(formatCurrency(condition.getSecondaryValue()));
                }
            } else if (condition.getTarget() == PromotionEnum.Target.PRODUCT) {
                if (condition.getConditionType() == PromotionEnum.ConditionType.PRODUCT_QTY) {
                    sb.append("Mua ");
                    if (condition.getProduct() != null) {
                        sb.append(condition.getProduct().getName());
                        sb.append(" ");
                    }
                    sb.append(formatComparator(condition.getComparator()));
                    sb.append(" ");
                    sb.append(condition.getPrimaryValue().intValue());
                    sb.append(" sản phẩm");
                } else if (condition.getConditionType() == PromotionEnum.ConditionType.PRODUCT_ID) {
                    sb.append("Sản phẩm: ");
                    if (condition.getProduct() != null) {
                        sb.append(condition.getProduct().getName());
                    }
                }
            }

            count++;
            if (count >= 2) {
                sb.append("...");
                break; // Limit to 2 conditions for display
            }
        }

        return sb.toString();
    }

    private String formatComparator(PromotionEnum.Comp comparator) {
        switch (comparator) {
            case GREATER_EQUAL: return "≥";
            case LESS_EQUAL: return "≤";
            case GREATER: return ">";
            case LESS: return "<";
            case EQUAL: return "=";
            case BETWEEN: return "từ";
            default: return "";
        }
    }

    private String formatCurrency(Double value) {
        if (value == null) return "0 ₫";
        return String.format("%,.0f ₫", value);
    }

    /**
     * Public method to refresh dashboard data
     */
    public void refresh() {
        loadData();
    }

    // Custom Cell Renderers

    /**
     * Conditional Row Renderer for Expiring Soon Table
     * Colors entire row based on days until expiry:
     * - Red: < 30 days (Danger)
     * - Yellow: < 90 days (Warning)
     */
    private class ExpiringRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Don't color the button column (column 6)
            if (column == 6) {
                return c;
            }

            // Get "Còn lại" value from column 4 (days remaining)
            Object daysObj = table.getValueAt(row, 4);
            if (daysObj != null && daysObj instanceof String) {
                String daysStr = (String) daysObj;
                try {
                    int days = Integer.parseInt(daysStr.split(" ")[0]);

                    if (!isSelected) {
                        if (days <= EXPIRY_DANGER_DAYS) {
                            // Red background for danger
                            c.setBackground(new Color(255, 200, 200));
                            c.setForeground(Color.BLACK);
                        } else if (days <= EXPIRY_WARNING_DAYS) {
                            // Yellow background for warning
                            c.setBackground(new Color(255, 255, 200));
                            c.setForeground(Color.BLACK);
                        } else {
                            c.setBackground(Color.WHITE);
                            c.setForeground(Color.BLACK);
                        }
                    }

                    // Bold font for "Còn lại" and "Mức độ" columns
                    if (column == 4 || column == 5) {
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                } catch (Exception ignored) {
                    if (!isSelected) {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }
            }

            setHorizontalAlignment(column == 2 || column == 4 ? CENTER : LEFT);
            return c;
        }
    }

    /**
     * Button Renderer for Action Column
     */
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "Copy ID" : value.toString());
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setBackground(AppColors.SECONDARY);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            return this;
        }
    }

    /**
     * Button Editor for Action Column
     */
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean clicked;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            button.setBackground(AppColors.SECONDARY);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> {
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                    boolean isSelected, int row, int column) {
            this.row = row;
            label = (value == null) ? "Copy ID" : value.toString();
            button.setText(label);
            clicked = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (clicked) {
                // Get lot batch number from column 0
                Object batchNumber = tblExpiringSoon.getValueAt(row, 0);
                if (batchNumber != null) {
                    // Copy to clipboard
                    StringSelection stringSelection = new StringSelection(batchNumber.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);

                    JOptionPane.showMessageDialog(button,
                        "Đã copy mã lô: " + batchNumber,
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            clicked = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            clicked = false;
            return super.stopCellEditing();
        }
    }

    private static class LowStockCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof Integer) {
                int stock = (Integer) value;
                if (stock == 0) {
                    c.setForeground(AppColors.DANGER);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (stock <= 50) {
                    c.setForeground(AppColors.WARNING);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(Color.BLACK);
                }
            }

            setHorizontalAlignment(CENTER);
            return c;
        }
    }

    private static class ExpiryDaysCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof String) {
                String daysStr = (String) value;
                try {
                    int days = Integer.parseInt(daysStr.split(" ")[0]);
                    if (days <= 30) {
                        c.setForeground(AppColors.DANGER);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (days <= 60) {
                        c.setForeground(AppColors.WARNING);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setForeground(AppColors.WARNING);
                    }
                } catch (Exception ignored) {}
            }

            setHorizontalAlignment(CENTER);
            return c;
        }
    }

    private static class ExpiryLevelCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof String) {
                String level = (String) value;
                setFont(getFont().deriveFont(Font.BOLD));
                switch (level) {
                    case "NGUY HIỂM":
                        c.setForeground(Color.WHITE);
                        c.setBackground(AppColors.DANGER);
                        break;
                    case "CAO":
                        c.setForeground(Color.WHITE);
                        c.setBackground(AppColors.WARNING);
                        break;
                    case "TRUNG BÌNH":
                        c.setForeground(Color.BLACK);
                        c.setBackground(AppColors.LIGHT);
                        break;
                }
            }

            setHorizontalAlignment(CENTER);
            return c;
        }
    }
}

