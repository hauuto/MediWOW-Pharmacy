package com.gui;

import com.utils.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Lớp hiển thị lịch
 * Thanh Khôi
 *
 */
public class GUI_DatePicker extends JPanel {
    private JTextField textField;
    private JButton calendarButton;
    private Date selectedDate;
    private SimpleDateFormat dateFormat;
    private JPopupMenu popup;
    private Calendar calendar;
    private Date minDate; // Ngày tối thiểu có thể chọn
    private Date maxDate; // Ngày tối đa có thể chọn

    public GUI_DatePicker(Date initialDate) {
        this.selectedDate = initialDate;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        this.calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);

        setLayout(new BorderLayout(4, 0));
        setOpaque(false);

        // Text field hiển thị ngày đã chọn
        textField = new JTextField(dateFormat.format(selectedDate), 12);
        textField.setEditable(false);
        textField.setBackground(Color.WHITE);

        // Nút calendar icon
        calendarButton = new JButton("📅");
        calendarButton.setPreferredSize(new Dimension(35, 25));
        calendarButton.setFocusPainted(false);
        calendarButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        add(textField, BorderLayout.CENTER);
        add(calendarButton, BorderLayout.EAST);

        // Tạo popup calendar
        createCalendarPopup();

        // Sự kiện click vào nút calendar
        calendarButton.addActionListener(e -> showCalendarPopup());
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCalendarPopup();
            }
        });
    }

    private void createCalendarPopup() {
        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.setBorder(BorderFactory.createLineBorder(AppColors.SECONDARY, 1));
    }

    private void showCalendarPopup() {
        // Tạo lại calendar panel mỗi khi hiển thị để cập nhật tháng hiện tại
        JPanel calendarPanel = createCalendarPanel();
        popup.removeAll();
        popup.add(calendarPanel);
        popup.pack();
        popup.show(textField, 0, textField.getHeight());
    }

    private JPanel createCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header: Tháng/Năm và nút điều hướng
        JPanel header = createHeader();
        panel.add(header, BorderLayout.NORTH);

        // Tên các ngày trong tuần
        JPanel weekDays = createWeekDaysPanel();
        panel.add(weekDays, BorderLayout.CENTER);

        // Grid các ngày trong tháng
        JPanel daysGrid = createDaysGrid();
        panel.add(daysGrid, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        // Nút Previous Month
        JButton prevBtn = new JButton("◀");
        styleNavButton(prevBtn);
        prevBtn.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);
            showCalendarPopup();
        });

        // Nút Next Month
        JButton nextBtn = new JButton("▶");
        styleNavButton(nextBtn);
        nextBtn.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);
            showCalendarPopup();
        });

        // Label hiển thị Tháng/Năm
        String monthYear = new SimpleDateFormat("MMMM yyyy").format(calendar.getTime());
        JLabel monthLabel = new JLabel(monthYear, SwingConstants.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 14f));

        header.add(prevBtn, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextBtn, BorderLayout.EAST);

        return header;
    }

    private void styleNavButton(JButton btn) {
        btn.setPreferredSize(new Dimension(40, 30));
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder());
    }

    private JPanel createWeekDaysPanel() {
        JPanel weekDays = new JPanel(new GridLayout(1, 7, 2, 2));
        weekDays.setOpaque(false);
        String[] days = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        for (String day : days) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
            label.setForeground(AppColors.PRIMARY);
            weekDays.add(label);
        }
        return weekDays;
    }

    private JPanel createDaysGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setOpaque(false);

        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sunday
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Thêm ô trống cho những ngày trước ngày đầu tiên của tháng
        for (int i = 1; i < firstDayOfWeek; i++) {
            grid.add(new JLabel(""));
        }

        // Thêm các ngày trong tháng
        for (int day = 1; day <= daysInMonth; day++) {
            JButton dayButton = createDayButton(day);
            grid.add(dayButton);
        }

        return grid;
    }

    private JButton createDayButton(int day) {
        JButton btn = new JButton(String.valueOf(day));
        btn.setPreferredSize(new Dimension(35, 30));
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));

        // Tạo date cho ngày hiện tại trong vòng lặp
        Calendar dayCalendar = (Calendar) calendar.clone();
        dayCalendar.set(Calendar.DAY_OF_MONTH, day);
        Date currentDate = dayCalendar.getTime();

        // Kiểm tra nếu ngày bị disable do minDate/maxDate
        boolean isDisabled = false;
        if (minDate != null && currentDate.before(minDate)) {
            isDisabled = true;
        }
        if (maxDate != null && currentDate.after(maxDate)) {
            isDisabled = true;
        }

        if (isDisabled) {
            btn.setEnabled(false);
            btn.setForeground(Color.LIGHT_GRAY);
            btn.setBackground(new Color(245, 245, 245));
            return btn;
        }

        // Highlight ngày hiện tại
        Calendar today = Calendar.getInstance();
        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)) {
            btn.setBackground(new Color(230, 240, 255));
            btn.setForeground(AppColors.PRIMARY);
            btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        }

        // Highlight ngày đã chọn
        Calendar selected = Calendar.getInstance();
        selected.setTime(selectedDate);
        if (calendar.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == selected.get(Calendar.MONTH) &&
                day == selected.get(Calendar.DAY_OF_MONTH)) {
            btn.setBackground(AppColors.PRIMARY);
            btn.setForeground(Color.WHITE);
            btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        }

        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            Color originalBg = btn.getBackground();

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!btn.getBackground().equals(AppColors.PRIMARY)) {
                    btn.setBackground(new Color(240, 245, 250));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!btn.getBackground().equals(AppColors.PRIMARY)) {
                    btn.setBackground(originalBg);
                }
            }
        });

        // Khi click chọn ngày
        btn.addActionListener(e -> {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            selectedDate = calendar.getTime();
            textField.setText(dateFormat.format(selectedDate));
            popup.setVisible(false);
            firePropertyChange("date", null, selectedDate);
        });

        return btn;
    }

    // Getter/Setter methods
    public Date getDate() {
        return selectedDate;
    }

    public void setDate(Date date) {
        this.selectedDate = date;
        this.calendar.setTime(date);
        this.textField.setText(dateFormat.format(date));
    }

    public String getFormattedDate() {
        return dateFormat.format(selectedDate);
    }

    /**
     * Đặt ngày tối thiểu có thể chọn
     */
    public void setMinDate(Date minDate) {
        this.minDate = minDate;
    }

    /**
     * Đặt ngày tối đa có thể chọn
     */
    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    public Date getMinDate() {
        return minDate;
    }

    public Date getMaxDate() {
        return maxDate;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        calendarButton.setEnabled(enabled);
    }
}
