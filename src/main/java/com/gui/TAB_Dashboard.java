package com.gui;

import org.knowm.xchart.*;
import org.knowm.xchart.internal.chartpart.Chart;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.PieSeries;
import org.knowm.xchart.CategorySeries;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard tab using XChart to visualize business metrics.
 * This implementation uses a stub data provider (SampleDashboardDataProvider) so the UI is functional.
 * Integrate with real services by providing another DashboardDataProvider implementation.
 */
public class TAB_Dashboard {
    // Root
    public JPanel pDashboard;

    // Metric cards
    private MetricCard cardOrders;
    private MetricCard cardRevenue;
    private MetricCard cardNewCustomers; // reused as cancellation rate card
    private MetricCard cardTopProducts;  // reused as inventory risk summary card

    // Alerts
    private JPanel pAlerts;
    private JLabel lblAlertMessage;

    // Charts (Management)
    private JPanel pChartsContainer;
    private JComboBox<String> cmbGranularity; // Day, Week, Month
    private XChartPanel<?> revenueChartPanel; // changed generic to wildcard
    private JTabbedPane chartsTabs;
    private XChartPanel<CategoryChart> topProductsChartPanel;
    private JPanel revenueTab; // keep reference to update chart
    private JPanel topProductsTab; // new: container for top products chart

    // Decision feed (Management)
    private JPanel pDecisionFeed;
    private JPanel pDecisionList;

    // Tables (Management)
    private JTable tblRecentOrders;
    private JTable tblTopCustomers;
    private JTable tblAnomalies;
    private JComboBox<String> cmbOrderStatusFilter;
    private DefaultTableModel recentOrdersModel;
    private DefaultTableModel topCustomersModel;
    private DefaultTableModel anomaliesModel;
    private TableRowSorter<DefaultTableModel> orderSorter;

    // New: Inventory Risk table for decision support
    private DefaultTableModel inventoryRiskModel;
    private JTable tblInventoryRisk;

    // Staff panel components
    private JPanel pStaffPanel;
    private XChartPanel<CategoryChart> kpiChartPanel;
    private JSplitPane splitStaffTop; // retain top split for KPI swap
    private DefaultListModel<String> promotionsModel;
    private JList<String> lstPromotions;
    private DefaultTableModel lowStockModel;
    private JTable tblLowStock;
    private DefaultTableModel recentOrdersStaffModel;
    private JTable tblRecentOrdersStaff;

    // Data Provider
    private final DashboardDataProvider dataProvider;

    // Formatting utilities
    private final DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM");
    private final Locale viVN = Locale.forLanguageTag("vi-VN"); // updated to avoid deprecated constructor
    private final java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance(viVN);

    // State
    private Granularity currentGranularity = Granularity.DAY;
    private RevenueTrend currentTrend;

    // Auto refresh
    private javax.swing.Timer refreshTimer;

    public TAB_Dashboard() {
        this(new SampleDashboardDataProvider());
    }

    public TAB_Dashboard(DashboardDataProvider provider) {
        this.dataProvider = provider;
        initUI();
        loadAllData();
        // periodic refresh every 30s
        refreshTimer = new javax.swing.Timer(30_000, e -> loadAllData());
        refreshTimer.start();
    }

    // Expose root for consistency with other tabs
    public JComponent $$$getRootComponent$$$() { // mimic IDEA pattern
        return pDashboard;
    }

    private void initUI() {
        pDashboard = new JPanel(new BorderLayout(12, 12));
        pDashboard.setBorder(new EmptyBorder(12, 12, 12, 12));

        // NORTH: Metrics + Alerts
        JPanel pNorth = new JPanel();
        pNorth.setLayout(new BorderLayout(12, 12));
        pNorth.add(buildMetricCardsPanel(), BorderLayout.CENTER);
        pNorth.add(buildAlertsPanel(), BorderLayout.SOUTH);
        pDashboard.add(pNorth, BorderLayout.NORTH);

        // CENTER: Audiences as tabs (Management / Staff)
        JTabbedPane audienceTabs = new JTabbedPane();
        audienceTabs.addTab("Quản lý", buildManagementPanel());
        audienceTabs.addTab("Nhân viên", buildStaffPanel());
        pDashboard.add(audienceTabs, BorderLayout.CENTER);
    }

