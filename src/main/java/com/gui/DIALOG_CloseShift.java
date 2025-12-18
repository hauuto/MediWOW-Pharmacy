package com.gui;

import com.bus.BUS_Shift;
import com.entities.Shift;
import com.entities.Staff;
import com.enums.Role;
import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DIALOG_CloseShift extends JDialog {
    private JTextField txtEndCash;
    private JTextField txtSystemCash;
    private JTextField txtDifference;
    private JTextArea txtNotes;
    private JTextArea txtCloseReason;
    private JButton btnConfirm;
    private JButton btnCancel;

    private Shift currentShift;
    private Staff currentStaff;
    private BUS_Shift busShift;
    private boolean confirmed = false;
    private boolean requireCloseReason = false; // Whether to require close reason

    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));
    private static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public DIALOG_CloseShift(Frame parent, Shift shift, Staff staff) {
        super(parent, "Đóng ca làm việc", true);
        this.currentShift = shift;
        this.currentStaff = staff;
        this.busShift = new BUS_Shift();

        // Check if current staff is closing their own shift
        boolean isShiftOwner = staff != null && shift != null &&
            shift.getStaff() != null &&
            staff.getId().equals(shift.getStaff().getId());

        // If not shift owner, require close reason (only manager can close)
        if (!isShiftOwner) {
            requireCloseReason = true;

            // Verify permission
            if (staff == null || staff.getRole() != Role.MANAGER) {
                JOptionPane.showMessageDialog(parent,
                    "Bạn không có quyền đóng ca này.\n" +
                    "Chỉ người mở ca hoặc Quản lý mới có thể đóng ca.",
                    "Không có quyền",
                    JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
        }

        initComponents();
        loadShiftData();

        setSize(800, requireCloseReason ? 900 : 800);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel lblTitle = new JLabel("ĐÓNG CA LÀM VIỆC");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(AppColors.PRIMARY);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Shift info panel
        JPanel shiftInfoPanel = createShiftInfoPanel();
        contentPanel.add(shiftInfoPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Cash info panel
        JPanel cashInfoPanel = createCashInfoPanel();
        contentPanel.add(cashInfoPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Close reason panel (only if not shift owner)
        if (requireCloseReason) {
            JPanel closeReasonPanel = createCloseReasonPanel();
            contentPanel.add(closeReasonPanel);
            contentPanel.add(Box.createVerticalStrut(15));
        }

        // Notes panel
        JPanel notesPanel = createNotesPanel();
        contentPanel.add(notesPanel);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createShiftInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Thông tin ca làm việc"),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Shift ID
        panel.add(createLabel("Mã ca:"));
        JLabel lblShiftId = new JLabel(currentShift != null ? currentShift.getId() : "N/A");
        lblShiftId.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(lblShiftId);

        // Staff
        panel.add(createLabel("Nhân viên:"));
        JLabel lblStaff = new JLabel(currentStaff != null ? currentStaff.getFullName() : "N/A");
        lblStaff.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(lblStaff);

        // Start time
        panel.add(createLabel("Giờ bắt đầu:"));
        String startTime = currentShift != null && currentShift.getStartTime() != null
            ? dateTimeFormat.format(currentShift.getStartTime()) : "N/A";
        JLabel lblStartTime = new JLabel(startTime);
        lblStartTime.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(lblStartTime);

        // Duration
        panel.add(createLabel("Thời gian làm việc:"));
        String duration = calculateDuration();
        JLabel lblDuration = new JLabel(duration);
        lblDuration.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(lblDuration);

        return panel;
    }

    private JPanel createCashInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Thông tin tiền mặt"),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Start cash
        panel.add(createLabel("Tiền đầu ca:"));
        String startCash = currentShift != null && currentShift.getStartCash() != null
            ? currencyFormat.format(currentShift.getStartCash()) : "0 ₫";
        JLabel lblStartCash = new JLabel(startCash);
        lblStartCash.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStartCash.setForeground(AppColors.PRIMARY);
        panel.add(lblStartCash);

        // System cash
        panel.add(createLabel("Tiền hệ thống:"));
        txtSystemCash = new JTextField();
        txtSystemCash.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtSystemCash.setEditable(false);
        txtSystemCash.setBackground(new Color(240, 240, 240));
        txtSystemCash.setForeground(AppColors.SUCCESS);
        panel.add(txtSystemCash);

        // End cash (manual input)
        panel.add(createLabel("Tiền thực tế cuối ca:"));
        txtEndCash = new JTextField("0");
        txtEndCash.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtEndCash.setHorizontalAlignment(JTextField.RIGHT);

        // Chỉ cho phép nhập số và tự động format
        ((AbstractDocument) txtEndCash.getDocument()).setDocumentFilter(new LiveCurrencyDocumentFilter(txtEndCash, this::calculateDifference));

        // Thêm FocusListener
        txtEndCash.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                // Select all để dễ thay thế
                txtEndCash.selectAll();
            }
        });

        panel.add(txtEndCash);

        // Difference
        panel.add(createLabel("Chênh lệch:"));
        txtDifference = new JTextField();
        txtDifference.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtDifference.setEditable(false);
        txtDifference.setBackground(new Color(240, 240, 240));
        panel.add(txtDifference);

        return panel;
    }

    private JPanel createNotesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Ghi chú"),
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

    private JPanel createCloseReasonPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Lý do đóng ca (BẮT BUỘC)"),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Add warning label
        JLabel lblWarning = new JLabel("⚠️ Bạn đang đóng ca của nhân viên khác. Vui lòng nhập lý do.");
        lblWarning.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblWarning.setForeground(AppColors.DANGER);
        panel.add(lblWarning, BorderLayout.NORTH);

        txtCloseReason = new JTextArea(3, 20);
        txtCloseReason.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtCloseReason.setLineWrap(true);
        txtCloseReason.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(txtCloseReason);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.setPreferredSize(new Dimension(100, 35));
        btnCancel.addActionListener(e -> handleCancel());

        btnConfirm = new JButton("Xác nhận");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConfirm.setPreferredSize(new Dimension(120, 35));
        btnConfirm.setBackground(AppColors.PRIMARY);
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

    private void loadShiftData() {
        if (currentShift == null) return;

        // Calculate and display system cash
        BigDecimal systemCash = busShift.calculateSystemCashForShift(currentShift);
        txtSystemCash.setText(currencyFormat.format(systemCash));

        // Set default end cash to system cash - use toBigInteger to get only integer part
        String systemCashDigits = systemCash.toBigInteger().toString();
        txtEndCash.setText(systemCashDigits);
        calculateDifference();
    }

    private String calculateDuration() {
        if (currentShift == null || currentShift.getStartTime() == null) {
            return "N/A";
        }

        Duration duration = Duration.between(currentShift.getStartTime(), LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        return String.format("%d giờ %d phút", hours, minutes);
    }

    private void calculateDifference() {
        try {
            // Remove all non-digit characters (including currency symbols, dots, commas)
            String endCashText = txtEndCash.getText().trim().replaceAll("[^0-9]", "");
            if (endCashText.isEmpty()) {
                txtDifference.setText("");
                txtDifference.setForeground(Color.BLACK);
                return;
            }

            String systemCashText = txtSystemCash.getText().trim().replaceAll("[^0-9]", "");

            BigDecimal endCash = new BigDecimal(endCashText);
            BigDecimal systemCash = new BigDecimal(systemCashText);
            BigDecimal difference = endCash.subtract(systemCash);

            txtDifference.setText(currencyFormat.format(difference));

            // Color based on difference
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                txtDifference.setForeground(AppColors.SUCCESS);
            } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                txtDifference.setForeground(AppColors.DANGER);
            } else {
                txtDifference.setForeground(Color.BLACK);
            }
        } catch (NumberFormatException e) {
            txtDifference.setText("Lỗi định dạng");
            txtDifference.setForeground(AppColors.DANGER);
        }
    }

    private void handleConfirm() {
        try {
            // Validate end cash - remove all non-digit characters
            String endCashText = txtEndCash.getText().trim().replaceAll("[^0-9]", "");
            if (endCashText.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập tiền thực tế cuối ca!",
                    "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            BigDecimal endCash = new BigDecimal(endCashText);
            String notes = txtNotes.getText().trim();
            String closeReason = null;

            // Validate close reason if required
            if (requireCloseReason) {
                closeReason = txtCloseReason.getText().trim();
                if (closeReason.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "Vui lòng nhập lý do đóng ca!",
                        "Thiếu thông tin",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            // Confirm action
            String confirmMessage = requireCloseReason ?
                "Bạn có chắc chắn muốn đóng ca của nhân viên khác?\nHành động này không thể hoàn tác." :
                "Bạn có chắc chắn muốn đóng ca làm việc?\nHành động này không thể hoàn tác.";

            int choice = JOptionPane.showConfirmDialog(this,
                confirmMessage,
                "Xác nhận đóng ca",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                // Close shift with closing staff and reason
                busShift.closeShift(currentShift, endCash, notes.isEmpty() ? null : notes, currentStaff, closeReason);
                confirmed = true;
                JOptionPane.showMessageDialog(this,
                    "Đóng ca thành công!",
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
                "Không thể đóng ca: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * DocumentFilter tự động format tiền tệ khi nhập (live formatting)
     * Chỉ cho phép nhập số và tự động thêm dấu chấm phân cách hàng nghìn
     */
    private static class LiveCurrencyDocumentFilter extends DocumentFilter {
        private final JTextField textField;
        private final Runnable onChangeCallback;
        private boolean isUpdating = false;

        public LiveCurrencyDocumentFilter(JTextField textField, Runnable onChangeCallback) {
            this.textField = textField;
            this.onChangeCallback = onChangeCallback;
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
                    // Gọi callback để tính toán chênh lệch
                    if (onChangeCallback != null) {
                        onChangeCallback.run();
                    }
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

