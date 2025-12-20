package com.utils;

import com.entities.*;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class to generate PDF invoices
 */
public class InvoicePDFGenerator {

    private static final DecimalFormat CURRENCY_FORMAT;
    private static final String FONT_PATH;

    static {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setGroupingSeparator('.');
        dfs.setDecimalSeparator(',');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", dfs);

        // Find available font path
        String[] fontPaths = {
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/times.ttf",
            "C:/Windows/Fonts/verdana.ttf"
        };

        String foundPath = null;
        for (String path : fontPaths) {
            if (new File(path).exists()) {
                foundPath = path;
                System.out.println("Found font: " + path);
                break;
            }
        }
        FONT_PATH = foundPath;
    }

    /**
     * Create a new font instance for the current PDF document
     * This is needed because iText font objects cannot be shared across PDF documents
     */
    private static PdfFont createFont() {
        try {
            if (FONT_PATH != null) {
                return PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
            } else {
                return PdfFontFactory.createFont("Helvetica", PdfEncodings.IDENTITY_H);
            }
        } catch (Exception e) {
            System.err.println("Error creating font: " + e.getMessage());
            try {
                return PdfFontFactory.createFont();
            } catch (Exception ex) {
                throw new RuntimeException("Could not create font", ex);
            }
        }
    }

    /**
     * Generate a PDF invoice
     *
     * @param invoice The invoice to generate PDF for
     * @param outputPath The output file path
     * @return The generated PDF file
     * @throws FileNotFoundException if the output path is invalid
     */
    public static File generateInvoicePDF(Invoice invoice, String outputPath) throws FileNotFoundException {
        // Create PDF writer
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Create a NEW font instance for this document (fonts cannot be shared across PDF documents)
        PdfFont font = createFont();

        // Set default font for the document
        document.setFont(font);

        // Add header
        addHeader(document, invoice, font);

        // Add spacing
        document.add(new Paragraph("\n"));

        // Add invoice info
        addInvoiceInfo(document, invoice, font);

        // Add spacing
        document.add(new Paragraph("\n"));

        // Add product table
        addProductTable(document, invoice, font);

        // Add spacing
        document.add(new Paragraph("\n"));

        // Add totals
        addTotals(document, invoice, font);

        // Add footer
        addFooter(document, invoice, font);

        // Close document
        document.close();

        return new File(outputPath);
    }

    private static void addHeader(Document document, Invoice invoice, PdfFont font) {
        // Company name
        Paragraph companyName = new Paragraph("MEDIWOW PHARMACY")
                .setFont(font)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(companyName);

        // Company details
        Paragraph companyDetails = new Paragraph("Địa chỉ: 12 Nguyễn Văn Bảo, Phường 4, Gò Vấp, TP.HCM\n" +
                "Điện thoại: (028) 3894 2345 | Email: info@mediwow.com")
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(companyDetails);

        // Invoice title
        Paragraph invoiceTitle = new Paragraph("HÓA ĐƠN BÁN HÀNG")
                .setFont(font)
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(invoiceTitle);
    }

    private static void addInvoiceInfo(Document document, Invoice invoice, PdfFont font) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        // Create a table for invoice info
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        // Left column
        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Mã hóa đơn: " + invoice.getId()).setFont(font).setFontSize(10))
                .add(new Paragraph("Ngày lập: " + dateFormat.format(new Date())).setFont(font).setFontSize(10))
                .add(new Paragraph("Nhân viên: " + invoice.getCreator().getFullName()).setFont(font).setFontSize(10));

        // Right column
        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Loại hóa đơn: " + invoice.getType().toString()).setFont(font).setFontSize(10))
                .add(new Paragraph("Phương thức: " + getPaymentMethodText(invoice.getPaymentMethod())).setFont(font).setFontSize(10));

        // Add prescription code if exists
        if (invoice.getPrescriptionCode() != null && !invoice.getPrescriptionCode().isEmpty()) {
            rightCell.add(new Paragraph("Mã đơn thuốc: " + invoice.getPrescriptionCode()).setFont(font).setFontSize(10));
        }

