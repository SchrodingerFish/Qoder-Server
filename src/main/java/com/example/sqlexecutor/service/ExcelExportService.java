package com.example.sqlexecutor.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.sqlexecutor.dto.MultiDatasourceQueryResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Excel导出服务
 */
@Slf4j
@Service
public class ExcelExportService {

    /**
     * 将多个数据源的查询结果导出为ZIP压缩包
     * 
     * @param results 多数据源查询结果列表
     * @return ZIP文件字节数组
     * @throws IOException IO异常
     */
    public byte[] exportMultiDatasourcesToZip(List<MultiDatasourceQueryResponse.DatasourceQueryResult> results)
            throws IOException {
        log.info("开始导出多数据源Excel为ZIP，数据源数量: {}", results.size());

        try (ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(zipOutputStream)) {

            for (MultiDatasourceQueryResponse.DatasourceQueryResult result : results) {
                String datasourceName = result.getDatasourceName();
                String datasourceCode = result.getDatasourceCode();

                log.debug("处理数据源: {} [{}]", datasourceName, datasourceCode);

                // 为每个数据源创建一个Excel文件
                byte[] excelData;
                String fileName;

                if (result.isSuccess() && result.getData() != null && !result.getData().isEmpty()) {
                    // 查询成功，导出数据
                    excelData = exportToExcel(result.getData(), datasourceName);
                    fileName = String.format("%s_%s.xlsx", datasourceCode, datasourceName);
                    log.debug("数据源 {} 导出成功，数据行数: {}", datasourceName, result.getData().size());
                } else if (!result.isSuccess()) {
                    // 查询失败，创建包含错误信息的Excel
                    excelData = createErrorExcel(datasourceName, result.getError(), result.getMessage());
                    fileName = String.format("%s_%s_错误.xlsx", datasourceCode, datasourceName);
                    log.debug("数据源 {} 查询失败，创建错误信息Excel", datasourceName);
                } else {
                    // 查询成功但无数据
                    excelData = exportToExcel(result.getData(), datasourceName);
                    fileName = String.format("%s_%s_无数据.xlsx", datasourceCode, datasourceName);
                    log.debug("数据源 {} 查询成功但无数据", datasourceName);
                }

                // 将Excel文件添加到ZIP
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                zos.write(excelData);
                zos.closeEntry();
            }

            zos.finish();
            byte[] zipData = zipOutputStream.toByteArray();
            log.info("多数据源Excel导出为ZIP完成，文件大小: {} bytes", zipData.length);
            return zipData;

        } catch (IOException e) {
            log.error("导出多数据源Excel为ZIP失败", e);
            throw e;
        }
    }

    /**
     * 创建包含错误信息的Excel文件
     * 
     * @param datasourceName 数据源名称
     * @param errorMessage   错误信息
     * @param additionalInfo 附加信息
     * @return Excel文件字节数组
     * @throws IOException IO异常
     */
    private byte[] createErrorExcel(String datasourceName, String errorMessage, String additionalInfo)
            throws IOException {
        log.debug("创建错误信息Excel: {}", datasourceName);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("错误信息");

            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle errorStyle = workbook.createCellStyle();
            Font errorFont = workbook.createFont();
            errorFont.setColor((short) 10); // 红色
            errorStyle.setFont(errorFont);

            // 创建标题行
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("查询失败");
            titleCell.setCellStyle(headerStyle);

            // 数据源名称
            Row datasourceRow = sheet.createRow(2);
            datasourceRow.createCell(0).setCellValue("数据源:");
            datasourceRow.createCell(1).setCellValue(datasourceName);

            // 错误信息
            Row errorRow = sheet.createRow(4);
            errorRow.createCell(0).setCellValue("错误信息:");
            Cell errorCell = errorRow.createCell(1);
            errorCell.setCellValue(errorMessage != null ? errorMessage : "未知错误");
            errorCell.setCellStyle(errorStyle);

            // 附加信息
            if (additionalInfo != null && !additionalInfo.isEmpty() &&
                    !additionalInfo.equals(errorMessage)) {
                Row infoRow = sheet.createRow(6);
                infoRow.createCell(0).setCellValue("详细信息:");
                infoRow.createCell(1).setCellValue(additionalInfo);
            }

            // 调整列宽
            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 15000);

            // 将工作簿写入字节数组
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    /**
     * 将查询结果导出为Excel文件
     */
    public byte[] exportToExcel(List<Map<String, Object>> data, String sheetName) throws IOException {
        log.debug("开始导出Excel，数据行数: {}", data != null ? data.size() : 0);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName != null ? sheetName : "查询结果");

