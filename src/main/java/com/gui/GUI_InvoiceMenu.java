package com.gui;

import com.entities.Staff;
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

    public GUI_InvoiceMenu(Staff staff) {
        $$$setupUI$$$();
        this.currentStaff = staff;
        pnlInvoiceMenu.add(createInvoiceButtonNavBar(), BorderLayout.NORTH);
        pnlInvoiceMenu.add(createContentPanel(), BorderLayout.CENTER);
    }

    private JPanel createContentPanel() {
        cardLayout = new CardLayout(); pnlContent = new JPanel(cardLayout);
        pnlContent.setBackground(AppColors.WHITE);
        TAB_Selling tabSelling = new TAB_Selling(currentStaff);
        pnlContent.add(tabSelling.pnlSelling, "selling");
        JPanel pnlExchange = new JPanel(); pnlExchange.setBackground(AppColors.WHITE);
        JPanel pnlReturn = new JPanel(); pnlReturn.setBackground(AppColors.WHITE);
        pnlContent.add(pnlExchange, "exchange"); pnlContent.add(pnlReturn, "return");
        cardLayout.show(pnlContent, "selling");
        setActiveButton(btnSalesInvoice);
        return pnlContent;
    }

    private JPanel createInvoiceButtonNavBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT)); p.setBackground(AppColors.WHITE);
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
            setActiveButton(btnSalesInvoice);
            cardLayout.show(pnlContent, "selling");
        } else if (src == btnExchangeInvoice) {
            setActiveButton(btnExchangeInvoice);
            cardLayout.show(pnlContent, "exchange");
        } else if (src == btnReturnInvoice) {
            setActiveButton(btnReturnInvoice);
            cardLayout.show(pnlContent, "return");
        }
    }

    private void setActiveButton(JButton activeButton) {
        btnSalesInvoice.setBackground(AppColors.WHITE);  btnExchangeInvoice.setBackground(AppColors.WHITE); btnReturnInvoice.setBackground(AppColors.WHITE);
        activeButton.setBackground(AppColors.LIGHT);
    }

    public JComponent $$$getRootComponent$$$() { return pnlInvoiceMenu; }

    private JButton createStyledButton(String text) {
        int arc = 12; // Corner radius
        Color pressedColor = AppColors.LIGHT, rolloverColor = AppColors.BACKGROUND;
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
        btnStyled.setForeground(AppColors.DARK); btnStyled.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); btnStyled.setName(text);
        btnStyled.setBackground(AppColors.WHITE);
        return btnStyled;
    }
}