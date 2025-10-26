package com.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Locale;

public class DIALOG_ChangePassword extends JDialog implements ActionListener {
    private JPanel contentPane;
    private JButton btnOK;
    private JButton btnCancel;
    private JPasswordField txtOldPassword;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;
    private JLabel lblOldPassword;
    private JLabel lblNewPassword;
    private JLabel lblConfirmPassword;
    private JLabel lblTitle;

    public DIALOG_ChangePassword(Frame parent) {
        super(parent, "Đổi mật khẩu", true);
        initComponents();
        setupUI();
    }

    private void initComponents() {
        // Initialize components
        contentPane = new JPanel();
        lblTitle = new JLabel("ĐỔI MẬT KHẨU");
        lblOldPassword = new JLabel("Mật khẩu cũ:");
        lblNewPassword = new JLabel("Mật khẩu mới:");
        lblConfirmPassword = new JLabel("Xác nhận mật khẩu:");

        txtOldPassword = new JPasswordField();
        txtNewPassword = new JPasswordField();
        txtConfirmPassword = new JPasswordField();

        btnOK = new JButton("Xác nhận");
        btnCancel = new JButton("Hủy");

        // Add action listeners
        btnOK.addActionListener(this);
        btnCancel.addActionListener(this);

        // Set action commands
        btnOK.setActionCommand("OK");
        btnCancel.setActionCommand("CANCEL");
    }

    private void setupUI() {
        // Set up main content pane
        contentPane.setLayout(new BorderLayout(0, 15));
        contentPane.setBorder(new EmptyBorder(20, 30, 20, 30));
        contentPane.setBackground(Color.WHITE);

        // Title panel
        JPanel pnlTitle = new JPanel();
        pnlTitle.setBackground(Color.WHITE);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(new Color(0, 102, 204));
        pnlTitle.add(lblTitle);
        contentPane.add(pnlTitle, BorderLayout.NORTH);

        // Form panel
        JPanel pnlForm = new JPanel();
        pnlForm.setLayout(new GridBagLayout());
        pnlForm.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);

        // Set font for labels
        Font labelFont = new Font("Segoe UI", Font.PLAIN, 16);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 15);

        lblOldPassword.setFont(labelFont);
        lblNewPassword.setFont(labelFont);
        lblConfirmPassword.setFont(labelFont);

        txtOldPassword.setFont(fieldFont);
        txtNewPassword.setFont(fieldFont);
        txtConfirmPassword.setFont(fieldFont);

        // Set preferred size for password fields
        Dimension fieldSize = new Dimension(300, 35);
        txtOldPassword.setPreferredSize(fieldSize);
        txtNewPassword.setPreferredSize(fieldSize);
        txtConfirmPassword.setPreferredSize(fieldSize);

        // Add old password
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        pnlForm.add(lblOldPassword, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        pnlForm.add(txtOldPassword, gbc);

        // Add new password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        pnlForm.add(lblNewPassword, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        pnlForm.add(txtNewPassword, gbc);

        // Add confirm password
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        pnlForm.add(lblConfirmPassword, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        pnlForm.add(txtConfirmPassword, gbc);

        contentPane.add(pnlForm, BorderLayout.CENTER);

        // Button panel
        JPanel pnlButton = new JPanel();
        pnlButton.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
        pnlButton.setBackground(Color.WHITE);

        // Style buttons
        Font buttonFont = new Font("Segoe UI", Font.BOLD, 15);
        Dimension buttonSize = new Dimension(130, 40);

        btnOK.setFont(buttonFont);
        btnOK.setPreferredSize(buttonSize);
        btnOK.setBackground(AppColors.PRIMARY);
        btnOK.setForeground(Color.WHITE);
        btnOK.setFocusPainted(false);
        btnOK.setBorderPainted(false);
        btnOK.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnCancel.setFont(buttonFont);
        btnCancel.setPreferredSize(buttonSize);
        btnCancel.setBackground(AppColors.DARK);
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        pnlButton.add(btnOK);
        pnlButton.add(btnCancel);

        contentPane.add(pnlButton, BorderLayout.SOUTH);

        // Set content pane and dialog properties
        setContentPane(contentPane);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getRootPane().setDefaultButton(btnOK);

        // Window listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // ESC key listener
        contentPane.registerKeyboardAction(
                e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        // Set dialog size and location
        pack();
        setMinimumSize(new Dimension(500, 350));
        setLocationRelativeTo(getParent());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case "OK":
                onOK();
                break;
            case "CANCEL":
                onCancel();
                break;
        }
    }

    private void onOK() {
        // TODO: Add validation and password change logic here
        char[] oldPass = txtOldPassword.getPassword();
        char[] newPass = txtNewPassword.getPassword();
        char[] confirmPass = txtConfirmPassword.getPassword();

        // Clear sensitive data
        Arrays.fill(oldPass, '0');
        Arrays.fill(newPass, '0');
        Arrays.fill(confirmPass, '0');

        dispose();
    }

    private void onCancel() {
        // Clear password fields before closing
        txtOldPassword.setText("");
        txtNewPassword.setText("");
        txtConfirmPassword.setText("");
        dispose();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            DIALOG_ChangePassword dialog = new DIALOG_ChangePassword(null);
            dialog.setVisible(true);
        });
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
