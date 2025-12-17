package com.gui;

import com.bus.BUS_Shift;
import com.entities.Shift;
import com.entities.Staff;
import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DIALOG_OpenShift extends JDialog {
    private JTextField txtStartCash;
    private JTextArea txtNotes;
    private JButton btnConfirm;
    private JButton btnCancel;

    private Staff currentStaff;
    private BUS_Shift busShift;
    private Shift openedShift = null;

    private static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public DIALOG_OpenShift(Frame parent, Staff staff) {
        super(parent, "Mở ca làm việc", true);
        this.currentStaff = staff;
        this.busShift = new BUS_Shift();

        initComponents();

        setSize(500, 450);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(AppColors.LIGHT);

        // Title
        JLabel lblTitle = new JLabel("MỞ CA LÀM VIỆC");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(AppColors.PRIMARY);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(AppColors.LIGHT);

        // Staff info panel
        JPanel staffInfoPanel = createStaffInfoPanel();
        contentPanel.add(staffInfoPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Cash input panel
        JPanel cashPanel = createCashPanel();
        contentPanel.add(cashPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Notes panel
        JPanel notesPanel = createNotesPanel();
        contentPanel.add(notesPanel);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createStaffInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Thông tin nhân viên"),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Staff name
        panel.add(createLabel("Nhân viên:"));
        JLabel lblStaff = new JLabel(currentStaff != null ? currentStaff.getFullName() : "N/A");
        lblStaff.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(lblStaff);

        // Staff ID
        panel.add(createLabel("Mã NV:"));
        JLabel lblStaffId = new JLabel(currentStaff != null ? currentStaff.getId() : "N/A");
        lblStaffId.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(lblStaffId);

        // Current time
        panel.add(createLabel("Thời gian:"));
        JLabel lblTime = new JLabel(dateTimeFormat.format(LocalDateTime.now()));
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(lblTime);

        return panel;
    }

    private JPanel createCashPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Tiền mặt đầu ca"),
            new EmptyBorder(10, 10, 10, 10)
        ));

        panel.add(createLabel("Số tiền (VND):"));
        txtStartCash = new JTextField("0");
        txtStartCash.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(txtStartCash);

        return panel;
    }

    private JPanel createNotesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Ghi chú (tùy chọn)"),
            new EmptyBorder(10, 10, 10, 10)
        ));

        txtNotes = new JTextArea(4, 20);
        txtNotes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(txtNotes);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(AppColors.LIGHT);

        btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.setPreferredSize(new Dimension(100, 35));
        btnCancel.addActionListener(e -> handleCancel());

        btnConfirm = new JButton("Mở ca");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConfirm.setPreferredSize(new Dimension(120, 35));
        btnConfirm.setBackground(AppColors.SUCCESS);
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.addActionListener(e -> handleConfirm());

        panel.add(btnCancel);
        panel.add(btnConfirm);

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return label;
    }

    private void handleConfirm() {
        try {
            // Validate start cash
            String startCashText = txtStartCash.getText().trim().replaceAll("[^0-9.]", "");
            if (startCashText.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập tiền đầu ca!",
                    "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            BigDecimal startCash = new BigDecimal(startCashText);
            if (startCash.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this,
                    "Tiền đầu ca không được âm!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            String notes = txtNotes.getText().trim();

            // Confirm action
            int choice = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn mở ca làm việc?",
                "Xác nhận mở ca",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                // Open shift
                openedShift = busShift.openShift(currentStaff, startCash, notes.isEmpty() ? null : notes);
                JOptionPane.showMessageDialog(this,
                    "Mở ca thành công!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Định dạng tiền không hợp lệ!",
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Không thể mở ca: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancel() {
        openedShift = null;
        dispose();
    }

    public Shift getOpenedShift() {
        return openedShift;
    }
}