    // region Panels builders
    private JComponent buildMetricCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 0));
        cardOrders = new MetricCard("Đơn hàng", "0 hôm nay / 0 tuần", UIManager.getIcon("OptionPane.informationIcon"), new Color(0x1976D2));
        cardRevenue = new MetricCard("Doanh thu", currencyFormat.format(0) + " hôm nay", UIManager.getIcon("OptionPane.questionIcon"), new Color(0x2E7D32));
        // Re-purpose to cancellation rate (decision-support)
        cardNewCustomers = new MetricCard("Tỷ lệ hủy", "0% (0)", UIManager.getIcon("OptionPane.warningIcon"), new Color(0xF9A825));
        // Re-purpose to inventory risk summary (decision-support)
        cardTopProducts = new MetricCard("Rủi ro tồn kho", "--", UIManager.getIcon("OptionPane.errorIcon"), new Color(0x6A1B9A));
        panel.add(cardOrders);
        panel.add(cardRevenue);
        panel.add(cardNewCustomers);
        panel.add(cardTopProducts);
        return panel;
    }

    private JComponent buildAlertsPanel() {
        pAlerts = new JPanel(new BorderLayout());
        pAlerts.setBorder(BorderFactory.createTitledBorder("Cảnh báo"));
        lblAlertMessage = new JLabel("Không có cảnh báo.");
        pAlerts.add(lblAlertMessage, BorderLayout.CENTER);
        return pAlerts;
    }

    private JComponent buildManagementPanel() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        // Decision feed at top
        root.add(buildDecisionFeedPanel(), BorderLayout.NORTH);
        // Charts + Tables split in center
        JSplitPane splitMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitMain.setResizeWeight(0.5);
        splitMain.setTopComponent(buildChartsPanel());
        splitMain.setBottomComponent(buildTablesPanel());
        root.add(splitMain, BorderLayout.CENTER);
        return root;
    }

    private JComponent buildDecisionFeedPanel() {
        pDecisionFeed = new JPanel(new BorderLayout(6, 6));
        pDecisionFeed.setBorder(BorderFactory.createTitledBorder("Quyết định đề xuất"));
        pDecisionList = new JPanel();
        pDecisionList.setLayout(new BoxLayout(pDecisionList, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(pDecisionList);
        scroll.setPreferredSize(new Dimension(200, 140));
        pDecisionFeed.add(scroll, BorderLayout.CENTER);
        return pDecisionFeed;
    }

    private JComponent buildChartsPanel() {
        pChartsContainer = new JPanel(new BorderLayout(8, 8));
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.add(new JLabel("Doanh thu theo:"));
        cmbGranularity = new JComboBox<>(new String[]{"Ngày", "Tuần", "Tháng", "Quý"});
        cmbGranularity.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Granularity g = Granularity.fromDisplay((String) e.getItem());
                if (g != currentGranularity) {
                    currentGranularity = g;
                    updateRevenueTrend();
                }
            }
        });
        topBar.add(cmbGranularity);
        pChartsContainer.add(topBar, BorderLayout.NORTH);

        // Initial revenue chart build
        currentTrend = dataProvider.revenueTrend(currentGranularity, 14);
        Chart<?, ?> chart = buildRevenueChart(currentTrend);
        revenueChartPanel = new XChartPanel<>(chart);

        chartsTabs = new JTabbedPane();
        // Tab 0: Revenue only (remove non-actionable Top Products chart)
        revenueTab = new JPanel(new BorderLayout());
        revenueTab.add(revenueChartPanel, BorderLayout.CENTER);
        chartsTabs.addTab("Doanh thu", revenueTab);

        pChartsContainer.add(chartsTabs, BorderLayout.CENTER);
        return pChartsContainer;
    }

    private JComponent buildTablesPanel() {
        JTabbedPane tabs = new JTabbedPane();
        // Remove non-actionable tables from management view; focus on risk and anomalies
        tabs.addTab("Rủi ro tồn kho", buildInventoryRiskPanel());
        tabs.addTab("Sản phẩm đột biến", buildAnomaliesPanel());
        return tabs;
    }

    private JComponent buildAnomaliesPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        anomaliesModel = new DefaultTableModel(new Object[]{"Sản phẩm", "Thay đổi", "Gợi ý"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblAnomalies = new JTable(anomaliesModel);
        tblAnomalies.setRowHeight(24);
        tblAnomalies.getColumnModel().getColumn(1).setCellRenderer(new PercentRenderer());
        panel.add(new JScrollPane(tblAnomalies), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildRecentOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filterBar.add(new JLabel("Lọc trạng thái:"));
        cmbOrderStatusFilter = new JComboBox<>(new String[]{"Tất cả", "Mới", "Đang xử lý", "Hoàn tất", "Hủy"});
        cmbOrderStatusFilter.addActionListener(e -> applyOrderStatusFilter());
        filterBar.add(cmbOrderStatusFilter);
        panel.add(filterBar, BorderLayout.NORTH);

        recentOrdersModel = new DefaultTableModel(new Object[]{"Mã", "Khách hàng", "Ngày", "Trạng thái", "Tổng"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblRecentOrders = new JTable(recentOrdersModel);
        tblRecentOrders.setFillsViewportHeight(true);
        tblRecentOrders.setRowHeight(24);
        orderSorter = new TableRowSorter<>(recentOrdersModel);
        tblRecentOrders.setRowSorter(orderSorter);

        // Status color renderer
        tblRecentOrders.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        // Currency renderer
        tblRecentOrders.getColumnModel().getColumn(4).setCellRenderer(new CurrencyRenderer());

        panel.add(new JScrollPane(tblRecentOrders), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildTopCustomersPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));

        topCustomersModel = new DefaultTableModel(new Object[]{"Khách hàng", "Tổng chi tiêu"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblTopCustomers = new JTable(topCustomersModel);
        tblTopCustomers.setFillsViewportHeight(true);
        tblTopCustomers.setRowHeight(24);
        tblTopCustomers.getColumnModel().getColumn(1).setCellRenderer(new CurrencyRenderer());

        tblTopCustomers.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tblTopCustomers.getSelectedRow();
                    if (row >= 0) {
                        String name = (String) tblTopCustomers.getValueAt(row, 0);
                        showCustomerHistoryDialog(name);
                    }
                }
            }
        });
        panel.add(new JScrollPane(tblTopCustomers), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildStaffPanel() {
        pStaffPanel = new JPanel(new BorderLayout(8, 8));

        // Top: KPI chart and Promotions
        JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitStaffTop = top;
        top.setResizeWeight(0.6);
        kpiChartPanel = new XChartPanel<>(createKPIChart(new SalesKPI(0, 100, 0, 700)));

        JPanel promoPanel = new JPanel(new BorderLayout());
        promoPanel.setBorder(BorderFactory.createTitledBorder("Khuyến mãi đang áp dụng"));
        promotionsModel = new DefaultListModel<>();
        lstPromotions = new JList<>(promotionsModel);
        promoPanel.add(new JScrollPane(lstPromotions), BorderLayout.CENTER);

        top.setLeftComponent(kpiChartPanel);
        top.setRightComponent(promoPanel);

        // Bottom: Low stock and recent orders
        JSplitPane bottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottom.setResizeWeight(0.6);

        JPanel lowStockPanel = new JPanel(new BorderLayout());
        lowStockPanel.setBorder(BorderFactory.createTitledBorder("Sản phẩm sắp hết / hết hàng"));
        lowStockModel = new DefaultTableModel(new Object[]{"Sản phẩm", "Tồn kho", "HSD", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLowStock = new JTable(lowStockModel);
        tblLowStock.setRowHeight(24);
        tblLowStock.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        lowStockPanel.add(new JScrollPane(tblLowStock), BorderLayout.CENTER);

        JPanel recentPanel = new JPanel(new BorderLayout());
        recentPanel.setBorder(BorderFactory.createTitledBorder("Đơn gần đây"));
        recentOrdersStaffModel = new DefaultTableModel(new Object[]{"Mã", "Khách hàng", "Ngày", "Trạng thái", "Tổng"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRecentOrdersStaff = new JTable(recentOrdersStaffModel);
        tblRecentOrdersStaff.setRowHeight(24);
        tblRecentOrdersStaff.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        tblRecentOrdersStaff.getColumnModel().getColumn(4).setCellRenderer(new CurrencyRenderer());
        recentPanel.add(new JScrollPane(tblRecentOrdersStaff), BorderLayout.CENTER);

        bottom.setLeftComponent(lowStockPanel);
        bottom.setRightComponent(recentPanel);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        vertical.setResizeWeight(0.45);
        pStaffPanel.add(vertical, BorderLayout.CENTER);
        return pStaffPanel;
    }
    // endregion

    // region Data load and updates
    private void loadAllData() {
        loadMetrics();
        updateRevenueTrend();
        // Remove non-actionable loads from management
        // loadRecentOrders();
        // loadTopCustomers();
        // loadTopProductsChart();
        loadAnomalies();
        loadDecisionFeed();
        loadInventoryRisk();
        loadStaffData();
        evaluateAlerts();
    }

    private void loadMetrics() {
        DashboardMetrics m = dataProvider.fetchMetrics();
        cardOrders.setValue(m.ordersToday + " hôm nay / " + m.ordersWeek + " tuần");
        cardRevenue.setValue(currencyFormat.format(m.revenueToday) + " hôm nay / " + currencyFormat.format(m.revenueWeek) + " tuần");
        cardRevenue.setDelta(calcDeltaPercent(m.revenueToday, m.revenueYesterday));
        // Compute cancellation rate from order status counts
        Map<String, Integer> statusCounts = dataProvider.orderStatusCounts();
        int totalOrders = statusCounts.values().stream().mapToInt(Integer::intValue).sum();
        int canceled = statusCounts.getOrDefault("Hủy", 0);
        double cancelRate = totalOrders > 0 ? (canceled * 100.0 / totalOrders) : 0.0;
        cardNewCustomers.setValue(String.format("%.1f%% (%d)", cancelRate, canceled));
        // Inventory risk summary
        List<StockItem> riskItems = dataProvider.lowStockItems(50);
        int outOfStock = 0, low = 0, nearExpiry = 0;
        LocalDate now = LocalDate.now();
        for (StockItem s : riskItems) {
            if (s.stock() <= 0) outOfStock++;
            else if (s.stock() < 10) low++;
            if (s.expiryDate() != null && !s.expiryDate().isAfter(now.plusDays(14))) nearExpiry++;
        }
        cardTopProducts.setValue(String.format("Hết: %d / Sắp hết: %d / Cận HSD: %d", outOfStock, low, nearExpiry));
    }

    private void updateRevenueTrend() {
        currentTrend = dataProvider.revenueTrend(currentGranularity, 14); // last 14 periods
        Chart<?, ?> newChart = buildRevenueChart(currentTrend);
        XChartPanel<?> newPanel = new XChartPanel<>(newChart);
        revenueChartPanel = newPanel;
        if (revenueTab != null) {
            revenueTab.removeAll();
            revenueTab.add(revenueChartPanel, BorderLayout.CENTER);
        }
        pChartsContainer.revalidate();
        pChartsContainer.repaint();
    }

    private void loadRecentOrders() {
        recentOrdersModel.setRowCount(0);
        for (OrderSummary o : dataProvider.recentOrders(20)) {
            recentOrdersModel.addRow(new Object[]{o.id(), o.customerName(), o.date().format(dateTimeFmt), o.status(), currencyFormat.format(o.total())});
        }
    }

    private void loadTopCustomers() {
        topCustomersModel.setRowCount(0);
        for (CustomerSpend c : dataProvider.topCustomers(5)) {
            topCustomersModel.addRow(new Object[]{c.customerName(), currencyFormat.format(c.totalSpend())});
        }
    }

    private void loadTopProductsChart() {
        List<ProductSales> tops = dataProvider.topProducts(5);
        CategoryChart chart = createTopProductsChart(tops);
        topProductsChartPanel = new XChartPanel<>(chart);
        if (topProductsTab != null) {
            topProductsTab.removeAll();
            topProductsTab.add(topProductsChartPanel, BorderLayout.CENTER);
        }
    }

    private void loadAnomalies() {
        if (anomaliesModel == null) return;
        anomaliesModel.setRowCount(0);
        for (ProductAnomaly a : dataProvider.productAnomalies()) {
            String change = String.format("%s%.1f%%", a.increased() ? "+" : "-", Math.abs(a.changePercent()));
            String suggestion = a.increased() ? "Tăng tồn kho/đặt thêm" : "Xem xét khuyến mãi đẩy bán";
            anomaliesModel.addRow(new Object[]{a.productName(), change, suggestion});
        }
    }

    private void loadDecisionFeed() {
        if (pDecisionList == null) return;
        pDecisionList.removeAll();
        List<DecisionItem> items = dataProvider.decisionFeed();
        if (items == null || items.isEmpty()) {
            JLabel none = new JLabel("Không có đề xuất nào.");
            none.setForeground(new Color(0x2E7D32));
            pDecisionList.add(none);
        } else {
            items.stream().sorted(Comparator.comparing(DecisionItem::severity).reversed())
                    .forEach(this::addDecisionCard);
        }
        pDecisionList.revalidate();
        pDecisionList.repaint();
    }

    private void addDecisionCard(DecisionItem d) {
        JPanel card = new JPanel(new BorderLayout(6, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(severityColor(d.severity())),
                new EmptyBorder(6, 8, 6, 8)));
        JLabel title = new JLabel(d.title());
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setForeground(severityColor(d.severity()));
        JLabel desc = new JLabel("<html>" + d.description() + "<br/><i>Tác động ước tính: " + d.impact() + "</i></html>");
        JButton btn = new JButton(d.actionLabel());
        btn.addActionListener(e -> JOptionPane.showMessageDialog(pDashboard,
                d.actionLabel() + " (stub) cho: " + d.title(),
                "Thực thi đề xuất", JOptionPane.INFORMATION_MESSAGE));
        card.add(title, BorderLayout.NORTH);
        card.add(desc, BorderLayout.CENTER);
        card.add(btn, BorderLayout.EAST);
        pDecisionList.add(card);
        pDecisionList.add(Box.createVerticalStrut(6));
    }

    private Color severityColor(DecisionSeverity s) {
        return switch (s) {
            case CRITICAL -> new Color(0xC62828);
            case WARNING -> new Color(0xF9A825);
            case INFO -> new Color(0x2E7D32);
        };
    }

    private void evaluateAlerts() {
        DashboardMetrics m = dataProvider.fetchMetrics();
        List<String> alerts = new ArrayList<>();
        double revenueDelta = calcDeltaPercent(m.revenueToday, m.revenueYesterday);
        if (!Double.isNaN(revenueDelta) && revenueDelta < -20) {
            alerts.add(String.format("Doanh thu hôm nay giảm %.1f%% so với hôm qua", Math.abs(revenueDelta)));
        }
        // Anomaly on total revenue vs average of current series
        if (currentTrend != null && currentTrend.revenues != null && !currentTrend.revenues.isEmpty()) {
            double last = currentTrend.revenues.get(currentTrend.revenues.size() - 1);
            double avg = currentTrend.revenues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            if (avg > 0) {
                double dev = (last - avg) / avg * 100.0;
                if (Math.abs(dev) >= 30) {
                    String dir = dev > 0 ? "tăng" : "giảm";
                    alerts.add(String.format("Biến động đột biến: Doanh thu %s %.1f%% so với trung bình", dir, Math.abs(dev)));
                }
            }
        }
        // Product anomalies summary
        List<ProductAnomaly> pa = dataProvider.productAnomalies();
        if (!pa.isEmpty()) {
            String summary = pa.stream().limit(2)
                    .map(a -> a.productName() + (a.increased() ? " ↑" : " ↓") + String.format("%.0f%%", Math.abs(a.changePercent())))
                    .collect(Collectors.joining(", "));
            alerts.add("Sản phẩm đột biến: " + summary);
        }
        if (m.ordersToday == 0 && m.zeroOrderStreakDays >= 3) {
            alerts.add("Không có đơn hàng trong " + m.zeroOrderStreakDays + " ngày!");
        }
        if (!dataProvider.slowProducts().isEmpty()) {
            alerts.add("Có sản phẩm bán chậm cần khuyến mãi");
        }
        // Simple anomaly suggestion (stub): if top product quantity swings randomly
        List<ProductSales> tps = dataProvider.topProducts(1);
        if (!tps.isEmpty() && new Random().nextBoolean()) {
            alerts.add("Khuyến nghị: " + tps.get(0).productName + " giảm 30% để kích cầu");
        }
        if (alerts.isEmpty()) {
            setAlertMessage("Không có cảnh báo.", new Color(0x2E7D32));
        } else {
            setAlertMessage(String.join(" | ", alerts), new Color(0xC62828));
        }
    }

    private void setAlertMessage(String msg, Color color) {
        lblAlertMessage.setText(msg);
        lblAlertMessage.setForeground(color);
    }
    // endregion

    // region Staff loads
    private void loadStaffData() {
        loadKPI();
        loadPromotions();
        loadLowStock();
        loadRecentOrdersStaff();
    }

    private void loadKPI() {
        SalesKPI kpi = dataProvider.salesKPI();
        CategoryChart c = createKPIChart(kpi);
        kpiChartPanel = new XChartPanel<>(c);
        if (splitStaffTop != null) {
            splitStaffTop.setLeftComponent(kpiChartPanel);
            splitStaffTop.setResizeWeight(0.6);
        }
    }

    private void loadPromotions() {
        promotionsModel.clear();
        for (PromotionInfo p : dataProvider.activePromotions()) {
            String line = String.format("%s - giảm %.0f%%, hết hạn %s", p.name(), p.discountPercent(), p.endDate().format(DateTimeFormatter.ofPattern("dd/MM")));
            promotionsModel.addElement(line);
        }
        if (promotionsModel.isEmpty()) promotionsModel.addElement("Không có khuyến mãi");
    }

    private void loadLowStock() {
        lowStockModel.setRowCount(0);
        List<StockItem> items = new ArrayList<>(dataProvider.lowStockItems(10));
        // Sort: expired first, then out of stock, then low stock
        items.sort(Comparator.comparing((StockItem s) -> s.expiryDate() == null ? 1 : (s.expiryDate().isBefore(LocalDate.now()) ? 0 : 1))
                .thenComparing((StockItem s) -> s.stock() <= 0 ? 0 : (s.stock() < 10 ? 1 : 2)));
        for (StockItem s : items) {
            boolean expired = s.expiryDate() != null && s.expiryDate().isBefore(LocalDate.now());
            String status = expired ? "Hết hạn" : (s.stock() <= 0 ? "Hết hàng" : (s.stock() < 10 ? "Sắp hết" : ""));
            String hsd = s.expiryDate() != null ? s.expiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
            lowStockModel.addRow(new Object[]{s.productName(), s.stock(), hsd, status});
        }
    }

    private void loadRecentOrdersStaff() {
        recentOrdersStaffModel.setRowCount(0);
        List<OrderSummary> list = dataProvider.recentOrders(10);
        for (OrderSummary o : list) {
            recentOrdersStaffModel.addRow(new Object[]{o.id(), o.customerName(), o.date().format(dateTimeFmt), o.status(), currencyFormat.format(o.total())});
        }
    }
    // endregion

    // region Helpers
    private double calcDeltaPercent(double current, double previous) {
        if (previous <= 0) return Double.NaN;
        return (current - previous) / previous * 100.0;
    }

    private void applyOrderStatusFilter() {
        String sel = (String) cmbOrderStatusFilter.getSelectedItem();
        if (sel == null || sel.equals("Tất cả")) {
            orderSorter.setRowFilter(null);
        } else {
            orderSorter.setRowFilter(RowFilter.regexFilter("^" + sel + "$", 3));
        }
    }

    private Chart<?, ?> buildRevenueChart(RevenueTrend trend) {
        // Always use CategoryChart; render style switches to Line for non-day
        CategoryChart chart = new CategoryChartBuilder().title("Doanh thu - " + currentGranularity.display).width(600).height(400).build();
        styleRevenueChart(chart);
        CategoryStyler st = chart.getStyler();
        switch (currentGranularity) {
            case DAY -> {
                st.setDefaultSeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Bar);
                st.setLegendVisible(true); // explain colors for day
                List<String> xLabels = trend.dates.stream().map(d -> formatLabel(d, currentGranularity)).toList();
                double avg = trend.revenues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                List<Number> above = new ArrayList<>();
                List<Number> below = new ArrayList<>();
                List<Number> avgLine = new ArrayList<>();
                for (Double v : trend.revenues) {
                    boolean isBelow = v != null && v < avg;
                    above.add(isBelow ? null : v);
                    below.add(isBelow ? v : null);
                    avgLine.add(avg);
                }
                chart.addSeries(">= Trung bình", xLabels, above);
                chart.addSeries("< Trung bình", xLabels, below);
                chart.addSeries("Trung bình", xLabels, avgLine);
                // Force average as line
                CategorySeries sAvg = chart.getSeriesMap().get("Trung bình");
                if (sAvg != null) sAvg.setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);
                // Colors: green, red, gray
                st.setSeriesColors(new Color[]{new Color(0x2E7D32), new Color(0xC62828), new Color(0x90A4AE)});
                return chart;
            }
            default -> {
                st.setDefaultSeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);
                List<String> xLabels = trend.dates.stream().map(d -> formatLabel(d, currentGranularity)).toList();
                chart.addSeries("Doanh thu", xLabels, trend.revenues);
                return chart;
            }
        }
    }

    private String formatLabel(LocalDate d, Granularity g) {
        return switch (g) {
            case DAY -> d.format(dateFmt);
            case WEEK -> {
                WeekFields wf = WeekFields.of(viVN);
                int w = d.get(wf.weekOfWeekBasedYear());
                int y = d.getYear();
                yield "Tuần " + w + "\n" + y; // line break for compactness
            }
            case MONTH -> d.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            case QUARTER -> {
                int q = (d.getMonthValue() - 1) / 3 + 1;
                yield "Q" + q + " " + d.getYear();
            }
        };
    }

    private void styleRevenueChart(Chart<?, ?> chart) {
        Styler styler = chart.getStyler();
        styler.setLegendVisible(false);
        styler.setChartBackgroundColor(pDashboard.getBackground());
        if (styler instanceof XYStyler xy) {
            xy.setPlotGridLinesVisible(true);
            xy.setPlotContentSize(0.92);
        } else if (styler instanceof CategoryStyler cc) {
            cc.setPlotGridLinesVisible(true);
            cc.setPlotContentSize(0.92);
        }
    }

    private PieChart createEmptyPieChart(String title) {
        PieChart chart = new PieChartBuilder().title(title).width(400).height(400).build();
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setChartBackgroundColor(pDashboard.getBackground());
        return chart;
    }

    private PieChart createDonutChart(String title, Map<String, ? extends Number> data) {
        // method retained for compatibility, but not used in management anymore
        PieChart chart = new PieChartBuilder().title(title).width(400).height(400).build();
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setDefaultSeriesRenderStyle(PieSeries.PieSeriesRenderStyle.Donut);
        chart.getStyler().setChartBackgroundColor(pDashboard.getBackground());
        chart.getStyler().setSeriesColors(new Color[]{new Color(0x1565C0), new Color(0xF9A825), new Color(0x2E7D32), new Color(0xC62828)});
        if (data == null || data.isEmpty()) {
            chart.addSeries("Không có dữ liệu", 1);
        } else {
            data.forEach(chart::addSeries);
        }
        return chart;
    }

    private CategoryChart createTopProductsChart(List<ProductSales> tops) {
        CategoryChart chart = new CategoryChartBuilder().title("Top sản phẩm bán chạy").width(500).height(400).build();
        CategoryStyler st = chart.getStyler();
        st.setLegendVisible(false);
        st.setChartBackgroundColor(pDashboard.getBackground());
        List<String> names = tops.stream().map(ProductSales::productName).toList();
        List<Integer> qty = tops.stream().map(ProductSales::quantity).toList();
        chart.addSeries("Bán chạy", names.isEmpty() ? List.of("Không có") : names, names.isEmpty() ? List.of(0) : qty);
        chart.getStyler().setSeriesColors(new Color[]{new Color(0x2E7D32)});
        return chart;
    }

    private CategoryChart createKPIChart(SalesKPI kpi) {
        CategoryChart chart = new CategoryChartBuilder().title("KPI cá nhân").width(500).height(400).build();
        CategoryStyler st = chart.getStyler();
        st.setLegendVisible(true);
        st.setChartBackgroundColor(pDashboard.getBackground());
        // st.setHasAnnotations(true); // removed for compatibility
        List<String> cats = List.of("Ngày", "Tuần");
        chart.addSeries("Hiện tại", cats, List.of(kpi.dayCurrent(), kpi.weekCurrent()));
        chart.addSeries("Mục tiêu", cats, List.of(kpi.dayTarget(), kpi.weekTarget()));
        chart.getStyler().setSeriesColors(new Color[]{new Color(0x1976D2), new Color(0x90A4AE)});
        return chart;
    }

    private void showCustomerHistoryDialog(String customerName) {
        // In real implementation, fetch history. Here stub summary.
        JOptionPane.showMessageDialog(pDashboard, "Lịch sử mua hàng của " + customerName + " (stub)", "Lịch sử", JOptionPane.INFORMATION_MESSAGE);
    }

    // New: Inventory risk panel for management
    private JComponent buildInventoryRiskPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        inventoryRiskModel = new DefaultTableModel(new Object[]{"Sản phẩm", "Tồn kho", "HSD", "Rủi ro", "Gợi ý"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblInventoryRisk = new JTable(inventoryRiskModel);
        tblInventoryRisk.setRowHeight(24);
        tblInventoryRisk.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        panel.add(new JScrollPane(tblInventoryRisk), BorderLayout.CENTER);
        return panel;
    }

    private void loadInventoryRisk() {
        if (inventoryRiskModel == null) return;
        inventoryRiskModel.setRowCount(0);
        List<StockItem> items = new ArrayList<>(dataProvider.lowStockItems(50));
        LocalDate now = LocalDate.now();
        // Priority sorting: expired (critical), out of stock, near expiry, low stock
        items.sort(Comparator
                .comparing((StockItem s) -> s.expiryDate() != null && s.expiryDate().isBefore(now))
                .reversed()
                .thenComparing((StockItem s) -> s.stock() <= 0)
                .reversed()
                .thenComparing((StockItem s) -> s.expiryDate() != null && !s.expiryDate().isAfter(now.plusDays(14)))
                .reversed()
                .thenComparing((StockItem s) -> s.stock() < 10)
                .reversed());
        for (StockItem s : items) {
            boolean expired = s.expiryDate() != null && s.expiryDate().isBefore(now);
            boolean nearExp = s.expiryDate() != null && !s.expiryDate().isAfter(now.plusDays(14));
            String risk;
            String suggestion;
            if (expired) {
                risk = "Hết hạn";
                suggestion = "Loại bỏ/Trả NCC";
            } else if (s.stock() <= 0) {
                risk = "Hết hàng";
                suggestion = "Đặt hàng ngay";
            } else if (nearExp) {
                risk = "Cận HSD";
                suggestion = "Giảm giá xả hàng";
            } else if (s.stock() < 10) {
                risk = "Sắp hết";
                suggestion = "Tăng mức đặt hàng";
            } else {
                risk = "";
                suggestion = "";
            }
            String hsd = s.expiryDate() != null ? s.expiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
            inventoryRiskModel.addRow(new Object[]{s.productName(), s.stock(), hsd, risk, suggestion});
        }
    }
    // endregion

    // region Inner classes & records
    private static class MetricCard extends JPanel {
        private final JLabel lblTitle;
        private final JLabel lblValue;
        private final JLabel lblDelta;

        MetricCard(String title, String value, Icon icon, Color color) {
            setLayout(new BorderLayout(4, 4));
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color.darker()), new EmptyBorder(8, 8, 8, 8)));
            setBackground(color);
            lblTitle = new JLabel(title + ":");
            lblTitle.setForeground(Color.WHITE);
            lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 13f));
            lblValue = new JLabel(value);
            lblValue.setForeground(Color.WHITE);
            lblValue.setFont(lblValue.getFont().deriveFont(Font.PLAIN, 12f));
            lblDelta = new JLabel("");
            lblDelta.setForeground(Color.WHITE);
            lblDelta.setFont(lblDelta.getFont().deriveFont(Font.PLAIN, 11f));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            top.setOpaque(false);
            if (icon != null) {
                JLabel iconLbl = new JLabel(icon);
                top.add(iconLbl);
            }
            top.add(lblTitle);
            add(top, BorderLayout.NORTH);
            add(lblValue, BorderLayout.CENTER);
            add(lblDelta, BorderLayout.SOUTH);
        }

        void setValue(String value) { lblValue.setText(value); }

        void setDelta(Double percent) {
            if (percent == null || percent.isNaN()) {
                lblDelta.setText("");
                return;
            }
            String arrow = percent > 0 ? "▲" : (percent < 0 ? "▼" : "→");
            lblDelta.setText(String.format("%s %.1f%%", arrow, Math.abs(percent)));
        }
    }

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            super.setValue(value);
            if (value == null) return;
            String status = value.toString();
            Color c;
            switch (status) {
                case "Mới" -> c = new Color(0x1565C0);
                case "Đang xử lý" -> c = new Color(0xF9A825);
                case "Hoàn tất" -> c = new Color(0x2E7D32);
                case "Hủy" -> c = new Color(0xC62828);
                case "Hết hàng" -> c = new Color(0xC62828);
                case "Sắp hết" -> c = new Color(0xF57C00);
                case "Hết hạn" -> c = new Color(0xB71C1C);
                case "Cận HSD" -> c = new Color(0x8E24AA);
                default -> c = getForeground();
            }
            setForeground(c);
            setFont(getFont().deriveFont(Font.BOLD));
        }
    }

    private class PercentRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            super.setValue(value);
            if (value == null) return;
            String s = value.toString();
            if (s.startsWith("+")) setForeground(new Color(0x2E7D32));
            else if (s.startsWith("-")) setForeground(new Color(0xC62828));
            else setForeground(getForeground());
            setFont(getFont().deriveFont(Font.BOLD));
        }
    }

    private class CurrencyRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            if (value instanceof Number num) {
                super.setValue(currencyFormat.format(num.doubleValue()));
            } else {
                super.setValue(value);
            }
        }
    }

    // Data abstractions
    public interface DashboardDataProvider {
        DashboardMetrics fetchMetrics();
        List<OrderSummary> recentOrders(int limit);
        List<CustomerSpend> topCustomers(int limit);
        List<ProductSales> topProducts(int limit);
        RevenueTrend revenueTrend(Granularity granularity, int periods);
        // Removed productDistribution
        List<ProductSales> slowProducts();
        Map<String, Integer> orderStatusCounts();
        List<StockItem> lowStockItems(int limit);
        List<PromotionInfo> activePromotions();
        SalesKPI salesKPI();
        List<ProductAnomaly> productAnomalies();
        List<DecisionItem> decisionFeed();
    }

    public record DashboardMetrics(int ordersToday, int ordersWeek, double revenueToday, double revenueWeek,
                                   double revenueYesterday, int newCustomers, int zeroOrderStreakDays) {}

    public record OrderSummary(String id, String customerName, LocalDateTime date, String status, double total) {}

    public record CustomerSpend(String customerId, String customerName, double totalSpend) {}

    public record ProductSales(String productId, String productName, int quantity) {}
    public record ProductAnomaly(String productName, double changePercent, boolean increased) {}
    public record DecisionItem(String title, String description, String actionLabel, String impact, DecisionSeverity severity, DecisionType type, String refId) {}
    public enum DecisionSeverity { CRITICAL, WARNING, INFO }
    public enum DecisionType { REPLENISH, PROMOTE, MARKDOWN, PRICE_ADJUST, INVESTIGATE }

    public static class RevenueTrend {
        public final List<LocalDate> dates;
        public final List<Double> revenues;
        public RevenueTrend(List<LocalDate> dates, List<Double> revenues) {
            this.dates = dates;
            this.revenues = revenues;
        }
    }

    public enum Granularity {
        DAY("Ngày"), WEEK("Tuần"), MONTH("Tháng"), QUARTER("Quý");
        public final String display;
        Granularity(String d) { this.display = d; }
        static Granularity fromDisplay(String d) {
            for (Granularity g : values()) if (g.display.equalsIgnoreCase(d)) return g;
            return DAY;
        }
    }

    // Additional records for Staff
    public record StockItem(String productId, String productName, int stock, LocalDate expiryDate) {}
    public record PromotionInfo(String id, String name, String description, double discountPercent, LocalDate endDate) {}
    public record SalesKPI(double dayCurrent, double dayTarget, double weekCurrent, double weekTarget) {}

    // Stub provider with randomized sample data
    public static class SampleDashboardDataProvider implements DashboardDataProvider {
        private final Random rnd = new Random(123);
        private final List<String> customerNames = List.of("Nguyễn An", "Trần Bình", "Lê Cường", "Phạm Dung", "Hoàng Em", "Vũ Giang");
        private final List<String> productNames = List.of("Paracetamol", "Vitamin C", "Khẩu trang", "Nước muối", "Omega 3", "Sữa rửa tay");
        private final String[] statuses = {"Mới", "Đang xử lý", "Hoàn tất", "Hủy"};

        @Override
        public DashboardMetrics fetchMetrics() {
            int ordersToday = 15 + rnd.nextInt(25);
            int ordersWeek = ordersToday * 6 + rnd.nextInt(80);
            double revToday = ordersToday * (50_000 + rnd.nextInt(150_000));
            double revYesterday = revToday * (0.7 + rnd.nextDouble() * 0.6); // +/- 30%
            double revWeek = revToday * 6.5;
            int newCustomers = rnd.nextInt(10);
            int zeroOrderStreak = 0; // rarely triggered in stub
            return new DashboardMetrics(ordersToday, ordersWeek, revToday, revWeek, revYesterday, newCustomers, zeroOrderStreak);
        }

        @Override
        public List<OrderSummary> recentOrders(int limit) {
            List<OrderSummary> list = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < limit; i++) {
                String id = "ORD" + (1000 + rnd.nextInt(9000));
                String customer = customerNames.get(rnd.nextInt(customerNames.size()));
                LocalDateTime date = now.minusMinutes(rnd.nextInt(60 * 24));
                String status = statuses[rnd.nextInt(statuses.length)];
                double total = 50_000 + rnd.nextInt(400_000);
                list.add(new OrderSummary(id, customer, date, status, total));
            }
            list.sort(Comparator.comparing(OrderSummary::date).reversed());
            return list;
        }

        @Override
        public List<CustomerSpend> topCustomers(int limit) {
            return customerNames.stream().limit(limit).map(n -> new CustomerSpend(UUID.randomUUID().toString(), n, 5_000_000 + rnd.nextInt(15_000_000))).sorted((a,b)->Double.compare(b.totalSpend(), a.totalSpend())).toList();
        }

        @Override
        public List<ProductSales> topProducts(int limit) {
            return productNames.stream().map(p -> new ProductSales(UUID.randomUUID().toString(), p, 50 + rnd.nextInt(300))).sorted((a,b)->Integer.compare(b.quantity(), a.quantity())).limit(limit).toList();
        }

        @Override
        public RevenueTrend revenueTrend(Granularity granularity, int periods) {
            List<LocalDate> dates = new ArrayList<>();
            List<Double> rev = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = periods - 1; i >= 0; i--) {
                LocalDate d;
                switch (granularity) {
                    case DAY -> d = today.minusDays(i);
                    case WEEK -> d = today.minusWeeks(i);
                    case MONTH -> d = today.minusMonths(i);
                    case QUARTER -> d = today.minusMonths(i * 3L);
                    default -> d = today.minusDays(i);
                }
                dates.add(d);
                rev.add(2_000_000d + rnd.nextInt(8_000_000)); // ensure Double
            }
            return new RevenueTrend(dates, rev);
        }

        @Override
        public List<ProductSales> slowProducts() {
            return productNames.stream().map(p -> new ProductSales(UUID.randomUUID().toString(), p, 50 + rnd.nextInt(300))).sorted((a,b)->Integer.compare(b.quantity(), a.quantity())).limit(3).toList();
        }

        @Override
        public Map<String, Integer> orderStatusCounts() {
            Map<String, Integer> map = new LinkedHashMap<>();
            map.put("Mới", 5 + rnd.nextInt(10));
            map.put("Đang xử lý", 3 + rnd.nextInt(8));
            map.put("Hoàn tất", 20 + rnd.nextInt(30));
            map.put("Hủy", rnd.nextInt(5));
            return map;
        }

        @Override
        public List<StockItem> lowStockItems(int limit) {
            List<StockItem> list = new ArrayList<>();
            for (int i = 0; i < Math.min(limit, productNames.size()); i++) {
                String name = productNames.get(i);
                int stock = rnd.nextInt(20) - 3; // can be negative to simulate out-of-stock
                LocalDate exp = LocalDate.now().plusDays(5 + rnd.nextInt(60));
                if (rnd.nextBoolean()) exp = null; // some items no expiry tracking
                list.add(new StockItem(UUID.randomUUID().toString(), name, stock, exp));
            }
            return list;
        }

        @Override
        public List<PromotionInfo> activePromotions() {
            List<PromotionInfo> list = new ArrayList<>();
            if (rnd.nextBoolean()) {
                list.add(new PromotionInfo("PR1", "Giảm giá Paracetamol", "Mua 2 tặng 1", 20, LocalDate.now().plusDays(7)));
            }
            list.add(new PromotionInfo("PR2", "Vitamin C - cuối tuần", "Giảm sốc", 15, LocalDate.now().plusDays(2)));
            return list;
        }

        @Override
        public SalesKPI salesKPI() {
            double dayTarget = 1000_000;
            double dayCurrent = 600_000 + rnd.nextInt(400_000);
            double weekTarget = 7_000_000;
            double weekCurrent = 3_500_000 + rnd.nextInt(3_000_000);
            return new SalesKPI(dayCurrent, dayTarget, weekCurrent, weekTarget);
        }

        @Override
        public List<ProductAnomaly> productAnomalies() {
            int n = rnd.nextInt(3); // 0..2 anomalies
            List<ProductAnomaly> list = new ArrayList<>();
            List<String> mutable = new ArrayList<>(productNames);
            Collections.shuffle(mutable, rnd);
            for (int i = 0; i < n && i < mutable.size(); i++) {
                boolean up = rnd.nextBoolean();
                double pct = 30 + rnd.nextInt(70); // 30%..100%
                list.add(new ProductAnomaly(mutable.get(i), pct, up));
            }
            return list;
        }

        @Override
        public List<DecisionItem> decisionFeed() {
            List<DecisionItem> list = new ArrayList<>();
            // Replenish: low stock or out of stock
            for (StockItem s : lowStockItems(5)) {
                if (s.stock() <= 0) {
                    list.add(new DecisionItem(
                            "Đặt lại hàng: " + s.productName(),
                            "Tồn kho đã hết. Đề xuất đặt bổ sung ngay.",
                            "Tạo phiếu đặt hàng",
                            "+15% doanh thu trong 7 ngày",
                            DecisionSeverity.CRITICAL, DecisionType.REPLENISH, s.productId()));
                } else if (s.stock() < 10) {
                    list.add(new DecisionItem(
                            "Sắp hết hàng: " + s.productName(),
                            "Tồn kho thấp (" + s.stock() + ") — cần đặt thêm để tránh hụt doanh số.",
                            "Thêm vào danh sách đặt",
                            "+5% doanh thu trong 7 ngày",
                            DecisionSeverity.WARNING, DecisionType.REPLENISH, s.productId()));
                }
            }
            // Markdown: near expiry
            LocalDate now = LocalDate.now();
            for (StockItem s : lowStockItems(10)) {
                if (s.expiryDate() != null && !s.expiryDate().isAfter(now.plusDays(14))) {
                    list.add(new DecisionItem(
                            "Cận hạn: " + s.productName(),
                            "HSD: " + (s.expiryDate() != null ? s.expiryDate().format(DateTimeFormatter.ofPattern("dd/MM")) : "?") + ". Đề xuất giảm giá để xả hàng.",
                            "Tạo khuyến mãi -20%",
                            "Giảm rủi ro huỷ bỏ hàng hết hạn",
                            DecisionSeverity.WARNING, DecisionType.MARKDOWN, s.productId()));
                }
            }
            // Promote: product anomalies decreasing
            for (ProductAnomaly a : productAnomalies()) {
                if (!a.increased() && a.changePercent() >= 30) {
                    list.add(new DecisionItem(
                            "Bán chậm: " + a.productName(),
                            "Doanh số giảm ~" + (int) a.changePercent() + "%. Đề xuất chạy khuyến mãi.",
                            "Tạo khuyến mãi -15%",
                            "+10% doanh thu sản phẩm",
                            DecisionSeverity.WARNING, DecisionType.PROMOTE, a.productName()));
                }
            }
            // Price adjust: over-performing could allow slight margin increase
            for (ProductAnomaly a : productAnomalies()) {
                if (a.increased() && a.changePercent() >= 50) {
                    list.add(new DecisionItem(
                            "Tăng giá nhẹ: " + a.productName(),
                            "Nhu cầu tăng ~" + (int) a.changePercent() + "%. Có thể tăng giá 3-5%.",
                            "Đề xuất +3% giá",
                            "+2% lợi nhuận biên",
                            DecisionSeverity.INFO, DecisionType.PRICE_ADJUST, a.productName()));
                }
            }
            return list;
        }
    }
    // endregion
}
