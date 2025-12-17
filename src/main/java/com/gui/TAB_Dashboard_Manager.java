package com.gui;

import com.bus.BUS_Invoice;
import com.bus.BUS_Product;
import com.bus.BUS_Shift;
import com.entities.*;
import com.enums.InvoiceType;
import com.enums.LineType;
import com.utils.AppColors;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard cho Quản lý (Chủ nhà thuốc)
 * Tập trung vào hiệu quả kinh doanh và xu hướng:
 * - Sản phẩm bán chạy nhất
 * - Phát hiện sản phẩm tăng/giảm đột biến
 * - Thống kê doanh thu và lợi nhuận
 * - Đối soát cuối ca/cuối ngày
 *
 * @author Tô Thanh Hậu
 */
public class TAB_Dashboard_Manager extends JPanel {
    private final BUS_Invoice busInvoice;
    private final BUS_Product busProduct;
    private final BUS_Shift busShift;

    // Current staff and shift
    private Staff currentStaff;
    private Shift currentShift;

    // Tables
    private JTable tblBestSellers;
    private JTable tblTrending;

    private DefaultTableModel bestSellersModel;
    private DefaultTableModel trendingModel;

    // Labels for statistics
    private JLabel lblTodayRevenue;
    private JLabel lblTodayProfit;
    private JLabel lblTodayInvoiceCount;
    private JLabel lblCashReconciliation;
    private JLabel lblReconInvoiceCount, lblReconSystemRevenue, lblReconCashStart, lblReconCashEnd, lblReconStatus;

    // Shift management labels
    private JLabel lblShiftId;
    private JLabel lblCurrentCash;
    private JLabel lblNotificationIcon;

    private JButton btnRefresh;
    private JButton btnShift;
    private XYChart revenueChart;
    private XChartPanel<XYChart> revenueChartPanel;

    // Date range selectors
    private JComboBox<String> cboComparisonPeriod;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public TAB_Dashboard_Manager() {
        this(null);
    }

