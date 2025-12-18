package com.gui;

import com.bus.BUS_Shift;
import com.entities.Shift;
import com.entities.Staff;
import com.gui.invoice_options.TAB_ExchangeInvoice;
import com.gui.invoice_options.TAB_InvoiceList;
import com.gui.invoice_options.TAB_SalesInvoice;
import com.interfaces.ShiftChangeListener;
import com.utils.AppColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GUI_InvoiceMenu extends JFrame implements ActionListener, ShiftChangeListener {
    JPanel pnlInvoiceMenu;
    private JPanel pnlContent;
    private CardLayout cardLayout;
    private JButton btnSalesInvoice, btnExchangeInvoice, btnReturnInvoice, btnInvoiceList;
    private Staff currentStaff;
    private TAB_SalesInvoice tabSelling;
    private TAB_ExchangeInvoice tabExchange;
    private TAB_InvoiceList tabInvoiceList;
    private boolean salesInvoiceInitialized = false;
    private boolean exchangeInvoiceInitialized = false;
    private ShiftChangeListener shiftChangeListener;
    private BUS_Shift busShift;
    private boolean hasActiveShift = false;

    public GUI_InvoiceMenu(Staff staff) {
        $$$setupUI$$$();
        this.currentStaff = staff;
        this.busShift = new BUS_Shift();

        // Check if there is an active shift
        checkActiveShift();

        pnlInvoiceMenu.add(createInvoiceButtonNavBar(), BorderLayout.NORTH);
        pnlInvoiceMenu.add(createContentPanel(), BorderLayout.CENTER);
    }

    public void setShiftChangeListener(ShiftChangeListener listener) {
        this.shiftChangeListener = listener;
    }

    @Override
    public void onShiftOpened(Shift shift) {
        // When a shift is opened, enable all invoice functions
        hasActiveShift = true;
        refreshInvoiceMenu();
    }

    @Override
    public void onShiftClosed(Shift shift) {
        // When a shift is closed, disable all invoice functions
        hasActiveShift = false;
        refreshInvoiceMenu();
    }

    /**
     * Refresh the invoice menu to reflect the current shift status
     */
    private void refreshInvoiceMenu() {
        // Reset initialization flags
        salesInvoiceInitialized = false;
        exchangeInvoiceInitialized = false;

        // Rebuild the entire menu
        pnlInvoiceMenu.removeAll();
        pnlInvoiceMenu.add(createInvoiceButtonNavBar(), BorderLayout.NORTH);
        pnlInvoiceMenu.add(createContentPanel(), BorderLayout.CENTER);
        pnlInvoiceMenu.revalidate();
        pnlInvoiceMenu.repaint();

        // If shift was opened, initialize the sales tab
        if (hasActiveShift) {
            ensureCurrentTabInitialized();
        }
    }

    private void checkActiveShift() {
        // Check if there's ANY open shift on current workstation (not just current staff's shift)
        String workstation = busShift.getCurrentWorkstation();
        Shift currentShift = busShift.getOpenShiftOnWorkstation(workstation);
        hasActiveShift = (currentShift != null);
    }

    private JPanel createContentPanel() {
        cardLayout = new CardLayout();
        pnlContent = new JPanel(cardLayout);
        pnlContent.setBackground(AppColors.WHITE);

        // If no active shift, show warning message
        if (!hasActiveShift) {
            JPanel pnlNoShift = new JPanel(new BorderLayout());
            pnlNoShift.setBackground(AppColors.WHITE);

            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBackground(AppColors.WHITE);

            JLabel lblWarning = new JLabel("Không có ca làm việc đang mở");
            lblWarning.setFont(new Font("Segoe UI", Font.BOLD, 24));
            lblWarning.setForeground(AppColors.DANGER); // Red color
            lblWarning.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblMessage = new JLabel("Vui lòng mở ca làm việc trước khi bán hàng");
            lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            lblMessage.setForeground(AppColors.TEXT);
            lblMessage.setAlignmentX(Component.CENTER_ALIGNMENT);

            messagePanel.add(Box.createVerticalGlue());
            messagePanel.add(lblWarning);
            messagePanel.add(Box.createRigidArea(new Dimension(0, 20)));
            messagePanel.add(lblMessage);
            messagePanel.add(Box.createVerticalGlue());

            pnlNoShift.add(messagePanel, BorderLayout.CENTER);
            pnlContent.add(pnlNoShift, "noshift");
            cardLayout.show(pnlContent, "noshift");
            return pnlContent;
        }

        // Create placeholder panels - actual tabs will be initialized on demand
        JPanel pnlSalesPlaceholder = new JPanel(new BorderLayout());
        pnlSalesPlaceholder.setBackground(AppColors.WHITE);
        JLabel lblSalesPlaceholder = new JLabel("Vui lòng chờ...", SwingConstants.CENTER);
        lblSalesPlaceholder.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblSalesPlaceholder.setForeground(AppColors.TEXT);
        pnlSalesPlaceholder.add(lblSalesPlaceholder, BorderLayout.CENTER);
        pnlContent.add(pnlSalesPlaceholder, "selling");

        JPanel pnlExchangePlaceholder = new JPanel(new BorderLayout());
        pnlExchangePlaceholder.setBackground(AppColors.WHITE);
        JLabel lblExchangePlaceholder = new JLabel("Vui lòng chờ...", SwingConstants.CENTER);
        lblExchangePlaceholder.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblExchangePlaceholder.setForeground(AppColors.TEXT);
        pnlExchangePlaceholder.add(lblExchangePlaceholder, BorderLayout.CENTER);
        pnlContent.add(pnlExchangePlaceholder, "exchange");

        JPanel pnlReturn = new JPanel();
        pnlReturn.setBackground(AppColors.WHITE);
        pnlContent.add(pnlReturn, "return");
        tabInvoiceList = new TAB_InvoiceList();
        pnlContent.add(tabInvoiceList.pnlInvoiceList, "invoicelist");
        cardLayout.show(pnlContent, "selling");
        setActiveButton(btnSalesInvoice);
        return pnlContent;
    }

    private void initializeSalesInvoice() {
        if (!salesInvoiceInitialized) {
            try {
                tabSelling = new TAB_SalesInvoice(currentStaff, shiftChangeListener);
                pnlContent.add(tabSelling.pnlSalesInvoice, "selling");
                salesInvoiceInitialized = true;
            } catch (IllegalStateException e) {
                // User cancelled shift opening - this is OK
                // Keep the placeholder panel
            }
        }
    }

    private void initializeExchangeInvoice() {
        if (!exchangeInvoiceInitialized) {
            try {
                tabExchange = new TAB_ExchangeInvoice(currentStaff);
                pnlContent.add(tabExchange.pnlExchangeInvoice, "exchange");
                exchangeInvoiceInitialized = true;
            } catch (IllegalStateException e) {
                // User cancelled shift opening - this is OK
                // Keep the placeholder panel
            }
        }
    }

    /**
     * Ensures the current visible tab is initialized.
     * This should be called when the invoice menu becomes visible.
     */
    public void ensureCurrentTabInitialized() {
        // If no active shift, don't initialize any tab
        if (!hasActiveShift) {
            return;
        }

        // By default, sales invoice tab is shown, so initialize it
        initializeSalesInvoice();
        if (salesInvoiceInitialized) {
            cardLayout.show(pnlContent, "selling");
        }
    }

    private JPanel createInvoiceButtonNavBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT)); p.setBackground(AppColors.DARK);
        btnSalesInvoice = createStyledButton("Hóa đơn mua"); btnExchangeInvoice = createStyledButton("Hóa đơn đổi"); btnReturnInvoice = createStyledButton("Hóa đơn trả"); btnInvoiceList = createStyledButton("Danh sách hóa đơn");
        btnSalesInvoice.addActionListener(this); btnExchangeInvoice.addActionListener(this); btnReturnInvoice.addActionListener(this); btnInvoiceList.addActionListener(this);

        // Disable all buttons if no active shift
        if (!hasActiveShift) {
            btnSalesInvoice.setEnabled(false);
            btnExchangeInvoice.setEnabled(false);
            btnReturnInvoice.setEnabled(false);
        }

        p.add(btnSalesInvoice); p.add(btnExchangeInvoice); p.add(btnReturnInvoice); p.add(btnInvoiceList);
        return p;
    }

    private void $$$setupUI$$$() {
        pnlInvoiceMenu = new JPanel(); pnlInvoiceMenu.setLayout(new BorderLayout(0, 0));
        pnlInvoiceMenu.setBackground(AppColors.WHITE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Don't allow any action if no active shift
        if (!hasActiveShift) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng mở ca làm việc trước khi bán hàng",
                "Không có ca làm việc",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object src = e.getSource();
        if (src == btnSalesInvoice) {
            initializeSalesInvoice();
            if (salesInvoiceInitialized) {
                setActiveButton(btnSalesInvoice);
                cardLayout.show(pnlContent, "selling");
            }
        } else if (src == btnExchangeInvoice) {
            initializeExchangeInvoice();
            if (exchangeInvoiceInitialized) {
                setActiveButton(btnExchangeInvoice);
                cardLayout.show(pnlContent, "exchange");
            }
        } else if (src == btnReturnInvoice) {
            setActiveButton(btnReturnInvoice);
            cardLayout.show(pnlContent, "return");
        } else if (src == btnInvoiceList) {
            setActiveButton(btnInvoiceList);
            tabInvoiceList.refreshData();
            cardLayout.show(pnlContent, "invoicelist");
        }
    }

    private void setActiveButton(JButton activeButton) {
        btnSalesInvoice.setBackground(AppColors.DARK); btnExchangeInvoice.setBackground(AppColors.DARK); btnReturnInvoice.setBackground(AppColors.DARK); btnInvoiceList.setBackground(AppColors.DARK);
        activeButton.setBackground(AppColors.WHITE);
    }

    public JComponent $$$getRootComponent$$$() { return pnlInvoiceMenu; }

    private JButton createStyledButton(String text) {
        int arc = 12; // Corner radius
        Color pressedColor = AppColors.WHITE, rolloverColor = AppColors.PRIMARY;
        JButton btnStyled = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = getModel();
                Color fill = model.isPressed() ? pressedColor
                        : (model.isRollover() ? rolloverColor : getBackground());
                g2.setColor(fill); g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc); g2.dispose(); super.paintComponent(g);
            }
        };
        btnStyled.setContentAreaFilled(false); btnStyled.setOpaque(false); btnStyled.setBorderPainted(false);
        btnStyled.setFocusPainted(false);  btnStyled.setRolloverEnabled(true);
        btnStyled.setMargin(new Insets(10, 10, 10, 10)); btnStyled.setFont(new Font("Arial", Font.BOLD, 16));
        btnStyled.setForeground(AppColors.TEXT); btnStyled.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); btnStyled.setName(text);
        btnStyled.setBackground(AppColors.DARK);
        return btnStyled;
    }
}