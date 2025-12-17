package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final StatisticsService statisticsService;
    private final ExpenseService expenseService;

    public byte[] exportExpensesToExcel(ExpenseSearchRequest searchRequest) throws IOException {
        log.info("Exporting expenses to Excel with filters: {}", searchRequest);
        
        // Obtener gastos con paginación grande para exportar todos
        searchRequest.setPage(0);
        searchRequest.setSize(10000); // Máximo 10,000 registros
        var expensesPage = expenseService.searchExpenses(searchRequest);
        List<ExpenseResponse> expenses = expensesPage.getContent();
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Gastos");
        
        // Estilos
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        CellStyle currencyStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        currencyStyle.setDataFormat(format.getFormat("$#,##0.00"));
        
        CellStyle dateStyle = workbook.createCellStyle();
        DataFormat dateFormat = workbook.createDataFormat();
        dateStyle.setDataFormat(dateFormat.getFormat("dd/mm/yyyy"));
        
        // Encabezados
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Fecha", "Descripción", "Categoría", "Monto", "Método de Pago", "Notas"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Datos
        int rowNum = 1;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (ExpenseResponse expense : expenses) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(expense.getId());
            
            Cell dateCell = row.createCell(1);
            dateCell.setCellValue(expense.getExpenseDate().format(dateFormatter));
            dateCell.setCellStyle(dateStyle);
            
            row.createCell(2).setCellValue(expense.getDescription());
            row.createCell(3).setCellValue(expense.getCategory());
            
            Cell amountCell = row.createCell(4);
            amountCell.setCellValue(expense.getAmount().doubleValue());
            amountCell.setCellStyle(currencyStyle);
            
            row.createCell(5).setCellValue(expense.getPaymentMethod());
            row.createCell(6).setCellValue(expense.getNotes() != null ? expense.getNotes() : "");
            
            totalAmount = totalAmount.add(expense.getAmount());
        }
        
        // Fila de totales
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabelCell = totalRow.createCell(3);
        totalLabelCell.setCellValue("TOTAL:");
        totalLabelCell.setCellStyle(headerStyle);
        
        Cell totalAmountCell = totalRow.createCell(4);
        totalAmountCell.setCellValue(totalAmount.doubleValue());
        totalAmountCell.setCellStyle(currencyStyle);
        totalAmountCell.setCellStyle(headerStyle);
        
        // Auto-ajustar columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        byte[] data = outputStream.toByteArray();
        outputStream.close();
        workbook.close();
        
        log.info("Exported {} expenses to Excel. Total: {}", expenses.size(), totalAmount);
        return data;
    }

    public byte[] exportFinancialStatsToExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Exporting financial statistics to Excel from {} to {}", startDate, endDate);
        
        FinancialStatsResponse stats = statisticsService.getFinancialStats(startDate, endDate);
        BusinessStatsResponse businessStats = statisticsService.getBusinessStats();
        
        Workbook workbook = new XSSFWorkbook();
        
        try {
            // Hoja 1: Resumen Financiero
            Sheet summarySheet = workbook.createSheet("Resumen Financiero");
            createFinancialSummarySheet(summarySheet, stats, workbook);
            
            // Hoja 2: Gastos por Categoría
            Sheet categorySheet = workbook.createSheet("Gastos por Categoría");
            createCategoryExpensesSheet(categorySheet, stats, workbook);
            
            // Hoja 3: Evolución Diaria
            Sheet dailySheet = workbook.createSheet("Evolución Diaria");
            createDailyStatsSheet(dailySheet, stats, workbook);
            
            // Hoja 4: Estadísticas de Negocio
            Sheet businessSheet = workbook.createSheet("Estadísticas de Negocio");
            createBusinessStatsSheet(businessSheet, businessStats, workbook);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] data = outputStream.toByteArray();
            outputStream.close();
            
            log.info("Financial statistics exported to Excel");
            return data;
        } finally {
            workbook.close();
        }
    }

    private void createFinancialSummarySheet(Sheet sheet, FinancialStatsResponse stats, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        
        int rowNum = 0;
        
        // Encabezado
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("RESUMEN FINANCIERO");
        headerCell.setCellStyle(headerStyle);
        
        rowNum++; // Espacio
        
        // Ingresos
        createSummaryRow(sheet, rowNum++, "Ingresos Totales", stats.getTotalRevenue(), currencyStyle);
        createSummaryRow(sheet, rowNum++, "  - Pedidos Mesa", stats.getOrdersRevenue(), currencyStyle);
        createSummaryRow(sheet, rowNum++, "  - Domicilios", stats.getDeliveriesRevenue(), currencyStyle);
        rowNum++; // Espacio
        
        // Gastos
        createSummaryRow(sheet, rowNum++, "Gastos Totales", stats.getTotalExpenses(), currencyStyle);
        rowNum++; // Espacio
        
        // Ganancia
        createSummaryRow(sheet, rowNum++, "Ganancia Neta", stats.getNetProfit(), currencyStyle);
        
        // Margen de ganancia
        double profitMargin = 0;
        if (stats.getTotalRevenue().doubleValue() > 0) {
            profitMargin = ((stats.getNetProfit().doubleValue() / stats.getTotalRevenue().doubleValue()) * 100);
        }
        Row marginRow = sheet.createRow(rowNum++);
        marginRow.createCell(0).setCellValue("Margen de Ganancia (%)");
        Cell marginCell = marginRow.createCell(1);
        marginCell.setCellValue(profitMargin);
        CellStyle percentStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        percentStyle.setDataFormat(format.getFormat("0.00%"));
        marginCell.setCellStyle(percentStyle);
        
        // Auto-ajustar columnas
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createCategoryExpensesSheet(Sheet sheet, FinancialStatsResponse stats, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        
        // Encabezados
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Categoría");
        headerRow.createCell(1).setCellValue("Monto Total");
        
        for (int i = 0; i < 2; i++) {
            headerRow.getCell(i).setCellStyle(headerStyle);
        }
        
        // Datos
        int rowNum = 1;
        for (CategoryExpenseResponse category : stats.getExpensesByCategory()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(category.getCategory());
            Cell amountCell = row.createCell(1);
            amountCell.setCellValue(category.getTotalAmount().doubleValue());
            amountCell.setCellStyle(currencyStyle);
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createDailyStatsSheet(Sheet sheet, FinancialStatsResponse stats, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        
        // Encabezados
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Fecha", "Ingresos", "Gastos", "Ganancia", "Pedidos Mesa", "Domicilios"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Datos
        int rowNum = 1;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (DailyStatsResponse daily : stats.getDailyStats()) {
            Row row = sheet.createRow(rowNum++);
            
            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(daily.getDate().format(dateFormatter));
            dateCell.setCellStyle(dateStyle);
            
            Cell revenueCell = row.createCell(1);
            revenueCell.setCellValue(daily.getRevenue().doubleValue());
            revenueCell.setCellStyle(currencyStyle);
            
            Cell expensesCell = row.createCell(2);
            expensesCell.setCellValue(daily.getExpenses().doubleValue());
            expensesCell.setCellStyle(currencyStyle);
            
            Cell profitCell = row.createCell(3);
            profitCell.setCellValue(daily.getProfit().doubleValue());
            profitCell.setCellStyle(currencyStyle);
            
            row.createCell(4).setCellValue(daily.getOrdersCount());
            row.createCell(5).setCellValue(daily.getDeliveriesCount());
        }
        
        // Auto-ajustar columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createBusinessStatsSheet(Sheet sheet, BusinessStatsResponse stats, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        
        int rowNum = 0;
        
        // Estadísticas generales
        createSummaryRow(sheet, rowNum++, "Total Pedidos", BigDecimal.valueOf(stats.getTotalOrders()), null);
        createSummaryRow(sheet, rowNum++, "Total Domicilios", BigDecimal.valueOf(stats.getTotalDeliveries()), null);
        createSummaryRow(sheet, rowNum++, "Total Reservas", BigDecimal.valueOf(stats.getTotalReservations()), null);
        createSummaryRow(sheet, rowNum++, "Total Clientes", BigDecimal.valueOf(stats.getTotalCustomers()), null);
        createSummaryRow(sheet, rowNum++, "Total Productos", BigDecimal.valueOf(stats.getTotalProducts()), null);
        
        rowNum++; // Espacio
        
        // Top Productos
        Row productsHeaderRow = sheet.createRow(rowNum++);
        productsHeaderRow.createCell(0).setCellValue("TOP PRODUCTOS");
        productsHeaderRow.getCell(0).setCellStyle(headerStyle);
        
        Row productsSubHeaderRow = sheet.createRow(rowNum++);
        productsSubHeaderRow.createCell(0).setCellValue("Producto");
        productsSubHeaderRow.createCell(1).setCellValue("Cantidad");
        productsSubHeaderRow.createCell(2).setCellValue("Ingresos");
        for (int i = 0; i < 3; i++) {
            productsSubHeaderRow.getCell(i).setCellStyle(headerStyle);
        }
        
        for (TopProductResponse product : stats.getTopProducts()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(product.getProductName());
            row.createCell(1).setCellValue(product.getTotalQuantity());
            Cell revenueCell = row.createCell(2);
            revenueCell.setCellValue(product.getTotalRevenue().doubleValue());
            revenueCell.setCellStyle(currencyStyle);
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("dd/mm/yyyy"));
        return style;
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, BigDecimal value, CellStyle currencyStyle) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value.doubleValue());
        if (currencyStyle != null) {
            valueCell.setCellStyle(currencyStyle);
        }
    }

    public byte[] exportFinancialStatsToPdf(LocalDate startDate, LocalDate endDate) throws IOException {
        log.info("Exporting financial statistics to PDF from {} to {}", startDate, endDate);
        
        FinancialStatsResponse stats = statisticsService.getFinancialStats(startDate, endDate);
        BusinessStatsResponse businessStats = statisticsService.getBusinessStats();
        
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        try {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float yPosition = pageHeight - margin;
            
            // Fuentes
            PDType1Font titleFont = PDType1Font.HELVETICA_BOLD;
            PDType1Font headingFont = PDType1Font.HELVETICA_BOLD;
            PDType1Font normalFont = PDType1Font.HELVETICA;
            
            // Título
            String title = "REPORTE FINANCIERO";
            float titleWidth = titleFont.getStringWidth(title) / 1000 * 20;
            contentStream.beginText();
            contentStream.setFont(titleFont, 20);
            contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, yPosition);
            contentStream.showText(title);
            contentStream.endText();
            yPosition -= 40;
            
            // Período
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String period = String.format("Período: %s - %s", startDate.format(formatter), endDate.format(formatter));
            float periodWidth = normalFont.getStringWidth(period) / 1000 * 12;
            contentStream.beginText();
            contentStream.setFont(normalFont, 12);
            contentStream.newLineAtOffset((pageWidth - periodWidth) / 2, yPosition);
            contentStream.showText(period);
            contentStream.endText();
            yPosition -= 40;
            
            // Resumen Financiero
            yPosition = addSectionTitle(contentStream, "RESUMEN FINANCIERO", headingFont, margin, yPosition);
            yPosition -= 15;
            
            // Ingresos
            yPosition = addTableRow(contentStream, "Ingresos Totales", formatCurrency(stats.getTotalRevenue()), 
                    headingFont, normalFont, margin, yPosition, true);
            yPosition = addTableRow(contentStream, "  - Pedidos Mesa", formatCurrency(stats.getOrdersRevenue()), 
                    normalFont, normalFont, margin, yPosition, false);
            yPosition = addTableRow(contentStream, "  - Domicilios", formatCurrency(stats.getDeliveriesRevenue()), 
                    normalFont, normalFont, margin, yPosition, false);
            yPosition -= 10;
            
            // Gastos
            yPosition = addTableRow(contentStream, "Gastos Totales", formatCurrency(stats.getTotalExpenses()), 
                    headingFont, normalFont, margin, yPosition, true);
            yPosition -= 10;
            
            // Ganancia
            yPosition = addTableRow(contentStream, "Ganancia Neta", formatCurrency(stats.getNetProfit()), 
                    headingFont, normalFont, margin, yPosition, true);
            
            // Margen de ganancia
            double profitMargin = 0;
            if (stats.getTotalRevenue().doubleValue() > 0) {
                profitMargin = ((stats.getNetProfit().doubleValue() / stats.getTotalRevenue().doubleValue()) * 100);
            }
            yPosition = addTableRow(contentStream, "Margen de Ganancia (%)", String.format("%.2f%%", profitMargin), 
                    headingFont, normalFont, margin, yPosition, true);
            yPosition -= 20;
            
            // Gastos por Categoría
            if (!stats.getExpensesByCategory().isEmpty() && yPosition > margin + 100) {
                yPosition = addSectionTitle(contentStream, "GASTOS POR CATEGORÍA", headingFont, margin, yPosition);
                yPosition -= 15;
                
                // Encabezados
                contentStream.setLineWidth(1f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(pageWidth - margin, yPosition);
                contentStream.stroke();
                yPosition -= 20;
                
                yPosition = addTableRow(contentStream, "Categoría", "Monto Total", 
                        headingFont, headingFont, margin, yPosition, true);
                yPosition -= 5;
                
                for (CategoryExpenseResponse category : stats.getExpensesByCategory()) {
                    if (yPosition < margin + 50) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = pageHeight - margin;
                    }
                    yPosition = addTableRow(contentStream, category.getCategory(), formatCurrency(category.getTotalAmount()), 
                            normalFont, normalFont, margin, yPosition, false);
                }
                yPosition -= 20;
            }
            
            // Estadísticas del Negocio
            if (yPosition < margin + 150) {
                contentStream.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                yPosition = pageHeight - margin;
            }
            
            yPosition = addSectionTitle(contentStream, "ESTADÍSTICAS DEL NEGOCIO", headingFont, margin, yPosition);
            yPosition -= 15;
            
            yPosition = addTableRow(contentStream, "Total Pedidos", String.valueOf(businessStats.getTotalOrders()), 
                    normalFont, normalFont, margin, yPosition, false);
            yPosition = addTableRow(contentStream, "Total Domicilios", String.valueOf(businessStats.getTotalDeliveries()), 
                    normalFont, normalFont, margin, yPosition, false);
            yPosition = addTableRow(contentStream, "Total Reservas", String.valueOf(businessStats.getTotalReservations()), 
                    normalFont, normalFont, margin, yPosition, false);
            yPosition = addTableRow(contentStream, "Total Clientes", String.valueOf(businessStats.getTotalCustomers()), 
                    normalFont, normalFont, margin, yPosition, false);
            yPosition = addTableRow(contentStream, "Total Productos", String.valueOf(businessStats.getTotalProducts()), 
                    normalFont, normalFont, margin, yPosition, false);
            
            log.info("Financial statistics exported to PDF");
        } finally {
            contentStream.close();
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        document.close();
        
        return outputStream.toByteArray();
    }
    
    private float addSectionTitle(PDPageContentStream contentStream, String title, PDType1Font font, 
                                  float margin, float yPosition) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, 16);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(title);
        contentStream.endText();
        return yPosition - 25;
    }
    
    private float addTableRow(PDPageContentStream contentStream, String label, String value, 
                              PDType1Font labelFont, PDType1Font valueFont, 
                              float margin, float yPosition, boolean isBold) throws IOException {
        float pageWidth = 595; // A4 width in points
        float rightMargin = pageWidth - margin;
        float valueX = rightMargin - (valueFont.getStringWidth(value) / 1000 * 10);
        
        // Label
        contentStream.beginText();
        contentStream.setFont(labelFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(label);
        contentStream.endText();
        
        // Value (alineado a la derecha)
        contentStream.beginText();
        contentStream.setFont(valueFont, 10);
        contentStream.newLineAtOffset(valueX, yPosition);
        contentStream.showText(value);
        contentStream.endText();
        
        return yPosition - 20;
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%,.2f", amount.doubleValue());
    }
}

