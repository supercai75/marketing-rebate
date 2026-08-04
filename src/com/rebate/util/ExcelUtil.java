package com.rebate.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Excel 工具（基于 Apache POI，xlsx）
 */
public class ExcelUtil {

    /**
     * 读取 Excel 第一行为表头，返回 List<Map<colName, value>>
     * 列名按 Excel 表头原始字符串
     */
    public static List<Map<String, String>> readSheetAsMap(InputStream in) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return result;
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return result;
            int colCount = headerRow.getLastCellNum();
            String[] headers = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                Cell c = headerRow.getCell(i);
                headers[i] = c == null ? "" : getString(c);
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                boolean emptyRow = true;
                for (int i = 0; i < colCount; i++) {
                    Cell c = row.getCell(i);
                    String v = c == null ? "" : getString(c);
                    if (!v.isEmpty()) emptyRow = false;
                    map.put(headers[i], v);
                }
                if (!emptyRow) result.add(map);
            }
        }
        return result;
    }

    public static String getString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return BigDecimal.valueOf(d).toPlainString();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }

    /**
     * 导出简单列表
     */
    public static Workbook exportSimple(List<String> headers, List<List<String>> rows) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");
        Row hr = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) hr.createCell(i).setCellValue(headers.get(i));
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            List<String> vals = rows.get(r);
            for (int c = 0; c < vals.size(); c++) {
                row.createCell(c).setCellValue(vals.get(c));
            }
        }
        return wb;
    }

    /**
     * 导出多页签Excel
     * @param sheets Map<sheetName, List<row>> 每行是一个List<String>
     */
    public static Workbook exportMultiSheet(Map<String, List<List<String>>> sheets) {
        Workbook wb = new XSSFWorkbook();
        for (Map.Entry<String, List<List<String>>> e : sheets.entrySet()) {
            Sheet sheet = wb.createSheet(e.getKey());
            List<List<String>> rows = e.getValue();
            if (rows == null || rows.isEmpty()) continue;
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r);
                List<String> vals = rows.get(r);
                if (vals == null) continue;
                for (int c = 0; c < vals.size(); c++) {
                    row.createCell(c).setCellValue(vals.get(c) == null ? "" : vals.get(c));
                }
            }
        }
        return wb;
    }
}
