package com.openmarket.service;

import com.openmarket.dto.receipt.ImportError;
import com.openmarket.dto.shipment.ShipmentImportRequest;
import com.openmarket.dto.shipment.ShipmentImportResponse;
import com.openmarket.entity.Nomenclature;
import com.openmarket.entity.Shipment;
import com.openmarket.entity.ShipmentItem;
import com.openmarket.entity.Shop;
import com.openmarket.exception.AppAlreadyExistException;
import com.openmarket.exception.AppBusinessException;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentExcelImportService {

    private static final Pattern COLUMN_PATTERN = Pattern.compile("^[A-Z]+$");
    private static final long ONE_MB = 1024L * 1024L;
    private static final long MAX_FILE_SIZE = 10 * ONE_MB;
    private static final int MAX_ROWS = 100000;
    private static final String SKU_KEY = "sku";
    private static final String ARTICLE_KEY = "article";
    private static final String QUANTITY_KEY = "quantity";
    private static final List<String> REQUIRED_FIELDS = List.of(SKU_KEY, QUANTITY_KEY);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final AuditLogRepository auditLogRepository;
    private final ShipmentService shipmentService;
    private final ShopRepository shopRepository;

    /**
     * Import shipment from Excel file
     */
    @Transactional
    public ShipmentImportResponse importShipment(UUID shopId,
                                                 MultipartFile file,
                                                 ShipmentImportRequest importRequest,
                                                 UUID userId) {
        Shop shop = shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        validateFile(file);
        validateMapping(importRequest.getMapping());
        String shipmentNumber = file.getOriginalFilename();
        if (Objects.nonNull(shipmentNumber)) {
            shipmentNumber = shipmentNumber.replaceFirst("[.][^.]+$", "");
        } else {
            shipmentNumber = "Shipment_" + System.currentTimeMillis();
        }
        if (shipmentRepository.existsByShopIdAndShipmentNumber(shop.getId(), shipmentNumber)) {
            throw new AppAlreadyExistException("Shipment with number '%s' already exists".formatted(shipmentNumber));
        }
        Shipment shipment = shipmentRepository.saveShipment(shop, shipmentNumber, importRequest);

        Map<Long, Nomenclature> shopNomenclature = nomenclatureRepository.findByShopId(shop.getId()).stream().collect(Collectors.toMap(
                Nomenclature::getSku,
                Function.identity()
        ));

        List<ImportError> errors = new ArrayList<>();
        List<ShipmentItem> createdItems = new ArrayList<>();
        Set<Long> processedSkus = new HashSet<>();

        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            // Calculate start row: use user-specified startRow (1-based), but consider header
            int userStartRow = Objects.nonNull(importRequest.getStartRow()) ? importRequest.getStartRow() : 1;
            int startRow = importRequest.isHasHeader() ? Math.max(userStartRow, 2) : userStartRow;
            // Convert to 0-based index
            startRow = startRow - 1;
            int lastRowNum = sheet.getLastRowNum();
            
            if (lastRowNum - startRow + 1 > MAX_ROWS) {
                throw new AppBusinessException("Too many rows. Maximum allowed: %s".formatted(MAX_ROWS));
            }

            Map<String, Integer> columnMapping = parseColumnMapping(importRequest.getMapping());
            int rowsProcessed = 0;

            for (int rowIndex = startRow; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (Objects.nonNull(row)) {
                    rowsProcessed = rowProcessing(rowsProcessed, row, columnMapping, shipment, shopNomenclature, processedSkus, createdItems, errors, rowIndex, importRequest);
                }
            }

            if (!createdItems.isEmpty()) {
                shipmentItemRepository.saveAll(createdItems);
                shipmentService.recalculateTotalQuantity(shipment.getId());
            }

            // Reload shipment to get updated total quantity
            shipment = shipmentRepository.findById(shipment.getId()).orElseThrow();

            Map<String, Object> payload = Map.of(
                    "filename", file.getOriginalFilename(),
                    "rowsProcessed", rowsProcessed,
                    "rowsCreated", createdItems.size(),
                    "errorsCount", errors.size(),
                    "totalQuantityDelta", shipment.getTotalQuantity(),
                    "strict", importRequest.isStrict()
            );
            auditLogRepository.saveShipmentAuditLog(userId, shipment, "IMPORT_SHIPMENT_ITEMS", payload);

            log.info("Excel import completed for shipment {}: processed={}, created={}, errors={}",
                    shipment.getId(), rowsProcessed, createdItems.size(), errors.size());

            return ShipmentImportResponse.builder()
                    .shipmentId(shipment.getId())
                    .rowsProcessed(rowsProcessed)
                    .rowsCreated(createdItems.size())
                    .totalQuantityDelta(shipment.getTotalQuantity())
                    .errors(errors)
                    .build();
        } catch (IOException e) {
            throw new AppBusinessException("Failed to read Excel file: %s".formatted(e.getMessage()), e);
        }
    }

    private int rowProcessing(int rowsProcessed,
                              Row row,
                              Map<String, Integer> columnMapping,
                              Shipment shipment,
                              Map<Long, Nomenclature> shopNomenclature,
                              Set<Long> processedSkus,
                              List<ShipmentItem> createdItems,
                              List<ImportError> errors,
                              int rowIndex,
                              ShipmentImportRequest importRequest) {
        rowsProcessed++;
        try {
            ShipmentItem item = parseRowToShipmentItem(row, columnMapping, shipment, shopNomenclature, processedSkus);
            if (Objects.nonNull(item)) {
                createdItems.add(item);
            }
        } catch (Exception e) {
            errors.add(ImportError.builder()
                    .row(rowIndex + 1)
                    .error(e.getMessage())
                    .build());
            if (importRequest.isStrict()) {
                throw new AppBusinessException("Import failed at row %s: %s".formatted(rowIndex + 1, e.getMessage()));
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

    private ShipmentItem parseRowToShipmentItem(Row row, 
                                               Map<String, Integer> columnMapping, 
                                               Shipment shipment,
                                               Map<Long, Nomenclature> shopNomenclature,
                                               Set<Long> processedSkus) {
        String skuStr = getCellValueAsString(row, columnMapping.get(SKU_KEY));
        String article = getCellValueAsString(row, columnMapping.get(ARTICLE_KEY));
        String quantityStr = getCellValueAsString(row, columnMapping.get(QUANTITY_KEY));

        if (isEmptyRow(skuStr, article, quantityStr)) {
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

        // Check for duplicate SKU in current import
        if (processedSkus.contains(sku)) {
            throw new AppBusinessException("Duplicate SKU in file: %s".formatted(sku));
        }
        processedSkus.add(sku);

        // Parse quantity
        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr.trim());
            if (quantity <= 0) {
                throw new AppBusinessException("Quantity must be positive");
            }
        } catch (NumberFormatException e) {
            throw new AppBusinessException("Invalid quantity format: %s".formatted(quantityStr));
        }

        ShipmentItem item = new ShipmentItem();
        item.setShipment(shipment);
        item.setSku(sku);
        item.setArticle(article);
        item.setQuantity(quantity);
        
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
