package com.gui;

import com.bus.BUS_Shift;
import com.entities.Shift;
import com.entities.Staff;
import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DIALOG_OpenShift extends JDialog {
    JTextField txtStartCash;
    private JTextArea txtNotes;
    private JButton btnConfirm;
    private JButton btnCancel;

    private Staff currentStaff;
    private BUS_Shift busShift;
    private Shift openedShift = null;
    private Shift existingShift = null; // Existing shift on workstation
    private boolean continueExistingShift = false;

    private static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public DIALOG_OpenShift(Frame parent, Staff staff) {
        super(parent, "Mở ca làm việc", true);
        this.currentStaff = staff;
        this.busShift = new BUS_Shift();

        // Check for existing shift on this workstation
        String workstation = busShift.getCurrentWorkstation();
        existingShift = busShift.getOpenShiftOnWorkstation(workstation);

        if (existingShift != null && !existingShift.getStaff().getId().equals(staff.getId())) {
            // Another staff has an open shift on this workstation
            handleExistingShift(parent);
        } else {
            // No existing shift or same staff - proceed normally
            initComponents();
        }

        setSize(800, 600);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void handleExistingShift(Frame parent) {
        String message = String.format(
            "Hiện đang có ca do nhân viên %s mở từ %s.\n\n" +
            "Bạn có muốn tiếp tục ca này không?\n\n" +
            "Lưu ý: Nếu chọn 'Có', bạn sẽ không thể bán hàng cho đến khi đóng ca này.",
            existingShift.getStaff().getFullName(),
            existingShift.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
        );

        int choice = JOptionPane.showConfirmDialog(
            parent,
            message,
            "Ca làm việc đang mở",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            // User wants to continue existing shift - they cannot sell
            continueExistingShift = true;
            openedShift = existingShift;

            JOptionPane.showMessageDialog(
                parent,
                "Bạn đã chọn tiếp tục ca hiện tại.\n" +
                "Lưu ý: Bạn KHÔNG THỂ bán hàng khi chưa đóng ca này.",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE
            );
            dispose();
        } else {
            // User does not want to continue - cannot open new shift
            JOptionPane.showMessageDialog(
                parent,
                "Không thể mở ca mới khi đã có ca đang mở trên máy này.\n" +
                "Vui lòng đóng ca hiện tại trước.",
                "Không thể mở ca",
                JOptionPane.WARNING_MESSAGE
            );
            dispose();
        }
    }

    void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel lblTitle = new JLabel("MỞ CA LÀM VIỆC");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(AppColors.PRIMARY);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

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
        txtStartCash.setHorizontalAlignment(JTextField.RIGHT);

        // Chỉ cho phép nhập số và tự động format
        ((AbstractDocument) txtStartCash.getDocument()).setDocumentFilter(new LiveCurrencyDocumentFilter(txtStartCash));

        // Thêm FocusListener
        txtStartCash.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                // Select all để dễ thay thế
                txtStartCash.selectAll();
            }
        });

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
            // Validate start cash - remove all non-digit characters
            String startCashText = txtStartCash.getText().trim().replaceAll("[^0-9]", "");
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

            // Double-check for existing shift before confirming
            String workstation = busShift.getCurrentWorkstation();
            Shift existingCheck = busShift.getOpenShiftOnWorkstation(workstation);

            if (existingCheck != null && !existingCheck.getStaff().getId().equals(currentStaff.getId())) {
                JOptionPane.showMessageDialog(this,
                    "Máy này đang có ca mở bởi nhân viên: " + existingCheck.getStaff().getFullName() + "\n" +
                    "Vui lòng đóng ca đó trước khi mở ca mới.",
                    "Không thể mở ca",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Confirm action
            int choice = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn mở ca làm việc?",
                "Xác nhận mở ca",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                // Open shift - BUS layer will also check workstation
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
        } catch (IllegalStateException e) {
            // Caught from BUS_Shift when shift already exists
            JOptionPane.showMessageDialog(this,
                e.getMessage(),
                "Không thể mở ca",
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

    /**
     * DocumentFilter tự động format tiền tệ khi nhập (live formatting)
     * Chỉ cho phép nhập số và tự động thêm dấu chấm phân cách hàng nghìn
     */
    private static class LiveCurrencyDocumentFilter extends DocumentFilter {
        private final JTextField textField;
        private boolean isUpdating = false;

        public LiveCurrencyDocumentFilter(JTextField textField) {
            this.textField = textField;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null || isUpdating) return;

            // Chỉ cho phép số
            if (string.matches("\\d+")) {
                isUpdating = true;
                super.insertString(fb, offset, string, attr);
                formatTextField(fb);
                isUpdating = false;
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (isUpdating) {
                super.replace(fb, offset, length, text, attrs);
                return;
            }

            if (text == null) {
                isUpdating = true;
                super.replace(fb, offset, length, text, attrs);
                formatTextField(fb);
                isUpdating = false;
                return;
            }

            // Chỉ cho phép số
            if (text.matches("\\d*")) {
                isUpdating = true;
                super.replace(fb, offset, length, text, attrs);
                formatTextField(fb);
                isUpdating = false;
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            if (isUpdating) {
                super.remove(fb, offset, length);
                return;
            }

            isUpdating = true;
            super.remove(fb, offset, length);
            formatTextField(fb);
            isUpdating = false;
        }

        private void formatTextField(FilterBypass fb) throws BadLocationException {
            String text = fb.getDocument().getText(0, fb.getDocument().getLength());
            String digitsOnly = text.replaceAll("[^0-9]", "");

            if (digitsOnly.isEmpty()) {
                digitsOnly = "0";
            }

            // Format với dấu chấm phân cách hàng nghìn
            String formatted = formatWithThousandSeparator(digitsOnly);

            // Lưu vị trí cursor
            int caretPos = textField.getCaretPosition();
            int oldLength = text.length();

            // Cập nhật text
            fb.remove(0, fb.getDocument().getLength());
            fb.insertString(0, formatted, null);

            // Điều chỉnh cursor position
            int diff = formatted.length() - oldLength;
            SwingUtilities.invokeLater(() -> {
                try {
                    textField.setCaretPosition(Math.min(Math.max(0, caretPos + diff), formatted.length()));
                } catch (IllegalArgumentException ignored) {
                }
            });
        }

        private String formatWithThousandSeparator(String number) {
            // Xóa các số 0 ở đầu (trừ khi chỉ có số 0)
            number = number.replaceFirst("^0+(?!$)", "");

            // Thêm dấu chấm phân cách hàng nghìn
            StringBuilder formatted = new StringBuilder();
            int count = 0;
            for (int i = number.length() - 1; i >= 0; i--) {
                if (count > 0 && count % 3 == 0) {
                    formatted.insert(0, '.');
                }
                formatted.insert(0, number.charAt(i));
                count++;
            }
            return formatted.toString();
        }
    }
}

