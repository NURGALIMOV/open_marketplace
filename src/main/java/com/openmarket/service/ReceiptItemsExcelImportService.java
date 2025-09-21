package com.openmarket.service;

import com.openmarket.dto.receipt.ExcelImportRequest;
import com.openmarket.dto.receipt.ExcelImportResponse;
import com.openmarket.dto.receipt.ImportError;
import com.openmarket.entity.Nomenclature;
import com.openmarket.entity.Receipt;
import com.openmarket.entity.ReceiptItem;
import com.openmarket.exception.AppBusinessException;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.repository.AuditLogRepository;
import com.openmarket.repository.NomenclatureRepository;
import com.openmarket.repository.ReceiptItemRepository;
import com.openmarket.repository.ReceiptRepository;
import com.openmarket.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptItemsExcelImportService {

    private static final Pattern COLUMN_PATTERN = Pattern.compile("^[A-Z]+$");
    private static final long ONE_MB = 1024L * 1024L;
    // 10MB
    private static final long MAX_FILE_SIZE = 10 * ONE_MB;
    private static final int MAX_ROWS = 100000;
    private static final String SKU_KEY = "sku";
    private static final String ARTICLE_KEY = "article";
    private static final String QUANTITY_KEY = "quantity";
    private static final String COST_KEY = "cost";
    private static final List<String> REQUIRED_FIELDS = List.of(SKU_KEY, ARTICLE_KEY, QUANTITY_KEY, COST_KEY);

    private final ReceiptItemRepository receiptItemRepository;
    private final ReceiptRepository receiptRepository;
    private final ShopRepository shopRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReceiptService receiptService;

    /**
     * Import receipt items from Excel file
     */
    @Transactional
    public ExcelImportResponse importReceiptItems(UUID shopId,
                                                  UUID receiptId,
                                                  MultipartFile file,
                                                  ExcelImportRequest importRequest,
                                                  UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        Receipt receipt = receiptRepository.findByIdAndShopId(receiptId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.RECEIPT_NOT_FOUND));
        validateFile(file);
        validateMapping(importRequest.getMapping());
        BigDecimal initialTotalCost = receipt.getTotalCost();
        
        // Load shop nomenclature for validation
        Map<Long, Nomenclature> shopNomenclature = nomenclatureRepository.findByShopId(shopId).stream().collect(Collectors.toMap(
                Nomenclature::getSku,
                Function.identity()
        ));
        
        List<ImportError> errors = new ArrayList<>();
        List<ReceiptItem> createdItems = new ArrayList<>();
        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            int startRow = importRequest.isHasHeader() ? 1 : 0;
            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum - startRow + 1 > MAX_ROWS) {
                throw new AppBusinessException("Too many rows. Maximum allowed: %s".formatted(MAX_ROWS));
            }
            Map<String, Integer> columnMapping = parseColumnMapping(importRequest.getMapping());
            int rowsProcessed = 0;
            for (int rowIndex = startRow; rowIndex <= lastRowNum; rowIndex++) {
                rowsProcessed = rowProcessing(importRequest, sheet, rowIndex, rowsProcessed, columnMapping, receipt, shopNomenclature, createdItems, errors);
            }
            if (!createdItems.isEmpty()) {
                receiptItemRepository.saveAll(createdItems);
                receiptService.recalculateTotalCost(receiptId);
            }
            receipt = receiptRepository.findById(receiptId).orElseThrow();
            BigDecimal totalCostDelta = receipt.getTotalCost().subtract(initialTotalCost);
            Map<String, Object> payload = Map.of(
                    "filename", file.getOriginalFilename(),
                    "rowsProcessed", rowsProcessed,
                    "rowsCreated", createdItems.size(),
                    "errorsCount", errors.size(),
                    "totalCostDelta", totalCostDelta,
                    "strict", importRequest.isStrict()
            );
            auditLogRepository.saveReceiptAuditLog(userId, receipt, "IMPORT_RECEIPT_ITEMS", payload);
            log.info("Excel import completed for receipt {}: processed={}, created={}, errors={}",
                    receiptId, rowsProcessed, createdItems.size(), errors.size());
            return ExcelImportResponse.builder()
                    .rowsProcessed(rowsProcessed)
                    .rowsCreated(createdItems.size())
                    .totalCostDelta(totalCostDelta)
                    .errors(errors)
                    .build();
        } catch (IOException e) {
            throw new AppBusinessException("Failed to read Excel file: %s".formatted(e.getMessage()), e);
        }
    }

    private int rowProcessing(ExcelImportRequest importRequest,
                              Sheet sheet,
                              int rowIndex,
                              int rowsProcessed,
                              Map<String, Integer> columnMapping,
                              Receipt receipt,
                              Map<Long, Nomenclature> shopNomenclature,
                              List<ReceiptItem> createdItems,
                              List<ImportError> errors) {
        Row row = sheet.getRow(rowIndex);
        if (Objects.nonNull(row)) {
            rowsProcessed++;
            try {
                Optional.ofNullable(parseRowToReceiptItem(row, columnMapping, receipt, shopNomenclature)).ifPresent(createdItems::add);
            } catch (Exception e) {
                errors.add(ImportError.builder().row(rowIndex + 1).error(e.getMessage()).build());
                if (importRequest.isStrict()) {
                    throw new AppBusinessException("Import failed at row %s: %s".formatted(rowIndex + 1, e.getMessage()));
                }
            }
        }
        return rowsProcessed;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppBusinessException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppBusinessException("File size exceeds maximum allowed size: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
        String filename = file.getOriginalFilename();
        if (Objects.isNull(filename) || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            throw new AppBusinessException("Only Excel files (.xlsx, .xls) are allowed");
        }
        if (filename.toLowerCase().endsWith(".xlsm")) {
            throw new AppBusinessException("Macro-enabled Excel files are not allowed for security reasons");
        }
    }

    private void validateMapping(Map<String, String> mapping) {
        if (Objects.isNull(mapping) || mapping.isEmpty()) {
            throw new AppBusinessException("Column mapping is required");
        }
        REQUIRED_FIELDS.forEach(field -> {
            String column = mapping.get(field);
            if (!StringUtils.hasText(column)) {
                throw new AppBusinessException("Column mapping for field '%s' is required".formatted(field));
            }
            if (!COLUMN_PATTERN.matcher(column.toUpperCase()).matches()) {
                throw new AppBusinessException("Invalid column format for field '%s': %s".formatted(field, column));
            }
        });
    }

    private Workbook createWorkbook(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        return (Objects.nonNull(filename) && filename.toLowerCase().endsWith(".xlsx")) ?
                new XSSFWorkbook(file.getInputStream()) : new HSSFWorkbook(file.getInputStream());
    }

    private Map<String, Integer> parseColumnMapping(Map<String, String> mapping) {
        return mapping.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    String column = entry.getValue().toUpperCase().trim();
                    return parseColumnToIndex(column);
                }
        ));
    }

    private int parseColumnToIndex(String column) {
        int index = 0;
        for (int i = 0; i < column.length(); i++) {
            index = index * 26 + (column.charAt(i) - 'A' + 1);
        }
        return index - 1;
    }

    private ReceiptItem parseRowToReceiptItem(Row row, Map<String, Integer> columnMapping, Receipt receipt, Map<Long, Nomenclature> shopNomenclature) {
        String skuStr = getCellValueAsString(row, columnMapping.get(SKU_KEY));
        String article = getCellValueAsString(row, columnMapping.get(ARTICLE_KEY));
        String quantityStr = getCellValueAsString(row, columnMapping.get(QUANTITY_KEY));
        String costStr = getCellValueAsString(row, columnMapping.get(COST_KEY));
        if (isEmptyRow(skuStr, article, quantityStr, costStr)) {
            return null;
        }
        
        // Parse and validate SKU
        Long sku;
        try {
            sku = Long.valueOf(skuStr.trim());
        } catch (NumberFormatException e) {
            throw new AppBusinessException("Invalid SKU format: %s".formatted(skuStr));
        }
        
        // Validate SKU exists in shop's nomenclature
        if (!shopNomenclature.containsKey(sku)) {
            throw new AppBusinessException("SKU %s not found in shop nomenclature".formatted(sku));
        }
        
        ReceiptItem item = new ReceiptItem();
        item.setReceipt(receipt);
        item.setSku(sku);
        item.setArticle(article);
        if (StringUtils.hasText(quantityStr)) {
            try {
                int quantity = Integer.parseInt(quantityStr.trim());
                if (quantity < 0) {
                    throw new AppBusinessException("Quantity must be non-negative");
                }
                item.setQuantity(quantity);
            } catch (NumberFormatException e) {
                throw new AppBusinessException("Invalid quantity format: %s".formatted(quantityStr));
            }
        }
        if (StringUtils.hasText(costStr)) {
            try {
                String normalizedCost = costStr.trim().replace(",", ".");
                BigDecimal cost = new BigDecimal(normalizedCost);
                if (cost.compareTo(BigDecimal.ZERO) < 0) {
                    throw new AppBusinessException("Cost must be non-negative");
                }
                item.setCost(cost);
            } catch (NumberFormatException e) {
                throw new AppBusinessException("Invalid cost format: %s".formatted(costStr));
            }
        }
        OffsetDateTime now = OffsetDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }

    private String getCellValueAsString(Row row, Integer columnIndex) {
        if (Objects.isNull(columnIndex) || columnIndex < 0) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (Objects.isNull(cell)) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        yield String.valueOf((long) numericValue);
                    } else {
                        yield String.valueOf(numericValue);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue().trim();
                }
            }
            default -> null;
        };
    }

    private boolean isEmptyRow(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }
}

