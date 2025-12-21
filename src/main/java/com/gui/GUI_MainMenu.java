package com.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.interfaces.DataChangeListener;
import com.utils.AppColors;
import com.entities.Staff;
import com.entities.Shift;
import com.bus.BUS_Shift;
import com.interfaces.ShiftChangeListener;
import com.bus.BUS_Product;
import com.bus.BUS_Customer;
import com.bus.BUS_Invoice;
import com.entities.Product;
import com.entities.PrescribedCustomer;
import com.entities.Invoice;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;
import java.util.regex.Pattern;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;

public class GUI_MainMenu implements ActionListener, ShiftChangeListener {
    JPanel pnlMainMenu;
    private JPanel pnlLeftHeader;
    private JLabel lblLogo;
    private JPanel pnlMenu;
    private JPanel pnlMain;
    private JPanel pnlRightHeader;
    private JButton btnHome;
    private JButton btnSales;
    private JPanel pnlOption;
    private JComboBox cbbOption;
    private JButton btnProduct;
    private JButton btnPromotion;
    private JButton btnStatistics;
    private JButton btnStaff;
    private JButton btnGuideLine;
    private JButton btnLogout;
    private JButton btnCustomer;
    private JLabel lblTime;
    private JPanel pnlSearch;
    private JTextField txtSearch;
    private JLabel lblSearch;
    private JButton btnShift;
    private CardLayout cardLayout;
    private Staff currentStaff;
    private BUS_Shift busShift;
    private Shift currentShift;
    private GUI_InvoiceMenu invoiceMenu;
    private ShiftChangeListener shiftChangeListener;
    private TAB_Dashboard dashboard;
    // Buses for omni-search
    private final BUS_Product productBUS = new BUS_Product();
    private final BUS_Customer customerBUS = new BUS_Customer();
    private final BUS_Invoice invoiceBUS = new BUS_Invoice();

