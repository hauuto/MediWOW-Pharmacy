package com.gui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.utils.AppColors;
import com.utils.MoMoPaymentService;
import com.utils.MoMoPaymentService.MoMoPaymentResponse;
import com.utils.MoMoPaymentService.MoMoQueryResponse;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;

/**
 * Dialog to display MoMo QR Code for payment
 * @author MediWOW Pharmacy
 */
public class MoMoQRCodeDialog extends JDialog {
    private final String orderId;
    private final long amount;
    private final String orderInfo;
    private Timer pollingTimer;
    private JLabel lblStatus;
    private JLabel lblQRCode;
    private JButton btnCancel;
    private boolean paymentSuccess = false;
    private boolean cancelled = false;
    private static final int POLLING_INTERVAL = 3000; // 3 seconds
    private static final int TIMEOUT = 300000; // 5 minutes timeout
    private long startTime;

    public MoMoQRCodeDialog(Window owner, long amount, String orderInfo) {
        super(owner, "Thanh toán MoMo", ModalityType.APPLICATION_MODAL);
        this.orderId = MoMoPaymentService.generateOrderId();
        this.amount = amount;
        this.orderInfo = orderInfo;
        initComponents();
        setSize(450, 550);
        setLocationRelativeTo(owner);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cancelPayment();
            }
        });
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Quét mã QR để thanh toán", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setForeground(new Color(166, 32, 128)); // MoMo pink color
        headerPanel.add(lblTitle, BorderLayout.NORTH);

        JLabel lblAmount = new JLabel(formatCurrency(amount), SwingConstants.CENTER);
        lblAmount.setFont(new Font("Arial", Font.BOLD, 24));
        lblAmount.setForeground(AppColors.DARK);
        headerPanel.add(lblAmount, BorderLayout.CENTER);

        JLabel lblOrderInfo = new JLabel(orderInfo, SwingConstants.CENTER);
        lblOrderInfo.setFont(new Font("Arial", Font.PLAIN, 14));
        lblOrderInfo.setForeground(AppColors.TEXT);
        headerPanel.add(lblOrderInfo, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // QR Code Panel
        JPanel qrPanel = new JPanel(new BorderLayout());
        qrPanel.setBackground(Color.WHITE);

        lblQRCode = new JLabel("Đang tạo mã QR...", SwingConstants.CENTER);
        lblQRCode.setFont(new Font("Arial", Font.PLAIN, 16));
        lblQRCode.setPreferredSize(new Dimension(300, 300));
        qrPanel.add(lblQRCode, BorderLayout.CENTER);

        mainPanel.add(qrPanel, BorderLayout.CENTER);

        // Status and Button Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);

        lblStatus = new JLabel("Đang chờ thanh toán...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Arial", Font.ITALIC, 14));
        lblStatus.setForeground(new Color(255, 153, 0)); // Orange for waiting
        bottomPanel.add(lblStatus, BorderLayout.NORTH);

        btnCancel = new JButton("Hủy thanh toán");
        btnCancel.setFont(new Font("Arial", Font.BOLD, 16));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setBackground(new Color(220, 53, 69)); // Red
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.setPreferredSize(new Dimension(200, 45));
        btnCancel.addActionListener(e -> cancelPayment());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btnCancel);
        bottomPanel.add(btnPanel, BorderLayout.CENTER);

        // MoMo branding
        JLabel lblMoMo = new JLabel("Powered by MoMo", SwingConstants.CENTER);
        lblMoMo.setFont(new Font("Arial", Font.PLAIN, 12));
        lblMoMo.setForeground(new Color(166, 32, 128));
        bottomPanel.add(lblMoMo, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Start the payment process
     */
    public void startPayment() {
        startTime = System.currentTimeMillis();

        // Create payment in background
        SwingWorker<MoMoPaymentResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected MoMoPaymentResponse doInBackground() throws Exception {
                return MoMoPaymentService.createPayment(orderId, amount, orderInfo);
            }

            @Override
            protected void done() {
                try {
                    MoMoPaymentResponse response = get();
                    if (response.isSuccess()) {
                        // Try to load QR image from MoMo first, otherwise generate from deeplink/shortLink
                        if (response.hasQrCodeImage()) {
                            loadQRCodeFromUrl(response.qrCodeUrl);
                        } else {
                            String qrContent = response.getQrContent();
                            if (qrContent != null && !qrContent.isEmpty()) {
                                generateAndDisplayQRCode(qrContent);
                            } else {
                                showError("Không thể tạo mã QR thanh toán");
                                return;
                            }
                        }
                        startPolling();
                    } else {
                        showError("Không thể tạo thanh toán: " + response.message);
                    }
                } catch (Exception e) {
                    showError("Lỗi: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Load QR code image from MoMo's URL
     */
    private void loadQRCodeFromUrl(String qrCodeUrl) {
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return ImageIO.read(URI.create(qrCodeUrl).toURL());
            }

            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image != null) {
                        Image scaledImage = image.getScaledInstance(280, 280, Image.SCALE_SMOOTH);
                        lblQRCode.setIcon(new ImageIcon(scaledImage));
                        lblQRCode.setText("");
                    } else {
                        lblQRCode.setText("Không thể tải mã QR");
                    }
                } catch (Exception e) {
                    lblQRCode.setText("Lỗi tải mã QR: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Generate QR code from deeplink/shortLink and display it
     */
    private void generateAndDisplayQRCode(String content) {
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return generateQRCodeImage(content, 280, 280);
            }

            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image != null) {
                        lblQRCode.setIcon(new ImageIcon(image));
                        lblQRCode.setText("");
                    } else {
                        lblQRCode.setText("Không thể tạo mã QR");
                    }
                } catch (Exception e) {
                    lblQRCode.setText("Lỗi tạo mã QR: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Generate QR code image from text/URL
     */
    private BufferedImage generateQRCodeImage(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Start polling for payment status
     */
    private void startPolling() {
        pollingTimer = new Timer(POLLING_INTERVAL, e -> checkPaymentStatus());
        pollingTimer.start();
    }

    /**
     * Check payment status
     */
    private void checkPaymentStatus() {
        // Check timeout
        if (System.currentTimeMillis() - startTime > TIMEOUT) {
            stopPolling();
            showError("Hết thời gian thanh toán. Vui lòng thử lại.");
            return;
        }

        SwingWorker<MoMoQueryResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected MoMoQueryResponse doInBackground() throws Exception {
                return MoMoPaymentService.queryPaymentStatus(orderId);
            }

            @Override
            protected void done() {
                try {
                    MoMoQueryResponse response = get();
                    if (response.isPaid()) {
                        paymentSuccess = true;
                        stopPolling();
                        lblStatus.setText("Thanh toán thành công!");
                        lblStatus.setForeground(new Color(40, 167, 69)); // Green
                        btnCancel.setText("Đóng");
                        btnCancel.setBackground(new Color(40, 167, 69));

                        // Auto close after 2 seconds
                        Timer closeTimer = new Timer(2000, evt -> dispose());
                        closeTimer.setRepeats(false);
                        closeTimer.start();
                    } else if (response.isPending()) {
                        // Still waiting, update status
                        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                        lblStatus.setText(String.format("Đang chờ thanh toán... (%ds)", elapsed));
                    } else {
                        // Payment failed or cancelled
                        stopPolling();
                        showError("Thanh toán không thành công: " + response.message);
                    }
                } catch (Exception e) {
                    // Continue polling on network errors
                    System.err.println("Error checking payment status: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Stop polling timer
     */
    private void stopPolling() {
        if (pollingTimer != null && pollingTimer.isRunning()) {
            pollingTimer.stop();
        }
    }

    /**
     * Cancel the payment
     */
    private void cancelPayment() {
        stopPolling();
        cancelled = true;
        dispose();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        lblStatus.setText(message);
        lblStatus.setForeground(new Color(220, 53, 69)); // Red
        btnCancel.setText("Đóng");
    }

    /**
     * Format currency in Vietnamese format
     */
    private String formatCurrency(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(amount) + " VNĐ";
    }

    /**
     * Check if payment was successful
     */
    public boolean isPaymentSuccess() {
        return paymentSuccess;
    }

    /**
     * Check if payment was cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Get the order ID
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Show the dialog and start payment process
     * @return true if payment was successful, false otherwise
     */
    public static boolean showAndPay(Window owner, long amount, String orderInfo) {
        MoMoQRCodeDialog dialog = new MoMoQRCodeDialog(owner, amount, orderInfo);
        dialog.startPayment();
        dialog.setVisible(true);
        return dialog.isPaymentSuccess();
    }
}
