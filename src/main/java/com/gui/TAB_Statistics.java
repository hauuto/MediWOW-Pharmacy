package com.gui;

import com.bus.BUS_Statistic;
import com.entities.Staff;
import com.enums.Role;
import com.interfaces.IStatistic.*;
import com.utils.AppColors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Module Thống kê & Báo cáo (Reporting Module)
 *
 * Features:
 * - Tab 1: Doanh thu & Lợi nhuận (Manager Only)
 * - Tab 2: Hàng hóa (Shared View - filter theo quyền)
 * - Tab 3: Kiểm soát & Nhân viên (Audit)
 * - Tab 4: Hiệu quả Khuyến mãi (Manager Only)
 *
 * UX/UI:
 * - Initial State: Bảng dữ liệu RỖNG khi mở màn hình
 * - Advanced Date Filter với preset options
 * - Trigger Action: Chỉ load dữ liệu khi nhấn nút [Xem Thống Kê]
 * - Export Excel: Chỉ sáng khi có dữ liệu
 *
 * @author Tô Thanh Hậu
 */
public class TAB_Statistics extends JPanel {

    // Current staff & role
    private Staff currentStaff;
    private Role currentRole;

    // Business layer
    private final BUS_Statistic busStatistic;

    // Date filter components
    private JComboBox<String> cbDatePreset;
    private DIALOG_DatePicker dpFromDate;
    private DIALOG_DatePicker dpToDate;
    private JButton btnViewReport;
    private JButton btnExportExcel;

    // Tab pane
    private JTabbedPane tabbedPane;

    // Tab 1: Doanh thu & Lợi nhuận
    private JPanel pnlRevenueChart;
    private JTable tblRevenue;
    private DefaultTableModel mdlRevenue;

    // Tab 2: Hàng hóa
    private JComboBox<String> cbProductReportType;
    private JTable tblProduct;
    private DefaultTableModel mdlProduct;

    // Tab 3: Kiểm soát & Nhân viên
    private JTable tblCashAudit;
    private DefaultTableModel mdlCashAudit;
    private JTable tblStaffPerformance;
    private DefaultTableModel mdlStaffPerformance;

    // Tab 4: Hiệu quả Khuyến mãi
    private JTable tblPromotion;
    private DefaultTableModel mdlPromotion;

    // Formatters
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Date preset options
    private static final String[] DATE_PRESETS = {
        "Hôm nay",
        "3 ngày qua",
        "7 ngày qua",
        "Tháng này",
        "Quý này",
        "Năm nay",
        "Tùy chọn"
    };

    // Product report types
    private static final String[] PRODUCT_REPORT_TYPES = {
        "Sản phẩm bán chạy",
        "Tồn kho lâu (Dead Stock)",
        "Tăng đột biến (Trending)",
        "Sắp hết hạn"
    };

    // State flags
    private boolean hasData = false;

    public TAB_Statistics() {
        this(null);
    }

