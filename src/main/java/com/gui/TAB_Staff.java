package com.gui;

import com.enums.Role;
import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;

public class TAB_Staff extends JFrame {
    JPanel pnlStaff;


    private JTextField txtSearch;
    private JTextField txtStaffId;
    private JTextField txtFullName;
    private JTextField txtUsername;
    private JTextField txtPhoneNumber;
    private JTextField txtEmail;
    private JTextField txtLicenseNumber;
    private JComboBox<Role> cboRole;
    private JComboBox<String> cboFilterRole;
    private JComboBox<String> cboFilterStatus;
    private JSpinner spnHireDate;
    private JCheckBox chkIsActive;
    private JTable tblStaff;
    private DefaultTableModel tableModel;

    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnExport;
    private JButton btnClear;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TAB_Staff() {
        initComponents();

    }

    private void initComponents() {

        pnlStaff.setLayout(new BorderLayout(10, 10));
        pnlStaff.setBackground(AppColors.WHITE);
        pnlStaff.setBorder(new EmptyBorder(15, 15, 15, 15));

        // North Panel - Search and Filter
        pnlStaff.add(createSearchPanel(), BorderLayout.NORTH);

        // Center Panel - Split between Table and Form
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(750);
        splitPane.setLeftComponent(createTablePanel());
        splitPane.setRightComponent(createFormPanel());
        splitPane.setBorder(null);
        pnlStaff.add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Title
        JLabel lblTitle = new JLabel("QUẢN LÝ NHÂN VIÊN");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(AppColors.PRIMARY);

        // Search bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchBar.setBackground(AppColors.WHITE);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        txtSearch = new JTextField(25);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                new EmptyBorder(5, 10, 5, 10)
        ));

        JButton btnSearch = createStyledButton("Tìm", AppColors.PRIMARY);

        // Filter
        JLabel lblFilterRole = new JLabel("Vai trò:");
        lblFilterRole.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        cboFilterRole = new JComboBox<>(new String[]{"Tất cả", "Quản lý", "Dược sĩ"});
        cboFilterRole.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        styleComboBox(cboFilterRole);

        JLabel lblFilterStatus = new JLabel("Trạng thái:");
        lblFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        cboFilterStatus = new JComboBox<>(new String[]{"Tất cả", "Hoạt động", "Ngưng hoạt động"});
        cboFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        styleComboBox(cboFilterStatus);

        btnRefresh = createStyledButton("Làm mới", AppColors.SECONDARY);

        btnExport = createStyledButton("Xuất Excel", AppColors.DARK);

        searchBar.add(lblSearch);
        searchBar.add(txtSearch);
        searchBar.add(btnSearch);
        searchBar.add(Box.createHorizontalStrut(20));
        searchBar.add(lblFilterRole);
        searchBar.add(cboFilterRole);
        searchBar.add(lblFilterStatus);
        searchBar.add(cboFilterStatus);
        searchBar.add(Box.createHorizontalStrut(20));
        searchBar.add(btnRefresh);
        searchBar.add(btnExport);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(searchBar, BorderLayout.CENTER);

        return panel;
    }


    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                        "Danh sách nhân viên",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14),
                        AppColors.DARK
                ),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Table
        String[] columns = {"Mã NV", "Họ tên", "Vai trò", "Số điện thoại", "Email", "Ngày vào làm", "Trạng thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblStaff = new JTable(tableModel);
        tblStaff.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblStaff.setRowHeight(35);
        tblStaff.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblStaff.setShowGrid(true);
        tblStaff.setGridColor(AppColors.LIGHT);
        tblStaff.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
            }
        });

        // Header styling
        JTableHeader header = tblStaff.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(AppColors.PRIMARY);
        header.setForeground(AppColors.DARK);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));

        // Cell renderer for center alignment
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tblStaff.getColumnCount(); i++) {
            tblStaff.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Custom renderer for status column
        tblStaff.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                if (value != null) {
                    String status = value.toString();
                    if (status.equals("Hoạt động")) {
                        setForeground(new Color(34, 139, 34));
                        setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else {
                        setForeground(new Color(220, 20, 60));
                        setFont(new Font("Segoe UI", Font.BOLD, 13));
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblStaff);
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
        panel.setBackground(AppColors.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                        "Thông tin nhân viên",
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

        // Mã nhân viên
        gbc.gridx = 0;
        gbc.gridy = 0;
        formContent.add(createLabel("Mã nhân viên:"), gbc);
        gbc.gridx = 1;
        txtStaffId = createTextField();
        txtStaffId.setEditable(false);
        txtStaffId.setBackground(AppColors.BACKGROUND);
        formContent.add(txtStaffId, gbc);

        // Họ tên
        gbc.gridx = 0;
        gbc.gridy = 1;
        formContent.add(createLabel("Họ tên:"), gbc);
        gbc.gridx = 1;
        txtFullName = createTextField();
        formContent.add(txtFullName, gbc);

        // Tên đăng nhập
        gbc.gridx = 0;
        gbc.gridy = 2;
        formContent.add(createLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1;
        txtUsername = createTextField();
        formContent.add(txtUsername, gbc);

        // Vai trò
        gbc.gridx = 0;
        gbc.gridy = 3;
        formContent.add(createLabel("Vai trò:"), gbc);
        gbc.gridx = 1;
        cboRole = new JComboBox<>(Role.values());
        cboRole.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        styleComboBox(cboRole);
        formContent.add(cboRole, gbc);

        // Số điện thoại
        gbc.gridx = 0;
        gbc.gridy = 4;
        formContent.add(createLabel("Số điện thoại:"), gbc);
        gbc.gridx = 1;
        txtPhoneNumber = createTextField();
        formContent.add(txtPhoneNumber, gbc);

        // Email
        gbc.gridx = 0;
        gbc.gridy = 5;
        formContent.add(createLabel("Email:"), gbc);
        gbc.gridx = 1;
        txtEmail = createTextField();
        formContent.add(txtEmail, gbc);

        // Số chứng chỉ
        gbc.gridx = 0;
        gbc.gridy = 6;
        formContent.add(createLabel("Số chứng chỉ:"), gbc);
        gbc.gridx = 1;
        txtLicenseNumber = createTextField();
        formContent.add(txtLicenseNumber, gbc);

        // Ngày vào làm
        gbc.gridx = 0;
        gbc.gridy = 7;
        formContent.add(createLabel("Ngày vào làm:"), gbc);
        gbc.gridx = 1;
        SpinnerDateModel dateModel = new SpinnerDateModel();
        spnHireDate = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(spnHireDate, "dd/MM/yyyy");
        spnHireDate.setEditor(dateEditor);
        spnHireDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formContent.add(spnHireDate, gbc);

        // Trạng thái
        gbc.gridx = 0;
        gbc.gridy = 8;
        formContent.add(createLabel("Trạng thái:"), gbc);
        gbc.gridx = 1;
        chkIsActive = new JCheckBox("Đang hoạt động");
        chkIsActive.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chkIsActive.setBackground(AppColors.WHITE);
        chkIsActive.setForeground(AppColors.DARK);
        chkIsActive.setSelected(true);
        formContent.add(chkIsActive, gbc);

        // Add some spacing
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.weighty = 1.0;
        formContent.add(Box.createVerticalGlue(), gbc);

        JScrollPane scrollPane = new JScrollPane(formContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Helper methods
     */
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
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
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 35));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }

            public void mouseExited(MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                new EmptyBorder(2, 5, 2, 5)
        ));
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
        pnlStaff = new JPanel();
        pnlStaff.setLayout(new BorderLayout(0, 0));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return pnlStaff;
    }

}
