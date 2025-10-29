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
import com.itextpdf.layout.borders.SolidBorder;
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
    private static PdfFont vietnameseFont;

    static {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setGroupingSeparator('.');
        dfs.setDecimalSeparator(',');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", dfs);

        // Initialize Vietnamese font
        try {
            // Try to use Arial font from Windows fonts directory
            String[] fontPaths = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/times.ttf",
                "C:/Windows/Fonts/verdana.ttf"
            };

            boolean fontLoaded = false;
            for (String fontPath : fontPaths) {
                try {
                    vietnameseFont = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                    fontLoaded = true;
                    System.out.println("Successfully loaded font: " + fontPath);
                    break;
                } catch (Exception e) {
                    // Try next font
                }
            }

            if (!fontLoaded) {
                // Fallback to Helvetica (may not display Vietnamese correctly)
                System.err.println("Warning: Could not load Vietnamese font. Vietnamese characters may not display correctly.");
                vietnameseFont = PdfFontFactory.createFont("Helvetica", PdfEncodings.IDENTITY_H);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading font: " + e.getMessage());
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

        // Set default font for the document
        if (vietnameseFont != null) {
            document.setFont(vietnameseFont);
        }

        // Add header
        addHeader(document, invoice);

        // Add spacing
        document.add(new Paragraph("\n"));

        // Add invoice info
        addInvoiceInfo(document, invoice);

        // Add spacing
        document.add(new Paragraph("\n"));

        // Add product table
        addProductTable(document, invoice);

        // Add spacing
        document.add(new Paragraph("\n"));

        // Add totals
        addTotals(document, invoice);

        // Add footer
        addFooter(document, invoice);

        // Close document
        document.close();

        return new File(outputPath);
    }

    private static void addHeader(Document document, Invoice invoice) {
        // Company name
        Paragraph companyName = new Paragraph("MEDIWOW PHARMACY")
                .setFont(vietnameseFont)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(companyName);

        // Company details
        Paragraph companyDetails = new Paragraph("Địa chỉ: 12 Nguyễn Văn Bảo, Phường 4, Gò Vấp, TP.HCM\n" +
                "Điện thoại: (028) 3894 2345 | Email: info@mediwow.com")
                .setFont(vietnameseFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(companyDetails);

        // Invoice title
        Paragraph invoiceTitle = new Paragraph("HÓA ĐƠN BÁN HÀNG")
                .setFont(vietnameseFont)
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(invoiceTitle);
    }

    private static void addInvoiceInfo(Document document, Invoice invoice) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        // Create a table for invoice info
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        // Left column
        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Mã hóa đơn: " + invoice.getId()).setFont(vietnameseFont).setFontSize(10))
                .add(new Paragraph("Ngày lập: " + dateFormat.format(new Date())).setFont(vietnameseFont).setFontSize(10))
                .add(new Paragraph("Nhân viên: " + invoice.getCreator().getFullName()).setFont(vietnameseFont).setFontSize(10));

        // Right column
        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Loại hóa đơn: " + invoice.getType().toString()).setFont(vietnameseFont).setFontSize(10))
                .add(new Paragraph("Phương thức: " + getPaymentMethodText(invoice.getPaymentMethod())).setFont(vietnameseFont).setFontSize(10));

        // Add prescription code if exists
        if (invoice.getPrescriptionCode() != null && !invoice.getPrescriptionCode().isEmpty()) {
            rightCell.add(new Paragraph("Mã đơn thuốc: " + invoice.getPrescriptionCode()).setFont(vietnameseFont).setFontSize(10));
        }

        infoTable.addCell(leftCell);
        infoTable.addCell(rightCell);

        document.add(infoTable);
    }

    private static void addProductTable(Document document, Invoice invoice) {
        // Create table with 6 columns
        float[] columnWidths = {1, 3, 1.5f, 1, 1.5f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

        // Header styling
        DeviceRgb headerColor = new DeviceRgb(41, 128, 185);

        // Add headers
        table.addHeaderCell(createHeaderCell("STT", headerColor));
        table.addHeaderCell(createHeaderCell("Tên sản phẩm", headerColor));
        table.addHeaderCell(createHeaderCell("Đơn vị", headerColor));
        table.addHeaderCell(createHeaderCell("Số lượng", headerColor));
        table.addHeaderCell(createHeaderCell("Đơn giá", headerColor));
        table.addHeaderCell(createHeaderCell("Thành tiền", headerColor));

        // Add products
        int index = 1;
        for (InvoiceLine line : invoice.getInvoiceLineList()) {
            table.addCell(createCell(String.valueOf(index++), TextAlignment.CENTER));
            table.addCell(createCell(line.getProduct().getName(), TextAlignment.LEFT));
            table.addCell(createCell(line.getUnitOfMeasure().getName(), TextAlignment.CENTER));
            table.addCell(createCell(String.valueOf(line.getQuantity()), TextAlignment.CENTER));
            table.addCell(createCell(CURRENCY_FORMAT.format(line.getUnitPrice()) + " đ", TextAlignment.RIGHT));
            table.addCell(createCell(CURRENCY_FORMAT.format(line.getUnitPrice() * line.getQuantity()) + " đ", TextAlignment.RIGHT));
        }

        document.add(table);
    }

    private static void addTotals(Document document, Invoice invoice) {
        // Create totals table
        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .useAllAvailableWidth()
                .setMarginTop(10);

        // Subtotal
        double subtotal = invoice.calculateSubtotal();
        totalsTable.addCell(createTotalCell("Tạm tính:", false));
        totalsTable.addCell(createTotalCell(CURRENCY_FORMAT.format(subtotal) + " đ", false));

        // VAT
        double vat = invoice.calculateVatAmount();
        totalsTable.addCell(createTotalCell("VAT:", false));
        totalsTable.addCell(createTotalCell(CURRENCY_FORMAT.format(vat) + " đ", false));

        // Promotion discount
        if (invoice.getPromotion() != null) {
            double discount = invoice.calculatePromotion();
            totalsTable.addCell(createTotalCell("Khuyến mãi:", false));
            totalsTable.addCell(createTotalCell("- " + CURRENCY_FORMAT.format(discount) + " đ", false));
        }

        // Total
        double total = invoice.calculateTotal();
        totalsTable.addCell(createTotalCell("TỔNG CỘNG:", true));
        totalsTable.addCell(createTotalCell(CURRENCY_FORMAT.format(total) + " đ", true));

        document.add(totalsTable);
    }

    private static void addFooter(Document document, Invoice invoice) {
        document.add(new Paragraph("\n"));

        // Thank you message
        Paragraph thankYou = new Paragraph("Cảm ơn quý khách đã mua hàng!")
                .setFont(vietnameseFont)
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(thankYou);

        // Footer note
        Paragraph note = new Paragraph("Vui lòng giữ hóa đơn để đổi trả hàng trong vòng 7 ngày.")
                .setFont(vietnameseFont)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic();
        document.add(note);

        // Signature area
        document.add(new Paragraph("\n\n"));
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        Cell customerSign = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Khách hàng").setFont(vietnameseFont).setTextAlignment(TextAlignment.CENTER).setFontSize(10).setBold())
                .add(new Paragraph("(Ký và ghi rõ họ tên)").setFont(vietnameseFont).setTextAlignment(TextAlignment.CENTER).setFontSize(8).setItalic())
                .add(new Paragraph("\n\n\n"));

        Cell staffSign = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Nhân viên").setFont(vietnameseFont).setTextAlignment(TextAlignment.CENTER).setFontSize(10).setBold())
                .add(new Paragraph("(Ký và ghi rõ họ tên)").setFont(vietnameseFont).setTextAlignment(TextAlignment.CENTER).setFontSize(8).setItalic())
                .add(new Paragraph("\n\n\n"))
                .add(new Paragraph(invoice.getCreator().getFullName()).setFont(vietnameseFont).setTextAlignment(TextAlignment.CENTER).setFontSize(10));

        signatureTable.addCell(customerSign);
        signatureTable.addCell(staffSign);

        document.add(signatureTable);
    }

    private static Cell createHeaderCell(String text, DeviceRgb color) {
        return new Cell()
                .add(new Paragraph(text).setFont(vietnameseFont).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(color)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10)
                .setPadding(5);
    }

    private static Cell createCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text).setFont(vietnameseFont))
                .setTextAlignment(alignment)
                .setFontSize(9)
                .setPadding(5);
    }

    private static Cell createTotalCell(String text, boolean isBold) {
        Paragraph para = new Paragraph(text).setFont(vietnameseFont);

        Cell cell = new Cell()
                .add(para)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(11)
                .setBorder(Border.NO_BORDER)
                .setPadding(3);

        if (isBold) {
            para.setBold();
            cell.setFontSize(13);
            cell.setBorderTop(new SolidBorder(1));
        }

        return cell;
    }

    private static String getPaymentMethodText(com.enums.PaymentMethod method) {
        if (method == null) {
            return "N/A";
        }
        switch (method) {
            case CASH:
                return "Tiền mặt";
            case BANK_TRANSFER:
                return "Chuyển khoản/Ví điện tử";
            default:
                return method.toString();
        }
    }
}

