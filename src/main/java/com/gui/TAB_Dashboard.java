package com.gui;

import org.knowm.xchart.*;
import org.knowm.xchart.internal.chartpart.Chart;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.CategoryStyler;

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
    private MetricCard cardNewCustomers;
    private MetricCard cardTopProducts;

    // Alerts
    private JPanel pAlerts;
    private JLabel lblAlertMessage;

    // Charts
    private JPanel pChartsContainer;
    private JComboBox<String> cmbGranularity; // Day, Week, Month
    private XChartPanel<?> revenueChartPanel; // changed generic to wildcard
    private XChartPanel<PieChart> productDistributionChartPanel;
    private JSplitPane splitCharts; // new field to swap chart panels

    // Tables
    private JTable tblRecentOrders;
    private JTable tblTopCustomers;
    private JComboBox<String> cmbOrderStatusFilter;
    private DefaultTableModel recentOrdersModel;
    private DefaultTableModel topCustomersModel;
    private TableRowSorter<DefaultTableModel> orderSorter;

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

    public TAB_Dashboard() {
        this(new SampleDashboardDataProvider());
    }

    public TAB_Dashboard(DashboardDataProvider provider) {
        this.dataProvider = provider;
        initUI();
        loadAllData();
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

        // CENTER: Charts + Tables split
        JSplitPane splitMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitMain.setResizeWeight(0.45);
        splitMain.setTopComponent(buildChartsPanel());
        splitMain.setBottomComponent(buildTablesPanel());
        pDashboard.add(splitMain, BorderLayout.CENTER);
    }

    // region Panels builders
    private JComponent buildMetricCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 0));
        cardOrders = new MetricCard("Đơn hàng", "0 hôm nay / 0 tuần", UIManager.getIcon("OptionPane.informationIcon"), new Color(0x1976D2));
        cardRevenue = new MetricCard("Doanh thu", currencyFormat.format(0) + " hôm nay", UIManager.getIcon("OptionPane.questionIcon"), new Color(0x2E7D32));
        cardNewCustomers = new MetricCard("Khách hàng mới", "0", UIManager.getIcon("OptionPane.warningIcon"), new Color(0xF9A825));
        cardTopProducts = new MetricCard("Top 3 sản phẩm", "--", UIManager.getIcon("OptionPane.errorIcon"), new Color(0x6A1B9A));
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

    private JComponent buildChartsPanel() {
        pChartsContainer = new JPanel(new BorderLayout(8, 8));
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.add(new JLabel("Doanh thu theo:"));
        cmbGranularity = new JComboBox<>(new String[]{"Ngày", "Tuần", "Tháng"});
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

        // Product distribution pie
        productDistributionChartPanel = new XChartPanel<>(createEmptyPieChart("Phân bố sản phẩm"));
        splitCharts = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, revenueChartPanel, productDistributionChartPanel);
        splitCharts.setResizeWeight(0.65);
        pChartsContainer.add(splitCharts, BorderLayout.CENTER);
        return pChartsContainer;
    }

    private JComponent buildTablesPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Đơn hàng gần nhất", buildRecentOrdersPanel());
        tabs.addTab("Khách hàng nổi bật", buildTopCustomersPanel());
        return tabs;
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
    // endregion

    // region Data load and updates
    private void loadAllData() {
        loadMetrics();
        updateRevenueTrend();
        loadRecentOrders();
        loadTopCustomers();
        loadProductDistribution();
        evaluateAlerts();
    }

    private void loadMetrics() {
        DashboardMetrics m = dataProvider.fetchMetrics();
        cardOrders.setValue(m.ordersToday + " hôm nay / " + m.ordersWeek + " tuần");
        cardRevenue.setValue(currencyFormat.format(m.revenueToday) + " hôm nay / " + currencyFormat.format(m.revenueWeek) + " tuần");
        cardRevenue.setDelta(calcDeltaPercent(m.revenueToday, m.revenueYesterday));
        cardNewCustomers.setValue(String.valueOf(m.newCustomers));
        List<ProductSales> top3 = dataProvider.topProducts(3);
        if (top3.isEmpty()) {
            cardTopProducts.setValue("Không có dữ liệu");
        } else {
            String txt = top3.stream().map(p -> p.productName + "(" + p.quantity + ")").collect(Collectors.joining(", "));
            cardTopProducts.setValue(txt);
        }
    }

    private void updateRevenueTrend() {
        currentTrend = dataProvider.revenueTrend(currentGranularity, 14); // last 14 periods
        Chart<?, ?> newChart = buildRevenueChart(currentTrend);
        XChartPanel<?> newPanel = new XChartPanel<>(newChart);
        revenueChartPanel = newPanel;
        if (splitCharts != null) {
            splitCharts.setLeftComponent(revenueChartPanel);
            splitCharts.setResizeWeight(0.65);
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

    private void loadProductDistribution() {
        Map<String, Double> dist = dataProvider.productDistribution();
        PieChart pie = createEmptyPieChart("Phân bố sản phẩm");
        if (dist.isEmpty()) {
            pie.addSeries("Không có dữ liệu", 1);
        } else {
            dist.forEach(pie::addSeries);
        }
        XChartPanel<PieChart> newPiePanel = new XChartPanel<>(pie);
        productDistributionChartPanel = newPiePanel;
        if (splitCharts != null) {
            splitCharts.setRightComponent(productDistributionChartPanel);
            splitCharts.setResizeWeight(0.65);
        }
        pChartsContainer.revalidate();
        pChartsContainer.repaint();
    }

    private void evaluateAlerts() {
        DashboardMetrics m = dataProvider.fetchMetrics();
        List<String> alerts = new ArrayList<>();
        double revenueDelta = calcDeltaPercent(m.revenueToday, m.revenueYesterday);
        if (!Double.isNaN(revenueDelta) && revenueDelta < -20) {
            alerts.add(String.format("Doanh thu hôm nay giảm %.1f%% so với hôm qua", Math.abs(revenueDelta)));
        }
        if (m.ordersToday == 0 && m.zeroOrderStreakDays >= 3) {
            alerts.add("Không có đơn hàng trong " + m.zeroOrderStreakDays + " ngày!");
        }
        if (!dataProvider.slowProducts().isEmpty()) {
            alerts.add("Có sản phẩm bán chậm cần khuyến mãi");
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
        boolean lineChart = currentGranularity != Granularity.DAY; // Use line for week/month
        if (lineChart) {
            XYChart chart = new XYChartBuilder().title("Doanh thu - " + currentGranularity.display).width(600).height(400).build();
            styleRevenueChart(chart);
            List<String> xLabels = trend.dates.stream().map(d -> d.format(dateFmt)).toList();
            chart.addSeries("Doanh thu", xLabels, trend.revenues);
            return chart;
        } else {
            CategoryChart chart = new CategoryChartBuilder().title("Doanh thu - " + currentGranularity.display).width(600).height(400).build();
            styleRevenueChart(chart);
            List<String> xLabels = trend.dates.stream().map(d -> d.format(dateFmt)).toList();
            chart.addSeries("Doanh thu", xLabels, trend.revenues);
            return chart;
        }
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

    private void showCustomerHistoryDialog(String customerName) {
        // In real implementation, fetch history. Here stub summary.
        JOptionPane.showMessageDialog(pDashboard, "Lịch sử mua hàng của " + customerName + " (stub)", "Lịch sử", JOptionPane.INFORMATION_MESSAGE);
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
                default -> c = getForeground();
            }
            setForeground(c);
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
        Map<String, Double> productDistribution();
        List<ProductSales> slowProducts();
    }

    public record DashboardMetrics(int ordersToday, int ordersWeek, double revenueToday, double revenueWeek,
                                   double revenueYesterday, int newCustomers, int zeroOrderStreakDays) {}

    public record OrderSummary(String id, String customerName, LocalDateTime date, String status, double total) {}

    public record CustomerSpend(String customerId, String customerName, double totalSpend) {}

    public record ProductSales(String productId, String productName, int quantity) {}

    public static class RevenueTrend {
        public final List<LocalDate> dates;
        public final List<Double> revenues;
        public RevenueTrend(List<LocalDate> dates, List<Double> revenues) {
            this.dates = dates;
            this.revenues = revenues;
        }
    }

    public enum Granularity {
        DAY("Ngày"), WEEK("Tuần"), MONTH("Tháng");
        public final String display;
        Granularity(String d) { this.display = d; }
        static Granularity fromDisplay(String d) {
            for (Granularity g : values()) if (g.display.equalsIgnoreCase(d)) return g;
            return DAY;
        }
    }

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
                    default -> d = today.minusDays(i);
                }
                dates.add(d);
                rev.add(2_000_000d + rnd.nextInt(8_000_000)); // ensure Double
            }
            return new RevenueTrend(dates, rev);
        }

        @Override
        public Map<String, Double> productDistribution() {
            Map<String, Double> map = new LinkedHashMap<>();
            for (String p : productNames) {
                map.put(p, 100d + rnd.nextInt(900)); // ensure Double
            }
            return map;
        }

        @Override
        public List<ProductSales> slowProducts() {
            // Simulate 1 slow product occasionally
            if (rnd.nextBoolean()) return List.of(new ProductSales("SLOW", "Sản phẩm chậm", 5));
            return Collections.emptyList();
        }
    }
    // endregion
}
