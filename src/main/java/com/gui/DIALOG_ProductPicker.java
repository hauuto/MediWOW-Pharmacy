package com.gui;

import com.bus.BUS_Product;
import com.entities.Product;
import com.utils.AppColors;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.util.List;

public class DIALOG_ProductPicker extends JDialog {

    private JTable table;
    private Product selectedProduct = null;
    private List<Product> products;
    private List<Product> filteredProducts; // For search results
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtSearch;

    // Pagination fields
    private int currentPage = 0;
    private final int itemsPerPage = 5;
    private JLabel lblPageInfo;
    private JButton btnPrevPage;
    private JButton btnNextPage;

    public DIALOG_ProductPicker(Window owner) {
        super(owner, "Chọn sản phẩm", ModalityType.APPLICATION_MODAL);
        setSize(900, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        BUS_Product bus = new BUS_Product();
        products = bus.getAllProducts();
        filteredProducts = products; // Initially show all products

        // Top panel with search
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtSearch = new JTextField(30);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên, mã, hoạt chất hoặc hãng sản xuất...");

        JButton btnClearSearch = new JButton("Xóa");
        styleButton(btnClearSearch, new Color(108, 117, 125));

        topPanel.add(lblSearch);
        topPanel.add(txtSearch);
        topPanel.add(btnClearSearch);

        // Table with images
        String[] cols = {"Hình ảnh", "Mã SP", "Tên sản phẩm", "Hoạt chất", "Hãng SX"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? ImageIcon.class : String.class;
            }
        };

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(60);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(AppColors.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(200, 230, 255));
        table.setSelectionForeground(Color.BLACK);

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(80);  // Image
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // Mã
        table.getColumnModel().getColumn(2).setPreferredWidth(250); // Tên
        table.getColumnModel().getColumn(3).setPreferredWidth(200); // Hoạt chất
        table.getColumnModel().getColumn(4).setPreferredWidth(150); // Hãng

        // Center align image column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        // Initialize pagination components BEFORE calling loadPage
        btnPrevPage = new JButton("◄ Trang trước");
        btnNextPage = new JButton("Trang sau ►");
        lblPageInfo = new JLabel();
        lblPageInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // NOW load the page data
        loadPage();

        // Search functionality
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { search(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { search(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { search(); }
        });

        btnClearSearch.addActionListener(e -> {
            txtSearch.setText("");
            filteredProducts = products;
            currentPage = 0;
            loadPage();
        });

        // Double click to select
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectProduct();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        // Pagination panel
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        paginationPanel.setBackground(Color.WHITE);
        paginationPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        styleButton(btnPrevPage, new Color(108, 117, 125));
        btnPrevPage.setPreferredSize(new Dimension(120, 30));

        styleButton(btnNextPage, new Color(108, 117, 125));
        btnNextPage.setPreferredSize(new Dimension(120, 30));

        btnPrevPage.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadPage();
            }
        });

        btnNextPage.addActionListener(e -> {
            int totalPages = (int) Math.ceil((double) filteredProducts.size() / itemsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                loadPage();
            }
        });

        paginationPanel.add(btnPrevPage);
        paginationPanel.add(lblPageInfo);
        paginationPanel.add(btnNextPage);

        // Bottom panel with buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JButton btnSelect = new JButton("Chọn sản phẩm");
        styleButton(btnSelect, new Color(40, 167, 69));

        JButton btnCancel = new JButton("Hủy");
        styleButton(btnCancel, new Color(220, 53, 69));

        btnSelect.addActionListener(e -> selectProduct());
        btnCancel.addActionListener(e -> dispose());

        bottomPanel.add(btnSelect);
        bottomPanel.add(btnCancel);

        // Center panel with table and pagination
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(paginationPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Set white background
        getContentPane().setBackground(Color.WHITE);
    }

    private void loadPage() {
        model.setRowCount(0);
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredProducts.size());

        for (int i = start; i < end; i++) {
            Product p = filteredProducts.get(i);
            ImageIcon icon = loadProductImage(p.getId());
            model.addRow(new Object[]{
                icon,
                p.getId(),
                p.getName(),
                p.getActiveIngredient() != null ? p.getActiveIngredient() : "",
                p.getManufacturer() != null ? p.getManufacturer() : ""
            });
        }

        updatePageInfo();
    }

    private void updatePageInfo() {
        int totalPages = (int) Math.ceil((double) filteredProducts.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        lblPageInfo.setText("Trang " + (currentPage + 1) + " / " + totalPages + " (Tổng: " + filteredProducts.size() + " sản phẩm)");

        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
    }

    private ImageIcon loadProductImage(String productId) {
        try {
            // Try to load image from resources/images/products/
            String imagePath = "src/main/resources/images/products/" + productId + ".png";
            File imageFile = new File(imagePath);

            if (imageFile.exists()) {
                ImageIcon originalIcon = new ImageIcon(imagePath);
                Image scaledImage = originalIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            } else {
                // Use placeholder image
                return createPlaceholderImage();
            }
        } catch (Exception e) {
            return createPlaceholderImage();
        }
    }

    private ImageIcon createPlaceholderImage() {
        // Create a simple placeholder image
        int size = 50;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw rounded rectangle background
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRoundRect(0, 0, size, size, 8, 8);

        // Draw border
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawRoundRect(0, 0, size - 1, size - 1, 8, 8);

        // Draw icon placeholder (medicine icon)
        g2d.setColor(new Color(150, 150, 150));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 20));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "💊";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(text, x, y);

        g2d.dispose();
        return new ImageIcon(img);
    }

    private void search() {
        String text = txtSearch.getText().trim().toLowerCase();
        if (text.isEmpty()) {
            filteredProducts = products;
        } else {
            filteredProducts = new java.util.ArrayList<>();
            for (Product p : products) {
                if ((p.getId() != null && p.getId().toLowerCase().contains(text)) ||
                    (p.getName() != null && p.getName().toLowerCase().contains(text)) ||
                    (p.getActiveIngredient() != null && p.getActiveIngredient().toLowerCase().contains(text)) ||
                    (p.getManufacturer() != null && p.getManufacturer().toLowerCase().contains(text))) {
                    filteredProducts.add(p);
                }
            }
        }
        currentPage = 0;
        loadPage();
    }

    private void selectProduct() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            int actualIndex = currentPage * itemsPerPage + row;
            if (actualIndex < filteredProducts.size()) {
                selectedProduct = filteredProducts.get(actualIndex);
                dispose();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn một sản phẩm!",
                "Thông báo",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 35));
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }
}
