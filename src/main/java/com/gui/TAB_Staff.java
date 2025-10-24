package com.gui;

import com.bus.StaffBUS;
import com.entities.Staff;
import com.enums.Role;
import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TAB_Staff extends JFrame implements ActionListener {
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
    private JButton btnRefresh;
    private JButton btnExport;
    private JButton btnClear;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    private final StaffBUS staffBUS = new StaffBUS();

    private List<Staff> staffCache = new ArrayList<>();


    public TAB_Staff() {
        initComponents();

        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        btnRefresh.addActionListener(this);
        btnRefresh.addActionListener(this);
        btnExport.addActionListener(this);

        loadStaffTable();


    }

    private void initComponents() {

        pnlStaff.setLayout(new BorderLayout(10, 10));
        pnlStaff.setBackground(AppColors.WHITE);
        pnlStaff.setBorder(new EmptyBorder(15, 15, 15, 15));

        // North Panel - Search and Filter
        pnlStaff.add(createSearchPanel(), BorderLayout.NORTH);

        // Center Panel - Split between Table and Form
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(1200);
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
        tblStaff.setCellEditor(null);
        tblStaff.setGridColor(AppColors.LIGHT);
        tblStaff.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblStaff.getSelectedRow();
                if (row >= 0) {
                    fillFormRow(row);
                }

            }
        });

        // Header styling
        JTableHeader header = tblStaff.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(AppColors.PRIMARY);
        header.setForeground(AppColors.WHITE);
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
        btnClear = createStyledButton("Xóa trắng", AppColors.DARK);

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
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
        cboRole.setSelectedItem(Role.PHARMACIST);
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

    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o == btnAdd) {
            getStaffInfoFromGUI();
        } else if (o == btnClear) {
            clearInput();
        }

    }

    public void getStaffInfoFromGUI() {
        Staff staff = new Staff();
        try {
            staff.setFullName(txtFullName.getText().trim());
            staff.setUsername(txtUsername.getText().trim());
            staff.setRole((Role) cboRole.getSelectedItem());
            staff.setPhoneNumber(txtPhoneNumber.getText().trim());
            staff.setEmail(txtEmail.getText().trim());
            staff.setLicenseNumber(txtLicenseNumber.getText().trim());

            Date hireDate = (Date) spnHireDate.getValue();
            LocalDate localHireDate = LocalDate.ofInstant(hireDate.toInstant(), ZoneId.systemDefault());
            staff.setHireDate(localHireDate);

            staff.setActive(chkIsActive.isSelected());

            boolean ok = staffBUS.addStaff(staff);

            if (ok) {
                JOptionPane.showMessageDialog(this, "Thêm nhân viên thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearInput();
                loadStaffTable();
            } else {
                JOptionPane.showMessageDialog(this, "Thêm nhân viên thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }


    }

    public void clearInput() {
        txtStaffId.setText("");
        txtFullName.setText("");
        txtUsername.setText("");
        cboRole.setSelectedItem(Role.PHARMACIST);
        txtPhoneNumber.setText("");
        txtEmail.setText("");
        txtLicenseNumber.setText("");
        spnHireDate.setValue(new Date());
        chkIsActive.setSelected(true);
    }

    private void loadStaffTable() {
        try {
            staffCache = staffBUS.getAllStaffs();
            populateTable(staffCache);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu" + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateTable(List<Staff> ls) {
        tableModel.setRowCount(0);
        if (ls == null) return;
        for (Staff s : ls) {
            tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getFullName(),
                    s.getRole(),
                    s.getPhoneNumber(),
                    s.getEmail(),
                    s.getHireDate().format(dtf),
                    s.isActive() ? "Hoạt động" : "Đã nghỉ việc"
            });
        }
    }

    private void fillFormRow(int row) {
        if (row < 0 || staffCache == null || staffCache.isEmpty()) return;

        int modelRow = (tblStaff.getRowSorter() != null) ? tblStaff.getRowSorter().convertRowIndexToModel(row) : row;

        if (modelRow < 0 || modelRow >= staffCache.size()) return;
        Staff s = staffCache.get(modelRow);

        txtStaffId.setText(s.getId() != null ? s.getId().toString() : "");
        txtFullName.setText(s.getFullName() != null ? s.getFullName() : "");
        txtUsername.setText(s.getUsername() != null ? s.getUsername() : "");
        cboRole.setSelectedItem(s.getRole() != null ? s.getRole() : Role.PHARMACIST);
        txtPhoneNumber.setText(s.getPhoneNumber() != null ? s.getPhoneNumber() : "");
        txtEmail.setText(s.getEmail() != null ? s.getEmail() : "");
        txtLicenseNumber.setText(s.getLicenseNumber() != null ? s.getLicenseNumber() : "");

        if (s.getHireDate() != null) {
            Date utilDate = Date.from(s.getHireDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            spnHireDate.setValue(utilDate);
        }

        chkIsActive.setSelected(s.isActive());
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
