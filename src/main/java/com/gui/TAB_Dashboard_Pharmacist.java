package com.gui;

import com.bus.BUS_Product;
import com.bus.BUS_Promotion;
import com.bus.BUS_Shift;
import com.bus.BUS_Invoice;
import com.entities.*;
import com.enums.InvoiceType;
import com.enums.LotStatus;
import com.enums.PromotionEnum;
import com.interfaces.DataChangeListener;
import com.interfaces.ShiftChangeListener;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.Timer;

/**
 * Dashboard cho Nhân viên (Dược sĩ)
 * Tập trung vào vận hành và xử lý tức thời:
 * - Cảnh báo thuốc sắp hết hàng
 * - Cảnh báo thuốc sắp hết hạn
 * - Tra cứu khuyến mãi đang áp dụng
 *
 * @author Tô Thanh Hậu
 */
public class TAB_Dashboard_Pharmacist extends JPanel implements DataChangeListener {
    private final BUS_Product busProduct;
    private final BUS_Promotion busPromotion;
    private final BUS_Shift busShift;
    private final BUS_Invoice busInvoice;

    // Current staff and shift
    private Staff currentStaff;
    private Shift currentShift;
    private ShiftChangeListener shiftChangeListener;

    // Tables
    private JTable tblLowStock;
    private JTable tblExpiringSoon;
    private JTable tblActivePromotions;
    private JTable tblTopSelling;

    private DefaultTableModel lowStockModel;
    private DefaultTableModel expiringSoonModel;
    private DefaultTableModel promotionModel;
    private DefaultTableModel topSellingModel;

    // Summary card labels
    private JLabel lblCardExpiringCount;
    private JLabel lblCardLowStockCount;
    private JLabel lblCardShiftRevenue;

    // Table header labels
    private JLabel lblExpiringCount;
    private JLabel lblLowStockCount;
    private JLabel lblActivePromotionCount;

    // Shift management labels
    private JLabel lblShiftId;
    private JLabel lblCurrentCash;

    private JButton btnCloseShift;

    // Auto-refresh timer
    private Timer refreshTimer;

    // Constants
    private static final int LOW_STOCK_THRESHOLD = 100; // Định mức tồn kho thấp
    private static final int CRITICAL_STOCK_THRESHOLD = 10; // Ngưỡng tồn kho nguy hiểm
    private static final int EXPIRY_WARNING_DAYS = 90; // Cảnh báo thuốc còn 90 ngày hết hạn
    private static final int EXPIRY_DANGER_DAYS = 30; // Cảnh báo nguy hiểm còn 30 ngày
    private static final int AUTO_REFRESH_INTERVAL = 5000; // Auto-refresh every 5 seconds

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
        this.busInvoice = new BUS_Invoice();
        initComponents();
        loadData();
        loadShiftData();
        startAutoRefresh();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(AppColors.WHITE);

        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Main content with summary cards
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(AppColors.WHITE);

        // Summary Cards at top
        contentPanel.add(createSummaryCardsPanel(), BorderLayout.NORTH);