        infoTable.addCell(leftCell);
        infoTable.addCell(rightCell);

        document.add(infoTable);
    }

    private static void addProductTable(Document document, Invoice invoice, PdfFont font) {
        // Create table with 6 columns
        float[] columnWidths = {1, 3, 1.5f, 1, 1.5f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

        // Header styling
        DeviceRgb headerColor = new DeviceRgb(41, 128, 185);

        // Add headers
        table.addHeaderCell(createHeaderCell("STT", headerColor, font));
        table.addHeaderCell(createHeaderCell("Tên sản phẩm", headerColor, font));
        table.addHeaderCell(createHeaderCell("Đơn vị", headerColor, font));
        table.addHeaderCell(createHeaderCell("Số lượng", headerColor, font));
        table.addHeaderCell(createHeaderCell("Đơn giá", headerColor, font));
        table.addHeaderCell(createHeaderCell("Thành tiền", headerColor, font));

        // Add products
        int index = 1;
        for (InvoiceLine line : invoice.getInvoiceLineList()) {
            table.addCell(createCell(String.valueOf(index++), TextAlignment.CENTER, font));
            table.addCell(createCell(line.getProduct().getName(), TextAlignment.LEFT, font));
            table.addCell(createCell(line.getUnitOfMeasure().getName(), TextAlignment.CENTER, font));
            table.addCell(createCell(String.valueOf(line.getQuantity()), TextAlignment.CENTER, font));
            table.addCell(createCell(formatCurrency(line.getUnitPrice()), TextAlignment.RIGHT, font));
            table.addCell(createCell(formatCurrency(line.calculateSubtotal()), TextAlignment.RIGHT, font));
        }

        document.add(table);
    }

    private static void addTotals(Document document, Invoice invoice, PdfFont font) {
        // Create table for totals
        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .useAllAvailableWidth();

        // Subtotal
        totalsTable.addCell(createTotalLabelCell("Tổng tiền hàng:", font));
        totalsTable.addCell(createTotalValueCell(formatCurrency(invoice.calculateSubtotal()), font));

        // VAT
        totalsTable.addCell(createTotalLabelCell("Thuế VAT:", font));
        totalsTable.addCell(createTotalValueCell(formatCurrency(invoice.calculateVatAmount()), font));

        // Discount if applicable
        if (invoice.getPromotion() != null) {
            totalsTable.addCell(createTotalLabelCell("Giảm giá (" + invoice.getPromotion().getName() + "):", font));
            totalsTable.addCell(createTotalValueCell("-" + formatCurrency(invoice.calculatePromotion()), font));
        }

        // Total
        totalsTable.addCell(createTotalLabelCell("TỔNG CỘNG:", font).setBold());
        totalsTable.addCell(createTotalValueCell(formatCurrency(invoice.calculateTotal()), font).setBold());

        document.add(totalsTable);
    }

    private static void addFooter(Document document, Invoice invoice, PdfFont font) {
        document.add(new Paragraph("\n"));

        Paragraph thankYou = new Paragraph("Cảm ơn quý khách đã mua hàng!")
                .setFont(font)
                .setFontSize(12)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(thankYou);

        Paragraph note = new Paragraph("Vui lòng giữ hóa đơn để đổi trả hàng trong vòng 7 ngày.")
                .setFont(font)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(note);
    }

    private static Cell createHeaderCell(String text, DeviceRgb bgColor, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10).setBold())
                .setBackgroundColor(bgColor)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
    }

    private static Cell createCell(String text, TextAlignment alignment, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9))
                .setTextAlignment(alignment)
                .setPadding(3);
    }

    private static Cell createTotalLabelCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPaddingRight(10);
    }

    private static Cell createTotalValueCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
    }

    private static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount) + " đ";
    }

    private static String getPaymentMethodText(com.enums.PaymentMethod method) {
        if (method == null) return "Không xác định";
        return switch (method) {
            case CASH -> "Tiền mặt";
            case BANK_TRANSFER -> "Chuyển khoản";
            default -> method.toString();
        };
    }
}

