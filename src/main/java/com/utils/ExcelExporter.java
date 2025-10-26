package com.utils;

import com.entities.Staff;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility class để xuất dữ liệu ra file Excel
 */
public class ExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Xuất danh sách nhân viên ra file Excel
     * @param staffList Danh sách nhân viên cần xuất
     * @param fileName Tên file (không cần đuôi .xlsx)
     * @return File path nếu thành công, null nếu thất bại
     */
    public static String exportStaffToExcel(List<Staff> staffList, String fileName) {
        if (staffList == null || staffList.isEmpty()) {
            throw new IllegalArgumentException("Danh sách nhân viên trống, không thể xuất Excel");
        }

        // Tạo thư mục Documents/MediWOW nếu chưa tồn tại
        String userHome = System.getProperty("user.home");
        String documentsPath = userHome + File.separator + "Documents" + File.separator + "MediWOW";
        File directory = new File(documentsPath);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new RuntimeException("Không thể tạo thư mục: " + documentsPath);
            }
        }

        // Tạo đường dẫn file đầy đủ
        String filePath = documentsPath + File.separator + fileName + ".xlsx";

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh sách nhân viên");

            // Tạo style cho header
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Tạo style cho dữ liệu
            CellStyle dataStyle = createDataStyle(workbook);

            // Tạo style cho ngày tháng
            CellStyle dateStyle = createDateStyle(workbook);

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"STT", "Mã nhân viên", "Họ tên", "Tên đăng nhập", "Vai trò",
                               "Số điện thoại", "Email", "Số chứng chỉ", "Ngày vào làm", "Trạng thái"};

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Thêm dữ liệu
            int rowNum = 1;
            for (Staff staff : staffList) {
                Row row = sheet.createRow(rowNum++);

                // STT
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum - 1);
                cell0.setCellStyle(dataStyle);

                // Mã nhân viên
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(staff.getId() != null ? staff.getId() : "");
                cell1.setCellStyle(dataStyle);

                // Họ tên
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(staff.getFullName() != null ? staff.getFullName() : "");
                cell2.setCellStyle(dataStyle);

                // Tên đăng nhập
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(staff.getUsername() != null ? staff.getUsername() : "");
                cell3.setCellStyle(dataStyle);

                // Vai trò
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(getRoleDisplayName(staff.getRole()));
                cell4.setCellStyle(dataStyle);

                // Số điện thoại
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(staff.getPhoneNumber() != null ? staff.getPhoneNumber() : "");
                cell5.setCellStyle(dataStyle);

                // Email
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(staff.getEmail() != null ? staff.getEmail() : "");
                cell6.setCellStyle(dataStyle);

                // Số chứng chỉ
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(staff.getLicenseNumber() != null ? staff.getLicenseNumber() : "");
                cell7.setCellStyle(dataStyle);

                // Ngày vào làm
                Cell cell8 = row.createCell(8);
                if (staff.getHireDate() != null) {
                    cell8.setCellValue(staff.getHireDate().format(DATE_FORMATTER));
                } else {
                    cell8.setCellValue("");
                }
                cell8.setCellStyle(dateStyle);

                // Trạng thái
                Cell cell9 = row.createCell(9);
                cell9.setCellValue(staff.isActive() ? "Hoạt động" : "Đã nghỉ việc");
                cell9.setCellStyle(dataStyle);
            }

            // Tự động điều chỉnh độ rộng cột
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                // Thêm padding
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            // Ghi file
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }

            return filePath;

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo style cho header
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Màu nền
        style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Font
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        // Căn giữa
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Border
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * Tạo style cho dữ liệu
     */
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Font
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        // Căn lề
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Border
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * Tạo style cho ngày tháng
     */
    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Chuyển đổi Role sang tên tiếng Việt
     */
    private static String getRoleDisplayName(com.enums.Role role) {
        if (role == null) return "";
        switch (role) {
            case MANAGER:
                return "Quản lý";
            case PHARMACIST:
                return "Dược sĩ";
            default:
                return role.toString();
        }
    }
}