        // Tables and info panels - 2 Column Grid Layout
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 15, 10));
        mainPanel.setBackground(AppColors.WHITE);

        // Left Column: Alerts & High Activity (Expiring & Top Selling)
        JPanel leftColumn = new JPanel(new GridLayout(2, 1, 10, 15));
        leftColumn.setBackground(AppColors.WHITE);
        leftColumn.add(createExpiringSoonPanel());
        leftColumn.add(createTopSellingPanel());

        // Right Column: Stock Management (Low Stock & Promotions)
        JPanel rightColumn = new JPanel(new GridLayout(2, 1, 10, 15));
        rightColumn.setBackground(AppColors.WHITE);
        rightColumn.add(createLowStockPanel());
        rightColumn.add(createPromotionPanel());

        mainPanel.add(leftColumn);
        mainPanel.add(rightColumn);

        contentPanel.add(mainPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
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
        btnCloseShift.setForeground(AppColors.WHITE);
        btnCloseShift.setFocusPainted(false);
        btnCloseShift.setBorderPainted(false);
        btnCloseShift.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCloseShift.setPreferredSize(new Dimension(120, 40));
        btnCloseShift.addActionListener(e -> handleShiftButtonClick());
        rightSection.add(btnCloseShift);

        topSection.add(lblTitle, BorderLayout.WEST);
        topSection.add(rightSection, BorderLayout.EAST);

        // Bottom section: Date only
        JPanel bottomSection = new JPanel(new BorderLayout());
        bottomSection.setBackground(AppColors.WHITE);

        JLabel lblDate = new JLabel("Ngày: " + LocalDate.now().format(dateFormatter));
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDate.setForeground(AppColors.DARK);

        bottomSection.add(lblDate, BorderLayout.WEST);

        headerPanel.add(topSection, BorderLayout.NORTH);
        headerPanel.add(bottomSection, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createShiftWidget() {
        JPanel widget = new JPanel();
        widget.setLayout(new BoxLayout(widget, BoxLayout.Y_AXIS));
        widget.setBackground(AppColors.WHITE); // Light blue background
        widget.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppColors.SECONDARY, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));

        // Shift ID
        JPanel shiftIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        shiftIdPanel.setBackground(AppColors.WHITE);
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
        cashPanel.setBackground(AppColors.WHITE);
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
        // First, check if there's ANY open shift on this workstation
        String workstation = busShift.getCurrentWorkstation();
        Shift workstationShift = busShift.getOpenShiftOnWorkstation(workstation);

        // Then check staff's personal shift
        Shift staffShift = null;
        if (currentStaff != null) {
            staffShift = busShift.getCurrentOpenShiftForStaff(currentStaff);
        }

        // Use workstation shift if it exists, otherwise use staff shift
        currentShift = workstationShift != null ? workstationShift : staffShift;

        // Set common button properties
        btnCloseShift.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCloseShift.setForeground(AppColors.WHITE);
        btnCloseShift.setBorderPainted(false);
        btnCloseShift.setFocusPainted(false);
        btnCloseShift.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCloseShift.setPreferredSize(new Dimension(120, 40));

        if (currentShift != null) {
            // Shift is open - show shift info and "Đóng ca"
            lblShiftId.setText(currentShift.getId());
            BigDecimal currentCash = busShift.calculateSystemCashForShift(currentShift);
            lblCurrentCash.setText(currencyFormat.format(currentCash));

            // Check if this is the staff's own shift
            boolean isOwnShift = currentStaff != null &&
                currentShift.getStaff() != null &&
                currentShift.getStaff().getId().equals(currentStaff.getId());

            btnCloseShift.setText("Đóng ca");
            btnCloseShift.setToolTipText(isOwnShift ?
                "Nhấn để đóng ca làm việc" :
                "Đóng ca của: " + currentShift.getStaff().getFullName());
            btnCloseShift.setBackground(AppColors.DANGER); // Red color for close
            btnCloseShift.setEnabled(true);
        } else {
            // No open shift - show "Mở ca"
            lblShiftId.setText("Chưa mở ca");
            lblCurrentCash.setText("---");

            btnCloseShift.setText("Mở ca");
            btnCloseShift.setToolTipText("Nhấn để mở ca làm việc");
            btnCloseShift.setBackground(AppColors.SUCCESS); // Green color for open
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
                Shift closedShift = currentShift;
                currentShift = null;
                loadShiftData();

                // Notify listener that shift was closed
                if (shiftChangeListener != null) {
                    shiftChangeListener.onShiftClosed(closedShift);
                }

                JOptionPane.showMessageDialog(this,
                    "Ca làm việc đã được đóng thành công!",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            // Open shift - check for existing shift on workstation
            String workstation = busShift.getCurrentWorkstation();
            Shift existingShift = busShift.getOpenShiftOnWorkstation(workstation);

            if (existingShift != null && !existingShift.getStaff().getId().equals(currentStaff.getId())) {
                // Another staff has an open shift - show takeover dialog
                handleExistingShift(existingShift);
            } else {
                // No existing shift or same staff - open new shift
                openNewShift();
            }
        }
    }

    private void handleExistingShift(Shift existingShift) {
        String message = String.format(
            "Hiện đang có ca do nhân viên %s mở từ %s.\n\n" +
            "Bạn có muốn tiếp tục ca này không?\n\n" +
            "Lưu ý: Nếu chọn 'Có', bạn sẽ không thể bán hàng cho đến khi đóng ca này.",
            existingShift.getStaff().getFullName(),
            existingShift.getStartTime().format(dateTimeFormatter)
        );

        int choice = JOptionPane.showConfirmDialog(
            this,
            message,
            "Ca làm việc đang mở",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            // User wants to continue existing shift
            currentShift = existingShift;
            loadShiftData();

            JOptionPane.showMessageDialog(
                this,
                "Bạn đã chọn tiếp tục ca hiện tại.\n" +
                "Lưu ý: Bạn KHÔNG THỂ bán hàng khi chưa đóng ca này.",
                "Thông báo",
                JOptionPane.WARNING_MESSAGE
            );
        } else {
            // User does not want to continue
            JOptionPane.showMessageDialog(
                this,
                "Không thể mở ca mới khi đã có ca đang mở trên máy này.\n" +
                "Vui lòng đóng ca hiện tại trước.",
                "Không thể mở ca",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void openNewShift() {
        DIALOG_OpenShift openShiftDialog = new DIALOG_OpenShift(
            (Frame) SwingUtilities.getWindowAncestor(this),
            currentStaff
        );
        openShiftDialog.setVisible(true);

        // Update button and shift info if shift was opened
        if (openShiftDialog.getOpenedShift() != null) {
            currentShift = openShiftDialog.getOpenedShift();
            loadShiftData();

            // Notify listener that shift was opened
            if (shiftChangeListener != null) {
                shiftChangeListener.onShiftOpened(currentShift);
            }

            JOptionPane.showMessageDialog(this,
                "Ca làm việc đã được mở thành công!",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
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
        lblTitle.setForeground(AppColors.WARNING);

        lblLowStockCount = new JLabel("0 sản phẩm");
        lblLowStockCount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLowStockCount.setForeground(AppColors.WARNING);

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
        tblLowStock.getTableHeader().setReorderingAllowed(false);
        tblLowStock.getColumnModel().getColumn(2).setCellRenderer(new LowStockCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblLowStock);
        scrollPane.setPreferredSize(new Dimension(0, 300));
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
        lblTitle.setForeground(AppColors.DANGER);

        lblExpiringCount = new JLabel("0 lô hàng");
        lblExpiringCount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblExpiringCount.setForeground(AppColors.DANGER);

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
        tblExpiringSoon.getTableHeader().setReorderingAllowed(false);

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
        scrollPane.setPreferredSize(new Dimension(0, 300));
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
        tblActivePromotions.getTableHeader().setReorderingAllowed(false);

        // Adjust column widths
        tblActivePromotions.getColumnModel().getColumn(0).setPreferredWidth(80);  // Mã KM
        tblActivePromotions.getColumnModel().getColumn(1).setPreferredWidth(200); // Tên KM
        tblActivePromotions.getColumnModel().getColumn(2).setPreferredWidth(100); // Ngày bắt đầu
        tblActivePromotions.getColumnModel().getColumn(3).setPreferredWidth(100); // Ngày kết thúc
        tblActivePromotions.getColumnModel().getColumn(4).setPreferredWidth(300); // Điều kiện

        JScrollPane scrollPane = new JScrollPane(tblActivePromotions);
        scrollPane.setPreferredSize(new Dimension(0, 300));
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTopSellingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppColors.WHITE);

        JLabel lblTitle = new JLabel("TOP 5 BÁN CHẠY CA HIỆN TẠI");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.PRIMARY);

        JLabel lblSubtitle = new JLabel("Để chuẩn bị hàng nhanh hơn");
        lblSubtitle.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblSubtitle.setForeground(AppColors.PLACEHOLDER_TEXT);

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(lblSubtitle, BorderLayout.EAST);

        // Table
        String[] columns = {"Top", "Mã SP", "Tên sản phẩm", "Đã bán", "Đơn vị"};
        topSellingModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblTopSelling = createStyledTable(topSellingModel);
        tblTopSelling.getTableHeader().setReorderingAllowed(false);

        // Adjust column widths
        tblTopSelling.getColumnModel().getColumn(0).setPreferredWidth(50);  // Top
        tblTopSelling.getColumnModel().getColumn(1).setPreferredWidth(100); // Mã SP
        tblTopSelling.getColumnModel().getColumn(2).setPreferredWidth(300); // Tên SP
        tblTopSelling.getColumnModel().getColumn(3).setPreferredWidth(80);  // Đã bán
        tblTopSelling.getColumnModel().getColumn(4).setPreferredWidth(80);  // Đơn vị

        // Custom renderer for Top column
        tblTopSelling.getColumnModel().getColumn(0).setCellRenderer(new TopRankCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblTopSelling);
        scrollPane.setPreferredSize(new Dimension(0, 300));
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
        table.setSelectionForeground(AppColors.TEXT);
        table.setGridColor(AppColors.BACKGROUND);
        table.setShowGrid(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(AppColors.SECONDARY);
        header.setForeground(AppColors.WHITE);
        header.setPreferredSize(new Dimension(0, 35));

        return table;
    }

    private void loadData() {
        try {
            loadExpiringSoonLots();
            loadLowStockProducts();
            loadActivePromotions();
            loadTopSellingProducts();
            updateSummaryCards();
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

            if (totalStock <= LOW_STOCK_THRESHOLD) {
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

            String status = totalStock == 0 ? "HẾT HÀNG" :
                           totalStock <= CRITICAL_STOCK_THRESHOLD ? "NGUY HIỂM" : "SẮP HẾT";

            lowStockModel.addRow(new Object[]{
                product.getId(),
                product.getName(),
                totalStock,
                product.getBaseUnitOfMeasure(),
                status
            });
        }

        lblLowStockCount.setText(lowStockProducts.size() + " sản phẩm");

        // Apply row-level color renderer
        tblLowStock.setDefaultRenderer(Object.class, new LowStockRowRenderer());
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

//                if (condition.getComparator() == PromotionEnum.Comp.BETWEEN && condition.getSecondaryValue() != null) {
//                    sb.append(" - ");
//                    sb.append(formatCurrency(condition.getSecondaryValue()));
//                }
            } else if (condition.getTarget() == PromotionEnum.Target.PRODUCT) {
                if (condition.getConditionType() == PromotionEnum.ConditionType.PRODUCT_QTY) {
                    sb.append("Mua ");
                    if (condition.getProduct() != null) {
                        sb.append(condition.getProduct().getName());
                        sb.append(" ");
                    }
                    sb.append(formatComparator(condition.getComparator()));
                    sb.append(" ");
//                    sb.append(condition.getPrimaryValue().intValue());
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

    /**
     * Set shift change listener to notify when shift is opened/closed
     */
    public void setShiftChangeListener(ShiftChangeListener listener) {
        this.shiftChangeListener = listener;
    }

    private void loadTopSellingProducts() {
        topSellingModel.setRowCount(0);

        if (currentShift == null) {
            // No active shift - show empty or placeholder message
            return;
        }

        // Get all invoices for current shift
        List<Invoice> allInvoices = busInvoice.getAllInvoices();
        if (allInvoices == null) return;

        // Filter invoices by current shift
        List<Invoice> shiftInvoices = allInvoices.stream()
            .filter(inv -> inv.getShift() != null && inv.getShift().getId().equals(currentShift.getId()))
            .filter(inv -> inv.getType() == InvoiceType.SALES)
            .collect(Collectors.toList());

        // Count quantities sold per product
        Map<Product, Integer> productSalesMap = new HashMap<>();

        for (Invoice invoice : shiftInvoices) {
            if (invoice.getInvoiceLineList() != null) {
                for (InvoiceLine line : invoice.getInvoiceLineList()) {
                    Product product = line.getProduct();
                    int quantity = line.getQuantity();
                    productSalesMap.merge(product, quantity, Integer::sum);
                }
            }
        }

        // Sort by quantity sold (descending) and take top 5
        List<Map.Entry<Product, Integer>> topProducts = productSalesMap.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
            .limit(5)
            .collect(Collectors.toList());

        // Add to table
        int rank = 1;
        for (Map.Entry<Product, Integer> entry : topProducts) {
            Product product = entry.getKey();
            int quantity = entry.getValue();

            topSellingModel.addRow(new Object[]{
                rank++,
                product.getId(),
                product.getName(),
                quantity,
                product.getBaseUnitOfMeasure()
            });
        }
    }

    private void updateSummaryCards() {
        // Card 1: Expiring Products (RED - CRITICAL)
        int expiringCount = expiringSoonModel.getRowCount();
        lblCardExpiringCount.setText(String.valueOf(expiringCount));

        // Card 2: Low Stock Products (ORANGE - WARNING)
        int lowStockCount = lowStockModel.getRowCount();
        lblCardLowStockCount.setText(String.valueOf(lowStockCount));

        // Card 3: Current Shift Revenue (GREEN - PERFORMANCE)
        BigDecimal shiftRevenue = BigDecimal.ZERO;
        if (currentShift != null) {
            List<Invoice> allInvoices = busInvoice.getAllInvoices();
            if (allInvoices != null) {
                double revenueSum = allInvoices.stream()
                    .filter(inv -> inv.getShift() != null && inv.getShift().getId().equals(currentShift.getId()))
                    .filter(inv -> inv.getType() == InvoiceType.SALES)
                    .map(Invoice::calculateTotal)
                    .reduce(0.0, Double::sum);
                shiftRevenue = BigDecimal.valueOf(revenueSum);
            }
        }
        lblCardShiftRevenue.setText(currencyFormat.format(shiftRevenue));
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
                            // Light red background for danger
                            c.setBackground(new Color(255, 230, 230));
                            c.setForeground(AppColors.TEXT);
                        } else if (days <= EXPIRY_WARNING_DAYS) {
                            // Light yellow background for warning
                            c.setBackground(new Color(255, 250, 220));
                            c.setForeground(AppColors.TEXT);
                        } else {
                            c.setBackground(AppColors.WHITE);
                            c.setForeground(AppColors.TEXT);
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
                        c.setBackground(AppColors.WHITE);
                        c.setForeground(AppColors.TEXT);
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
            setForeground(AppColors.WHITE);
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
            button.setForeground(AppColors.WHITE);
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
                    c.setForeground(AppColors.TEXT);
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
                        c.setForeground(AppColors.WHITE);
                        c.setBackground(AppColors.DANGER);
                        break;
                    case "CAO":
                        c.setForeground(AppColors.WHITE);
                        c.setBackground(AppColors.WARNING);
                        break;
                    case "TRUNG BÌNH":
                        c.setForeground(AppColors.TEXT);
                        c.setBackground(AppColors.LIGHT);
                        break;
                }
            }

            setHorizontalAlignment(CENTER);
            return c;
        }
    }

    /**
     * Row-level color renderer for Low Stock Table
     * Colors entire row based on stock quantity:
     * - Dark Red/Gray: stock = 0 (out of stock)
     * - Yellow: stock <= CRITICAL_STOCK_THRESHOLD (critical)
     */
    private class LowStockRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Get stock quantity from column 2
            Object stockObj = table.getValueAt(row, 2);
            if (stockObj != null && stockObj instanceof Integer) {
                int stock = (Integer) stockObj;

                if (!isSelected) {
                    if (stock == 0) {
                        // Gray for out of stock
                        c.setBackground(AppColors.BACKGROUND);
                        c.setForeground(AppColors.TEXT);
                    } else if (stock <= CRITICAL_STOCK_THRESHOLD) {
                        // Light yellow for critical stock
                        c.setBackground(new Color(255, 250, 220));
                        c.setForeground(AppColors.TEXT);
                    } else {
                        c.setBackground(AppColors.WHITE);
                        c.setForeground(AppColors.TEXT);
                    }
                }

                // Bold font for stock quantity and status columns
                if (column == 2 || column == 4) {
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
            }

            setHorizontalAlignment(column == 2 ? CENTER : LEFT);
            return c;
        }
    }

    /**
     * Custom renderer for Top Rank column in Top Selling table
     * Highlights top 3 with medals/badges
     */
    private static class TopRankCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof Integer) {
                int rank = (Integer) value;
                setFont(getFont().deriveFont(Font.BOLD, 14f));
                setHorizontalAlignment(CENTER);

                if (!isSelected) {
                    switch (rank) {
                        case 1:
                            c.setBackground(new Color(255, 215, 0)); // Gold
                            c.setForeground(AppColors.TEXT);
                            setText(String.valueOf(rank));
                            break;
                        case 2:
                            c.setBackground(new Color(192, 192, 192)); // Silver
                            c.setForeground(AppColors.TEXT);
                            setText(String.valueOf(rank));
                            break;
                        case 3:
                            c.setBackground(new Color(205, 127, 50)); // Bronze
                            c.setForeground(AppColors.TEXT);
                            setText(String.valueOf(rank));
                            break;
                        default:
                            c.setBackground(AppColors.WHITE);
                            c.setForeground(AppColors.TEXT);
                            setText(String.valueOf(rank));
                            break;
                    }
                }
            }

            return c;
        }
    }

    private JPanel createSummaryCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 15, 0));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Card 1: Expiring Products (RED - CRITICAL)
        JPanel cardExpiring = createSummaryCard(
            "Thuốc Sắp Hết Hạn",
            "0",
            "lô hàng cần xử lý",
            AppColors.DANGER, // Red
            ""
        );
        lblCardExpiringCount = (JLabel) ((JPanel) cardExpiring.getComponent(0)).getComponent(0);

        // Card 2: Low Stock Products (ORANGE - WARNING)
        JPanel cardLowStock = createSummaryCard(
            "Thuốc Sắp Hết Hàng",
            "0",
            "sản phẩm cần nhập",
            AppColors.WARNING, // Orange/Yellow
            ""
        );
        lblCardLowStockCount = (JLabel) ((JPanel) cardLowStock.getComponent(0)).getComponent(0);

        // Card 3: Current Shift Revenue (GREEN - PERFORMANCE)
        JPanel cardRevenue = createSummaryCard(
            "Doanh Thu Ca Hiện Tại",
            "0 đ",
            "từ đầu ca đến hiện tại",
            AppColors.SUCCESS, // Green
            ""
        );
        lblCardShiftRevenue = (JLabel) ((JPanel) cardRevenue.getComponent(0)).getComponent(0);

        panel.add(cardExpiring);
        panel.add(cardLowStock);
        panel.add(cardRevenue);

        return panel;
    }

    private JPanel createSummaryCard(String title, String value, String subtitle, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(AppColors.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            new EmptyBorder(20, 20, 20, 20)
        ));

        // Top section: Value only (no icon)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(AppColors.WHITE);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblValue.setForeground(color);
        lblValue.setHorizontalAlignment(SwingConstants.CENTER);

        topPanel.add(lblValue, BorderLayout.CENTER);

        // Bottom section: Title + Subtitle
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBackground(AppColors.WHITE);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(AppColors.DARK);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(AppColors.PLACEHOLDER_TEXT);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottomPanel.add(lblTitle);
        bottomPanel.add(Box.createVerticalStrut(5));
        bottomPanel.add(lblSubtitle);

        card.add(topPanel, BorderLayout.CENTER);
        card.add(bottomPanel, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Start auto-refresh timer to update dashboard data periodically
     */
    private void startAutoRefresh() {
        if (refreshTimer != null && refreshTimer.isRunning()) {
            refreshTimer.stop();
        }

        refreshTimer = new Timer(AUTO_REFRESH_INTERVAL, e -> {
            try {
                loadData();
                loadShiftData();
            } catch (Exception ex) {
                // Silent fail - don't interrupt user with errors during auto-refresh
                System.err.println("Auto-refresh error: " + ex.getMessage());
            }
        });
        refreshTimer.setRepeats(true);
        refreshTimer.start();
    }

    /**
     * Stop auto-refresh timer when component is no longer needed
     */
    public void stopAutoRefresh() {
        if (refreshTimer != null && refreshTimer.isRunning()) {
            refreshTimer.stop();
        }
    }

    // DataChangeListener implementation
    @Override
    public void onInvoiceCreated() {
        // Immediately refresh dashboard when a new invoice is created
        SwingUtilities.invokeLater(() -> {
            loadData();
            loadShiftData();
        });
    }

    @Override
    public void onProductChanged() {
        // Immediately refresh dashboard when products change
        SwingUtilities.invokeLater(() -> {
            loadData();
            loadShiftData();
        });
    }

    @Override
    public void onPromotionChanged() {
        // Immediately refresh dashboard when promotions change
        SwingUtilities.invokeLater(() -> {
            loadData();
            loadShiftData();
        });
    }

    @Override
    public void onDataChanged() {
        // General data change - refresh everything
        SwingUtilities.invokeLater(() -> {
            loadData();
            loadShiftData();
        });
    }
}
