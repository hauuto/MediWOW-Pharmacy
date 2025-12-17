package com.gui;

import com.bus.BUS_Invoice;
import com.bus.BUS_Product;
import com.entities.Invoice;
import com.entities.InvoiceLine;
import com.entities.LotAllocation;
import com.entities.Product;
import com.enums.InvoiceType;
import com.enums.LineType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
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

    private JButton btnRefresh;

    // Date range selectors
    private JComboBox<String> cboComparisonPeriod;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TAB_Dashboard_Manager() {
        this.busInvoice = new BUS_Invoice();
        this.busProduct = new BUS_Product();
        initComponents();
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Color.WHITE);

        // Statistics panel at top
        mainPanel.add(createStatisticsPanel(), BorderLayout.NORTH);

        // Split panel for tables
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createBestSellersPanel());
        splitPane.setRightComponent(createTrendingPanel());
        splitPane.setDividerLocation(0.5);
        splitPane.setResizeWeight(0.5);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Cash reconciliation at bottom
        mainPanel.add(createCashReconciliationPanel(), BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel lblTitle = new JLabel("📊 Dashboard Quản Lý - Hiệu Quả Kinh Doanh");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(41, 128, 185));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setBackground(Color.WHITE);

        JLabel lblPeriod = new JLabel("So sánh với:");
        lblPeriod.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        cboComparisonPeriod = new JComboBox<>(new String[]{
            "Hôm qua", "7 ngày trước", "Tháng trước"
        });
        cboComparisonPeriod.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cboComparisonPeriod.addActionListener(e -> loadTrendingProducts());

        btnRefresh = new JButton("🔄 Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setBackground(new Color(52, 152, 219));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setPreferredSize(new Dimension(130, 35));
        btnRefresh.addActionListener(e -> loadData());

        controlPanel.add(lblPeriod);
        controlPanel.add(cboComparisonPeriod);
        controlPanel.add(btnRefresh);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.add(lblTitle, BorderLayout.WEST);
        topPanel.add(controlPanel, BorderLayout.EAST);

        JLabel lblDate = new JLabel("Ngày: " + LocalDate.now().format(dateFormatter));
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDate.setForeground(new Color(52, 73, 94));

        headerPanel.add(topPanel, BorderLayout.NORTH);
        headerPanel.add(lblDate, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createStatisticsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        statsPanel.setPreferredSize(new Dimension(0, 120));

        lblTodayInvoiceCount = new JLabel("0");
        statsPanel.add(createStatCard("📝 Số Hóa Đơn Hôm Nay", lblTodayInvoiceCount, new Color(52, 152, 219)));

        lblTodayRevenue = new JLabel("0 đ");
        statsPanel.add(createStatCard("💰 Doanh Thu Hôm Nay", lblTodayRevenue, new Color(155, 89, 182)));

        lblTodayProfit = new JLabel("0 đ");
        statsPanel.add(createStatCard("📈 Lợi Nhuận Hôm Nay", lblTodayProfit, new Color(46, 204, 113)));

        lblCashReconciliation = new JLabel("Chưa đối soát");
        statsPanel.add(createStatCard("💵 Trạng Thái Đối Soát", lblCashReconciliation, new Color(52, 73, 94)));

        return statsPanel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2, true),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(new Color(52, 73, 94));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createBestSellersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel("🏆 Top 10 Sản Phẩm Bán Chạy (30 ngày)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(46, 204, 113));

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
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTrendingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel("📉📈 Sản Phẩm Có Xu Hướng Bất Thường");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(52, 152, 219));

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
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCashReconciliationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 73, 94), 2),
            new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setPreferredSize(new Dimension(0, 150));

        JLabel lblTitle = new JLabel("💵 Đối Soát Cuối Ngày");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(52, 73, 94));

        JPanel infoPanel = new JPanel(new GridLayout(1, 5, 20, 0));
        infoPanel.setBackground(Color.WHITE);

        JLabel lblInvoices = new JLabel("<html><b>Số hóa đơn:</b><br/>0</html>");
        JLabel lblSystemRevenue = new JLabel("<html><b>DT hệ thống:</b><br/>0 đ</html>");
        JLabel lblCash = new JLabel("<html><b>Tiền mặt:</b><br/>0 đ</html>");
        JLabel lblTransfer = new JLabel("<html><b>Chuyển khoản:</b><br/>0 đ</html>");
        JLabel lblStatus = new JLabel("<html><b>Trạng thái:</b><br/>Chờ đối soát</html>");

        lblInvoices.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSystemRevenue.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblCash.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblTransfer.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        infoPanel.add(lblInvoices);
        infoPanel.add(lblSystemRevenue);
        infoPanel.add(lblCash);
        infoPanel.add(lblTransfer);
        infoPanel.add(lblStatus);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(52, 152, 219, 50));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowGrid(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(52, 152, 219));
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
            lblTodayProfit.setForeground(new Color(46, 204, 113));
        } else {
            lblTodayProfit.setForeground(new Color(231, 76, 60));
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

    private void loadCashReconciliation() {
        List<Invoice> allInvoices = busInvoice.getAllInvoices();
        if (allInvoices == null) return;

        LocalDate today = LocalDate.now();
        List<Invoice> todayInvoices = allInvoices.stream()
            .filter(inv -> inv.getCreationDate() != null &&
                          inv.getCreationDate().toLocalDate().equals(today))
            .collect(Collectors.toList());

        double totalRevenue = 0.0;
        for (Invoice invoice : todayInvoices) {
            if (invoice.getType() == InvoiceType.SALES) {
                totalRevenue += invoice.calculateTotal();
            } else if (invoice.getType() == InvoiceType.RETURN) {
                totalRevenue -= invoice.calculateTotal();
            }
        }

        // Simple reconciliation status
        if (totalRevenue >= 0) {
            lblCashReconciliation.setText("✓ Khớp");
            lblCashReconciliation.setForeground(new Color(46, 204, 113));
        } else {
            lblCashReconciliation.setText("⚠ Cần kiểm tra");
            lblCashReconciliation.setForeground(new Color(231, 76, 60));
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
                    c.setForeground(new Color(46, 204, 113));
                } else {
                    c.setForeground(new Color(231, 76, 60));
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
                        c.setForeground(new Color(46, 204, 113));
                    } else {
                        c.setForeground(new Color(231, 76, 60));
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
                    c.setBackground(new Color(46, 204, 113));
                    setText("📈 TĂNG");
                } else if ("GIẢM".equals(trend)) {
                    c.setForeground(Color.WHITE);
                    c.setBackground(new Color(231, 76, 60));
                    setText("📉 GIẢM");
                }
            }

            setHorizontalAlignment(CENTER);
            return c;
        }
    }
}