            if (data.isEmpty()) {
                // 如果没有数据，创建一个空的工作表
                Row headerRow = sheet.createRow(0);
                Cell cell = headerRow.createCell(0);
                cell.setCellValue("无数据");
            } else {
                // 创建样式
                CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle dateStyle = createDateStyle(workbook);
                CellStyle numberStyle = createNumberStyle(workbook);
                CellStyle decimalStyle = createDecimalStyle(workbook);

                // 获取列名（从第一行数据中提取）
                Map<String, Object> firstRow = data.get(0);
                String[] columnNames = firstRow.keySet().toArray(new String[0]);

                // 创建表头
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columnNames.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columnNames[i]);
                    cell.setCellStyle(headerStyle);
                }

                // 填充数据
                for (int i = 0; i < data.size(); i++) {
                    Row dataRow = sheet.createRow(i + 1);
                    Map<String, Object> rowData = data.get(i);

                    for (int j = 0; j < columnNames.length; j++) {
                        Cell cell = dataRow.createCell(j);
                        Object value = rowData.get(columnNames[j]);

                        // 设置单元格值和样式
                        setCellValueWithStyle(cell, value, dateStyle, numberStyle, decimalStyle);
                    }
                }

                // 自动调整列宽
                for (int i = 0; i < columnNames.length; i++) {
                    sheet.autoSizeColumn(i);
                    // 设置最大列宽避免过宽
                    if (sheet.getColumnWidth(i) > 15000) {
                        sheet.setColumnWidth(i, 15000);
                    }
                    // 设置最小列宽保证可读性
                    if (sheet.getColumnWidth(i) < 2000) {
                        sheet.setColumnWidth(i, 2000);
                    }
                }
            }

            // 将工作簿写入字节数组
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        // 设置背景色
        style.setFillForegroundColor((short) 22); // 浅灰色
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

        // 设置边框
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

        return style;
    }

    /**
     * 设置单元格值，根据PostgreSQL数据类型进行处理
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        // 处理PostgreSQL特有的数据类型
        String className = value.getClass().getName();

        // 处理基本类型
        if (value instanceof String) {
            cell.setCellValue((String) value);
        }
        // 处理数值类型
        else if (value instanceof Integer || value instanceof Long ||
                value instanceof Short || value instanceof Byte) {
            cell.setCellValue(((Number) value).longValue());
        } else if (value instanceof Float || value instanceof Double) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof java.math.BigDecimal) {
            // PostgreSQL的DECIMAL/NUMERIC类型
            cell.setCellValue(((java.math.BigDecimal) value).doubleValue());
        } else if (value instanceof java.math.BigInteger) {
            // PostgreSQL的BIGINT类型
            cell.setCellValue(((java.math.BigInteger) value).longValue());
        }
        // 处理布尔类型
        else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        }
        // 处理日期时间类型
        else if (value instanceof java.sql.Date) {
            // PostgreSQL的DATE类型
            cell.setCellValue((java.sql.Date) value);
        } else if (value instanceof java.sql.Time) {
            // PostgreSQL的TIME类型
            cell.setCellValue(value.toString());
        } else if (value instanceof java.sql.Timestamp) {
            // PostgreSQL的TIMESTAMP类型
            java.sql.Timestamp timestamp = (java.sql.Timestamp) value;
            cell.setCellValue(new java.util.Date(timestamp.getTime()));
        } else if (value instanceof java.time.LocalDate) {
            // Java 8+ LocalDate
            java.time.LocalDate localDate = (java.time.LocalDate) value;
            cell.setCellValue(java.sql.Date.valueOf(localDate));
        } else if (value instanceof java.time.LocalTime) {
            // Java 8+ LocalTime
            cell.setCellValue(value.toString());
        } else if (value instanceof java.time.LocalDateTime) {
            // Java 8+ LocalDateTime
            java.time.LocalDateTime localDateTime = (java.time.LocalDateTime) value;
            cell.setCellValue(java.sql.Timestamp.valueOf(localDateTime));
        } else if (value instanceof java.time.OffsetDateTime) {
            // PostgreSQL的TIMESTAMPTZ类型
            java.time.OffsetDateTime offsetDateTime = (java.time.OffsetDateTime) value;
            cell.setCellValue(java.util.Date.from(offsetDateTime.toInstant()));
        } else if (value instanceof java.time.ZonedDateTime) {
            // PostgreSQL的TIMESTAMPTZ类型
            java.time.ZonedDateTime zonedDateTime = (java.time.ZonedDateTime) value;
            cell.setCellValue(java.util.Date.from(zonedDateTime.toInstant()));
        }
        // 处理PostgreSQL特有类型
        else if (className.equals("org.postgresql.util.PGobject")) {
            // PostgreSQL的JSON, JSONB, UUID等类型
            handlePGObject(cell, value);
        } else if (className.equals("org.postgresql.geometric.PGpoint")) {
            // PostgreSQL的POINT类型
            cell.setCellValue(value.toString());
        } else if (className.equals("org.postgresql.geometric.PGcircle")) {
            // PostgreSQL的CIRCLE类型
            cell.setCellValue(value.toString());
        } else if (className.equals("org.postgresql.geometric.PGpolygon")) {
            // PostgreSQL的POLYGON类型
            cell.setCellValue(value.toString());
        } else if (className.equals("org.postgresql.util.PGInterval")) {
            // PostgreSQL的INTERVAL类型
            cell.setCellValue(value.toString());
        } else if (value instanceof java.util.UUID) {
            // PostgreSQL的UUID类型
            cell.setCellValue(value.toString());
        }
        // 处理数组类型
        else if (value.getClass().isArray()) {
            handleArrayType(cell, value);
        }
        // 处理集合类型
        else if (value instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) value;
            cell.setCellValue(collection.toString());
        }
        // 处理Map类型（可能来自JSON）
        else if (value instanceof java.util.Map) {
            cell.setCellValue(value.toString());
        }
        // 处理字节数组（BYTEA类型）
        else if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            if (bytes.length <= 100) {
                // 小的字节数组转换为十六进制字符串
                cell.setCellValue(bytesToHex(bytes));
            } else {
                // 大的字节数组只显示长度信息
                cell.setCellValue("[BINARY DATA: " + bytes.length + " bytes]");
            }
        }
        // 其他类型转换为字符串
        else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 处理PostgreSQL的PGobject类型（JSON, JSONB, UUID等）
     */
    private void handlePGObject(Cell cell, Object pgObject) {
        try {
            // 使用反射获取PGobject的类型和值
            java.lang.reflect.Method getTypeMethod = pgObject.getClass().getMethod("getType");
            java.lang.reflect.Method getValueMethod = pgObject.getClass().getMethod("getValue");

            String type = (String) getTypeMethod.invoke(pgObject);
            String value = (String) getValueMethod.invoke(pgObject);

            if ("uuid".equals(type)) {
                // UUID类型
                cell.setCellValue(value);
            } else if ("json".equals(type) || "jsonb".equals(type)) {
                // JSON/JSONB类型，格式化输出
                cell.setCellValue(formatJson(value));
            } else {
                // 其他PGobject类型
                cell.setCellValue(value != null ? value : "");
            }
        } catch (Exception e) {
            // 反射失败时使用toString
            cell.setCellValue(pgObject.toString());
        }
    }

    /**
     * 处理数组类型
     */
    private void handleArrayType(Cell cell, Object array) {
        try {
            if (array instanceof Object[]) {
                Object[] objArray = (Object[]) array;
                if (objArray.length == 0) {
                    cell.setCellValue("[]");
                } else if (objArray.length <= 10) {
                    // 小数组直接显示内容
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < objArray.length; i++) {
                        if (i > 0)
                            sb.append(", ");
                        sb.append(objArray[i] != null ? objArray[i].toString() : "null");
                    }
                    sb.append("]");
                    cell.setCellValue(sb.toString());
                } else {
                    // 大数组只显示长度
                    cell.setCellValue("[ARRAY: " + objArray.length + " elements]");
                }
            } else {
                // 基本类型数组
                cell.setCellValue(array.toString());
            }
        } catch (Exception e) {
            cell.setCellValue(array.toString());
        }
    }

    /**
     * 格式化JSON字符串（简单版本）
     */
    private String formatJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "";
        }

        // 如果JSON太长，截断显示
        if (json.length() > 200) {
            return json.substring(0, 197) + "...";
        }

        return json;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 创建日期样式
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }

    /**
     * 创建数值样式
     */
    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0"));
        return style;
    }

    /**
     * 创建小数样式
     */
    private CellStyle createDecimalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    /**
     * 设置单元格值和样式
     */
    private void setCellValueWithStyle(Cell cell, Object value, CellStyle dateStyle,
            CellStyle numberStyle, CellStyle decimalStyle) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        // 根据数据类型设置值和样式
        if (value instanceof java.sql.Date || value instanceof java.sql.Timestamp ||
                value instanceof java.util.Date || value instanceof java.time.LocalDateTime ||
                value instanceof java.time.OffsetDateTime || value instanceof java.time.ZonedDateTime) {
            // 日期时间类型
            setCellValue(cell, value);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof Integer || value instanceof Long ||
                value instanceof Short || value instanceof Byte ||
                value instanceof java.math.BigInteger) {
            // 整数类型
            setCellValue(cell, value);
            cell.setCellStyle(numberStyle);
        } else if (value instanceof Float || value instanceof Double ||
                value instanceof java.math.BigDecimal) {
            // 小数类型
            setCellValue(cell, value);
            cell.setCellStyle(decimalStyle);
        } else {
            // 其他类型不设置特殊样式
            setCellValue(cell, value);
        }
    }
}