package com.gui;

import com.entities.Staff;
import com.gui.invoice_options.TAB_ExchangeInvoice;
import com.gui.invoice_options.TAB_InvoiceList;
import com.gui.invoice_options.TAB_SalesInvoice;
import com.utils.AppColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GUI_InvoiceMenu extends JFrame implements ActionListener {
    JPanel pnlInvoiceMenu;
    private JPanel pnlContent;
    private CardLayout cardLayout;
    private JButton btnSalesInvoice, btnExchangeInvoice, btnReturnInvoice, btnInvoiceList;
    private Staff currentStaff;
    private TAB_InvoiceList tabInvoiceList;

    public GUI_InvoiceMenu(Staff staff) {
        $$$setupUI$$$();
        this.currentStaff = staff;
        pnlInvoiceMenu.add(createInvoiceButtonNavBar(), BorderLayout.NORTH);
        pnlInvoiceMenu.add(createContentPanel(), BorderLayout.CENTER);
    }

    private JPanel createContentPanel() {
        cardLayout = new CardLayout(); pnlContent = new JPanel(cardLayout);
        pnlContent.setBackground(AppColors.WHITE);
        TAB_SalesInvoice tabSelling = new TAB_SalesInvoice(currentStaff);
        pnlContent.add(tabSelling.pnlSalesInvoice, "selling");
        TAB_ExchangeInvoice tabExchange = new TAB_ExchangeInvoice(currentStaff);
        pnlContent.add(tabExchange.pnlExchangeInvoice, "exchange");
        JPanel pnlReturn = new JPanel(); pnlReturn.setBackground(AppColors.WHITE);
        pnlContent.add(pnlReturn, "return");
        tabInvoiceList = new TAB_InvoiceList();
        pnlContent.add(tabInvoiceList.pnlInvoiceList, "invoicelist");
        cardLayout.show(pnlContent, "selling");
        setActiveButton(btnSalesInvoice);
        return pnlContent;
    }

    private JPanel createInvoiceButtonNavBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT)); p.setBackground(AppColors.DARK);
        btnSalesInvoice = createStyledButton("Hóa đơn mua"); btnExchangeInvoice = createStyledButton("Hóa đơn đổi"); btnReturnInvoice = createStyledButton("Hóa đơn trả"); btnInvoiceList = createStyledButton("Danh sách HĐ");
        btnSalesInvoice.addActionListener(this); btnExchangeInvoice.addActionListener(this); btnReturnInvoice.addActionListener(this); btnInvoiceList.addActionListener(this);
        p.add(btnSalesInvoice); p.add(btnExchangeInvoice); p.add(btnReturnInvoice); p.add(btnInvoiceList);
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
            setActiveButton(btnSalesInvoice);
            cardLayout.show(pnlContent, "selling");
        } else if (src == btnExchangeInvoice) {
            setActiveButton(btnExchangeInvoice);
            cardLayout.show(pnlContent, "exchange");
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