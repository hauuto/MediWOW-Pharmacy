package com.gui;

import com.bus.BUS_Customer;
import com.entities.Customer;
import com.utils.AppColors;
import com.utils.ExcelExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TAB_Customer extends JFrame implements ActionListener {
    public JPanel pCustomer;

    private JTextField txtSearch;
    private JTextField txtCustomerId;
    private JTextField txtCustomerName;
    private JTextField txtPhoneNumber;
    private JTextArea txtAddress;
    private JTable tblCustomer;
    private DefaultTableModel tableModel;

    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnRefresh;
    private JButton btnExport;
    private JButton btnClear;
    private JLabel lblRecordCount;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int LEFT_PANEL_MINIMAL_WIDTH = 800;
    private static final int RIGHT_PANEL_MINIMAL_WIDTH = 450;
    private static final int MAX_ADDRESS_LENGTH = 255;

    private final BUS_Customer busCustomer = new BUS_Customer();
    private List<Customer> customerCache = new ArrayList<>();

    public TAB_Customer() {
        initComponents();

        btnAdd.addActionListener(this);
        btnUpdate.addActionListener(this);
        btnClear.addActionListener(this);
        btnRefresh.addActionListener(this);
        btnExport.addActionListener(this);

        loadCustomerTable();
        setupSearchListener();
        setupTableListeners();
    }

    private void setupTableListeners() {
        // Double click to select row
        tblCustomer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tblCustomer.getSelectedRow();
                    if (row >= 0) {
                        fillFormRow(row);
                    }
                }
            }
        });
    }

    private void setupSearchListener() {
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performSearch();
            }
        });
    }

    private void performSearch() {
        String searchText = txtSearch.getText().trim().toLowerCase();
        List<Customer> filteredList = new ArrayList<>();

        for (Customer customer : customerCache) {
            boolean matchesSearch = searchText.isEmpty() ||
                    customer.getId().toLowerCase().contains(searchText) ||
                    customer.getName().toLowerCase().contains(searchText) ||
                    (customer.getPhoneNumber() != null && customer.getPhoneNumber().contains(searchText)) ||
                    (customer.getAddress() != null && customer.getAddress().toLowerCase().contains(searchText));

            if (matchesSearch) {
                filteredList.add(customer);
            }
        }

        populateTable(filteredList);
    }

    private void initComponents() {
        pCustomer = new JPanel();
        pCustomer.setLayout(new BorderLayout(10, 10));
        pCustomer.setBackground(AppColors.WHITE);
        pCustomer.setBorder(new EmptyBorder(15, 15, 15, 15));

        // North Panel - Search and Filter
        pCustomer.add(createSearchPanel(), BorderLayout.NORTH);

        // Center Panel - Split between Table and Form
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createTablePanel());
        splitPane.setRightComponent(createFormPanel());
        splitPane.setBorder(null);
        pCustomer.add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Title
        JLabel lblTitle = new JLabel("QUẢN LÝ KHÁCH HÀNG");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(AppColors.PRIMARY);

        // Search bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchBar.setBackground(AppColors.WHITE);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        txtSearch = new JTextField(30);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                new EmptyBorder(5, 10, 5, 10)
        ));
        txtSearch.setToolTipText("Tìm kiếm theo mã, tên, số điện thoại hoặc địa chỉ");



        btnRefresh = createStyledButton("Làm mới", AppColors.SECONDARY);
        btnRefresh.setToolTipText("Tải lại danh sách khách hàng");

        btnExport = createStyledButton("Xuất Excel", AppColors.DARK);
        btnExport.setToolTipText("Xuất dữ liệu ra file Excel");

        // Record count label
        lblRecordCount = new JLabel("Tổng: 0 khách hàng");
        lblRecordCount.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblRecordCount.setForeground(AppColors.DARK);

        searchBar.add(lblSearch);
        searchBar.add(txtSearch);
        searchBar.add(Box.createHorizontalStrut(20));
        searchBar.add(btnRefresh);
        searchBar.add(btnExport);
        searchBar.add(Box.createHorizontalStrut(20));
        searchBar.add(lblRecordCount);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(searchBar, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setMinimumSize(new Dimension(LEFT_PANEL_MINIMAL_WIDTH, 0));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                        "Danh sách khách hàng",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14),
                        AppColors.DARK
                ),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Table
        String[] columns = {"Mã KH", "Tên khách hàng", "Số điện thoại", "Địa chỉ", "Ngày tạo"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblCustomer = new JTable(tableModel);
        tblCustomer.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblCustomer.setRowHeight(35);
        tblCustomer.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCustomer.setShowGrid(true);
        tblCustomer.setCellEditor(null);
        tblCustomer.setGridColor(AppColors.LIGHT);
        tblCustomer.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblCustomer.getSelectedRow();
                if (row >= 0) {
                    fillFormRow(row);
                }
            }
        });

        // Header styling
        JTableHeader header = tblCustomer.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(AppColors.PRIMARY);
        header.setForeground(AppColors.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));

        // Cell renderer for center alignment
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tblCustomer.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblCustomer.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tblCustomer.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Set column widths
        tblCustomer.getColumnModel().getColumn(0).setPreferredWidth(100);
        tblCustomer.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblCustomer.getColumnModel().getColumn(2).setPreferredWidth(120);
        tblCustomer.getColumnModel().getColumn(3).setPreferredWidth(250);
        tblCustomer.getColumnModel().getColumn(4).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(tblCustomer);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppColors.LIGHT, 1));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(AppColors.WHITE);

        btnAdd = createStyledButton("Thêm mới", AppColors.SUCCESS);
        btnAdd.setToolTipText("Thêm khách hàng mới vào hệ thống");

        btnUpdate = createStyledButton("Cập nhật", AppColors.WARNING);
        btnUpdate.setToolTipText("Cập nhật thông tin khách hàng đã chọn");

        btnClear = createStyledButton("Xóa trắng", AppColors.DARK);
        btnClear.setToolTipText("Xóa các trường nhập liệu");

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnClear);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setMinimumSize(new Dimension(RIGHT_PANEL_MINIMAL_WIDTH, 0));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                        "Thông tin khách hàng",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14),
                        AppColors.DARK
                ),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel formContent = new JPanel(new GridBagLayout());
        formContent.setBackground(AppColors.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Mã khách hàng
        gbc.gridx = 0;
        gbc.gridy = 0;
        formContent.add(createLabel("Mã khách hàng:"), gbc);
        gbc.gridx = 1;
        txtCustomerId = createTextField();
        txtCustomerId.setEditable(false);
        txtCustomerId.setBackground(AppColors.BACKGROUND);
        formContent.add(txtCustomerId, gbc);

        // Tên khách hàng
        gbc.gridx = 0;
        gbc.gridy = 1;
        formContent.add(createLabel("Tên khách hàng: *"), gbc);
        gbc.gridx = 1;
        txtCustomerName = createTextField();
        txtCustomerName.setToolTipText("Nhập tên đầy đủ của khách hàng");
        formContent.add(txtCustomerName, gbc);

        // Số điện thoại
        gbc.gridx = 0;
        gbc.gridy = 2;
        formContent.add(createLabel("Số điện thoại:"), gbc);
        gbc.gridx = 1;
        txtPhoneNumber = createTextField();
        txtPhoneNumber.setToolTipText("Nhập số điện thoại 10 số (VD: 0912345678)");
        formContent.add(txtPhoneNumber, gbc);

        // Địa chỉ
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.NORTH;
        formContent.add(createLabel("Địa chỉ:"), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        txtAddress = new JTextArea(4, 20);
        txtAddress.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        txtAddress.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                new EmptyBorder(5, 10, 5, 10)
        ));
        txtAddress.setToolTipText("Nhập địa chỉ khách hàng (tối đa 255 ký tự)");

        // Add document listener to limit address length
        txtAddress.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (txtAddress.getText().length() >= MAX_ADDRESS_LENGTH) {
                    e.consume();
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });

        JScrollPane scrollAddress = new JScrollPane(txtAddress);
        scrollAddress.setPreferredSize(new Dimension(250, 100));
        formContent.add(scrollAddress, gbc);

        // Note
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JLabel lblNote = new JLabel("* Trường bắt buộc");
        lblNote.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblNote.setForeground(Color.RED);
        formContent.add(lblNote, gbc);

        panel.add(formContent, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(AppColors.DARK);
        return label;
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField(20);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                new EmptyBorder(5, 10, 5, 10)
        ));
        return textField;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(AppColors.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void loadCustomerTable() {
        try {
            List<Customer> customers = busCustomer.getAllCustomers();
            if (customers != null) {
                customerCache = customers;
                populateTable(customers);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi tải danh sách khách hàng: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateTable(List<Customer> customers) {
        tableModel.setRowCount(0);
        if (customers != null) {
            for (Customer customer : customers) {
                Object[] row = {
                        customer.getId(),
                        customer.getName(),
                        customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "",
                        customer.getAddress() != null ? customer.getAddress() : "",
                        customer.getCreationDate() != null ? customer.getCreationDate().format(dtf) : ""
                };
                tableModel.addRow(row);
            }
            updateRecordCount(customers.size());
        } else {
            updateRecordCount(0);
        }
    }

    private void updateRecordCount(int count) {
        if (lblRecordCount != null) {
            lblRecordCount.setText("Tổng: " + count + " khách hàng");
        }
    }

    private void fillFormRow(int row) {
        txtCustomerId.setText(tableModel.getValueAt(row, 0).toString());
        txtCustomerName.setText(tableModel.getValueAt(row, 1).toString());
        txtPhoneNumber.setText(tableModel.getValueAt(row, 2).toString());
        txtAddress.setText(tableModel.getValueAt(row, 3).toString());
    }

    private void clearForm() {
        txtCustomerId.setText("");
        txtCustomerName.setText("");
        txtPhoneNumber.setText("");
        txtAddress.setText("");
        tblCustomer.clearSelection();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            handleAdd();
        } else if (e.getSource() == btnUpdate) {
            handleUpdate();
        } else if (e.getSource() == btnClear) {
            clearForm();
        } else if (e.getSource() == btnRefresh) {
            loadCustomerTable();
            clearForm();
            txtSearch.setText("");
            performSearch();
        } else if (e.getSource() == btnExport) {
            handleExport();
        }
    }

    private void handleAdd() {
        try {
            String name = txtCustomerName.getText().trim();
            String phone = txtPhoneNumber.getText().trim();
            String address = txtAddress.getText().trim();

            // Validate name
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng nhập tên khách hàng",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                txtCustomerName.requestFocus();
                return;
            }

            // Validate phone number if provided
            if (!phone.isEmpty() && !isValidPhoneNumber(phone)) {
                JOptionPane.showMessageDialog(this,
                        "Số điện thoại không hợp lệ!\nVui lòng nhập 10 chữ số (VD: 0912345678)",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                txtPhoneNumber.requestFocus();
                return;
            }

            // Validate address length
            if (address.length() > MAX_ADDRESS_LENGTH) {
                JOptionPane.showMessageDialog(this,
                        "Địa chỉ không được vượt quá " + MAX_ADDRESS_LENGTH + " ký tự",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                txtAddress.requestFocus();
                return;
            }

            Customer customer = new Customer(
                    null,
                    name,
                    phone.isEmpty() ? null : phone,
                    address.isEmpty() ? null : address
            );

            boolean success = busCustomer.addCustomer(customer);

            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Thêm khách hàng thành công!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                loadCustomerTable();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Thêm khách hàng thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi thêm khách hàng: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Validate Vietnamese phone number format
     * Must be 10 digits starting with 0
     */
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return true; // Phone is optional
        }
        return phone.matches("^0\\d{9}$");
    }

    private void handleUpdate() {
        try {
            String id = txtCustomerId.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng chọn khách hàng cần cập nhật",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String name = txtCustomerName.getText().trim();
            String phone = txtPhoneNumber.getText().trim();
            String address = txtAddress.getText().trim();

            // Validate name
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng nhập tên khách hàng",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                txtCustomerName.requestFocus();
                return;
            }

            // Validate phone number if provided
            if (!phone.isEmpty() && !isValidPhoneNumber(phone)) {
                JOptionPane.showMessageDialog(this,
                        "Số điện thoại không hợp lệ!\nVui lòng nhập 10 chữ số (VD: 0912345678)",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                txtPhoneNumber.requestFocus();
                return;
            }

            // Validate address length
            if (address.length() > MAX_ADDRESS_LENGTH) {
                JOptionPane.showMessageDialog(this,
                        "Địa chỉ không được vượt quá " + MAX_ADDRESS_LENGTH + " ký tự",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                txtAddress.requestFocus();
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn cập nhật thông tin khách hàng này?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                Customer customer = busCustomer.getCustomerById(id);
                customer.setName(name);
                customer.setPhoneNumber(phone.isEmpty() ? null : phone);
                customer.setAddress(address.isEmpty() ? null : address);

                boolean success = busCustomer.updateCustomer(customer);

                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Cập nhật khách hàng thành công!",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadCustomerTable();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Cập nhật khách hàng thất bại!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi cập nhật khách hàng: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExport() {
        try {
            // Check if there's data to export
            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                        "Không có dữ liệu để xuất!",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Chọn nơi lưu file Excel");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();

                if (!filePath.toLowerCase().endsWith(".xlsx")) {
                    filePath += ".xlsx";
                }

                List<String[]> data = new ArrayList<>();
                data.add(new String[]{"Mã KH", "Tên khách hàng", "Số điện thoại", "Địa chỉ", "Ngày tạo"});

                // Export currently displayed data (respects search filter)
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    data.add(new String[]{
                            tableModel.getValueAt(i, 0).toString(),
                            tableModel.getValueAt(i, 1).toString(),
                            tableModel.getValueAt(i, 2).toString(),
                            tableModel.getValueAt(i, 3).toString(),
                            tableModel.getValueAt(i, 4).toString()
                    });
                }

                ExcelExporter.exportToExcel(data, filePath, "Danh sách khách hàng");

                JOptionPane.showMessageDialog(this,
                        "Xuất dữ liệu thành công!\nFile: " + filePath,
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi xuất file Excel: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