    public TAB_Statistics(Staff staff) {
        this.currentStaff = staff;
        this.currentRole = staff != null ? staff.getRole() : Role.PHARMACIST;
        this.busStatistic = new BUS_Statistic();

        initComponents();
        setupEventListeners();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(AppColors.WHITE);

        // Header panel with filters
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Tabbed pane for different reports
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setBackground(Color.WHITE);

        // Add tabs based on role
        if (currentRole == Role.MANAGER) {
            tabbedPane.addTab("Doanh thu & Lợi nhuận", createRevenueTab());
        }
        tabbedPane.addTab("Hàng hóa", createProductTab());
        tabbedPane.addTab("Kiểm soát & Nhân viên", createAuditTab());
        if (currentRole == Role.MANAGER) {
            tabbedPane.addTab("Hiệu quả Khuyến mãi", createPromotionTab());
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ========================================
    // Header Panel with Date Filters
    // ========================================

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Left: Title
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Thống kê & Báo cáo");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(AppColors.PRIMARY);

        JLabel lblSubtitle = new JLabel("Tra cứu lịch sử và phân tích dữ liệu kinh doanh");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(AppColors.DARK);

        leftPanel.add(lblTitle);
        leftPanel.add(Box.createVerticalStrut(4));
        leftPanel.add(lblSubtitle);

        // Right: Date filter controls
        JPanel rightPanel = createDateFilterPanel();

        header.add(leftPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createDateFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setOpaque(false);

        // Date preset dropdown
        JLabel lblPreset = new JLabel("Khoảng thời gian:");
        lblPreset.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbDatePreset = new JComboBox<>(DATE_PRESETS);
        cbDatePreset.setPreferredSize(new Dimension(130, 32));
        cbDatePreset.setSelectedIndex(0); // Default: Hôm nay

        // From date picker
        JLabel lblFrom = new JLabel("Từ:");
        lblFrom.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dpFromDate = new DIALOG_DatePicker(java.sql.Date.valueOf(LocalDate.now()));
        dpFromDate.setPreferredSize(new Dimension(140, 32));

        // To date picker
        JLabel lblTo = new JLabel("Đến:");
        lblTo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dpToDate = new DIALOG_DatePicker(java.sql.Date.valueOf(LocalDate.now()));
        dpToDate.setPreferredSize(new Dimension(140, 32));

        // Initial state: disable date pickers (non-custom preset)
        setDatePickersEnabled(false);
        updateDatePickersFromPreset();

        // View report button
        btnViewReport = new JButton("Xem Thống Kê");
        btnViewReport.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnViewReport.setBackground(AppColors.PRIMARY);
        btnViewReport.setForeground(Color.WHITE);
        btnViewReport.setFocusPainted(false);
        btnViewReport.setBorderPainted(false);
        btnViewReport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewReport.setPreferredSize(new Dimension(130, 35));

        // Export Excel button
        btnExportExcel = new JButton("Xuất Excel");
        btnExportExcel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnExportExcel.setBackground(AppColors.SUCCESS);
        btnExportExcel.setForeground(Color.WHITE);
        btnExportExcel.setFocusPainted(false);
        btnExportExcel.setBorderPainted(false);
        btnExportExcel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExportExcel.setPreferredSize(new Dimension(120, 35));
        btnExportExcel.setEnabled(false); // Disabled until data is loaded

        panel.add(lblPreset);
        panel.add(cbDatePreset);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(lblFrom);
        panel.add(dpFromDate);
        panel.add(lblTo);
        panel.add(dpToDate);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(btnViewReport);
        panel.add(btnExportExcel);

        return panel;
    }

    // ========================================
    // Tab 1: Doanh thu & Lợi nhuận
    // ========================================

    private JPanel createRevenueTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Chart panel (top)
        pnlRevenueChart = new JPanel(new BorderLayout());
        pnlRevenueChart.setBackground(Color.WHITE);
        pnlRevenueChart.setPreferredSize(new Dimension(0, 300));
        pnlRevenueChart.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppColors.LIGHT, 1),
            "Biểu đồ Doanh thu & Lợi nhuận",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13)
        ));

        // Initial message
        JLabel lblNoData = new JLabel("Chọn khoảng thời gian và nhấn 'Xem Thống Kê' để hiển thị dữ liệu", SwingConstants.CENTER);
        lblNoData.setForeground(AppColors.PLACEHOLDER_TEXT);
        lblNoData.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        pnlRevenueChart.add(lblNoData, BorderLayout.CENTER);

        // Table panel (bottom)
        String[] columns = {"Ngày", "Doanh thu gộp", "Tiền trả hàng", "Doanh thu thuần",
                           "Giá vốn (COGS)", "Lợi nhuận gộp", "Số hóa đơn", "Số đơn trả"};
        mdlRevenue = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblRevenue = new JTable(mdlRevenue);
        styleTable(tblRevenue);

        JScrollPane scrollPane = new JScrollPane(tblRevenue);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppColors.LIGHT, 1),
            "Chi tiết theo ngày",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13)
        ));

        panel.add(pnlRevenueChart, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ========================================
    // Tab 2: Hàng hóa
    // ========================================

    private JPanel createProductTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top: Report type selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topPanel.setOpaque(false);

        JLabel lblType = new JLabel("Loại báo cáo:");
        lblType.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbProductReportType = new JComboBox<>(PRODUCT_REPORT_TYPES);
        cbProductReportType.setPreferredSize(new Dimension(200, 30));

        topPanel.add(lblType);
        topPanel.add(cbProductReportType);

        // Table - columns will change based on report type
        mdlProduct = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblProduct = new JTable(mdlProduct);
        styleTable(tblProduct);

        JScrollPane scrollPane = new JScrollPane(tblProduct);

        // Hint panel
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hintPanel.setOpaque(false);
        JLabel lblHint = new JLabel("Chọn loại báo cáo và nhấn 'Xem Thống Kê' để hiển thị dữ liệu.");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblHint.setForeground(AppColors.DARK);
        hintPanel.add(lblHint);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(hintPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ========================================
    // Tab 3: Kiểm soát & Nhân viên
    // ========================================

    private JPanel createAuditTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Left: Cash Audit
        JPanel cashPanel = new JPanel(new BorderLayout());
        cashPanel.setBackground(Color.WHITE);
        cashPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppColors.LIGHT, 1),
            "Đối soát tiền mặt theo ca",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13)
        ));

        String[] cashColumns = {"Mã ca", "Nhân viên", "Ngày", "Giờ mở", "Giờ đóng",
                                "Tiền đầu ca", "Tiền hệ thống", "Tiền thực tế", "Chênh lệch"};
        mdlCashAudit = new DefaultTableModel(cashColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblCashAudit = new JTable(mdlCashAudit);
        styleTable(tblCashAudit);

        // Custom renderer for mismatch column (highlight red if negative)
        tblCashAudit.getColumnModel().getColumn(8).setCellRenderer(new MismatchCellRenderer());

        JLabel hintCash = new JLabel("Màu đỏ: Tiền thực tế thấp hơn hệ thống (nguy cơ thất thoát)");
        hintCash.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintCash.setForeground(AppColors.DANGER);
        hintCash.setBorder(new EmptyBorder(5, 5, 5, 5));

        cashPanel.add(new JScrollPane(tblCashAudit), BorderLayout.CENTER);
        cashPanel.add(hintCash, BorderLayout.SOUTH);

        // Right: Staff Performance
        JPanel staffPanel = new JPanel(new BorderLayout());
        staffPanel.setBackground(Color.WHITE);
        staffPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppColors.LIGHT, 1),
            "Hiệu suất nhân viên",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13)
        ));

        String[] staffColumns;
        if (currentRole == Role.MANAGER) {
            staffColumns = new String[]{"Hạng", "Mã NV", "Tên nhân viên", "Vai trò",
                                        "Số hóa đơn", "Tổng doanh thu", "TB/Hóa đơn"};
        } else {
            staffColumns = new String[]{"Hạng", "Mã NV", "Tên nhân viên", "Vai trò", "Số hóa đơn"};
        }

        mdlStaffPerformance = new DefaultTableModel(staffColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblStaffPerformance = new JTable(mdlStaffPerformance);
        styleTable(tblStaffPerformance);

        staffPanel.add(new JScrollPane(tblStaffPerformance), BorderLayout.CENTER);

        panel.add(cashPanel);
        panel.add(staffPanel);

        return panel;
    }

    // ========================================
    // Tab 4: Hiệu quả Khuyến mãi
    // ========================================

    private JPanel createPromotionTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] columns = {"Mã KM", "Tên khuyến mãi", "Ngày hiệu lực", "Ngày kết thúc",
                           "Số lượt dùng", "Tổng tiền giảm", "Doanh thu KM", "TB/Đơn hàng"};
        mdlPromotion = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblPromotion = new JTable(mdlPromotion);
        styleTable(tblPromotion);

        JScrollPane scrollPane = new JScrollPane(tblPromotion);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppColors.LIGHT, 1),
            "Thống kê hiệu quả khuyến mãi",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13)
        ));

        // Summary panel
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        summaryPanel.setBackground(new Color(240, 248, 255));
        summaryPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblSummary = new JLabel("Các chỉ số quan trọng: Số lượt sử dụng mã, Tổng tiền đã giảm, Doanh thu từ đơn khuyến mãi");
        lblSummary.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        summaryPanel.add(lblSummary);

        panel.add(summaryPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ========================================
    // Event Listeners
    // ========================================

    private void setupEventListeners() {
        // Date preset change
        cbDatePreset.addActionListener(e -> {
            int selectedIndex = cbDatePreset.getSelectedIndex();
            boolean isCustom = selectedIndex == DATE_PRESETS.length - 1; // "Tùy chọn"
            setDatePickersEnabled(isCustom);
            if (!isCustom) {
                updateDatePickersFromPreset();
            }
        });

        // View report button
        btnViewReport.addActionListener(e -> loadReportData());

        // Export Excel button
        btnExportExcel.addActionListener(e -> exportToExcel());

        // Product report type change
        if (cbProductReportType != null) {
            cbProductReportType.addActionListener(e -> {
                // Update table columns based on report type
                updateProductTableColumns();
            });
        }
    }

    /**
     * Enable/Disable date pickers based on preset selection
     * Key UX requirement: Only enable for "Tùy chọn" (Custom) option
     */
    private void setDatePickersEnabled(boolean enabled) {
        if (dpFromDate != null) {
            dpFromDate.setEnabled(enabled);
            // Visual feedback
            dpFromDate.setBackground(enabled ? Color.WHITE : new Color(240, 240, 240));
        }
        if (dpToDate != null) {
            dpToDate.setEnabled(enabled);
            dpToDate.setBackground(enabled ? Color.WHITE : new Color(240, 240, 240));
        }
    }

    /**
     * Update date pickers based on selected preset
     * Implements the Advanced Date Filter Logic requirement
     */
    private void updateDatePickersFromPreset() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate;
        LocalDate toDate = today;

        int selectedIndex = cbDatePreset.getSelectedIndex();
        switch (selectedIndex) {
            case 0: // Hôm nay
                fromDate = today;
                break;
            case 1: // 3 ngày qua
                fromDate = today.minusDays(2);
                break;
            case 2: // 7 ngày qua
                fromDate = today.minusDays(6);
                break;
            case 3: // Tháng này
                fromDate = today.with(TemporalAdjusters.firstDayOfMonth());
                toDate = today.with(TemporalAdjusters.lastDayOfMonth());
                if (toDate.isAfter(today)) toDate = today;
                break;
            case 4: // Quý này
                int currentQuarter = (today.getMonthValue() - 1) / 3;
                fromDate = today.withMonth(currentQuarter * 3 + 1).with(TemporalAdjusters.firstDayOfMonth());
                toDate = fromDate.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
                if (toDate.isAfter(today)) toDate = today;
                break;
            case 5: // Năm nay
                fromDate = today.with(TemporalAdjusters.firstDayOfYear());
                break;
            default: // Tùy chọn - don't change
                return;
        }

        // Update date pickers
        dpFromDate.setDate(java.sql.Date.valueOf(fromDate));
        dpToDate.setDate(java.sql.Date.valueOf(toDate));
    }

    // ========================================
    // Data Loading
    // ========================================

    /**
     * Load report data based on selected tab and date range
     * Trigger Action: Only loads data when this method is called
     */
    private void loadReportData() {
        LocalDate fromDate = getFromDate();
        LocalDate toDate = getToDate();

        if (fromDate == null || toDate == null) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn khoảng thời gian hợp lệ.",
                "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (fromDate.isAfter(toDate)) {
            JOptionPane.showMessageDialog(this,
                "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.",
                "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show loading cursor
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    // Load data for all tabs
                    if (currentRole == Role.MANAGER) {
                        loadRevenueData(fromDate, toDate);
                    }
                    loadProductData(fromDate, toDate);
                    loadAuditData(fromDate, toDate);
                    if (currentRole == Role.MANAGER) {
                        loadPromotionData(fromDate, toDate);
                    }
                    hasData = true;
                } catch (SecurityException se) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(TAB_Statistics.this,
                            se.getMessage(), "Không có quyền", JOptionPane.WARNING_MESSAGE));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(TAB_Statistics.this,
                            "Lỗi khi tải dữ liệu: " + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                btnExportExcel.setEnabled(hasData);
            }
        };
        worker.execute();
    }

    private void loadRevenueData(LocalDate fromDate, LocalDate toDate) {
        List<RevenueData> data = busStatistic.getRevenueReport(fromDate, toDate, currentRole);

        SwingUtilities.invokeLater(() -> {
            // Clear and populate table
            mdlRevenue.setRowCount(0);

            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalProfit = BigDecimal.ZERO;

            for (RevenueData rd : data) {
                mdlRevenue.addRow(new Object[]{
                    rd.date().format(dateFormat),
                    currencyFormat.format(rd.grossRevenue()),
                    currencyFormat.format(rd.returnAmount()),
                    currencyFormat.format(rd.netRevenue()),
                    currencyFormat.format(rd.cogs()),
                    currencyFormat.format(rd.grossProfit()),
                    rd.invoiceCount(),
                    rd.returnCount()
                });
                totalRevenue = totalRevenue.add(rd.netRevenue());
                totalProfit = totalProfit.add(rd.grossProfit());
            }

            // Update chart
            updateRevenueChart(data);
        });
    }

    private void updateRevenueChart(List<RevenueData> data) {
        if (data == null || data.isEmpty()) {
            pnlRevenueChart.removeAll();
            JLabel lblNoData = new JLabel("Không có dữ liệu trong khoảng thời gian đã chọn", SwingConstants.CENTER);
            lblNoData.setForeground(AppColors.PLACEHOLDER_TEXT);
            pnlRevenueChart.add(lblNoData, BorderLayout.CENTER);
            pnlRevenueChart.revalidate();
            pnlRevenueChart.repaint();
            return;
        }

        // Determine date range and pick an appropriate labeling step
        LocalDate startDate = data.get(0).date();
        LocalDate endDate = data.get(data.size() - 1).date();
        long daysSpan = ChronoUnit.DAYS.between(startDate, endDate);

        // Choose step for visible labels (leave blanks for intermediate points)
        int labelStep;
        DateTimeFormatter labelFmt;
        // Determine if we should use quarterly labels.
        // Use quarterly mode when the user selected "Năm nay", or when the span is roughly a year
        long monthsSpan = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), endDate.withDayOfMonth(1));
        boolean quarterlyMode = false;
        try {
            if (cbDatePreset != null && cbDatePreset.getSelectedIndex() == 5) { // "Năm nay"
                quarterlyMode = true;
            } else if (daysSpan >= 330) {
                quarterlyMode = true;
            } else if (monthsSpan >= 11 && startDate.getYear() == endDate.getYear()) {
                quarterlyMode = true;
            }
        } catch (Exception ignore) {
            quarterlyMode = (monthsSpan >= 11 && startDate.getYear() == endDate.getYear());
        }
        if (quarterlyMode) {
            labelStep = 90; // quarterly-ish for multi-year ranges
            labelFmt = DateTimeFormatter.ofPattern("MM/yyyy");
        } else if (daysSpan <= 31) {
            labelStep = 1; // daily
            labelFmt = DateTimeFormatter.ofPattern("dd/MM");
        } else if (daysSpan <= 120) {
            labelStep = 7; // weekly
            labelFmt = DateTimeFormatter.ofPattern("dd/MM");
        } else {
            labelStep = 30; // monthly-ish
            labelFmt = DateTimeFormatter.ofPattern("MM/yyyy");
        }

        // Create chart
        CategoryChart chart = new CategoryChartBuilder()
            .width(800)
            .height(280)
            .title("Doanh thu & Lợi nhuận")
            .xAxisTitle("Ngày")
            .yAxisTitle("VNĐ")
            .theme(Styler.ChartTheme.GGPlot2)
            .build();

        // Prepare data and sparse labels
        List<String> labels = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();
        List<Double> profits = new ArrayList<>();

        // If quarterlyMode, aggregate data per quarter instead of per-day points
        if (quarterlyMode) {
            // Build quarter boundaries starting from the quarter that contains startDate
            List<LocalDate> quarterStarts = new ArrayList<>();
            int startQuarterIdx = (startDate.getMonthValue() - 1) / 3; // 0..3
            LocalDate qStart = LocalDate.of(startDate.getYear(), startQuarterIdx * 3 + 1, 1);
            // include the quarter containing startDate even if startDate is after qStart
            // advance quarters until beyond endDate
            while (!qStart.isAfter(endDate)) {
                quarterStarts.add(qStart);
                qStart = qStart.plusMonths(3);
            }

            // For each quarter, sum revenues/profits from data points falling into that quarter
            for (LocalDate qs : quarterStarts) {
                LocalDate qe = qs.plusMonths(3).minusDays(1);
                BigDecimal sumNet = BigDecimal.ZERO;
                BigDecimal sumProfit = BigDecimal.ZERO;
                for (RevenueData rd : data) {
                    LocalDate d = rd.date();
                    if ((d.isEqual(qs) || d.isAfter(qs)) && (d.isEqual(qe) || d.isBefore(qe))) {
                        if (rd.netRevenue() != null) sumNet = sumNet.add(rd.netRevenue());
                        if (rd.grossProfit() != null) sumProfit = sumProfit.add(rd.grossProfit());
                    }
                }
                int qNum = (qs.getMonthValue() - 1) / 3 + 1;
                labels.add("Q" + qNum + " " + qs.getYear());
                revenues.add(sumNet.doubleValue());
                profits.add(sumProfit.doubleValue());
            }
            // Ensure at least one label/point exists (if no quarters were added, fallback to end date)
            if (labels.isEmpty()) {
                labels.add(endDate.format(labelFmt));
                // sum entire range
                BigDecimal totalNet = BigDecimal.ZERO;
                BigDecimal totalProfit = BigDecimal.ZERO;
                for (RevenueData rd : data) {
                    if (rd.netRevenue() != null) totalNet = totalNet.add(rd.netRevenue());
                    if (rd.grossProfit() != null) totalProfit = totalProfit.add(rd.grossProfit());
                }
                revenues.add(totalNet.doubleValue());
                profits.add(totalProfit.doubleValue());
            }
        } else {
            for (int i = 0; i < data.size(); i++) {
                RevenueData rd = data.get(i);
                LocalDate d = rd.date();
                // show label based on actual day distance from start to avoid relying on list index
                long offsetDays = ChronoUnit.DAYS.between(startDate, d);
                boolean shouldLabel = (offsetDays % labelStep == 0) || (d.equals(endDate));
                // Use a single space for unlabeled ticks instead of an empty string to prevent
                // java.awt.font.TextLayout from throwing IllegalArgumentException for zero-length text.
                String label = shouldLabel ? d.format(labelFmt) : " ";
                labels.add(label);
                revenues.add(rd.netRevenue().doubleValue());
                profits.add(rd.grossProfit().doubleValue());
            }
        }

        // Add series
        chart.addSeries("Doanh thu thuần", labels, revenues);
        chart.addSeries("Lợi nhuận gộp", labels, profits);

        // Styling: rotate labels and set spacing hint to improve readability
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideS);
        chart.getStyler().setSeriesColors(new Color[]{AppColors.PRIMARY, AppColors.SUCCESS});
        chart.getStyler().setXAxisLabelRotation(45);
        chart.getStyler().setXAxisTickMarkSpacingHint(50);

        // Update panel
        pnlRevenueChart.removeAll();
        pnlRevenueChart.add(new XChartPanel<>(chart), BorderLayout.CENTER);
        pnlRevenueChart.revalidate();
        pnlRevenueChart.repaint();
    }

    private void loadProductData(LocalDate fromDate, LocalDate toDate) {
        int reportType = cbProductReportType.getSelectedIndex();

        SwingUtilities.invokeLater(() -> {
            mdlProduct.setRowCount(0);
            updateProductTableColumns();

            switch (reportType) {
                case 0: // Best Sellers
                    loadBestSellers(fromDate, toDate);
                    break;
                case 1: // Dead Stock
                    loadDeadStock();
                    break;
                case 2: // Trending
                    loadTrending(toDate);
                    break;
                case 3: // Expiry Alert
                    loadExpiryAlert();
                    break;
            }
        });
    }

    private void updateProductTableColumns() {
        int reportType = cbProductReportType.getSelectedIndex();
        mdlProduct.setRowCount(0);
        mdlProduct.setColumnCount(0);

        switch (reportType) {
            case 0: // Best Sellers
                if (currentRole == Role.MANAGER) {
                    mdlProduct.setColumnIdentifiers(new String[]{"Mã SP", "Tên sản phẩm", "Nhóm",
                        "SL bán", "Doanh thu", "Giá vốn", "Lợi nhuận"});
                } else {
                    mdlProduct.setColumnIdentifiers(new String[]{"Mã SP", "Tên sản phẩm", "Nhóm",
                        "SL bán", "Doanh thu"});
                }
                break;
            case 1: // Dead Stock
                if (currentRole == Role.MANAGER) {
                    mdlProduct.setColumnIdentifiers(new String[]{"Mã SP", "Tên sản phẩm", "Nhóm",
                        "Tồn kho", "Lần bán cuối", "Số ngày", "Giá vốn TB"});
                } else {
                    mdlProduct.setColumnIdentifiers(new String[]{"Mã SP", "Tên sản phẩm", "Nhóm",
                        "Tồn kho", "Lần bán cuối", "Số ngày"});
                }
                break;
            case 2: // Trending
                mdlProduct.setColumnIdentifiers(new String[]{"Mã SP", "Tên sản phẩm", "Nhóm",
                    "SL hôm nay", "DT hôm nay", "TB 7 ngày", "% Tăng"});
                break;
            case 3: // Expiry Alert
                mdlProduct.setColumnIdentifiers(new String[]{"Mã Lô", "Số lô", "Mã SP",
                    "Tên sản phẩm", "Số lượng", "Ngày hết hạn", "Còn lại (ngày)"});
                break;
        }
    }

    private void loadBestSellers(LocalDate fromDate, LocalDate toDate) {
        List<BestSellerData> data = busStatistic.getBestSellers(fromDate, toDate, 20, currentRole);
        for (BestSellerData bs : data) {
            if (currentRole == Role.MANAGER) {
                mdlProduct.addRow(new Object[]{
                    bs.productId(), bs.productName(), bs.category(),
                    bs.quantitySold(), currencyFormat.format(bs.revenue()),
                    bs.rawPrice() != null ? currencyFormat.format(bs.rawPrice()) : "N/A",
                    bs.profit() != null ? currencyFormat.format(bs.profit()) : "N/A"
                });
            } else {
                mdlProduct.addRow(new Object[]{
                    bs.productId(), bs.productName(), bs.category(),
                    bs.quantitySold(), currencyFormat.format(bs.revenue())
                });
            }
        }
    }

    private void loadDeadStock() {
        List<DeadStockData> data = busStatistic.getDeadStock(90, currentRole);
        for (DeadStockData ds : data) {
            if (currentRole == Role.MANAGER) {
                mdlProduct.addRow(new Object[]{
                    ds.productId(), ds.productName(), ds.category(),
                    ds.currentStock(),
                    ds.lastSoldDate() != null ? ds.lastSoldDate().format(dateFormat) : "Chưa bán",
                    ds.daysSinceLastSale() == Integer.MAX_VALUE ? "N/A" : ds.daysSinceLastSale(),
                    ds.rawPrice() != null ? currencyFormat.format(ds.rawPrice()) : "N/A"
                });
            } else {
                mdlProduct.addRow(new Object[]{
                    ds.productId(), ds.productName(), ds.category(),
                    ds.currentStock(),
                    ds.lastSoldDate() != null ? ds.lastSoldDate().format(dateFormat) : "Chưa bán",
                    ds.daysSinceLastSale() == Integer.MAX_VALUE ? "N/A" : ds.daysSinceLastSale()
                });
            }
        }
    }

    private void loadTrending(LocalDate date) {
        List<TrendingData> data = busStatistic.getTrendingProducts(date, currentRole);
        for (TrendingData td : data) {
            mdlProduct.addRow(new Object[]{
                td.productId(), td.productName(), td.category(),
                td.quantityToday(), currencyFormat.format(td.revenueToday()),
                currencyFormat.format(td.avgRevenue7Days()),
                String.format("+%.1f%%", td.percentIncrease())
            });
        }
    }

    private void loadExpiryAlert() {
        List<ExpiryAlertData> data = busStatistic.getExpiryAlerts(0, 90);
        for (ExpiryAlertData ea : data) {
            mdlProduct.addRow(new Object[]{
                ea.lotId(), ea.batchNumber(), ea.productId(),
                ea.productName(), ea.quantity(),
                ea.expiryDate().format(dateFormat),
                ea.daysUntilExpiry()
            });
        }
    }

    private void loadAuditData(LocalDate fromDate, LocalDate toDate) {
        // Cash Audit
        List<CashAuditData> cashData = busStatistic.getCashAuditReport(fromDate, toDate);
        SwingUtilities.invokeLater(() -> {
            mdlCashAudit.setRowCount(0);
            for (CashAuditData ca : cashData) {
                mdlCashAudit.addRow(new Object[]{
                    ca.shiftId(), ca.staffName(), ca.shiftDate().format(dateFormat),
                    ca.startTime(), ca.endTime(),
                    currencyFormat.format(ca.startCash()),
                    currencyFormat.format(ca.systemCash()),
                    currencyFormat.format(ca.actualCash()),
                    currencyFormat.format(ca.mismatch())
                });
            }
        });

        // Staff Performance
        List<StaffPerformanceData> staffData = busStatistic.getStaffPerformance(fromDate, toDate, currentRole);
        SwingUtilities.invokeLater(() -> {
            mdlStaffPerformance.setRowCount(0);
            for (StaffPerformanceData sp : staffData) {
                if (currentRole == Role.MANAGER) {
                    mdlStaffPerformance.addRow(new Object[]{
                        sp.rank(), sp.staffId(), sp.staffName(), sp.role(),
                        sp.invoiceCount(),
                        sp.totalRevenue() != null ? currencyFormat.format(sp.totalRevenue()) : "N/A",
                        sp.averageInvoiceValue() != null ? currencyFormat.format(sp.averageInvoiceValue()) : "N/A"
                    });
                } else {
                    mdlStaffPerformance.addRow(new Object[]{
                        sp.rank(), sp.staffId(), sp.staffName(), sp.role(),
                        sp.invoiceCount()
                    });
                }
            }
        });
    }

    private void loadPromotionData(LocalDate fromDate, LocalDate toDate) {
        List<PromotionStatsData> data = busStatistic.getPromotionStats(fromDate, toDate, currentRole);

        SwingUtilities.invokeLater(() -> {
            mdlPromotion.setRowCount(0);
            for (PromotionStatsData ps : data) {
                mdlPromotion.addRow(new Object[]{
                    ps.promotionId(), ps.promotionName(),
                    ps.effectiveDate() != null ? ps.effectiveDate().format(dateFormat) : "N/A",
                    ps.endDate() != null ? ps.endDate().format(dateFormat) : "N/A",
                    ps.usageCount(),
                    currencyFormat.format(ps.totalDiscount()),
                    currencyFormat.format(ps.revenueFromPromo()),
                    currencyFormat.format(ps.avgOrderValue())
                });
            }
        });
    }

    // ========================================
    // Export to Excel
    // ========================================

    private void exportToExcel() {
        if (!hasData) {
            JOptionPane.showMessageDialog(this,
                "Không có dữ liệu để xuất. Vui lòng tải dữ liệu trước.",
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu báo cáo Excel");
        fileChooser.setSelectedFile(new File("ThongKe_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".xlsx")) {
                file = new File(file.getAbsolutePath() + ".xlsx");
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                // Export current tab data
                int selectedTab = tabbedPane.getSelectedIndex();
                String tabTitle = tabbedPane.getTitleAt(selectedTab);

                Sheet sheet = workbook.createSheet(tabTitle.replaceAll("[^a-zA-Z0-9\\s]", "").trim());

                JTable currentTable = getCurrentTable();
                if (currentTable != null) {
                    exportTableToSheet(sheet, currentTable);
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }

                JOptionPane.showMessageDialog(this,
                    "Xuất Excel thành công!\n" + file.getAbsolutePath(),
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Lỗi khi xuất Excel: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JTable getCurrentTable() {
        int selectedTab = tabbedPane.getSelectedIndex();
        String tabTitle = tabbedPane.getTitleAt(selectedTab);

        if (tabTitle.contains("Doanh thu")) {
            return tblRevenue;
        } else if (tabTitle.contains("Hàng hóa")) {
            return tblProduct;
        } else if (tabTitle.contains("Kiểm soát")) {
            return tblCashAudit; // Default to cash audit
        } else if (tabTitle.contains("Khuyến mãi")) {
            return tblPromotion;
        }
        return null;
    }

    private void exportTableToSheet(Sheet sheet, JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // Header row
        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < model.getColumnCount(); col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(model.getColumnName(col));
        }

        // Data rows
        for (int row = 0; row < model.getRowCount(); row++) {
            Row dataRow = sheet.createRow(row + 1);
            for (int col = 0; col < model.getColumnCount(); col++) {
                Cell cell = dataRow.createCell(col);
                Object value = model.getValueAt(row, col);
                if (value != null) {
                    cell.setCellValue(value.toString());
                }
            }
        }

        // Auto-size columns
        for (int col = 0; col < model.getColumnCount(); col++) {
            sheet.autoSizeColumn(col);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private LocalDate getFromDate() {
        try {
            java.util.Date date = dpFromDate.getDate();
            if (date != null) {
                return new java.sql.Date(date.getTime()).toLocalDate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private LocalDate getToDate() {
        try {
            java.util.Date date = dpToDate.getDate();
            if (date != null) {
                return new java.sql.Date(date.getTime()).toLocalDate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(AppColors.LIGHT);
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(AppColors.PRIMARY);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 35));

        // Center align for all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    /**
     * Custom cell renderer for mismatch column
     * Highlights negative values in red
     */
    private class MismatchCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null) {
                try {
                    // Check if original value starts with minus or is negative in model
                    String original = value.toString();
                    if (original.contains("-") || original.startsWith("(")) {
                        c.setBackground(new Color(255, 200, 200)); // Light red
                        c.setForeground(AppColors.DANGER);
                    } else if (!isSelected) {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }

            setHorizontalAlignment(CENTER);
            return c;
        }
    }

    /**
     * Public method to refresh data (can be called from parent)
     */
    public void refresh() {
        if (hasData) {
            loadReportData();
        }
    }

    /**
     * Set current staff (for role-based access control)
     */
    public void setCurrentStaff(Staff staff) {
        this.currentStaff = staff;
        this.currentRole = staff != null ? staff.getRole() : Role.PHARMACIST;
        // Rebuild UI if role changed
        removeAll();
        initComponents();
        setupEventListeners();
        revalidate();
        repaint();
    }
}
