package com.gui;

import com.bus.StaffBUS;
import com.entities.Staff;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;

public class GUI_Login {
    private JButton btnLogin;
    public JPanel pLogin;
    private JPanel pLeft;
    private JPanel pRight;
    private JLabel lblLogo;
    private JLabel lblBanner;
    private JLabel lblWelcome;
    private JLabel lblSubWelcome;
    private JLabel lblLogin;
    private JTextField txtLogin;
    private JLabel lblPassword;
    private JPasswordField txtPassword;
    private JPanel pButton;
    private JButton btnForgotPassword;
    private JLabel lblFooter;
    public JPanel panel1;

    private StaffBUS staffBUS;
    private Staff currentStaff;


    public GUI_Login() {
        staffBUS = new StaffBUS();
        btnLogin.addActionListener(e -> handleLogin());

        // Add Enter key listener for password field
        txtPassword.addActionListener(e -> handleLogin());

        // Add Enter key listener for username field
        txtLogin.addActionListener(e -> txtPassword.requestFocus());
    }

    private void handleLogin() {
        String username = txtLogin.getText().trim();
        String password = new String(txtPassword.getPassword());

        // Validate input
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(pLogin,
                    "Vui lòng nhập tên đăng nhập",
                    "Lỗi đăng nhập",
                    JOptionPane.WARNING_MESSAGE);
            txtLogin.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(pLogin,
                    "Vui lòng nhập mật khẩu",
                    "Lỗi đăng nhập",
                    JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return;
        }

        // Disable login button to prevent multiple clicks
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        // Use SwingWorker to perform login in background
        SwingWorker<Staff, Void> worker = new SwingWorker<Staff, Void>() {
            private String errorMessage = null;

            @Override
            protected Staff doInBackground() throws Exception {
                try {
                    return staffBUS.login(username, password);
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
                        // Login successful
                        JOptionPane.showMessageDialog(pLogin,
                                "Đăng nhập thành công!\nXin chào, " + currentStaff.getFullName(),
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        openMainMenu();
                    } else {
                        // Login failed
                        JOptionPane.showMessageDialog(pLogin,
                                errorMessage != null ? errorMessage : "Đăng nhập thất bại",
                                "Lỗi đăng nhập",
                                JOptionPane.ERROR_MESSAGE);

                        // Clear password field
                        txtPassword.setText("");
                        txtPassword.requestFocus();

                        // Re-enable login button
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Đăng nhập");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(pLogin,
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
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(pLogin);
        frame.dispose();


        JFrame mainMenuFrame = new JFrame("MediWOW");
        GUI_MainMenu mainMenu = new GUI_MainMenu(currentStaff);
        mainMenuFrame.setContentPane(mainMenu.pnlMainMenu);
        mainMenuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainMenuFrame.setLocationRelativeTo(null);
        mainMenuFrame.setVisible(true);
        mainMenuFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

    }

    public Staff getCurrentStaff() {
        return currentStaff;
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
        pLogin = new JPanel();
        pLogin.setLayout(new GridBagLayout());
        pLogin.setBackground(new Color(-2236963));
        pLogin.setOpaque(false);
        pLogin.setPreferredSize(new Dimension(1080, 600));
        pLeft = new JPanel();
        pLeft.setLayout(new GridBagLayout());
        pLeft.setBackground(new Color(-2236963));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        pLogin.add(pLeft, gbc);
        lblLogo = new JLabel();
        lblLogo.setIcon(new ImageIcon(getClass().getResource("/images/logo.png")));
        lblLogo.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.ipadx = 200;
        pLeft.add(lblLogo, gbc);
        lblBanner = new JLabel();
        lblBanner.setIcon(new ImageIcon(getClass().getResource("/images/login_banner.png")));
        lblBanner.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        pLeft.add(lblBanner, gbc);
        pRight = new JPanel();
        pRight.setLayout(new GridBagLayout());
        pRight.setAutoscrolls(false);
        pRight.setForeground(new Color(-2236963));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1000.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 20;
        pLogin.add(pRight, gbc);
        lblWelcome = new JLabel();
        Font lblWelcomeFont = this.$$$getFont$$$(null, Font.BOLD, 36, lblWelcome.getFont());
        if (lblWelcomeFont != null) lblWelcome.setFont(lblWelcomeFont);
        lblWelcome.setForeground(new Color(-16012317));
        lblWelcome.setText("Chào mừng trở lại!");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        pRight.add(lblWelcome, gbc);
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
        pRight.add(lblSubWelcome, gbc);
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
        pRight.add(lblLogin, gbc);
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
        pRight.add(txtLogin, gbc);
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
        pRight.add(lblPassword, gbc);
        txtPassword = new JPasswordField();
        txtPassword.setPreferredSize(new Dimension(70, 30));
        txtPassword.setToolTipText("Nhập mật khẩu");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pRight.add(txtPassword, gbc);
        pButton = new JPanel();
        pButton.setLayout(new GridLayoutManager(1, 2, new Insets(30, 0, 0, 0), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.BOTH;
        pRight.add(pButton, gbc);
        btnLogin = new JButton();
        btnLogin.setBackground(new Color(-16012317));
        Font btnLoginFont = this.$$$getFont$$$(null, -1, 16, btnLogin.getFont());
        if (btnLoginFont != null) btnLogin.setFont(btnLoginFont);
        btnLogin.setForeground(new Color(-1286));
        btnLogin.setText("Đăng nhập");
        btnLogin.setToolTipText("Nhấp vào để đăng nhập");
        pButton.add(btnLogin, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 50), null, 0, false));
        btnForgotPassword = new JButton();
        btnForgotPassword.setBackground(new Color(-16012317));
        btnForgotPassword.setDoubleBuffered(true);
        Font btnForgotPasswordFont = this.$$$getFont$$$(null, -1, 16, btnForgotPassword.getFont());
        if (btnForgotPasswordFont != null) btnForgotPassword.setFont(btnForgotPasswordFont);
        btnForgotPassword.setForeground(new Color(-1286));
        btnForgotPassword.setText("Quên mật khẩu");
        btnForgotPassword.setToolTipText("Đặt lại mật khẩu");
        pButton.add(btnForgotPassword, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 50), null, 0, false));
        lblFooter = new JLabel();
        Font lblFooterFont = this.$$$getFont$$$(null, -1, 16, lblFooter.getFont());
        if (lblFooterFont != null) lblFooter.setFont(lblFooterFont);
        lblFooter.setText("© 2025 MediWOW");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.insets = new Insets(100, 0, 0, 0);
        pRight.add(lblFooter, gbc);
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
        return pLogin;
    }

}