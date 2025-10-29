package com.gui;

import com.bus.BUS_Invoice;
import com.entities.Invoice;
import com.enums.InvoiceType;
import com.enums.PaymentMethod;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Tô Thanh Hậu
 */
public class TAB_Dashboard extends JPanel {
    private final BUS_Invoice busInvoice;
    private JTable tblInvoices;
    private DefaultTableModel tableModel;
    private JLabel lblTotalInvoices;
    private JLabel lblTotalRevenue;
    private JLabel lblSalesInvoices;
    private JLabel lblReturnInvoices;
    private JButton btnRefresh;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TAB_Dashboard() {
        this.busInvoice = new BUS_Invoice();
        initComponents();
        loadTodayInvoices();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        // Header Panel
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Statistics Panel
        add(createStatisticsPanel(), BorderLayout.CENTER);

        // Table Panel
        add(createTablePanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Title
        JLabel lblTitle = new JLabel("Hóa Đơn Trong Ngày");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(41, 128, 185));

        // Current Date
        JLabel lblDate = new JLabel("Ngày: " + LocalDate.now().format(dateFormatter));
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDate.setForeground(new Color(52, 73, 94));

        // Refresh Button
        btnRefresh = new JButton("🔄 Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setBackground(new Color(52, 152, 219));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setPreferredSize(new Dimension(150, 40));
        btnRefresh.addActionListener(e -> loadTodayInvoices());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.add(lblTitle, BorderLayout.WEST);
        topPanel.add(btnRefresh, BorderLayout.EAST);

        headerPanel.add(topPanel, BorderLayout.NORTH);
        headerPanel.add(lblDate, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createStatisticsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        statsPanel.setPreferredSize(new Dimension(0, 120));

        // Card 1: Tổng hóa đơn
        lblTotalInvoices = new JLabel("0");
        statsPanel.add(createStatCard("📄 Tổng Hóa Đơn", lblTotalInvoices, new Color(52, 152, 219)));

        // Card 2: Hóa đơn bán
        lblSalesInvoices = new JLabel("0");
        statsPanel.add(createStatCard("🛒 Hóa Đơn Bán", lblSalesInvoices, new Color(46, 204, 113)));

        // Card 3: Hóa đơn trả
        lblReturnInvoices = new JLabel("0");
        statsPanel.add(createStatCard("↩️ Hóa Đơn Trả", lblReturnInvoices, new Color(231, 76, 60)));

        // Card 4: Tổng doanh thu
        lblTotalRevenue = new JLabel("0 đ");
        statsPanel.add(createStatCard("💰 Tổng Doanh Thu", lblTotalRevenue, new Color(155, 89, 182)));

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
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(52, 73, 94));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JScrollPane createTablePanel() {
        // Table Model
        String[] columns = {"Mã HĐ", "Loại", "Thời gian", "Người tạo", "Phương thức TT", "Tổng tiền", "Ghi chú"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Table
        tblInvoices = new JTable(tableModel);
        tblInvoices.setRowHeight(35);
        tblInvoices.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblInvoices.setSelectionBackground(new Color(52, 152, 219, 50));
        tblInvoices.setSelectionForeground(Color.BLACK);
        tblInvoices.setGridColor(new Color(230, 230, 230));
        tblInvoices.setShowGrid(true);

        // Table Header
        JTableHeader header = tblInvoices.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(52, 152, 219));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 40));

        // Column widths
        tblInvoices.getColumnModel().getColumn(0).setPreferredWidth(150); // Mã HĐ
        tblInvoices.getColumnModel().getColumn(1).setPreferredWidth(100); // Loại
        tblInvoices.getColumnModel().getColumn(2).setPreferredWidth(100); // Thời gian
        tblInvoices.getColumnModel().getColumn(3).setPreferredWidth(150); // Người tạo
        tblInvoices.getColumnModel().getColumn(4).setPreferredWidth(120); // PT thanh toán
        tblInvoices.getColumnModel().getColumn(5).setPreferredWidth(120); // Tổng tiền
        tblInvoices.getColumnModel().getColumn(6).setPreferredWidth(200); // Ghi chú

        // Center align for specific columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblInvoices.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tblInvoices.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tblInvoices.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Right align for money column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblInvoices.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);

        // Scroll Pane
        JScrollPane scrollPane = new JScrollPane(tblInvoices);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.setPreferredSize(new Dimension(0, 400));

        return scrollPane;
    }

    private void loadTodayInvoices() {
        try {
            // Get all invoices
            List<Invoice> allInvoices = busInvoice.getAllInvoices();

            if (allInvoices == null) {
                allInvoices = List.of();
            }

            // Filter today's invoices
            LocalDate today = LocalDate.now();
            List<Invoice> todayInvoices = allInvoices.stream()
                .filter(invoice -> invoice.getCreationDate() != null &&
                                 invoice.getCreationDate().toLocalDate().equals(today))
                .collect(Collectors.toList());

            // Clear table
            tableModel.setRowCount(0);

            // Calculate statistics
            int totalInvoices = todayInvoices.size();
            int salesInvoices = 0;
            int returnInvoices = 0;
            double totalRevenue = 0.0;

            // Add data to table
            for (Invoice invoice : todayInvoices) {
                String invoiceId = invoice.getId();
                String type = getInvoiceTypeText(invoice.getType());
                String time = invoice.getCreationDate().format(timeFormatter);
                String creator = invoice.getCreator() != null ? invoice.getCreator().getFullName() : "N/A";
                String paymentMethod = getPaymentMethodText(invoice.getPaymentMethod());
                double total = invoice.calculateTotal(); // Fixed: use calculateTotal() instead of calculateTotalPrice()
                String notes = invoice.getNotes() != null ? invoice.getNotes() : "";

                tableModel.addRow(new Object[]{
                    invoiceId,
                    type,
                    time,
                    creator,
                    paymentMethod,
                    String.format("%,.0f đ", total),
                    notes
                });

                // Update statistics
                if (invoice.getType() == InvoiceType.SALES) {
                    salesInvoices++;
                    totalRevenue += total;
                } else if (invoice.getType() == InvoiceType.RETURN) {
                    returnInvoices++;
                    totalRevenue -= total; // Subtract return amount
                }
            }

            // Update statistics labels
            lblTotalInvoices.setText(String.valueOf(totalInvoices));
            lblSalesInvoices.setText(String.valueOf(salesInvoices));
            lblReturnInvoices.setText(String.valueOf(returnInvoices));
            lblTotalRevenue.setText(String.format("%,.0f đ", totalRevenue));

            // Change color based on revenue
            if (totalRevenue >= 0) {
                lblTotalRevenue.setForeground(new Color(46, 204, 113));
            } else {
                lblTotalRevenue.setForeground(new Color(231, 76, 60));
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Lỗi khi tải danh sách hóa đơn: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getInvoiceTypeText(InvoiceType type) {
        if (type == null) return "N/A";
        return switch (type) {
            case SALES -> "Bán hàng";
            case RETURN -> "Trả hàng";
            case EXCHANGE -> "Đổi hàng";
            default -> type.toString();
        };
    }

    private String getPaymentMethodText(PaymentMethod method) {
        if (method == null) return "N/A";
        return switch (method) {
            case CASH -> "Tiền mặt";
            case BANK_TRANSFER -> "Chuyển khoản";
            default -> method.toString();
        };
    }

    /**
     * Public method to refresh dashboard data
     */
    public void refresh() {
        loadTodayInvoices();
    }
}
