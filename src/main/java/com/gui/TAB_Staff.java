package com.gui;

import com.bus.BUS_Staff;
import com.entities.Staff;
import com.enums.Role;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private JComboBox<RoleItem> cboRole; // Đổi từ Role sang RoleItem
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

    private static final int LEFT_PANEL_MINIMAL_WIDTH = 750;
    private static final int RIGHT_PANEL_MINIMAL_WIDTH = 600;


    private final BUS_Staff BUSStaff = new BUS_Staff();

    private List<Staff> staffCache = new ArrayList<>();

    private static class RoleItem {
        private final Role role;
        private final String displayName;

        public RoleItem(Role role, String displayName) {
            this.role = role;
            this.displayName = displayName;
        }

        public Role getRole() {
            return role;
        }

        @Override
        public String toString() {
            return displayName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RoleItem roleItem = (RoleItem) obj;
            return role == roleItem.role;
        }

        @Override
        public int hashCode() {
            return role != null ? role.hashCode() : 0;
        }
    }

    private String getRoleDisplayName(Role role) {
        if (role == Role.MANAGER) {
            return "Quản lý";
        } else if (role == Role.PHARMACIST) {
            return "Dược sĩ";
        }
        return role.toString();
    }


    public TAB_Staff() {
        initComponents();

        btnAdd.addActionListener(this);
        btnUpdate.addActionListener(this);
        btnClear.addActionListener(this);
        btnRefresh.addActionListener(this);
        btnExport.addActionListener(this);

        loadStaffTable();

        // Thêm listener cho các bộ lọc
        setupSearchAndFilterListeners();
    }

    private void setupSearchAndFilterListeners() {
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performSearch();
            }
        });

        cboFilterRole.addActionListener(e -> performSearch());

        cboFilterStatus.addActionListener(e -> performSearch());
    }

    private void performSearch() {
        String searchText = txtSearch.getText().trim().toLowerCase();
        String selectedRole = (String) cboFilterRole.getSelectedItem();
        String selectedStatus = (String) cboFilterStatus.getSelectedItem();

        List<Staff> filteredList = new ArrayList<>();

        for (Staff staff : staffCache) {
            // Tìm kiếm theo: Mã NV, Họ tên, Username, SĐT, Email
            boolean matchesSearch = searchText.isEmpty() ||
                    staff.getId().toLowerCase().contains(searchText) ||
                    staff.getFullName().toLowerCase().contains(searchText) ||
                    staff.getUsername().toLowerCase().contains(searchText) ||
                    (staff.getPhoneNumber() != null && staff.getPhoneNumber().contains(searchText)) ||
                    (staff.getEmail() != null && staff.getEmail().toLowerCase().contains(searchText));

            // Lọc theo vai trò - Map giá trị ComboBox với enum Role
            boolean matchesRole = "Tất cả".equals(selectedRole);
            if (!matchesRole) {
                if ("Quản lý".equals(selectedRole) && staff.getRole() == Role.MANAGER) {
                    matchesRole = true;
                } else if ("Dược sĩ".equals(selectedRole) && staff.getRole() == Role.PHARMACIST) {
                    matchesRole = true;
                }
            }

            // Lọc theo trạng thái
            boolean matchesStatus = "Tất cả".equals(selectedStatus);
            if (!matchesStatus) {
                if ("Hoạt động".equals(selectedStatus) && staff.isActive()) {
                    matchesStatus = true;
                } else if ("Ngưng hoạt động".equals(selectedStatus) && !staff.isActive()) {
                    matchesStatus = true;
                }
            }

            if (matchesSearch && matchesRole && matchesStatus) {
                filteredList.add(staff);
            }
        }

        populateTable(filteredList);
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
        panel.setMinimumSize(new Dimension(LEFT_PANEL_MINIMAL_WIDTH, 0));
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
                txtUsername.setEditable(false);
                txtUsername.setBackground(AppColors.BACKGROUND);


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
        panel.setMinimumSize(new Dimension(RIGHT_PANEL_MINIMAL_WIDTH, 0));
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

        // Tạo ComboBox với RoleItem để hiển thị tiếng Việt
        RoleItem[] roleItems = new RoleItem[]{
                new RoleItem(Role.PHARMACIST, "Dược sĩ"),
                new RoleItem(Role.MANAGER, "Quản lý")
        };
        cboRole = new JComboBox<>(roleItems);
        cboRole.setSelectedIndex(0); // Mặc định là Dược sĩ
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
        } else if (o == btnUpdate) {
            updateStaffInfoFromGUI();
        } else if (o == btnClear) {
            clearInput();
        } else if (o == btnRefresh) {
            refreshData();
        } else if (o == btnExport) {
            exportToExcel();
        }

    }

    private void exportToExcel() {
        try {
            // Kiểm tra có dữ liệu không
            if (staffCache == null || staffCache.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Không có dữ liệu để xuất!",
                        "Cảnh báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Tạo tên file với timestamp
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "DanhSachNhanVien_" + timestamp;

            // Xuất Excel
            String filePath = ExcelExporter.exportStaffToExcel(staffCache, fileName);

            // Hiển thị thông báo thành công với tùy chọn mở file
            int option = JOptionPane.showConfirmDialog(this,
                    "Xuất Excel thành công!\nĐường dẫn: " + filePath + "\n\nBạn có muốn mở thư mục chứa file?",
                    "Thành công",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

            // Mở thư mục nếu người dùng chọn Yes
            if (option == JOptionPane.YES_OPTION) {
                try {
                    File file = new File(filePath);
                    Desktop.getDesktop().open(file.getParentFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Không thể mở thư mục: " + ex.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi xuất Excel: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void refreshData() {
        // Reset bộ lọc về mặc định
        txtSearch.setText("");
        cboFilterRole.setSelectedIndex(0); // "Tất cả"
        cboFilterStatus.setSelectedIndex(0); // "Tất cả"

        // Tải lại dữ liệu từ database
        loadStaffTable();
    }

    public void getStaffInfoFromGUI() {
        Staff staff = new Staff();
        try {
            staff.setFullName(txtFullName.getText().trim());
            staff.setUsername(txtUsername.getText().trim());

            // Lấy Role từ RoleItem
            RoleItem selectedRoleItem = (RoleItem) cboRole.getSelectedItem();
            staff.setRole(selectedRoleItem != null ? selectedRoleItem.getRole() : Role.PHARMACIST);

            staff.setPhoneNumber(txtPhoneNumber.getText().trim());
            staff.setEmail(txtEmail.getText().trim());
            staff.setLicenseNumber(txtLicenseNumber.getText().trim());

            Date hireDate = (Date) spnHireDate.getValue();
            LocalDate localHireDate = LocalDate.ofInstant(hireDate.toInstant(), ZoneId.systemDefault());
            staff.setHireDate(localHireDate);

            staff.setActive(chkIsActive.isSelected());

            boolean ok = BUSStaff.addStaff(staff);

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

    public void updateStaffInfoFromGUI() {
        try {
            // Kiểm tra xem có chọn nhân viên không
            String staffId = txtStaffId.getText().trim();
            if (staffId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên cần cập nhật từ bảng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Lấy thông tin từ form
            Staff staff = new Staff();
            staff.setId(staffId);
            staff.setFullName(txtFullName.getText().trim());
            staff.setUsername(txtUsername.getText().trim());

            // Lấy Role từ RoleItem
            RoleItem selectedRoleItem = (RoleItem) cboRole.getSelectedItem();
            staff.setRole(selectedRoleItem != null ? selectedRoleItem.getRole() : Role.PHARMACIST);

            staff.setPhoneNumber(txtPhoneNumber.getText().trim());
            staff.setEmail(txtEmail.getText().trim());
            staff.setLicenseNumber(txtLicenseNumber.getText().trim());

            Date hireDate = (Date) spnHireDate.getValue();
            LocalDate localHireDate = LocalDate.ofInstant(hireDate.toInstant(), ZoneId.systemDefault());
            staff.setHireDate(localHireDate);

            staff.setActive(chkIsActive.isSelected());

            // Xác nhận cập nhật
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc chắn muốn cập nhật thông tin nhân viên này?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                boolean ok = BUSStaff.updateStaff(staff);

                if (ok) {
                    JOptionPane.showMessageDialog(this, "Cập nhật nhân viên thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    clearInput();
                    loadStaffTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Cập nhật nhân viên thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearInput() {
        txtStaffId.setText("");
        txtFullName.setText("");
        txtUsername.setText("");
        cboRole.setSelectedIndex(0); // Mặc định Dược sĩ
        txtPhoneNumber.setText("");
        txtEmail.setText("");
        txtLicenseNumber.setText("");
        spnHireDate.setValue(new Date());
        chkIsActive.setSelected(true);
        txtUsername.setEditable(true);
        txtUsername.setBackground(Color.WHITE);
    }

    private void loadStaffTable() {
        try {
            staffCache = BUSStaff.getAllStaffs();
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
                    getRoleDisplayName(s.getRole()),
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

        txtStaffId.setText(s.getId() != null ? s.getId() : "");
        txtFullName.setText(s.getFullName() != null ? s.getFullName() : "");
        txtUsername.setText(s.getUsername() != null ? s.getUsername() : "");

        // Chọn RoleItem tương ứng trong ComboBox
        Role staffRole = s.getRole() != null ? s.getRole() : Role.PHARMACIST;
        for (int i = 0; i < cboRole.getItemCount(); i++) {
            RoleItem item = (RoleItem) cboRole.getItemAt(i);
            if (item.getRole() == staffRole) {
                cboRole.setSelectedIndex(i);
                break;
            }
        }

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
