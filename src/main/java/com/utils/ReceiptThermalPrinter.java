package com.utils;

import com.entities.*;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Direct thermal receipt printer using EscPos-Coffee
 * Paper size: 82mm x 290mm
 * Left padding: 2mm, Right padding: 2mm
 * Effective print width: 78mm (~44 characters)
 */
public class ReceiptThermalPrinter {

    private static final int MAX_PRODUCT_NAME_LENGTH = 20;
    // 82mm paper - 2mm left padding - 2mm right padding = 78mm effective width
    // At ~12 chars per inch (standard), 78mm ≈ 40-44 characters
    private static final int LINE_WIDTH = 44;
    private static final String LEFT_PADDING = "  "; // 2mm padding (approx 2 spaces)
    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setGroupingSeparator('.');
        dfs.setDecimalSeparator(',');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", dfs);
    }

    /**
     * Print receipt directly to thermal printer
     *
     * @param invoice         The invoice to print
     * @param invoiceId       The generated invoice ID
     * @param customerPayment Amount customer paid
     * @param printerName     Name of the printer (null for default)
     * @throws IOException if printing fails
     */
    public static void printReceipt(Invoice invoice, String invoiceId, BigDecimal customerPayment, String printerName) throws IOException {
        PrintService printService = findPrinter(printerName);
        if (printService == null) {
            throw new IOException("Không tìm thấy máy in: " + (printerName != null ? printerName : "default"));
        }

        try (PrinterOutputStream printerStream = new PrinterOutputStream(printService);
             EscPos escpos = new EscPos(printerStream)) {

            // Define styles matching ReceiptPDFGenerator (2x size = FontSize._2)
            Style titleStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true)
                    .setJustification(EscPosConst.Justification.Center);

            Style headerStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true)
                    .setJustification(EscPosConst.Justification.Center);

            Style normalCenter = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setJustification(EscPosConst.Justification.Center);

            Style normalLeft = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1);

            Style boldLeft = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setBold(true);

            Style tableHeader = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setBold(true);

            // === HEADER ===
            escpos.writeLF(titleStyle, "NHA THUOC MEDIWOW");
            escpos.writeLF(normalCenter, "12 Nguyen Van Bao, P.4, Go Vap, TP.HCM");
            escpos.writeLF(normalCenter, "DT: (028) 3894 2345");
            escpos.feed(1);

            escpos.writeLF(headerStyle, "HOA DON BAN HANG");
            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === INVOICE INFO ===
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            // Row 1: Invoice ID | Staff (like PDF layout)
            String infoLine1 = padRight(LEFT_PADDING + "Ma HD: " + (invoiceId != null ? invoiceId : "N/A"), LINE_WIDTH / 2) +
                              padLeft("NV: " + truncateText(removeAccents(invoice.getCreator().getFullName()), 15), LINE_WIDTH / 2);
            escpos.writeLF(normalLeft, infoLine1);

            // Row 2: Date | Payment Method
            String infoLine2 = padRight(LEFT_PADDING + "Ngay: " + dateFormat.format(new Date()), LINE_WIDTH / 2) +
                              padLeft("TT: " + getPaymentMethodText(invoice.getPaymentMethod()), LINE_WIDTH / 2);
            escpos.writeLF(normalLeft, infoLine2);

            // Row 3: Shift ID
            if (invoice.getShift() != null && invoice.getShift().getId() != null) {
                escpos.writeLF(normalLeft, LEFT_PADDING + "Ma ca: " + invoice.getShift().getId());
            }

            // Row 4: Customer Name (if exists)
            if (invoice.getCustomer() != null && invoice.getCustomer().getName() != null) {
                escpos.writeLF(normalLeft, LEFT_PADDING + "Khach hang: " + truncateText(removeAccents(invoice.getCustomer().getName()), 30));
            }

            // Prescription code if exists
            if (invoice.getPrescriptionCode() != null && !invoice.getPrescriptionCode().isEmpty()) {
                escpos.writeLF(normalLeft, LEFT_PADDING + "Ma don thuoc: " + invoice.getPrescriptionCode());
            }

            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === PRODUCT TABLE HEADER ===
            escpos.writeLF(tableHeader, LEFT_PADDING + formatTableRow("Ten SP", "DV", "SL", "T.Tien"));
            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === PRODUCTS ===
            for (InvoiceLine line : invoice.getInvoiceLineList()) {
                String productName = line.getProduct().getShortName();
                if (productName == null || productName.isEmpty()) {
                    productName = line.getProduct().getName();
                }
                productName = truncateText(removeAccents(productName), MAX_PRODUCT_NAME_LENGTH);

                String uom = truncateText(removeAccents(line.getUnitOfMeasure().getName()), 6);
                String qty = String.valueOf(line.getQuantity());
                String subtotal = formatCurrency(line.calculateSubtotal());

                escpos.writeLF(normalLeft, LEFT_PADDING + formatTableRow(productName, uom, qty, subtotal));
            }

            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === TOTALS (Left aligned like PDF) ===
            escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine("Tong tien hang:", formatCurrency(invoice.calculateSubtotal())));
            escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine("Thue VAT:", formatCurrency(invoice.calculateVatAmount())));

            BigDecimal discount = invoice.calculatePromotion();
            if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                String discountLabel = "Giam gia:";
                if (invoice.getPromotion() != null) {
                    discountLabel = "Giam gia (" + truncateText(removeAccents(invoice.getPromotion().getName()), 10) + "):";
                }
                escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine(discountLabel, "-" + formatCurrency(discount)));
            }

            BigDecimal total = invoice.calculateTotal();
            escpos.writeLF(boldLeft, LEFT_PADDING + formatTotalLine("TONG CONG:", formatCurrency(total)));
            escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine("Tien khach dua:", formatCurrency(customerPayment)));

            BigDecimal change = customerPayment.subtract(total);
            String changeLabel = change.compareTo(BigDecimal.ZERO) >= 0 ? "Tien thua:" : "Con thieu:";
            escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine(changeLabel, formatCurrency(change.abs())));

            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === FOOTER ===
            escpos.feed(1);
            escpos.writeLF(headerStyle, "Cam on quy khach!");
            escpos.feed(1);
            escpos.writeLF(normalCenter, "Giu hoa don de doi tra trong 7 ngay");
            escpos.feed(3);

            // Cut paper (if supported)
            escpos.cut(EscPos.CutMode.PART);
        }
    }

    /**
     * Print exchange receipt directly to thermal printer.
     * Shows original invoice lines (with negative values) and exchange invoice lines.
     *
     * @param exchangeInvoice   The exchange invoice to print
     * @param invoiceId         The generated invoice ID
     * @param customerPayment   Amount customer paid
     * @param printerName       Name of the printer (null for default)
     * @throws IOException if printing fails
     */
    public static void printExchangeReceipt(Invoice exchangeInvoice, String invoiceId, BigDecimal customerPayment, String printerName) throws IOException {
        PrintService printService = findPrinter(printerName);
        if (printService == null) {
            throw new IOException("Không tìm thấy máy in: " + (printerName != null ? printerName : "default"));
        }

        Invoice originalInvoice = exchangeInvoice.getReferencedInvoice();

        try (PrinterOutputStream printerStream = new PrinterOutputStream(printService);
             EscPos escpos = new EscPos(printerStream)) {

            Style titleStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true)
                    .setJustification(EscPosConst.Justification.Center);

            Style headerStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true)
                    .setJustification(EscPosConst.Justification.Center);

            Style normalCenter = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setJustification(EscPosConst.Justification.Center);

            Style normalLeft = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1);

            Style boldLeft = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setBold(true);

            Style tableHeader = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setBold(true);

            // === HEADER ===
            escpos.writeLF(titleStyle, "NHA THUOC MEDIWOW");
            escpos.writeLF(normalCenter, "12 Nguyen Van Bao, P.4, Go Vap, TP.HCM");
            escpos.writeLF(normalCenter, "DT: (028) 3894 2345");
            escpos.feed(1);

            escpos.writeLF(headerStyle, "HOA DON DOI HANG");
            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === INVOICE INFO ===
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            String infoLine1 = padRight(LEFT_PADDING + "Ma HD: " + (invoiceId != null ? invoiceId : "N/A"), LINE_WIDTH / 2) +
                              padLeft("NV: " + truncateText(removeAccents(exchangeInvoice.getCreator().getFullName()), 15), LINE_WIDTH / 2);
            escpos.writeLF(normalLeft, infoLine1);

            String infoLine2 = padRight(LEFT_PADDING + "Ngay: " + dateFormat.format(new Date()), LINE_WIDTH / 2) +
                              padLeft("TT: " + getPaymentMethodText(exchangeInvoice.getPaymentMethod()), LINE_WIDTH / 2);
            escpos.writeLF(normalLeft, infoLine2);

            if (exchangeInvoice.getShift() != null && exchangeInvoice.getShift().getId() != null) {
                escpos.writeLF(normalLeft, LEFT_PADDING + "Ma ca: " + exchangeInvoice.getShift().getId());
            }

            if (originalInvoice != null) {
                escpos.writeLF(normalLeft, LEFT_PADDING + "HD goc: " + originalInvoice.getId());
            }

            if (exchangeInvoice.getCustomer() != null && exchangeInvoice.getCustomer().getName() != null) {
                escpos.writeLF(normalLeft, LEFT_PADDING + "Khach hang: " + truncateText(removeAccents(exchangeInvoice.getCustomer().getName()), 30));
            }

            if (exchangeInvoice.getPrescriptionCode() != null && !exchangeInvoice.getPrescriptionCode().isEmpty()) {
                escpos.writeLF(normalLeft, LEFT_PADDING + "Ma don thuoc: " + exchangeInvoice.getPrescriptionCode());
            }

            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === ORIGINAL INVOICE TABLE (NEGATIVE VALUES) ===
            escpos.writeLF(boldLeft, LEFT_PADDING + "** HANG TRA LAI (HD GOC) **");
            escpos.writeLF(tableHeader, LEFT_PADDING + formatTableRow("Ten SP", "DV", "SL", "T.Tien"));
            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            BigDecimal originalSubtotal = BigDecimal.ZERO;
            if (originalInvoice != null) {
                for (InvoiceLine line : originalInvoice.getInvoiceLineList()) {
                    String productName = line.getProduct().getShortName();
                    if (productName == null || productName.isEmpty()) {
                        productName = line.getProduct().getName();
                    }
                    productName = truncateText(removeAccents(productName), MAX_PRODUCT_NAME_LENGTH);

                    String uom = truncateText(removeAccents(line.getUnitOfMeasure().getName()), 6);
                    String qty = "-" + line.getQuantity(); // Negative quantity
                    BigDecimal lineSubtotal = line.calculateSubtotal();
                    originalSubtotal = originalSubtotal.add(lineSubtotal);
                    String subtotal = "-" + formatCurrency(lineSubtotal); // Negative value

                    escpos.writeLF(normalLeft, LEFT_PADDING + formatTableRow(productName, uom, qty, subtotal));
                }
            }

            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === EXCHANGE INVOICE TABLE (POSITIVE VALUES) ===
            escpos.writeLF(boldLeft, LEFT_PADDING + "** HANG DOI MOI **");
            escpos.writeLF(tableHeader, LEFT_PADDING + formatTableRow("Ten SP", "DV", "SL", "T.Tien"));
            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            BigDecimal exchangeSubtotal = BigDecimal.ZERO;
            for (InvoiceLine line : exchangeInvoice.getInvoiceLineList()) {
                String productName = line.getProduct().getShortName();
                if (productName == null || productName.isEmpty()) {
                    productName = line.getProduct().getName();
                }
                productName = truncateText(removeAccents(productName), MAX_PRODUCT_NAME_LENGTH);

                String uom = truncateText(removeAccents(line.getUnitOfMeasure().getName()), 6);
                String qty = String.valueOf(line.getQuantity());
                BigDecimal lineSubtotal = line.calculateSubtotal();
                exchangeSubtotal = exchangeSubtotal.add(lineSubtotal);
                String subtotal = formatCurrency(lineSubtotal);

                escpos.writeLF(normalLeft, LEFT_PADDING + formatTableRow(productName, uom, qty, subtotal));
            }

            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === TOTALS ===
            BigDecimal originalTotal = originalInvoice != null ? originalInvoice.calculateTotal() : BigDecimal.ZERO;
            BigDecimal exchangeTotal = exchangeInvoice.calculateTotal();
            BigDecimal netTotal = exchangeInvoice.calculateExchangeTotal();

            escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine("Tien hang tra:", "-" + formatCurrency(originalTotal)));
            escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine("Tien hang doi:", formatCurrency(exchangeTotal)));
            escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine("VAT hang doi:", formatCurrency(exchangeInvoice.calculateVatAmount())));

            String netLabel = netTotal.compareTo(BigDecimal.ZERO) >= 0 ? "TONG PHAI TRA:" : "TONG HOAN LAI:";
            escpos.writeLF(boldLeft, LEFT_PADDING + formatTotalLine(netLabel, formatCurrency(netTotal.abs())));

            if (netTotal.compareTo(BigDecimal.ZERO) > 0) {
                escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine("Tien khach dua:", formatCurrency(customerPayment)));
                BigDecimal change = customerPayment.subtract(netTotal);
                String changeLabel = change.compareTo(BigDecimal.ZERO) >= 0 ? "Tien thua:" : "Con thieu:";
                escpos.writeLF(normalLeft, LEFT_PADDING + formatTotalLine(changeLabel, formatCurrency(change.abs())));
            }

            escpos.writeLF(normalCenter, repeat("-", LINE_WIDTH));

            // === FOOTER ===
            escpos.feed(1);
            escpos.writeLF(headerStyle, "Cam on quy khach!");
            escpos.feed(1);
            escpos.writeLF(normalCenter, "Giu hoa don de doi tra trong 7 ngay");
            escpos.feed(3);

            escpos.cut(EscPos.CutMode.PART);
        }
    }

    /**
     * Get list of available printers
     */
    public static String[] getAvailablePrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        String[] names = new String[services.length];
        for (int i = 0; i < services.length; i++) {
            names[i] = services[i].getName();
        }
        return names;
    }

    /**
     * Check if any printer is available
     */
    public static boolean hasPrinters() {
        return PrintServiceLookup.lookupPrintServices(null, null).length > 0;
    }

    private static PrintService findPrinter(String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        if (printerName == null || printerName.isEmpty()) {
            PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultPrinter != null) return defaultPrinter;
            return services.length > 0 ? services[0] : null;
        }

        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(printerName)) {
                return service;
            }
        }
        // Fallback: partial match
        for (PrintService service : services) {
            if (service.getName().toLowerCase().contains(printerName.toLowerCase())) {
                return service;
            }
        }
        return null;
    }

    private static String formatTableRow(String name, String uom, String qty, String price) {
        // Format: Name(20) | UOM(6) | Qty(4) | Price(12) = 42 chars
        return String.format("%-20s %-6s %4s %12s", name, uom, qty, price);
    }

    private static String formatTotalLine(String label, String value) {
        // Left aligned format matching PDF layout
        return String.format("%-20s %s", label, value);
    }

    private static String formatCurrency(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        return CURRENCY_FORMAT.format(amount) + " D";
    }

    private static String getPaymentMethodText(com.enums.PaymentMethod method) {
        if (method == null) return "N/A";
        return switch (method) {
            case CASH -> "Tien mat";
            case BANK_TRANSFER -> "CK/Vi";
            default -> method.toString();
        };
    }

    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private static String repeat(String str, int times) {
        return str.repeat(Math.max(0, times));
    }

    private static String padRight(String text, int length) {
        if (text.length() >= length) return text.substring(0, length);
        return text + repeat(" ", length - text.length());
    }

    private static String padLeft(String text, int length) {
        if (text.length() >= length) return text.substring(0, length);
        return repeat(" ", length - text.length()) + text;
    }

    /**
     * Remove Vietnamese accents for thermal printer compatibility
     */
    private static String removeAccents(String text) {
        if (text == null) return "";
        String[][] replacements = {
            {"á", "a"}, {"à", "a"}, {"ả", "a"}, {"ã", "a"}, {"ạ", "a"},
            {"ă", "a"}, {"ắ", "a"}, {"ằ", "a"}, {"ẳ", "a"}, {"ẵ", "a"}, {"ặ", "a"},
            {"â", "a"}, {"ấ", "a"}, {"ầ", "a"}, {"ẩ", "a"}, {"ẫ", "a"}, {"ậ", "a"},
            {"é", "e"}, {"è", "e"}, {"ẻ", "e"}, {"ẽ", "e"}, {"ẹ", "e"},
            {"ê", "e"}, {"ế", "e"}, {"ề", "e"}, {"ể", "e"}, {"ễ", "e"}, {"ệ", "e"},
            {"í", "i"}, {"ì", "i"}, {"ỉ", "i"}, {"ĩ", "i"}, {"ị", "i"},
            {"ó", "o"}, {"ò", "o"}, {"ỏ", "o"}, {"õ", "o"}, {"ọ", "o"},
            {"ô", "o"}, {"ố", "o"}, {"ồ", "o"}, {"ổ", "o"}, {"ỗ", "o"}, {"ộ", "o"},
            {"ơ", "o"}, {"ớ", "o"}, {"ờ", "o"}, {"ở", "o"}, {"ỡ", "o"}, {"ợ", "o"},
            {"ú", "u"}, {"ù", "u"}, {"ủ", "u"}, {"ũ", "u"}, {"ụ", "u"},
            {"ư", "u"}, {"ứ", "u"}, {"ừ", "u"}, {"ử", "u"}, {"ữ", "u"}, {"ự", "u"},
            {"ý", "y"}, {"ỳ", "y"}, {"ỷ", "y"}, {"ỹ", "y"}, {"ỵ", "y"},
            {"đ", "d"}, {"Đ", "D"},
            {"Á", "A"}, {"À", "A"}, {"Ả", "A"}, {"Ã", "A"}, {"Ạ", "A"},
            {"Ă", "A"}, {"Ắ", "A"}, {"Ằ", "A"}, {"Ẳ", "A"}, {"Ẵ", "A"}, {"Ặ", "A"},
            {"Â", "A"}, {"Ấ", "A"}, {"Ầ", "A"}, {"Ẩ", "A"}, {"Ẫ", "A"}, {"Ậ", "A"},
            {"É", "E"}, {"È", "E"}, {"Ẻ", "E"}, {"Ẽ", "E"}, {"Ẹ", "E"},
            {"Ê", "E"}, {"Ế", "E"}, {"Ề", "E"}, {"Ể", "E"}, {"Ễ", "E"}, {"Ệ", "E"},
            {"Í", "I"}, {"Ì", "I"}, {"Ỉ", "I"}, {"Ĩ", "I"}, {"Ị", "I"},
            {"Ó", "O"}, {"Ò", "O"}, {"Ỏ", "O"}, {"Õ", "O"}, {"Ọ", "O"},
            {"Ô", "O"}, {"Ố", "O"}, {"Ồ", "O"}, {"Ổ", "O"}, {"Ỗ", "O"}, {"Ộ", "O"},
            {"Ơ", "O"}, {"Ớ", "O"}, {"Ờ", "O"}, {"Ở", "O"}, {"Ỡ", "O"}, {"Ợ", "O"},
            {"Ú", "U"}, {"Ù", "U"}, {"Ủ", "U"}, {"Ũ", "U"}, {"Ụ", "U"},
            {"Ư", "U"}, {"Ứ", "U"}, {"Ừ", "U"}, {"Ử", "U"}, {"Ữ", "U"}, {"Ự", "U"},
            {"Ý", "Y"}, {"Ỳ", "Y"}, {"Ỷ", "Y"}, {"Ỹ", "Y"}, {"Ỵ", "Y"}
        };
        for (String[] r : replacements) {
            text = text.replace(r[0], r[1]);
        }
        return text;
    }
}

