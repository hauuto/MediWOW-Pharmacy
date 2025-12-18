package com.gui;

import com.bus.BUS_Customer;
import com.entities.PrescribedCustomer;
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
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnExport;
    private JButton btnClear;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int LEFT_PANEL_MINIMAL_WIDTH = 800;
    private static final int RIGHT_PANEL_MINIMAL_WIDTH = 450;

    private final BUS_Customer busCustomer = new BUS_Customer();
    private List<PrescribedCustomer> customerCache = new ArrayList<>();

    public TAB_Customer() {
        initComponents();

        btnAdd.addActionListener(this);
        btnUpdate.addActionListener(this);
        btnDelete.addActionListener(this);
        btnClear.addActionListener(this);
        btnRefresh.addActionListener(this);
        btnExport.addActionListener(this);

        loadCustomerTable();
        setupSearchListener();
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
        List<PrescribedCustomer> filteredList = new ArrayList<>();

        for (PrescribedCustomer customer : customerCache) {
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

        btnRefresh = createStyledButton("Làm mới", AppColors.SECONDARY);
        btnExport = createStyledButton("Xuất Excel", AppColors.DARK);

        searchBar.add(lblSearch);
        searchBar.add(txtSearch);
        searchBar.add(Box.createHorizontalStrut(20));
        searchBar.add(btnRefresh);
        searchBar.add(btnExport);

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

        btnAdd = createStyledButton("Thêm mới", new Color(34, 139, 34));
        btnUpdate = createStyledButton("Cập nhật", new Color(255, 165, 0));
        btnDelete = createStyledButton("Xóa", new Color(220, 20, 60));
        btnClear = createStyledButton("Xóa trắng", AppColors.DARK);

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
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
        formContent.add(txtCustomerName, gbc);

        // Số điện thoại
        gbc.gridx = 0;
        gbc.gridy = 2;
        formContent.add(createLabel("Số điện thoại:"), gbc);
        gbc.gridx = 1;
        txtPhoneNumber = createTextField();
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
            List<PrescribedCustomer> customers = busCustomer.getAllCustomers();
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

    private void populateTable(List<PrescribedCustomer> customers) {
        tableModel.setRowCount(0);
        if (customers != null) {
            for (PrescribedCustomer customer : customers) {
                Object[] row = {
                        customer.getId(),
                        customer.getName(),
                        customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "",
                        customer.getAddress() != null ? customer.getAddress() : "",
                        customer.getCreationDate() != null ? customer.getCreationDate().format(dtf) : ""
                };
                tableModel.addRow(row);
            }
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
        } else if (e.getSource() == btnDelete) {
            handleDelete();
        } else if (e.getSource() == btnClear) {
            clearForm();
        } else if (e.getSource() == btnRefresh) {
            loadCustomerTable();
            clearForm();
        } else if (e.getSource() == btnExport) {
            handleExport();
        }
    }

    private void handleAdd() {
        try {
            String name = txtCustomerName.getText().trim();
            String phone = txtPhoneNumber.getText().trim();
            String address = txtAddress.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng nhập tên khách hàng",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                txtCustomerName.requestFocus();
                return;
            }

            PrescribedCustomer customer = new PrescribedCustomer(
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

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng nhập tên khách hàng",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                txtCustomerName.requestFocus();
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn cập nhật thông tin khách hàng này?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                PrescribedCustomer customer = busCustomer.getCustomerById(id);
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

    private void handleDelete() {
        try {
            String id = txtCustomerId.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng chọn khách hàng cần xóa",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn xóa khách hàng này?\nLưu ý: Thao tác này không thể hoàn tác!",
                    "Xác nhận xóa",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = busCustomer.deleteCustomer(id);

                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Xóa khách hàng thành công!",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadCustomerTable();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Xóa khách hàng thất bại!",
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
                    "Lỗi khi xóa khách hàng: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExport() {
        try {
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

                for (PrescribedCustomer customer : customerCache) {
                    data.add(new String[]{
                            customer.getId(),
                            customer.getName(),
                            customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "",
                            customer.getAddress() != null ? customer.getAddress() : "",
                            customer.getCreationDate() != null ? customer.getCreationDate().format(dtf) : ""
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        pCustomer = new JPanel();
        pCustomer.setLayout(new BorderLayout(0, 0));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return pCustomer;
    }
}
