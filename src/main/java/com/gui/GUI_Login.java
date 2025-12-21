package com.gui;

import com.bus.BUS_Staff;
import com.bus.BUS_Shift;
import com.entities.Shift;
import com.entities.Staff;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.Locale;

public class GUI_Login implements ActionListener {
    private JButton btnLogin;
    public JPanel pnlLogin;
    private JPanel pnlLeft;
    private JPanel pnlRight;
    private JLabel lblLogo;
    private JLabel lblBanner;
    private JLabel lblWelcome;
    private JLabel lblSubWelcome;
    private JLabel lblLogin;
    private JTextField txtLogin;
    private JLabel lblPassword;
    private JPasswordField txtPassword;
    private JPanel pnlButton;
    private JButton btnForgotPassword;
    private JLabel lblFooter;

    private BUS_Staff BUSStaff;
    private Staff currentStaff;


    public GUI_Login() {
        BUSStaff = new BUS_Staff();
        btnLogin.addActionListener(this);
        btnForgotPassword.addActionListener(this);

        txtPassword.addActionListener(this);

        txtLogin.addActionListener(e -> txtPassword.requestFocus());

        // For testing purposes, pre-fill with admin credentials
        txtLogin.setText("admin");
        txtPassword.setText("admin");
    }

    private void handleLogin() {
        String username = txtLogin.getText().trim();
        String password = new String(txtPassword.getPassword());

        // Validate input
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(pnlLogin,
                    "Vui lòng nhập tên đăng nhập",
                    "Lỗi đăng nhập",
                    JOptionPane.WARNING_MESSAGE);
            txtLogin.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(pnlLogin,
                    "Vui lòng nhập mật khẩu",
                    "Lỗi đăng nhập",
                    JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return;
        }

//        if (username.equals("admin") && password.equals("admin")) {
//            currentStaff = new Staff();
//            currentStaff.setFullName("Developer");
//            currentStaff.setUsername("admin");
//
//            JOptionPane.showMessageDialog(pnlLogin,
//                    "Đăng nhập thành công (Chế độ Developer)!\nXin chào, Developer",
//                    "Thành công",
//                    JOptionPane.INFORMATION_MESSAGE);
//            openMainMenu(password);
//            return;
//        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        // Store password for temporary password check
        final String loginPassword = password;

        // Use SwingWorker to perform login in background
        SwingWorker<Staff, Void> worker = new SwingWorker<Staff, Void>() {
            private String errorMessage = null;

            @Override
            protected Staff doInBackground() throws Exception {
                try {
                    return BUSStaff.login(username, loginPassword);
                } catch (IllegalArgumentException e) {
                    errorMessage = e.getMessage();
                    return null;
                } catch (Exception e) {
                    errorMessage = "Lỗi kết nối đến cơ sở dữ liệu. Vui lòng thử lại sau.";
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    currentStaff = get();

                    if (currentStaff != null) {
                        JOptionPane.showMessageDialog(pnlLogin,
                                "Đăng nhập thành công!\nXin chào, " + currentStaff.getFullName(),
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        openMainMenu();
                    } else {
                        JOptionPane.showMessageDialog(pnlLogin,
                                errorMessage != null ? errorMessage : "Đăng nhập thất bại",
                                "Lỗi đăng nhập",
                                JOptionPane.ERROR_MESSAGE);

                        txtPassword.setText("");
                        txtPassword.requestFocus();

                        btnLogin.setEnabled(true);
                        btnLogin.setText("Đăng nhập");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(pnlLogin,
                            "Đã xảy ra lỗi không mong muốn",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();

                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");
                }
            }
        };

        worker.execute();
    }

    private void openMainMenu() {
        JFrame loginFrame = (JFrame) SwingUtilities.getWindowAncestor(pnlLogin);

        // Check if staff is using temporary password (first login)
        if (BUSStaff.isFirstLogin(currentStaff)) {
            DIALOG_ChangePassword changePasswordDialog = new DIALOG_ChangePassword(loginFrame, currentStaff, true);
            changePasswordDialog.setVisible(true);
            if (!changePasswordDialog.isPasswordChanged()) {
                JOptionPane.showMessageDialog(
                        pnlLogin,
                        "Bạn phải đổi mật khẩu lần đầu để sử dụng hệ thống!",
                        "Yêu cầu đổi mật khẩu",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            } else {
                BUSStaff.updateChangePasswordFlag(currentStaff, false);
            }
        } else if (BUSStaff.isMustChangePassword(currentStaff)) {
            DIALOG_ChangePassword changePasswordDialog = new DIALOG_ChangePassword(loginFrame, currentStaff, false);
            changePasswordDialog.setVisible(true);
            if (!changePasswordDialog.isPasswordChanged()) {
                JOptionPane.showMessageDialog(
                        pnlLogin,
                        "Bạn phải đổi mật khẩu để tiếp tục sử dụng hệ thống!",
                        "Yêu cầu đổi mật khẩu",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            } else {
                BUSStaff.updateChangePasswordFlag(currentStaff, false);
            }
        }

        BUS_Shift busShift = new BUS_Shift();
        String workstation = busShift.getCurrentWorkstation();
        Shift openShift = busShift.getOpenShiftOnWorkstation(workstation);

        if (openShift == null) {
            // Không có ca nào đang mở, hỏi có muốn mở ca không
            int choice = JOptionPane.showConfirmDialog(
                    pnlLogin,
                    "Hiện chưa có ca làm việc nào đang mở. Bạn có muốn mở ca mới không?",
                    "Mở ca làm việc",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                DIALOG_OpenShift openShiftDialog = new DIALOG_OpenShift(loginFrame, currentStaff);
                openShiftDialog.setVisible(true);
                // Nếu user hủy thì vẫn vào main menu bình thường
            }
            // Nếu chọn No thì vào main menu luôn
        } else if (openShift.getStaff() != null && openShift.getStaff().getId().equals(currentStaff.getId())) {
            // Có ca đang mở của chính user
            Object[] options = {"Tiếp tục ca cũ", "Kết thúc ca cũ", "Bỏ qua"};
            int choice = JOptionPane.showOptionDialog(
                    pnlLogin,
                    "Bạn đang có ca chưa đóng. Bạn muốn tiếp tục ca cũ, kết thúc ca cũ hay vào mà không ca?",
                    "Ca làm việc chưa đóng",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (choice == 0) {
                // Tiếp tục ca cũ: vào main menu giữ session
            } else if (choice == 1) {
                // Kết thúc ca cũ
                DIALOG_CloseShift closeShiftDialog = new DIALOG_CloseShift(loginFrame, openShift, currentStaff);
                closeShiftDialog.setVisible(true);
                // Sau khi đóng ca, hỏi có muốn mở ca mới không
                if (closeShiftDialog.isConfirmed()) {
                    int openNew = JOptionPane.showConfirmDialog(
                            pnlLogin,
                            "Bạn có muốn mở ca mới không?",
                            "Mở ca mới",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );
                    if (openNew == JOptionPane.YES_OPTION) {
                        DIALOG_OpenShift openShiftDialog = new DIALOG_OpenShift(loginFrame, currentStaff);
                        openShiftDialog.setVisible(true);
                    }
                }
                // Nếu user hủy thì vẫn vào main menu
            } else {
                // Bỏ qua: vào main menu không ca
            }
        } else {
            // Có ca đang mở của người khác
            int choice = JOptionPane.showConfirmDialog(
                    pnlLogin,
                    "Máy này đang có ca mở bởi nhân viên khác. Bạn có muốn đóng ca này không?",
                    "Ca làm việc đang mở",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                // Force close by current user
                DIALOG_CloseShift closeShiftDialog = new DIALOG_CloseShift(loginFrame, openShift, currentStaff);
                closeShiftDialog.setVisible(true);
                if (closeShiftDialog.isConfirmed()) {
                    int openNew = JOptionPane.showConfirmDialog(
                            pnlLogin,
                            "Bạn có muốn mở ca mới không?",
                            "Mở ca mới",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );
                    if (openNew == JOptionPane.YES_OPTION) {
                        BigDecimal endCash = openShift.getEndCash();
                        DIALOG_OpenShift openShiftDialog = new DIALOG_OpenShift(loginFrame, currentStaff) {
                            @Override
                            protected void initComponents() {
                                super.initComponents();
                                if (txtStartCash != null && endCash != null) {
                                    txtStartCash.setText(endCash.toPlainString());
                                }
                            }
                        };
                        openShiftDialog.setVisible(true);
                    }
                }
            }
            // Nếu chọn No thì vào main menu luôn
        }

        // ===== END SHIFT MANAGEMENT LOGIC =====

        // Close login frame
        loginFrame.dispose();

        // Open main menu
        JFrame mainMenuFrame = new JFrame("MediWOW");
        GUI_MainMenu mainMenu = new GUI_MainMenu(currentStaff);
        mainMenuFrame.setContentPane(mainMenu.pnlMainMenu);
        mainMenuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainMenuFrame.setLocationRelativeTo(null);
        mainMenuFrame.setVisible(true);
        mainMenuFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
        pnlLogin = new JPanel();
        pnlLogin.setLayout(new GridBagLayout());
        pnlLogin.setBackground(new Color(-2236963));
        pnlLogin.setOpaque(false);
        pnlLogin.setPreferredSize(new Dimension(1080, 600));
        pnlLeft = new JPanel();
        pnlLeft.setLayout(new GridBagLayout());
        pnlLeft.setBackground(new Color(-2236963));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        pnlLogin.add(pnlLeft, gbc);
        lblLogo = new JLabel();
        lblLogo.setIcon(new ImageIcon(getClass().getResource("/images/logo.png")));
        lblLogo.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.ipadx = 200;
        pnlLeft.add(lblLogo, gbc);
        lblBanner = new JLabel();
        lblBanner.setIcon(new ImageIcon(getClass().getResource("/images/login_banner.png")));
        lblBanner.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        pnlLeft.add(lblBanner, gbc);
        pnlRight = new JPanel();
        pnlRight.setLayout(new GridBagLayout());
        pnlRight.setAutoscrolls(false);
        pnlRight.setForeground(new Color(-2236963));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1000.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 20;
        pnlLogin.add(pnlRight, gbc);
        lblWelcome = new JLabel();
        Font lblWelcomeFont = this.$$$getFont$$$(null, Font.BOLD, 36, lblWelcome.getFont());
        if (lblWelcomeFont != null) lblWelcome.setFont(lblWelcomeFont);
        lblWelcome.setForeground(new Color(-16012317));
        lblWelcome.setText("Chào mừng trở lại!");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        pnlRight.add(lblWelcome, gbc);
        lblSubWelcome = new JLabel();
        Font lblSubWelcomeFont = this.$$$getFont$$$(null, Font.PLAIN, 20, lblSubWelcome.getFont());
        if (lblSubWelcomeFont != null) lblSubWelcome.setFont(lblSubWelcomeFont);
        lblSubWelcome.setForeground(new Color(-16012317));
        lblSubWelcome.setText("Vui lòng đăng nhập để tiếp tục");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(20, 0, 50, 0);
        pnlRight.add(lblSubWelcome, gbc);
        lblLogin = new JLabel();
        Font lblLoginFont = this.$$$getFont$$$(null, -1, 16, lblLogin.getFont());
        if (lblLoginFont != null) lblLogin.setFont(lblLoginFont);
        lblLogin.setForeground(new Color(-16027943));
        lblLogin.setText("Tên đăng nhập");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        pnlRight.add(lblLogin, gbc);
        txtLogin = new JTextField();
        Font txtLoginFont = this.$$$getFont$$$(null, -1, 16, txtLogin.getFont());
        if (txtLoginFont != null) txtLogin.setFont(txtLoginFont);
        txtLogin.setPreferredSize(new Dimension(70, 30));
        txtLogin.setToolTipText("Nhập tài khoản");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        pnlRight.add(txtLogin, gbc);
        lblPassword = new JLabel();
        Font lblPasswordFont = this.$$$getFont$$$(null, -1, 16, lblPassword.getFont());
        if (lblPasswordFont != null) lblPassword.setFont(lblPasswordFont);
        lblPassword.setForeground(new Color(-16027943));
        lblPassword.setText("Mật khẩu");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        pnlRight.add(lblPassword, gbc);
        txtPassword = new JPasswordField();
        txtPassword.setPreferredSize(new Dimension(70, 30));
        txtPassword.setToolTipText("Nhập mật khẩu");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlRight.add(txtPassword, gbc);
        pnlButton = new JPanel();
        pnlButton.setLayout(new GridLayoutManager(1, 2, new Insets(30, 0, 0, 0), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.BOTH;
        pnlRight.add(pnlButton, gbc);
        btnLogin = new JButton();
        btnLogin.setBackground(new Color(-16012317));
        Font btnLoginFont = this.$$$getFont$$$(null, -1, 16, btnLogin.getFont());
        if (btnLoginFont != null) btnLogin.setFont(btnLoginFont);
        btnLogin.setForeground(new Color(-1286));
        btnLogin.setText("Đăng nhập");
        btnLogin.setToolTipText("Nhấp vào để đăng nhập");
        pnlButton.add(btnLogin, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 50), null, 0, false));
        btnForgotPassword = new JButton();
        btnForgotPassword.setBackground(new Color(-16012317));
        btnForgotPassword.setDoubleBuffered(true);
        Font btnForgotPasswordFont = this.$$$getFont$$$(null, -1, 16, btnForgotPassword.getFont());
        if (btnForgotPasswordFont != null) btnForgotPassword.setFont(btnForgotPasswordFont);
        btnForgotPassword.setForeground(new Color(-1286));
        btnForgotPassword.setText("Quên mật khẩu");
        btnForgotPassword.setToolTipText("Đặt lại mật khẩu");
        pnlButton.add(btnForgotPassword, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 50), null, 0, false));
        lblFooter = new JLabel();
        Font lblFooterFont = this.$$$getFont$$$(null, -1, 16, lblFooter.getFont());
        if (lblFooterFont != null) lblFooter.setFont(lblFooterFont);
        lblFooter.setText("© 2025 MediWOW");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.insets = new Insets(100, 0, 0, 0);
        pnlRight.add(lblFooter, gbc);
        lblLogin.setLabelFor(txtLogin);
        lblPassword.setLabelFor(txtPassword);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return pnlLogin;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();

        if (o == btnLogin || o == txtPassword) {
            handleLogin();
        } else if (o == btnForgotPassword) {
            handleForgotPassword();
        }
    }

    private void handleForgotPassword() {
        JFrame loginFrame = (JFrame) SwingUtilities.getWindowAncestor(pnlLogin);
        DIALOG_ForgotPassword forgotPasswordDialog = new DIALOG_ForgotPassword(loginFrame);
        forgotPasswordDialog.setVisible(true);
    }
}