    private JPopupMenu searchPopup = new JPopupMenu();
    private volatile long lastSearchAt = 0L;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10,11}$");
    private static final Pattern INVOICE_PREFIX = Pattern.compile("^(HD|INV).*", Pattern.CASE_INSENSITIVE);


    /**
     * @author Tô Thanh Hậu
     */

    public GUI_MainMenu(Staff staff) {
        this.currentStaff = staff;
        this.busShift = new BUS_Shift();

        // Attach simple document listener for search field with debounce
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void schedule() {
                lastSearchAt = System.currentTimeMillis();
                // small debounce
                new Thread(() -> {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ignored) {
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastSearchAt >= 240) {
                        String text = txtSearch.getText();
                        SwingUtilities.invokeLater(() -> performGlobalSearch(text));
                    }
                }).start();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                schedule();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                schedule();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                schedule();
            }
        });

        pnlSearch.setBackground(AppColors.LIGHT);
        pnlLeftHeader.setBackground(AppColors.LIGHT);
        pnlRightHeader.setBackground(AppColors.LIGHT);
        pnlOption.setBackground(AppColors.LIGHT);


        cardLayout = (CardLayout) pnlMain.getLayout();


        btnHome.addActionListener(this);
        btnSales.addActionListener(this);
        btnProduct.addActionListener(this);
        btnPromotion.addActionListener(this);
        btnStatistics.addActionListener(this);
        btnStaff.addActionListener(this);
        btnGuideLine.addActionListener(this);
        btnLogout.addActionListener(this);
        btnCustomer.addActionListener(this);

        // Check and update shift status

        //testing rules


        dashboard = new TAB_Dashboard(currentStaff);
        invoiceMenu = new GUI_InvoiceMenu(currentStaff);
        shiftChangeListener = invoiceMenu; // Set invoiceMenu as the shift change listener

        // Set this GUI_MainMenu as the listener for dashboard shift changes
        // so they can be forwarded to the invoice menu
        dashboard.setShiftChangeListener(this);

        // Connect dashboard as DataChangeListener to invoiceMenu
        // so dashboard refreshes immediately when data changes
        invoiceMenu.setDataChangeListener((DataChangeListener) dashboard);

        TAB_Promotion promotion = new TAB_Promotion();
        TAB_Statistics statistic = new TAB_Statistics(currentStaff);
        TAB_Product product = new TAB_Product();
        TAB_Staff staffTab = new TAB_Staff();
        TAB_Customer customer = new TAB_Customer();

        pnlMain.add(dashboard, "dashboard"); // Fixed: dashboard is already a JPanel
        pnlMain.add(invoiceMenu.pnlInvoiceMenu, "invoiceMenu");
        pnlMain.add(promotion, "promotion");
        pnlMain.add(statistic, "statistic"); // Fixed: statistic extends JPanel, add directly
        pnlMain.add(product.pProduct, "product");
        pnlMain.add(staffTab.pnlStaff, "staff");
        pnlMain.add(customer.pCustomer, "customer");

        // Update combobox with staff name BEFORE adding listener
        if (currentStaff != null) {
            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) cbbOption.getModel();
            model.removeElementAt(0);
            model.insertElementAt("Xin chào, " + currentStaff.getFullName(), 0);
            cbbOption.setSelectedIndex(0);
        }

        // Add ActionListener AFTER updating the combobox to prevent triggering during initialization
        cbbOption.addActionListener(e -> {
            if (cbbOption.getSelectedIndex() == 1) {
                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(pnlMainMenu);
                DIALOG_ChangePassword changePasswordDialog = new DIALOG_ChangePassword(parentFrame, currentStaff);
                changePasswordDialog.setVisible(true);
                cbbOption.setSelectedIndex(0);
            }
        });

        setActiveButton(btnHome);
        cardLayout.show(pnlMain, "dashboard");
        if (dashboard != null) {
            // Use data-change hook as the canonical refresh trigger
            dashboard.onDataChanged();
        }

        Locale locale = Locale.of("vi", "VN");
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss EEEE, dd/MM/yyyy", locale);
        Timer timer = new Timer(1000, e -> {
            LocalDateTime now = LocalDateTime.now();
            lblTime.setText(timeFormat.format(now));
        });
        timer.start();

        // Make search popup non-focusable and ensure txtSearch regains focus when popup appears
        searchPopup.setFocusable(false);
        searchPopup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // re-request focus to the search field when the popup is shown
                SwingUtilities.invokeLater(() -> {
                    if (txtSearch != null) txtSearch.requestFocusInWindow();
                });
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { /* no-op */ }
            @Override public void popupMenuCanceled(PopupMenuEvent e) { /* no-op */ }
        });

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == btnHome) {
            setActiveButton(btnHome);
            cardLayout.show(pnlMain, "dashboard");
            if (dashboard != null) {
                dashboard.onDataChanged();
            }
        } else if (src == btnSales) {
            setActiveButton(btnSales);
            cardLayout.show(pnlMain, "invoiceMenu");
            // Ensure the current tab in invoice menu is initialized
            invoiceMenu.ensureCurrentTabInitialized();

        } else if (src == btnProduct) {
            setActiveButton(btnProduct);
            cardLayout.show(pnlMain, "product");

        } else if (src == btnPromotion) {
            setActiveButton(btnPromotion);
            cardLayout.show(pnlMain, "promotion");

        } else if (src == btnStatistics) {
            setActiveButton(btnStatistics);
            cardLayout.show(pnlMain, "statistic");

        } else if (src == btnStaff) {
            setActiveButton(btnStaff);
            cardLayout.show(pnlMain, "staff");

        } else if (src == btnGuideLine) {
            setActiveButton(btnGuideLine);

        } else if (src == btnLogout) {
            // Check if there's an open shift on this workstation
            String workstation = busShift.getCurrentWorkstation();
            Shift openShift = busShift.getOpenShiftOnWorkstation(workstation);

            if (openShift != null) {
                // Check if it's current staff's shift or another staff's shift
                boolean isOwnShift = currentStaff != null &&
                        openShift.getStaff() != null &&
                        openShift.getStaff().getId().equals(currentStaff.getId());

                String warningMessage = isOwnShift ?
                        "CẢNH BÁO: Bạn đang có ca làm việc chưa đóng!\n\n" +
                                "Vui lòng chọn một trong các hành động sau:" :
                        "CẢNH BÁO: Máy này đang có ca mở bởi nhân viên " + openShift.getStaff().getFullName() + "!\n\n" +
                                "Vui lòng chọn một trong các hành động sau:";

                // Show warning with 3 options: Close Shift, Logout Anyway, Cancel
                Object[] options = {"Đóng ca", "Đăng xuất", "Hủy"};
                int choice = JOptionPane.showOptionDialog(
                        pnlMainMenu,
                        warningMessage,
                        "Xác nhận đăng xuất",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[0]); // Default to "Đóng ca"

                if (choice == 0) {
                    // Close shift first
                    JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(pnlMainMenu);
                    DIALOG_CloseShift closeShiftDialog = new DIALOG_CloseShift(parentFrame, openShift, currentStaff);
                    closeShiftDialog.setVisible(true);

                    // If shift was closed successfully, ask again to logout
                    if (closeShiftDialog.isConfirmed()) {
                        Shift closedShift = currentShift;
                        currentShift = null;

                        // Notify listener that shift was closed
                        if (shiftChangeListener != null) {
                            shiftChangeListener.onShiftClosed(closedShift);
                        }

                        int logoutChoice = JOptionPane.showConfirmDialog(
                                pnlMainMenu,
                                "Ca làm việc đã được đóng thành công!\n\nBạn có muốn đăng xuất không?",
                                "Xác nhận đăng xuất",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);

                        if (logoutChoice == JOptionPane.YES_OPTION) {
                            // Close current main menu window
                            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(pnlMainMenu);
                            topFrame.dispose();

                            // Open login window
                            JFrame loginFrame = new JFrame("MediWOW - Đăng nhập");
                            loginFrame.setContentPane(new GUI_Login().pnlLogin);
                            loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            loginFrame.setSize(1080, 600);
                            loginFrame.setLocationRelativeTo(null);
                            loginFrame.setResizable(false);
                            loginFrame.setVisible(true);
                        }
                    }
                } else if (choice == 1) {
                    // Logout without closing shift
                    // Close current main menu window
                    JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(pnlMainMenu);
                    topFrame.dispose();

                    // Open login window
                    JFrame loginFrame = new JFrame("MediWOW - Đăng nhập");
                    loginFrame.setContentPane(new GUI_Login().pnlLogin);
                    loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    loginFrame.setSize(1080, 600);
                    loginFrame.setLocationRelativeTo(null);
                    loginFrame.setResizable(false);
                    loginFrame.setVisible(true);
                }
                // If choice == 2 (Hủy) or dialog closed, do nothing
            } else {
                // No open shift, normal logout confirmation
                int choice = JOptionPane.showConfirmDialog(
                        pnlMainMenu,
                        "Bạn có chắc chắn muốn đăng xuất không?",
                        "Xác nhận đăng xuất",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    // Close current main menu window
                    JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(pnlMainMenu);
                    topFrame.dispose();

                    // Open login window
                    JFrame loginFrame = new JFrame("MediWOW - Đăng nhập");
                    loginFrame.setContentPane(new GUI_Login().pnlLogin);
                    loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    loginFrame.setSize(1080, 600);
                    loginFrame.setLocationRelativeTo(null);
                    loginFrame.setResizable(false);
                    loginFrame.setVisible(true);
                }
            }

        } else if (src == btnCustomer) {
            setActiveButton(btnCustomer);
            cardLayout.show(pnlMain, "customer");

        }

    }

    private void setActiveButton(JButton activeButton) {

        for (Component comp : pnlMenu.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                setStyleButton(btn);
            }
        }

        activeButton.setBackground(AppColors.LIGHT);

    }

    private void setStyleButton(JButton button) {
        button.setBackground(AppColors.BACKGROUND);
        button.setBorderPainted(false);
    }

    // Implement ShiftChangeListener methods
    @Override
    public void onShiftOpened(Shift shift) {
        currentShift = shift;

        // Forward event to invoice menu
        if (invoiceMenu != null) {
            invoiceMenu.onShiftOpened(shift);
        }

        // Don't show message here - DIALOG_OpenShift already shows success message
    }

    @Override
    public void onShiftClosed(Shift shift) {
        currentShift = null;

        // Forward event to invoice menu
        if (invoiceMenu != null) {
            invoiceMenu.onShiftClosed(shift);
        }

        // Don't show message here - DIALOG_CloseShift already shows success message
    }

    private void performGlobalSearch(String text) {
        // Trim and check if the search text is empty
        String searchText = text != null ? text.trim() : "";
        if (searchText.isEmpty()) {
            // If search text is empty, dispose the popup and return
            searchPopup.setVisible(false);
            return;
        }

        // For debugging: print the search text
        System.out.println("Searching for: " + searchText);

        // Determine if the search text is a phone number or an invoice code
        boolean isPhoneNumber = PHONE_PATTERN.matcher(searchText).matches();
        boolean isInvoiceCode = INVOICE_PREFIX.matcher(searchText).matches();

        // For debugging: print the detection results
        System.out.println("Is phone number: " + isPhoneNumber);
        System.out.println("Is invoice code: " + isInvoiceCode);

        // Clear previous items in the popup
        searchPopup.removeAll();

        // Helper to load icons (fall back to null)
        Icon productIcon = null;
        Icon customerIcon = null;
        Icon invoiceIcon = null;
        try {
            productIcon = new ImageIcon(getClass().getResource("/icons/btn_product.png"));
            customerIcon = new ImageIcon(getClass().getResource("/icons/btn_customer.png"));
            invoiceIcon = new ImageIcon(getClass().getResource("/icons/btn_selling.png"));
        } catch (Exception ex) {
            // ignore missing icons
        }

        int totalAdded = 0;

        // If input looks like phone number -> query customers
        if (isPhoneNumber) {
            List<PrescribedCustomer> customers = customerBUS.searchTop5ByNameOrPhone(searchText);
            if (customers != null && !customers.isEmpty()) {
                for (PrescribedCustomer c : customers) {
                    String label = String.format("%s - %s", c.getName(), c.getPhoneNumber() == null ? "" : c.getPhoneNumber());
                    JMenuItem item = new JMenuItem(label, customerIcon);
                    item.setFocusable(false);
                    item.addActionListener(e -> {
                        // Fallback: open customer tab and show basic info
                        SwingUtilities.invokeLater(() -> {
                            try {
                                setActiveButton(btnCustomer);
                                cardLayout.show(pnlMain, "customer");
                            } catch (Exception ex) { /* ignore if UI not ready */ }
                            String info = c.getName() + (c.getPhoneNumber() != null && !c.getPhoneNumber().isEmpty() ? "\nSĐT: " + c.getPhoneNumber() : "");
                            JOptionPane.showMessageDialog(pnlMainMenu, info, "Khách hàng", JOptionPane.INFORMATION_MESSAGE);
                        });
                    });
                    searchPopup.add(item);
                    totalAdded++;
                }
            }
            // Also search invoices by id that may contain phone (rare) and products as fallback
            List<Invoice> invoices = invoiceBUS.searchTop5ById(searchText);
            if (invoices != null && !invoices.isEmpty()) {
                for (Invoice inv : invoices) {
                    String who = inv.getPrescribedCustomer() != null ? inv.getPrescribedCustomer().getName() : (inv.getCreator() != null ? inv.getCreator().getFullName() : "");
                    JMenuItem item = new JMenuItem("Hóa đơn: " + inv.getId() + " - " + who, invoiceIcon);
                    item.setFocusable(false);
                    final String whoDisplay = who;
                    item.addActionListener(e -> {
                        // Fallback: switch to invoice menu and show basic info
                        SwingUtilities.invokeLater(() -> {
                            try {
                                setActiveButton(btnSales);
                                cardLayout.show(pnlMain, "invoiceMenu");
                            } catch (Exception ex) { /* ignore */ }
                            String info = "Hóa đơn: " + inv.getId() + (whoDisplay != null && !whoDisplay.isEmpty() ? "\nKhách: " + whoDisplay : "");
                            JOptionPane.showMessageDialog(pnlMainMenu, info, "Hóa đơn", JOptionPane.INFORMATION_MESSAGE);
                        });
                    });
                    searchPopup.add(item);
                    totalAdded++;
                }
            }

            List<Product> products = productBUS.searchTop5ByNameOrBarcode(searchText);
            if (products != null && !products.isEmpty()) {
                for (Product p : products) {
                    String label = String.format("%s - %s", p.getName(), p.getBarcode() == null ? "" : p.getBarcode());
                    JMenuItem item = new JMenuItem(label, productIcon);
                    item.setFocusable(false);
                    item.addActionListener(e -> {
                        // Fallback: open product tab and show basic info
                        SwingUtilities.invokeLater(() -> {
                            try {
                                setActiveButton(btnProduct);
                                cardLayout.show(pnlMain, "product");
                            } catch (Exception ex) { /* ignore */ }
                            String info = p.getName() + (p.getBarcode() != null && !p.getBarcode().isEmpty() ? "\nMã vạch: " + p.getBarcode() : "");
                            JOptionPane.showMessageDialog(pnlMainMenu, info, "Sản phẩm", JOptionPane.INFORMATION_MESSAGE);
                        });
                    });
                    searchPopup.add(item);
                    totalAdded++;
                }
            }

        } else if (isInvoiceCode) {
            // Only query invoices
            List<Invoice> invoices = invoiceBUS.searchTop5ById(searchText);
            if (invoices != null && !invoices.isEmpty()) {
                for (Invoice inv : invoices) {
                    String who = inv.getPrescribedCustomer() != null ? inv.getPrescribedCustomer().getName() : (inv.getCreator() != null ? inv.getCreator().getFullName() : "");
                    JMenuItem item = new JMenuItem("Hóa đơn: " + inv.getId() + " - " + who, invoiceIcon);
                    item.setFocusable(false);
                    final String whoDisplay = who;
                    item.addActionListener(e -> {
                        // Fallback: switch to invoice menu and show basic info
                        SwingUtilities.invokeLater(() -> {
                            try {
                                setActiveButton(btnSales);
                                cardLayout.show(pnlMain, "invoiceMenu");
                            } catch (Exception ex) { /* ignore */ }
                            String info = "Hóa đơn: " + inv.getId() + (whoDisplay != null && !whoDisplay.isEmpty() ? "\nKhách: " + whoDisplay : "");
                            JOptionPane.showMessageDialog(pnlMainMenu, info, "Hóa đơn", JOptionPane.INFORMATION_MESSAGE);
                        });
                    });
                    searchPopup.add(item);
                    totalAdded++;
                }
            }
        } else {
            // Default: search products and customers in parallel (sequentially here)
            List<Product> products = productBUS.searchTop5ByNameOrBarcode(searchText);
            if (products != null && !products.isEmpty()) {
                for (Product p : products) {
                    String label = String.format("%s - %s", p.getName(), p.getBarcode() == null ? "" : p.getBarcode());
                    JMenuItem item = new JMenuItem(label, productIcon);
                    item.setFocusable(false);
                    item.addActionListener(e -> {
                        // Fallback: open product tab and show basic info
                        SwingUtilities.invokeLater(() -> {
                            try {
                                setActiveButton(btnProduct);
                                cardLayout.show(pnlMain, "product");
                            } catch (Exception ex) { /* ignore */ }
                            String info = p.getName() + (p.getBarcode() != null && !p.getBarcode().isEmpty() ? "\nMã vạch: " + p.getBarcode() : "");
                            JOptionPane.showMessageDialog(pnlMainMenu, info, "Sản phẩm", JOptionPane.INFORMATION_MESSAGE);
                        });
                    });
                    searchPopup.add(item);
                    totalAdded++;
                }
            }

            List<PrescribedCustomer> customers = customerBUS.searchTop5ByNameOrPhone(searchText);
            if (customers != null && !customers.isEmpty()) {
                for (PrescribedCustomer c : customers) {
                    String label = String.format("%s - %s", c.getName(), c.getPhoneNumber() == null ? "" : c.getPhoneNumber());
                    JMenuItem item = new JMenuItem(label, customerIcon);
                    item.setFocusable(false);
                    item.addActionListener(e -> {
                        // Fallback: open customer tab and show basic info
                        SwingUtilities.invokeLater(() -> {
                            try {
                                setActiveButton(btnCustomer);
                                cardLayout.show(pnlMain, "customer");
                            } catch (Exception ex) { /* ignore if UI not ready */ }
                            String info = c.getName() + (c.getPhoneNumber() != null && !c.getPhoneNumber().isEmpty() ? "\nSĐT: " + c.getPhoneNumber() : "");
                            JOptionPane.showMessageDialog(pnlMainMenu, info, "Khách hàng", JOptionPane.INFORMATION_MESSAGE);
                        });
                    });
                    searchPopup.add(item);
                    totalAdded++;
                }
            }
        }

        if (totalAdded == 0) {
            JMenuItem none = new JMenuItem("Không có kết quả");
            none.setEnabled(false);
            none.setFocusable(false);
            searchPopup.add(none);
        }

        // Show the popup below the search field
        searchPopup.show(txtSearch, 0, txtSearch.getHeight());
        // Ensure the text field retains focus (popup may attempt to steal it)
        SwingUtilities.invokeLater(() -> {
            if (txtSearch != null) txtSearch.requestFocusInWindow();
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
        pnlMainMenu = new JPanel();
        pnlMainMenu.setLayout(new GridBagLayout());
        pnlMainMenu.setBackground(new Color(-2236963));
        pnlMainMenu.setPreferredSize(new Dimension(1920, 1080));
        pnlLeftHeader = new JPanel();
        pnlLeftHeader.setLayout(new BorderLayout(0, 0));
        pnlLeftHeader.setBackground(new Color(-16724789));
        pnlLeftHeader.setForeground(new Color(-16012317));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        pnlMainMenu.add(pnlLeftHeader, gbc);
        lblLogo = new JLabel();
        lblLogo.setBackground(new Color(-2236963));
        lblLogo.setIcon(new ImageIcon(getClass().getResource("/images/logo.png")));
        lblLogo.setText("");
        pnlLeftHeader.add(lblLogo, BorderLayout.CENTER);
        pnlMenu = new JPanel();
        pnlMenu.setLayout(new GridBagLayout());
        pnlMenu.setAlignmentY(0.5f);
        pnlMenu.setBackground(new Color(-2236963));
        pnlMenu.setForeground(new Color(-16027943));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        pnlMainMenu.add(pnlMenu, gbc);
        btnHome = new JButton();
        btnHome.setAlignmentY(0.5f);
        btnHome.setAutoscrolls(false);
        btnHome.setBackground(new Color(-1));
        btnHome.setBorderPainted(true);
        btnHome.setContentAreaFilled(true);
        btnHome.setDefaultCapable(true);
        btnHome.setDoubleBuffered(false);
        btnHome.setEnabled(true);
        btnHome.setFocusCycleRoot(false);
        btnHome.setFocusPainted(true);
        Font btnHomeFont = this.$$$getFont$$$("Segoe UI", Font.BOLD, 16, btnHome.getFont());
        if (btnHomeFont != null) btnHome.setFont(btnHomeFont);
        btnHome.setForeground(new Color(-16027943));
        btnHome.setHideActionText(false);
        btnHome.setHorizontalAlignment(2);
        btnHome.setIcon(new ImageIcon(getClass().getResource("/icons/btn_home.png")));
        btnHome.setIconTextGap(10);
        btnHome.setMargin(new Insets(0, 10, 0, 0));
        btnHome.setPreferredSize(new Dimension(180, 50));
        btnHome.setRolloverEnabled(true);
        btnHome.setSelected(false);
        btnHome.setText("Màn hình chính");
        btnHome.setToolTipText("Nhấp để quay về trang chủ");
        btnHome.putClientProperty("hideActionText", Boolean.FALSE);
        btnHome.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        pnlMenu.add(btnHome, gbc);
        btnSales = new JButton();
        btnSales.setAlignmentY(0.5f);
        btnSales.setAutoscrolls(false);
        btnSales.setBackground(new Color(-1));
        btnSales.setBorderPainted(true);
        btnSales.setContentAreaFilled(true);
        btnSales.setDefaultCapable(true);
        btnSales.setDoubleBuffered(false);
        btnSales.setEnabled(true);
        btnSales.setFocusCycleRoot(false);
        btnSales.setFocusPainted(true);
        Font btnSalesFont = this.$$$getFont$$$("Segoe UI", Font.BOLD, 16, btnSales.getFont());
        if (btnSalesFont != null) btnSales.setFont(btnSalesFont);
        btnSales.setForeground(new Color(-16027943));
        btnSales.setHideActionText(false);
        btnSales.setHorizontalAlignment(2);
        btnSales.setIcon(new ImageIcon(getClass().getResource("/icons/btn_selling.png")));
        btnSales.setIconTextGap(10);
        btnSales.setMargin(new Insets(0, 10, 0, 0));
        btnSales.setPreferredSize(new Dimension(150, 50));
        btnSales.setRolloverEnabled(true);
        btnSales.setSelected(false);
        btnSales.setText("Đơn hàng");
        btnSales.setToolTipText("Nhấp vào để bắt đầu bán hàng");
        btnSales.putClientProperty("hideActionText", Boolean.FALSE);
        btnSales.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        pnlMenu.add(btnSales, gbc);
        btnProduct = new JButton();
        btnProduct.setAlignmentY(0.5f);
        btnProduct.setAutoscrolls(false);
        btnProduct.setBackground(new Color(-1));
        btnProduct.setBorderPainted(true);
        btnProduct.setContentAreaFilled(true);
        btnProduct.setDefaultCapable(true);
        btnProduct.setDoubleBuffered(false);
        btnProduct.setEnabled(true);
        btnProduct.setFocusCycleRoot(false);
        btnProduct.setFocusPainted(true);
        Font btnProductFont = this.$$$getFont$$$("Segoe UI", Font.BOLD, 16, btnProduct.getFont());
        if (btnProductFont != null) btnProduct.setFont(btnProductFont);
        btnProduct.setForeground(new Color(-16027943));
        btnProduct.setHideActionText(false);
        btnProduct.setHorizontalAlignment(2);
        btnProduct.setIcon(new ImageIcon(getClass().getResource("/icons/btn_product.png")));
        btnProduct.setIconTextGap(10);
        btnProduct.setMargin(new Insets(0, 10, 0, 0));
        btnProduct.setPreferredSize(new Dimension(150, 50));
        btnProduct.setRolloverEnabled(true);
        btnProduct.setSelected(false);
        btnProduct.setText("Sản phẩm");
        btnProduct.setToolTipText("Nhấp để quản lý sản phẩm");
        btnProduct.putClientProperty("hideActionText", Boolean.FALSE);
        btnProduct.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        pnlMenu.add(btnProduct, gbc);
        btnPromotion = new JButton();
        btnPromotion.setAlignmentY(0.5f);
        btnPromotion.setAutoscrolls(false);
        btnPromotion.setBackground(new Color(-1));
        btnPromotion.setBorderPainted(true);
        btnPromotion.setContentAreaFilled(true);
        btnPromotion.setDefaultCapable(true);
        btnPromotion.setDoubleBuffered(false);
        btnPromotion.setEnabled(true);
        btnPromotion.setFocusCycleRoot(false);
        btnPromotion.setFocusPainted(true);
        Font btnPromotionFont = this.$$$getFont$$$("Segoe UI", Font.BOLD, 16, btnPromotion.getFont());
        if (btnPromotionFont != null) btnPromotion.setFont(btnPromotionFont);
        btnPromotion.setForeground(new Color(-16027943));
        btnPromotion.setHideActionText(false);
        btnPromotion.setHorizontalAlignment(2);
        btnPromotion.setIcon(new ImageIcon(getClass().getResource("/icons/btn_promotion.png")));
        btnPromotion.setIconTextGap(10);
        btnPromotion.setMargin(new Insets(0, 10, 0, 0));
        btnPromotion.setPreferredSize(new Dimension(150, 50));
        btnPromotion.setRolloverEnabled(true);
        btnPromotion.setSelected(false);
        btnPromotion.setText("Khuyến mại");
        btnPromotion.setToolTipText("Nhấp vào để quản lý khuyến mại");
        btnPromotion.putClientProperty("hideActionText", Boolean.FALSE);
        btnPromotion.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        pnlMenu.add(btnPromotion, gbc);
        btnStatistics = new JButton();
        btnStatistics.setAlignmentY(0.5f);
        btnStatistics.setAutoscrolls(false);
        btnStatistics.setBackground(new Color(-1));
        btnStatistics.setBorderPainted(true);
        btnStatistics.setContentAreaFilled(true);
        btnStatistics.setDefaultCapable(true);
        btnStatistics.setDoubleBuffered(false);
        btnStatistics.setEnabled(true);
        btnStatistics.setFocusCycleRoot(false);
        btnStatistics.setFocusPainted(true);
        Font btnStatisticsFont = this.$$$getFont$$$("Segoe UI", Font.BOLD, 16, btnStatistics.getFont());
        if (btnStatisticsFont != null) btnStatistics.setFont(btnStatisticsFont);
        btnStatistics.setForeground(new Color(-16027943));
        btnStatistics.setHideActionText(false);
        btnStatistics.setHorizontalAlignment(2);
        btnStatistics.setIcon(new ImageIcon(getClass().getResource("/icons/btn_statistic.png")));
        btnStatistics.setIconTextGap(10);
        btnStatistics.setMargin(new Insets(0, 10, 0, 0));
        btnStatistics.setPreferredSize(new Dimension(150, 50));
        btnStatistics.setRolloverEnabled(true);
        btnStatistics.setSelected(false);
        btnStatistics.setText("Thống kê");
        btnStatistics.setToolTipText("Nhấp vào để bắt đầu thống kê");
        btnStatistics.putClientProperty("hideActionText", Boolean.FALSE);
        btnStatistics.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        pnlMenu.add(btnStatistics, gbc);
        btnStaff = new JButton();
        btnStaff.setAlignmentY(0.5f);
        btnStaff.setAutoscrolls(false);
        btnStaff.setBackground(new Color(-1));
        btnStaff.setBorderPainted(true);
        btnStaff.setContentAreaFilled(true);
        btnStaff.setDefaultCapable(true);
        btnStaff.setDoubleBuffered(false);
        btnStaff.setEnabled(true);
        btnStaff.setFocusCycleRoot(false);
        btnStaff.setFocusPainted(true);
        Font btnStaffFont = this.$$$getFont$$$("Segoe UI", Font.BOLD, 16, btnStaff.getFont());
        if (btnStaffFont != null) btnStaff.setFont(btnStaffFont);
        btnStaff.setForeground(new Color(-16027943));
        btnStaff.setHideActionText(false);
        btnStaff.setHorizontalAlignment(2);
        btnStaff.setIcon(new ImageIcon(getClass().getResource("/icons/btn_staff.png")));
        btnStaff.setIconTextGap(10);
        btnStaff.setMargin(new Insets(0, 10, 0, 0));
        btnStaff.setPreferredSize(new Dimension(150, 50));
        btnStaff.setRolloverEnabled(true);
        btnStaff.setSelected(false);
        btnStaff.setText("Nhân viên");
        btnStaff.setToolTipText("Nhấp vào để quản lý thông tin nhân viên");
        btnStaff.putClientProperty("hideActionText", Boolean.FALSE);
        btnStaff.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        pnlMenu.add(btnStaff, gbc);
        btnGuideLine = new JButton();
        btnGuideLine.setAlignmentY(0.5f);
        btnGuideLine.setAutoscrolls(false);
        btnGuideLine.setBackground(new Color(-1));
        btnGuideLine.setBorderPainted(true);
        btnGuideLine.setContentAreaFilled(true);
        btnGuideLine.setDefaultCapable(true);
        btnGuideLine.setDoubleBuffered(false);
        btnGuideLine.setEnabled(true);
        btnGuideLine.setFocusCycleRoot(false);
        btnGuideLine.setFocusPainted(true);
        Font btnGuideLineFont = this.$$$getFont$$$("Segoe UI", Font.BOLD, 16, btnGuideLine.getFont());
        if (btnGuideLineFont != null) btnGuideLine.setFont(btnGuideLineFont);
        btnGuideLine.setForeground(new Color(-16027943));
        btnGuideLine.setHideActionText(false);
        btnGuideLine.setHorizontalAlignment(2);
        btnGuideLine.setIcon(new ImageIcon(getClass().getResource("/icons/btn_help.png")));
        btnGuideLine.setIconTextGap(10);
        btnGuideLine.setMargin(new Insets(0, 10, 0, 0));
        btnGuideLine.setPreferredSize(new Dimension(150, 50));
        btnGuideLine.setRolloverEnabled(true);
        btnGuideLine.setSelected(false);
        btnGuideLine.setText("Hướng dẫn");
        btnGuideLine.setToolTipText("Nhấp vào để xem hướng dẫn");
        btnGuideLine.putClientProperty("hideActionText", Boolean.FALSE);
        btnGuideLine.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 50, 0);
        pnlMenu.add(btnGuideLine, gbc);
        btnLogout = new JButton();
        btnLogout.setAlignmentY(0.5f);
        btnLogout.setAutoscrolls(false);
        btnLogout.setBackground(new Color(-1));
        btnLogout.setBorderPainted(true);
        btnLogout.setContentAreaFilled(true);
        btnLogout.setDefaultCapable(true);
        btnLogout.setDoubleBuffered(false);
        btnLogout.setEnabled(true);
        btnLogout.setFocusCycleRoot(false);
        btnLogout.setFocusPainted(true);
        Font btnLogoutFont = this.$$$getFont$$$("Segoe UI", Font.BOLD, 16, btnLogout.getFont());
        if (btnLogoutFont != null) btnLogout.setFont(btnLogoutFont);
        btnLogout.setForeground(new Color(-16027943));
        btnLogout.setHideActionText(false);
        btnLogout.setHorizontalAlignment(2);
        btnLogout.setIcon(new ImageIcon(getClass().getResource("/icons/btn_logout.png")));
        btnLogout.setIconTextGap(10);
        btnLogout.setMargin(new Insets(0, 10, 0, 0));
        btnLogout.setPreferredSize(new Dimension(150, 50));
        btnLogout.setRolloverEnabled(true);
        btnLogout.setSelected(false);
        btnLogout.setText("Đăng xuất");
        btnLogout.setToolTipText("Nhấp vào để đăng xuất");
        btnLogout.putClientProperty("hideActionText", Boolean.FALSE);
        btnLogout.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(150, 0, 20, 0);
        pnlMenu.add(btnLogout, gbc);
        btnCustomer = new JButton();
        btnCustomer.setAlignmentY(0.5f);
        btnCustomer.setAutoscrolls(false);
        btnCustomer.setBackground(new Color(-1));
        btnCustomer.setBorderPainted(true);
        btnCustomer.setContentAreaFilled(true);
        btnCustomer.setDefaultCapable(true);
        btnCustomer.setDoubleBuffered(false);
        btnCustomer.setEnabled(true);
        btnCustomer.setFocusCycleRoot(false);
        btnCustomer.setFocusPainted(true);
        Font btnCustomerFont = this.$$$getFont$$$("Segoe UI", Font.BOLD, 16, btnCustomer.getFont());
        if (btnCustomerFont != null) btnCustomer.setFont(btnCustomerFont);
        btnCustomer.setForeground(new Color(-16027943));
        btnCustomer.setHideActionText(false);
        btnCustomer.setHorizontalAlignment(2);
        btnCustomer.setIcon(new ImageIcon(getClass().getResource("/icons/btn_customer.png")));
        btnCustomer.setIconTextGap(10);
        btnCustomer.setMargin(new Insets(0, 10, 0, 0));
        btnCustomer.setPreferredSize(new Dimension(150, 50));
        btnCustomer.setRolloverEnabled(true);
        btnCustomer.setSelected(false);
        btnCustomer.setText("Khách hàng");
        btnCustomer.setToolTipText("Nhấp vào để quản lý thông tin khách hàng");
        btnCustomer.putClientProperty("hideActionText", Boolean.FALSE);
        btnCustomer.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        pnlMenu.add(btnCustomer, gbc);
        pnlMain = new JPanel();
        pnlMain.setLayout(new CardLayout(0, 0));
        pnlMain.setBackground(new Color(-2236963));
        pnlMain.setEnabled(true);
        pnlMain.putClientProperty("html.disable", Boolean.FALSE);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 10.0;
        gbc.weighty = 100.0;
        gbc.fill = GridBagConstraints.BOTH;
        pnlMainMenu.add(pnlMain, gbc);
        pnlRightHeader = new JPanel();
        pnlRightHeader.setLayout(new BorderLayout(0, 0));
        pnlRightHeader.setBackground(new Color(-16724789));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        pnlMainMenu.add(pnlRightHeader, gbc);
        pnlOption = new JPanel();
        pnlOption.setLayout(new GridLayoutManager(1, 1, new Insets(0, 50, 0, 50), -1, 30));
        pnlOption.setBackground(new Color(-16724789));
        pnlRightHeader.add(pnlOption, BorderLayout.EAST);
        cbbOption = new JComboBox();
        cbbOption.setAlignmentX(0.5f);
        cbbOption.setAlignmentY(0.5f);
        cbbOption.setBackground(new Color(-1));
        cbbOption.setEditable(false);
        cbbOption.setEnabled(true);
        Font cbbOptionFont = this.$$$getFont$$$(null, -1, 16, cbbOption.getFont());
        if (cbbOptionFont != null) cbbOption.setFont(cbbOptionFont);
        cbbOption.setForeground(new Color(-16012317));
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Xin chào, Tô Thanh Hậu");
        defaultComboBoxModel1.addElement("Đổi mật khẩu");
        cbbOption.setModel(defaultComboBoxModel1);
        cbbOption.setName("");
        pnlOption.add(cbbOption, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 10), null, 0, false));
        lblTime = new JLabel();
        lblTime.setAlignmentY(0.5f);
        Font lblTimeFont = this.$$$getFont$$$(null, Font.BOLD, 22, lblTime.getFont());
        if (lblTimeFont != null) lblTime.setFont(lblTimeFont);
        lblTime.setForeground(new Color(-15774605));
        lblTime.setHorizontalAlignment(4);
        lblTime.setHorizontalTextPosition(11);
        lblTime.setText("");
        pnlRightHeader.add(lblTime, BorderLayout.CENTER);
        pnlSearch = new JPanel();
        pnlSearch.setLayout(new GridLayoutManager(1, 2, new Insets(0, 100, 0, 0), -1, -1));
        pnlSearch.setAlignmentX(0.0f);
        pnlSearch.setBackground(new Color(-16724789));
        pnlRightHeader.add(pnlSearch, BorderLayout.WEST);
        txtSearch = new JTextField();
        txtSearch.setAlignmentX(0.0f);
        Font txtSearchFont = this.$$$getFont$$$(null, -1, 18, txtSearch.getFont());
        if (txtSearchFont != null) txtSearch.setFont(txtSearchFont);
        txtSearch.setText("");
        pnlSearch.add(txtSearch, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(500, 10), null, 0, false));
        lblSearch = new JLabel();
        lblSearch.setBackground(new Color(-16724789));
        Font lblSearchFont = this.$$$getFont$$$(null, -1, 18, lblSearch.getFont());
        if (lblSearchFont != null) lblSearch.setFont(lblSearchFont);
        lblSearch.setForeground(new Color(-15774605));
        lblSearch.setIcon(new ImageIcon(getClass().getResource("/icons/ico_search.png")));
        lblSearch.setName("");
        lblSearch.setText("Tra cứu");
        pnlSearch.add(lblSearch, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblSearch.setLabelFor(txtSearch);
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
        return pnlMainMenu;
    }


}
