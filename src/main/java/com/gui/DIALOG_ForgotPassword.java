package com.gui;

import com.bus.BUS_Staff;
import com.utils.AppColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Tô Thanh Hậu
 * Dialog for forgot password functionality
 */
public class DIALOG_ForgotPassword extends JDialog implements ActionListener {
    private JPanel pnlMain;
    private JLabel lblTitle;
    private JLabel lblDescription;
    private JLabel lblUsername;
    private JTextField txtUsername;
    private JLabel lblEmail;
    private JTextField txtEmail;
    private JLabel lblPhoneNumber;
    private JTextField txtPhoneNumber;
    private JButton btnSubmit;
    private JButton btnCancel;
    private final BUS_Staff busStaff;

    public DIALOG_ForgotPassword(Frame parent) {
        super(parent, "Quên mật khẩu", true);
        busStaff = new BUS_Staff();

        initComponents();
        setupUI();

        setContentPane(pnlMain);
        setSize(800, 600);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        pnlMain = new JPanel();
        pnlMain.setLayout(new GridBagLayout());
        pnlMain.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        pnlMain.setBackground(AppColors.WHITE);

        lblTitle = new JLabel("Quên mật khẩu");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(AppColors.PRIMARY);

        lblDescription = new JLabel("<html>Vui lòng nhập đầy đủ thông tin để xác thực tài khoản.<br/>Mật khẩu mới sẽ được gửi về email của bạn.</html>");
        lblDescription.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDescription.setForeground(AppColors.DARK);

        lblUsername = new JLabel("Tên đăng nhập: *");
        lblUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setPreferredSize(new Dimension(300, 35));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        lblEmail = new JLabel("Email: *");
        lblEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        txtEmail = new JTextField();
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEmail.setPreferredSize(new Dimension(300, 35));
        txtEmail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        lblPhoneNumber = new JLabel("Số điện thoại: *");
        lblPhoneNumber.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        txtPhoneNumber = new JTextField();
        txtPhoneNumber.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPhoneNumber.setPreferredSize(new Dimension(300, 35));
        txtPhoneNumber.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.LIGHT, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        btnSubmit = new JButton("Đặt lại mật khẩu");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSubmit.setBackground(AppColors.PRIMARY);
        btnSubmit.setForeground(AppColors.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.setPreferredSize(new Dimension(160, 40));
        btnSubmit.setBorderPainted(false);
        btnSubmit.addActionListener(this);

        btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.setBackground(AppColors.BACKGROUND);
        btnCancel.setForeground(Color.BLACK);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setPreferredSize(new Dimension(100, 40));
        btnCancel.setBorderPainted(false);
        btnCancel.addActionListener(this);
    }

    private void setupUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        pnlMain.add(lblTitle, gbc);

        // Description
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 20, 5);
        pnlMain.add(lblDescription, gbc);

        // Username Label
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        pnlMain.add(lblUsername, gbc);

        // Username TextField
        gbc.gridy = 3;
        pnlMain.add(txtUsername, gbc);

        // Email Label
        gbc.gridy = 4;
        pnlMain.add(lblEmail, gbc);

        // Email TextField
        gbc.gridy = 5;
        pnlMain.add(txtEmail, gbc);

        // Phone Number Label
        gbc.gridy = 6;
        pnlMain.add(lblPhoneNumber, gbc);

        // Phone Number TextField
        gbc.gridy = 7;
        pnlMain.add(txtPhoneNumber, gbc);

        // Button Panel
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        pnlButtons.setBackground(AppColors.WHITE);
        pnlButtons.add(btnSubmit);
        pnlButtons.add(btnCancel);

        gbc.gridy = 8;
        gbc.insets = new Insets(20, 5, 5, 5);
        pnlMain.add(pnlButtons, gbc);
    }

    private void handleForgotPassword() {
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String phoneNumber = txtPhoneNumber.getText().trim();

        // Validate all fields are filled
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập tên đăng nhập",
                    "Lỗi",
                    JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập email",
                    "Lỗi",
                    JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocus();
            return;
        }

        if (phoneNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập số điện thoại",
                    "Lỗi",
                    JOptionPane.WARNING_MESSAGE);
            txtPhoneNumber.requestFocus();
            return;
        }

        // Validate email format
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            JOptionPane.showMessageDialog(this,
                    "Email không hợp lệ",
                    "Lỗi",
                    JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocus();
            return;
        }

        // Validate phone number format
        if (!phoneNumber.matches("^(0[9|3|7|8|5|2])+([0-9]{8})$")) {
            JOptionPane.showMessageDialog(this,
                    "Số điện thoại không hợp lệ (phải bắt đầu bằng 09, 03, 07, 08, 05, 02 và có 10 số)",
                    "Lỗi",
                    JOptionPane.WARNING_MESSAGE);
            txtPhoneNumber.requestFocus();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang xử lý...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() {
                try {
                    return busStaff.resetPasswordWithVerification(username, email, phoneNumber);
                } catch (IllegalArgumentException e) {
                    errorMessage = e.getMessage();
                    return false;
                } catch (Exception e) {
                    errorMessage = "Đã xảy ra lỗi khi xử lý yêu cầu. Vui lòng thử lại sau.";
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    Boolean success = get();

                    if (success) {
                        JOptionPane.showMessageDialog(DIALOG_ForgotPassword.this,
                                "Mật khẩu mới đã được gửi về email của bạn.\nVui lòng kiểm tra hộp thư.",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(DIALOG_ForgotPassword.this,
                                errorMessage != null ? errorMessage : "Không thể đặt lại mật khẩu",
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);

                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Đặt lại mật khẩu");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DIALOG_ForgotPassword.this,
                            "Đã xảy ra lỗi không mong muốn",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);

                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Đặt lại mật khẩu");
                }
            }
        };

        worker.execute();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == btnSubmit) {
            handleForgotPassword();
        } else if (source == btnCancel) {
            dispose();
        }
    }
}