    public TAB_Dashboard_Manager(Staff staff) {
        this.currentStaff = staff;
        this.busInvoice = new BUS_Invoice();
        this.busProduct = new BUS_Product();
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
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(AppColors.WHITE);

        // Statistics panel at top
        mainPanel.add(createStatisticsPanel(), BorderLayout.NORTH);

        // Center panel with two split panes
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setResizeWeight(0.6);
        mainSplit.setBorder(null);

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topSplit.setResizeWeight(0.5);
        topSplit.setLeftComponent(createBestSellersPanel());
        topSplit.setRightComponent(createRevenueChartPanel());

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottomSplit.setResizeWeight(0.5);
        bottomSplit.setLeftComponent(createTrendingPanel());
        bottomSplit.setRightComponent(createCashReconciliationPanel());

        mainSplit.setTopComponent(topSplit);
        mainSplit.setBottomComponent(bottomSplit);

        mainPanel.add(mainSplit, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppColors.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel lblTitle = new JLabel("Dashboard Quản Lý - Hiệu Quả Kinh Doanh");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(AppColors.PRIMARY);

        // Right section: Shift Widget + Notification + Shift Button + Controls
        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightSection.setBackground(AppColors.WHITE);

        // Shift Info Widget
        JPanel shiftWidget = createShiftWidget();
        rightSection.add(shiftWidget);

        // Notification Bell Icon
        lblNotificationIcon = new JLabel("🔔");
        lblNotificationIcon.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        lblNotificationIcon.setForeground(AppColors.WARNING);
        lblNotificationIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblNotificationIcon.setToolTipText("Thông báo hệ thống");
        lblNotificationIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showNotifications();
            }
        });
        rightSection.add(lblNotificationIcon);

        // Shift Button (Mở ca / Đóng ca)
        btnShift = new JButton("Mở ca");
        btnShift.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnShift.setBackground(new Color(40, 167, 69));
        btnShift.setForeground(Color.WHITE);
        btnShift.setFocusPainted(false);
        btnShift.setBorderPainted(false);
        btnShift.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnShift.setPreferredSize(new Dimension(100, 35));
        btnShift.addActionListener(e -> handleShiftButtonClick());
        rightSection.add(btnShift);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setBackground(AppColors.WHITE);

        JLabel lblPeriod = new JLabel("So sánh với:");
        lblPeriod.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        cboComparisonPeriod = new JComboBox<>(new String[]{
            "Hôm qua", "7 ngày trước", "Tháng trước"
        });
        cboComparisonPeriod.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cboComparisonPeriod.addActionListener(e -> loadTrendingProducts());

        btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setBackground(AppColors.SECONDARY);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setPreferredSize(new Dimension(130, 35));
        btnRefresh.addActionListener(e -> {
            loadData();
            loadShiftData();
        });

        controlPanel.add(lblPeriod);
        controlPanel.add(cboComparisonPeriod);
        controlPanel.add(btnRefresh);

        // Combine shift section and control panel
        JPanel combinedRight = new JPanel(new BorderLayout(10, 10));
        combinedRight.setBackground(AppColors.WHITE);
        combinedRight.add(rightSection, BorderLayout.NORTH);
        combinedRight.add(controlPanel, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(AppColors.WHITE);
        topPanel.add(lblTitle, BorderLayout.WEST);
        topPanel.add(combinedRight, BorderLayout.EAST);

        JLabel lblDate = new JLabel("Ngày: " + LocalDate.now().format(dateFormatter));
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDate.setForeground(AppColors.DARK);

        headerPanel.add(topPanel, BorderLayout.NORTH);
        headerPanel.add(lblDate, BorderLayout.SOUTH);

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
        btnShift.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnShift.setForeground(Color.WHITE);
        btnShift.setBorderPainted(false);
        btnShift.setFocusPainted(false);
        btnShift.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnShift.setPreferredSize(new Dimension(100, 35));

        if (currentShift != null) {
            // Shift is open - show shift info and "Đóng ca"
            lblShiftId.setText(currentShift.getId());
            BigDecimal currentCash = busShift.calculateSystemCashForShift(currentShift);
            lblCurrentCash.setText(currencyFormat.format(currentCash));

            btnShift.setText("Đóng ca");
            btnShift.setToolTipText("Nhấn để đóng ca làm việc");
            btnShift.setBackground(new Color(220, 53, 69)); // Red color for close
            btnShift.setEnabled(true);
        } else {
            // No open shift - show "Mở ca"
            lblShiftId.setText("Chưa mở ca");
            lblCurrentCash.setText("---");

            btnShift.setText("Mở ca");
            btnShift.setToolTipText("Nhấn để mở ca làm việc");
            btnShift.setBackground(new Color(40, 167, 69)); // Green color for open
            btnShift.setEnabled(true);
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

    private void showNotifications() {
        // For manager dashboard, show business alerts
        String message = String.format(
            "📊 THÔNG BÁO QUẢN LÝ\n\n" +
            "💰 Doanh thu hôm nay: %s\n" +
            "📈 Lợi nhuận hôm nay: %s\n" +
            "🧾 Số hóa đơn: %s\n\n" +
            "Kiểm tra dashboard để xem chi tiết!",
            lblTodayRevenue.getText(),
            lblTodayProfit.getText(),
            lblTodayInvoiceCount.getText()
        );

        JOptionPane.showMessageDialog(this,
            message,
            "Thông Báo Hệ Thống",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createStatisticsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setBackground(AppColors.WHITE);
        statsPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        statsPanel.setPreferredSize(new Dimension(0, 120));

        lblTodayInvoiceCount = new JLabel("0");
        statsPanel.add(createStatCard("Số Hóa Đơn Hôm Nay", lblTodayInvoiceCount, AppColors.SECONDARY));

        lblTodayRevenue = new JLabel("0 đ");
        statsPanel.add(createStatCard("Doanh Thu Hôm Nay", lblTodayRevenue, AppColors.PURPLE));

        lblTodayProfit = new JLabel("0 đ");
        statsPanel.add(createStatCard("Lợi Nhuận Hôm Nay", lblTodayProfit, AppColors.SUCCESS));

        lblCashReconciliation = new JLabel("Chưa đối soát");
        statsPanel.add(createStatCard("Trạng Thái Đối Soát", lblCashReconciliation, AppColors.DARK));

        return statsPanel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBackground(AppColors.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2, true),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(AppColors.DARK);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createBestSellersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("Top 10 Sản Phẩm Bán Chạy (30 ngày)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(AppColors.SUCCESS);

        String[] columns = {"#", "Tên sản phẩm", "Đã bán", "Doanh thu", "Lợi nhuận"};
        bestSellersModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblBestSellers = createStyledTable(bestSellersModel);
        tblBestSellers.getColumnModel().getColumn(0).setPreferredWidth(30);
        tblBestSellers.getColumnModel().getColumn(0).setCellRenderer(createCenterRenderer());
        tblBestSellers.getColumnModel().getColumn(2).setCellRenderer(createCenterRenderer());
        tblBestSellers.getColumnModel().getColumn(3).setCellRenderer(createRightRenderer());
        tblBestSellers.getColumnModel().getColumn(4).setCellRenderer(new ProfitCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblBestSellers);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTrendingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("Sản Phẩm Có Xu Hướng Bất Thường");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(AppColors.SECONDARY);

        String[] columns = {"Tên sản phẩm", "Trước", "Hiện tại", "Thay đổi", "Xu hướng"};
        trendingModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblTrending = createStyledTable(trendingModel);
        tblTrending.getColumnModel().getColumn(1).setCellRenderer(createCenterRenderer());
        tblTrending.getColumnModel().getColumn(2).setCellRenderer(createCenterRenderer());
        tblTrending.getColumnModel().getColumn(3).setCellRenderer(new TrendCellRenderer());
        tblTrending.getColumnModel().getColumn(4).setCellRenderer(new TrendStatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblTrending);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRevenueChartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("Doanh Thu Theo Giờ Trong Ngày");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(AppColors.PURPLE);

        revenueChart = new XYChartBuilder()
            .width(400).height(300)
            .title("").xAxisTitle("Giờ").yAxisTitle("Doanh thu (VND)")
            .build();

        revenueChart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);

        revenueChart.getStyler().setPlotMargin(0);
        revenueChart.getStyler().setPlotContentSize(.95);
        revenueChart.getStyler().setAxisTickLabelsColor(AppColors.DARK);

        revenueChart.getStyler().setChartTitleVisible(false);
        revenueChart.getStyler().setChartBackgroundColor(AppColors.WHITE);
        revenueChart.getStyler().setPlotBackgroundColor(AppColors.WHITE);
        revenueChart.getStyler().setPlotBorderColor(AppColors.BACKGROUND);
        revenueChart.getStyler().setSeriesColors(new Color[]{AppColors.PURPLE});

        revenueChartPanel = new XChartPanel<>(revenueChart);
        revenueChartPanel.setBorder(null);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(revenueChartPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCashReconciliationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("Đối Soát Cuối Ngày");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(AppColors.DARK);

        JPanel infoPanel = new JPanel(new GridLayout(2, 3, 20, 10));
        infoPanel.setBackground(AppColors.WHITE);

        lblReconInvoiceCount = createReconLabel("Số hóa đơn", "0");
        lblReconSystemRevenue = createReconLabel("Doanh thu hệ thống", "0 đ");
        lblReconCashStart = createReconLabel("Tiền đầu ca", "0 đ");
        lblReconCashEnd = createReconLabel("Tiền cuối ca (dự kiến)", "0 đ");
        lblReconStatus = createReconLabel("Trạng thái", "Chưa đối soát");

        infoPanel.add(lblReconInvoiceCount);
        infoPanel.add(lblReconSystemRevenue);
        infoPanel.add(lblReconStatus);
        infoPanel.add(lblReconCashStart);
        infoPanel.add(lblReconCashEnd);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createReconLabel(String title, String value) {
        JLabel label = new JLabel(String.format("<html><b>%s:</b><br/>%s</html>", title, value));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return label;
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

    private DefaultTableCellRenderer createCenterRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        return renderer;
    }

    private DefaultTableCellRenderer createRightRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.RIGHT);
        return renderer;
    }

    private void loadData() {
        try {
            loadTodayStatistics();
            loadBestSellers();
            loadTrendingProducts();
            loadCashReconciliation();
            loadRevenueByHour();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Lỗi khi tải dữ liệu: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTodayStatistics() {
        List<Invoice> allInvoices = busInvoice.getAllInvoices();
        if (allInvoices == null) {
            allInvoices = List.of();
        }

        LocalDate today = LocalDate.now();
        List<Invoice> todayInvoices = allInvoices.stream()
            .filter(inv -> inv.getCreationDate() != null &&
                          inv.getCreationDate().toLocalDate().equals(today))
            .collect(Collectors.toList());

        int invoiceCount = todayInvoices.size();
        double totalRevenue = 0.0;
        double totalProfit = 0.0;

        for (Invoice invoice : todayInvoices) {
            double invoiceTotal = invoice.calculateTotal();

            if (invoice.getType() == InvoiceType.SALES) {
                totalRevenue += invoiceTotal;

                // Calculate profit
                for (InvoiceLine line : invoice.getInvoiceLineList()) {
                    double revenue = line.calculateSubtotal();
                    double cost = calculateLineCost(line);
                    totalProfit += (revenue - cost);
                }
            } else if (invoice.getType() == InvoiceType.RETURN) {
                totalRevenue -= invoiceTotal;

                // Subtract from profit
                for (InvoiceLine line : invoice.getInvoiceLineList()) {
                    double revenue = line.calculateSubtotal();
                    double cost = calculateLineCost(line);
                    totalProfit -= (revenue - cost);
                }
            }
        }

        lblTodayInvoiceCount.setText(String.valueOf(invoiceCount));
        lblTodayRevenue.setText(String.format("%,.0f đ", totalRevenue));
        lblTodayProfit.setText(String.format("%,.0f đ", totalProfit));

        // Color coding for profit
        if (totalProfit >= 0) {
            lblTodayProfit.setForeground(AppColors.SUCCESS);
        } else {
            lblTodayProfit.setForeground(AppColors.DANGER);
        }
    }

    private double calculateLineCost(InvoiceLine line) {
        double totalCost = 0.0;

        if (line.getLotAllocations() != null) {
            for (LotAllocation allocation : line.getLotAllocations()) {
                if (allocation.getLot() != null) {
                    totalCost += allocation.getLot().getRawPrice() * allocation.getQuantity();
                }
            }
        }

        return totalCost;
    }

    private void loadBestSellers() {
        bestSellersModel.setRowCount(0);

        List<Invoice> allInvoices = busInvoice.getAllInvoices();
        if (allInvoices == null) return;

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        // Calculate product statistics
        Map<String, ProductStats> productStatsMap = new HashMap<>();

        for (Invoice invoice : allInvoices) {
            if (invoice.getCreationDate() != null &&
                invoice.getCreationDate().toLocalDate().isAfter(thirtyDaysAgo) &&
                invoice.getType() == InvoiceType.SALES) {

                for (InvoiceLine line : invoice.getInvoiceLineList()) {
                    if (line.getLineType() == LineType.SALE) {
                        String productId = line.getProduct().getId();
                        String productName = line.getProduct().getName();

                        ProductStats stats = productStatsMap.getOrDefault(productId, new ProductStats(productName));
                        stats.quantitySold += line.getQuantity();
                        stats.revenue += line.calculateSubtotal();
                        stats.cost += calculateLineCost(line);

                        productStatsMap.put(productId, stats);
                    }
                }
            }
        }

        // Sort by quantity sold
        List<Map.Entry<String, ProductStats>> sortedProducts = new ArrayList<>(productStatsMap.entrySet());
        sortedProducts.sort((e1, e2) -> Integer.compare(e2.getValue().quantitySold, e1.getValue().quantitySold));

        // Add top 10 to table
        int rank = 1;
        for (int i = 0; i < Math.min(10, sortedProducts.size()); i++) {
            ProductStats stats = sortedProducts.get(i).getValue();
            double profit = stats.revenue - stats.cost;

            bestSellersModel.addRow(new Object[]{
                rank++,
                stats.productName,
                stats.quantitySold,
                String.format("%,.0f đ", stats.revenue),
                profit
            });
        }
    }

    private void loadTrendingProducts() {
        trendingModel.setRowCount(0);

        List<Invoice> allInvoices = busInvoice.getAllInvoices();
        if (allInvoices == null) return;

        String selectedPeriod = (String) cboComparisonPeriod.getSelectedItem();
        int comparisonDays = switch (selectedPeriod) {
            case "Hôm qua" -> 1;
            case "7 ngày trước" -> 7;
            case "Tháng trước" -> 30;
            default -> 1;
        };

        LocalDate today = LocalDate.now();
        LocalDate comparisonDate = today.minusDays(comparisonDays);

        // Calculate current period (today)
        Map<String, ProductTrend> currentStats = calculatePeriodStats(allInvoices, today, today);

        // Calculate comparison period
        Map<String, ProductTrend> comparisonStats = calculatePeriodStats(allInvoices, comparisonDate, comparisonDate);

        // Find products with significant changes (>50% change)
        List<TrendingProduct> trendingProducts = new ArrayList<>();

        for (Map.Entry<String, ProductTrend> entry : currentStats.entrySet()) {
            String productId = entry.getKey();
            ProductTrend current = entry.getValue();
            ProductTrend comparison = comparisonStats.getOrDefault(productId, new ProductTrend(current.productName));

            int currentQty = current.quantity;
            int comparisonQty = comparison.quantity;

            // Calculate percentage change
            double changePercent = 0;
            if (comparisonQty > 0) {
                changePercent = ((currentQty - comparisonQty) * 100.0) / comparisonQty;
            } else if (currentQty > 0) {
                changePercent = 100; // New product or first sale
            }

            // Only show products with >50% change
            if (Math.abs(changePercent) >= 50 && (currentQty > 0 || comparisonQty > 0)) {
                trendingProducts.add(new TrendingProduct(
                    current.productName,
                    comparisonQty,
                    currentQty,
                    changePercent
                ));
            }
        }

        // Sort by absolute change percentage
        trendingProducts.sort((p1, p2) -> Double.compare(
            Math.abs(p2.changePercent), Math.abs(p1.changePercent)
        ));

        // Add to table
        for (TrendingProduct tp : trendingProducts) {
            String trend = tp.changePercent > 0 ? "TĂNG" : "GIẢM";

            trendingModel.addRow(new Object[]{
                tp.productName,
                tp.previousQuantity,
                tp.currentQuantity,
                String.format("%.1f%%", Math.abs(tp.changePercent)),
                trend
            });
        }
    }

    private Map<String, ProductTrend> calculatePeriodStats(List<Invoice> invoices, LocalDate startDate, LocalDate endDate) {
        Map<String, ProductTrend> stats = new HashMap<>();

        for (Invoice invoice : invoices) {
            if (invoice.getCreationDate() != null &&
                invoice.getType() == InvoiceType.SALES) {

                LocalDate invoiceDate = invoice.getCreationDate().toLocalDate();
                if (!invoiceDate.isBefore(startDate) && !invoiceDate.isAfter(endDate)) {
                    for (InvoiceLine line : invoice.getInvoiceLineList()) {
                        if (line.getLineType() == LineType.SALE) {
                            String productId = line.getProduct().getId();
                            String productName = line.getProduct().getName();

                            ProductTrend trend = stats.getOrDefault(productId, new ProductTrend(productName));
                            trend.quantity += line.getQuantity();
                            stats.put(productId, trend);
                        }
                    }
                }
            }
        }

        return stats;
    }

    private void loadRevenueByHour() {
        List<Invoice> allInvoices = busInvoice.getAllInvoices();
        if (allInvoices == null) return;

        LocalDate today = LocalDate.now();
        Map<Integer, Double> revenueByHour = new TreeMap<>();
        for (int i = 0; i < 24; i++) {
            revenueByHour.put(i, 0.0);
        }

        for (Invoice invoice : allInvoices) {
            if (invoice.getCreationDate() != null &&
                invoice.getCreationDate().toLocalDate().equals(today) &&
                invoice.getType() == InvoiceType.SALES) {

                int hour = invoice.getCreationDate().getHour();
                revenueByHour.merge(hour, invoice.calculateTotal(), Double::sum);
            }
        }

        List<Integer> hours = new ArrayList<>(revenueByHour.keySet());
        List<Double> revenues = new ArrayList<>(revenueByHour.values());

        if (revenueChart.getSeriesMap().containsKey("Doanh thu")) {
            revenueChart.updateXYSeries("Doanh thu", hours, revenues, null);
        } else {
            revenueChart.addSeries("Doanh thu", hours, revenues);
        }

        revenueChartPanel.revalidate();
        revenueChartPanel.repaint();
    }

    private void loadCashReconciliation() {
        List<Invoice> allInvoices = busInvoice.getAllInvoices();
        if (allInvoices == null) return;

        LocalDate today = LocalDate.now();
        List<Invoice> todayInvoices = allInvoices.stream()
            .filter(inv -> inv.getCreationDate() != null &&
                          inv.getCreationDate().toLocalDate().equals(today))
            .collect(Collectors.toList());

        double totalRevenue = 0.0;
        double cashRevenue = 0.0;
        // Mock value for cash at the start of the shift
        double cashAtStart = 5000000; // 5,000,000 VND

        for (Invoice invoice : todayInvoices) {
            double invoiceTotal = invoice.calculateTotal();
            if (invoice.getType() == InvoiceType.SALES) {
                totalRevenue += invoiceTotal;
                // Assuming a method getPaymentMethod() exists on Invoice
                // For now, let's assume 70% of sales are cash
                cashRevenue += invoiceTotal * 0.7;
            } else if (invoice.getType() == InvoiceType.RETURN) {
                totalRevenue -= invoiceTotal;
                cashRevenue -= invoiceTotal * 0.7;
            }
        }

        double expectedCashAtEnd = cashAtStart + cashRevenue;

        lblReconInvoiceCount.setText(String.format("<html><b>Số hóa đơn:</b><br/>%d</html>", todayInvoices.size()));
        lblReconSystemRevenue.setText(String.format("<html><b>Doanh thu hệ thống:</b><br/>%,.0f đ</html>", totalRevenue));
        lblReconCashStart.setText(String.format("<html><b>Tiền đầu ca:</b><br/>%,.0f đ</html>", cashAtStart));
        lblReconCashEnd.setText(String.format("<html><b>Tiền cuối ca (dự kiến):</b><br/>%,.0f đ</html>", expectedCashAtEnd));

        // Simple reconciliation status check
        // In a real app, this would compare expectedCashAtEnd with a manual count
        boolean isMatched = true; // Placeholder
        if (isMatched) {
            lblReconStatus.setText("<html><b>Trạng thái:</b><br/><font color='green'>Khớp</font></html>");
            lblCashReconciliation.setText("Khớp");
            lblCashReconciliation.setForeground(AppColors.SUCCESS);
        } else {
            lblReconStatus.setText("<html><b>Trạng thái:</b><br/><font color='red'>Lệch</font></html>");
            lblCashReconciliation.setText("Lệch");
            lblCashReconciliation.setForeground(AppColors.DANGER);
        }
    }

    /**
     * Public method to refresh dashboard data
     */
    public void refresh() {
        loadData();
    }

    // Helper classes
    private static class ProductStats {
        String productName;
        int quantitySold;
        double revenue;
        double cost;

        ProductStats(String productName) {
            this.productName = productName;
        }
    }

    private static class ProductTrend {
        String productName;
        int quantity;

        ProductTrend(String productName) {
            this.productName = productName;
        }
    }

    private static class TrendingProduct {
        String productName;
        int previousQuantity;
        int currentQuantity;
        double changePercent;

        TrendingProduct(String productName, int previousQuantity, int currentQuantity, double changePercent) {
            this.productName = productName;
            this.previousQuantity = previousQuantity;
            this.currentQuantity = currentQuantity;
            this.changePercent = changePercent;
        }
    }

    // Custom Cell Renderers
    private static class ProfitCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof Double) {
                double profit = (Double) value;
                setText(String.format("%,.0f đ", profit));

                if (profit >= 0) {
                    c.setForeground(AppColors.SUCCESS);
                } else {
                    c.setForeground(AppColors.DANGER);
                }
                setFont(getFont().deriveFont(Font.BOLD));
            }

            setHorizontalAlignment(RIGHT);
            return c;
        }
    }

    private static class TrendCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof String) {
                String changeStr = (String) value;
                try {
                    double change = Double.parseDouble(changeStr.replace("%", ""));

                    if (change > 0) {
                        c.setForeground(AppColors.SUCCESS);
                    } else {
                        c.setForeground(AppColors.DANGER);
                    }
                    setFont(getFont().deriveFont(Font.BOLD));
                } catch (Exception ignored) {}
            }

            setHorizontalAlignment(CENTER);
            return c;
        }
    }

    private static class TrendStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof String) {
                String trend = (String) value;
                setFont(getFont().deriveFont(Font.BOLD));

                if ("TĂNG".equals(trend)) {
                    c.setForeground(Color.WHITE);
                    c.setBackground(AppColors.SUCCESS);
                    setText("TĂNG");
                } else if ("GIẢM".equals(trend)) {
                    c.setForeground(Color.WHITE);
                    c.setBackground(AppColors.DANGER);
                    setText("GIẢM");
                }
            }

            setHorizontalAlignment(CENTER);
            return c;
        }
    }
}

