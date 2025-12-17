package com.gui;

import com.bus.BUS_Product;
import com.bus.BUS_Promotion;
import com.entities.Lot;
import com.entities.Product;
import com.entities.Promotion;
import com.enums.LotStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
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

    private JButton btnRefresh;

    // Constants
    private static final int LOW_STOCK_THRESHOLD = 100; // Định mức tồn kho thấp
    private static final int EXPIRY_WARNING_DAYS = 90; // Cảnh báo thuốc còn 90 ngày hết hạn
    private static final int EXPIRY_DANGER_DAYS = 30; // Cảnh báo nguy hiểm còn 30 ngày

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TAB_Dashboard_Pharmacist() {
        this.busProduct = new BUS_Product();
        this.busPromotion = new BUS_Promotion();
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
        JPanel mainPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        mainPanel.setBackground(Color.WHITE);

        mainPanel.add(createLowStockPanel());
        mainPanel.add(createExpiringSoonPanel());
        mainPanel.add(createPromotionPanel());

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel lblTitle = new JLabel("🏥 Dashboard Dược Sĩ - Vận Hành");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(41, 128, 185));

        JLabel lblDate = new JLabel("Ngày: " + LocalDate.now().format(dateFormatter));
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDate.setForeground(new Color(52, 73, 94));

        btnRefresh = new JButton("🔄 Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setBackground(new Color(52, 152, 219));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setPreferredSize(new Dimension(150, 40));
        btnRefresh.addActionListener(e -> loadData());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.add(lblTitle, BorderLayout.WEST);
        topPanel.add(btnRefresh, BorderLayout.EAST);

        headerPanel.add(topPanel, BorderLayout.NORTH);
        headerPanel.add(lblDate, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createLowStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
            new EmptyBorder(15, 15, 15, 15)
        ));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("⚠️ CẢNH BÁO: Thuốc Sắp Hết Hàng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(231, 76, 60));

        lblLowStockCount = new JLabel("0 sản phẩm");
        lblLowStockCount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLowStockCount.setForeground(new Color(231, 76, 60));

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
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createExpiringSoonPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 126, 34), 2),
            new EmptyBorder(15, 15, 15, 15)
        ));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("⏰ CẢNH BÁO: Thuốc Sắp Hết Hạn");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(230, 126, 34));

        lblExpiringCount = new JLabel("0 lô hàng");
        lblExpiringCount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblExpiringCount.setForeground(new Color(230, 126, 34));

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(lblExpiringCount, BorderLayout.EAST);

        // Table
        String[] columns = {"Mã lô", "Tên sản phẩm", "Số lượng", "Hạn sử dụng", "Còn lại", "Mức độ"};
        expiringSoonModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblExpiringSoon = createStyledTable(expiringSoonModel);
        tblExpiringSoon.getColumnModel().getColumn(4).setCellRenderer(new ExpiryDaysCellRenderer());
        tblExpiringSoon.getColumnModel().getColumn(5).setCellRenderer(new ExpiryLevelCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblExpiringSoon);
        scrollPane.setPreferredSize(new Dimension(0, 180));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPromotionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
            new EmptyBorder(15, 15, 15, 15)
        ));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("🎁 Khuyến Mãi Đang Áp Dụng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(46, 204, 113));

        lblActivePromotionCount = new JLabel("0 chương trình");
        lblActivePromotionCount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblActivePromotionCount.setForeground(new Color(46, 204, 113));

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(lblActivePromotionCount, BorderLayout.EAST);

        // Table
        String[] columns = {"Mã KM", "Tên khuyến mãi", "Ngày bắt đầu", "Ngày kết thúc", "Mô tả"};
        promotionModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblActivePromotions = createStyledTable(promotionModel);

        JScrollPane scrollPane = new JScrollPane(tblActivePromotions);
        scrollPane.setPreferredSize(new Dimension(0, 180));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

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

    private void loadData() {
        try {
            loadLowStockProducts();
            loadExpiringSoonLots();
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
                level
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
            promotionModel.addRow(new Object[]{
                promotion.getId(),
                promotion.getName(),
                promotion.getEffectiveDate() != null ? promotion.getEffectiveDate().format(dateFormatter) : "N/A",
                promotion.getEndDate() != null ? promotion.getEndDate().format(dateFormatter) : "N/A",
                promotion.getDescription() != null ? promotion.getDescription() : ""
            });
        }

        lblActivePromotionCount.setText(activePromotions.size() + " chương trình");
    }

    /**
     * Public method to refresh dashboard data
     */
    public void refresh() {
        loadData();
    }

    // Custom Cell Renderers
    private static class LowStockCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof Integer) {
                int stock = (Integer) value;
                if (stock == 0) {
                    c.setForeground(new Color(231, 76, 60));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (stock <= 50) {
                    c.setForeground(new Color(230, 126, 34));
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
                        c.setForeground(new Color(231, 76, 60));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (days <= 60) {
                        c.setForeground(new Color(230, 126, 34));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setForeground(new Color(241, 196, 15));
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
                        c.setBackground(new Color(231, 76, 60));
                        break;
                    case "CAO":
                        c.setForeground(Color.WHITE);
                        c.setBackground(new Color(230, 126, 34));
                        break;
                    case "TRUNG BÌNH":
                        c.setForeground(Color.BLACK);
                        c.setBackground(new Color(241, 196, 15));
                        break;
                }
            }

            setHorizontalAlignment(CENTER);
            return c;
        }
    }
}

