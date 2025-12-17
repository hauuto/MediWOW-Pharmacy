package com.gui;

import com.entities.Staff;
import com.gui.invoice_options.TAB_ExchangeInvoice;
import com.gui.invoice_options.TAB_SalesInvoice;
import com.interfaces.ShiftChangeListener;
import com.utils.AppColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GUI_InvoiceMenu extends JFrame implements ActionListener {
    JPanel pnlInvoiceMenu;
    private JPanel pnlContent;
    private CardLayout cardLayout;
    private JButton btnSalesInvoice, btnExchangeInvoice, btnReturnInvoice;
    private Staff currentStaff;
    private TAB_SalesInvoice tabSelling;
    private TAB_ExchangeInvoice tabExchange;
    private boolean salesInvoiceInitialized = false;
    private boolean exchangeInvoiceInitialized = false;
    private ShiftChangeListener shiftChangeListener;

    public GUI_InvoiceMenu(Staff staff) {
        $$$setupUI$$$();
        this.currentStaff = staff;
        pnlInvoiceMenu.add(createInvoiceButtonNavBar(), BorderLayout.NORTH);
        pnlInvoiceMenu.add(createContentPanel(), BorderLayout.CENTER);
    }

    public void setShiftChangeListener(ShiftChangeListener listener) {
        this.shiftChangeListener = listener;
    }

    private JPanel createContentPanel() {
        cardLayout = new CardLayout();
        pnlContent = new JPanel(cardLayout);
        pnlContent.setBackground(AppColors.WHITE);

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

        // Default to sales tab but don't initialize yet
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
        // By default, sales invoice tab is shown, so initialize it
        initializeSalesInvoice();
        if (salesInvoiceInitialized) {
            cardLayout.show(pnlContent, "selling");
        }
    }

    private JPanel createInvoiceButtonNavBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT)); p.setBackground(AppColors.DARK);
        btnSalesInvoice = createStyledButton("Hóa đơn mua"); btnExchangeInvoice = createStyledButton("Hóa đơn đổi"); btnReturnInvoice = createStyledButton("Hóa đơn trả");
        btnSalesInvoice.addActionListener(this); btnExchangeInvoice.addActionListener(this); btnReturnInvoice.addActionListener(this);
        p.add(btnSalesInvoice); p.add(btnExchangeInvoice); p.add(btnReturnInvoice);
        return p;
    }

    private void $$$setupUI$$$() {
        pnlInvoiceMenu = new JPanel(); pnlInvoiceMenu.setLayout(new BorderLayout(0, 0));
        pnlInvoiceMenu.setBackground(AppColors.WHITE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
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
        }
    }

    private void setActiveButton(JButton activeButton) {
        btnSalesInvoice.setBackground(AppColors.DARK);  btnExchangeInvoice.setBackground(AppColors.DARK); btnReturnInvoice.setBackground(AppColors.DARK);
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