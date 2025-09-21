import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { router } from '../router';
import { Shipment, PageResponse, ShipmentImportResponse } from '../types';
import { Toast, Modal } from '../utils/ui';
import { formatDate, formatNumber, debounce } from '../utils/ui';

export function ShipmentsPage(): HTMLElement {
    const content = document.createElement('div');
    const params = router.getParams(router.getCurrentPath());
    const shopId = params.id;

    if (!shopId) {
        content.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-title">Магазин не найден</div>
                <button class="btn btn-primary" onclick="router.back()">Назад</button>
            </div>
        `;
        return createLayout(content);
    }

    content.innerHTML = `
        <div class="mb-6">
            <button class="btn btn-outline btn-sm mb-4" onclick="router.navigate('/shops/${shopId}')">← К магазину</button>
            <h1 class="text-xl font-semibold">Отгрузки</h1>
            <p class="text-gray-600 mt-1">Управление отгрузками товаров</p>
        </div>

        <div class="card">
            <div class="card-header">
                <div class="search-box">
                    <input type="text" 
                           id="search-input" 
                           class="search-input" 
                           placeholder="Поиск по номеру отгрузки...">
                    <span class="search-icon">🔍</span>
                </div>
                <button class="btn btn-primary" id="create-shipment-btn">
                    Создать отгрузку
                </button>
            </div>
            <div class="card-body">
                <div id="shipments-table">
                    <div class="loading">
                        <div class="loading-spinner"></div>
                        <p>Загрузка отгрузок...</p>
                    </div>
                </div>
                
                <div id="pagination-container" class="hidden">
                    <div class="pagination">
                        <button class="pagination-btn" id="prev-btn" disabled>← Предыдущая</button>
                        <span id="page-info"></span>
                        <button class="pagination-btn" id="next-btn" disabled>Следующая →</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    let currentPage = 0;
    let currentSearch = '';

    // Search handler with debounce
    const searchInput = content.querySelector('#search-input') as HTMLInputElement;
    const debouncedSearch = debounce((query: string) => {
        currentSearch = query;
        currentPage = 0;
        loadShipments();
    }, 300);

    searchInput.addEventListener('input', (e) => {
        const query = (e.target as HTMLInputElement).value.trim();
        debouncedSearch(query);
    });

    // Event handlers
    content.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        if (target.matches('#create-shipment-btn')) {
            showCreateShipmentModal();
        }
        
        if (target.matches('#prev-btn')) {
            if (currentPage > 0) {
                currentPage--;
                loadShipments();
            }
        }
        
        if (target.matches('#next-btn')) {
            currentPage++;
            loadShipments();
        }
        
        if (target.matches('.shipment-row')) {
            const shipmentId = target.dataset.shipmentId;
            if (shipmentId) {
                router.navigate(`/shops/${shopId}/shipments/${shipmentId}`);
            }
        }
        
        if (target.matches('.delete-shipment-btn')) {
            e.stopPropagation();
            const shipmentId = target.dataset.shipmentId;
            const shipmentNumber = target.dataset.shipmentNumber;
            if (shipmentId && shipmentNumber) {
                handleDeleteShipment(shipmentId, shipmentNumber);
            }
        }
    });

    async function loadShipments(): Promise<void> {
        try {
            const tableContainer = content.querySelector('#shipments-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка отгрузок...</p>
                </div>
            `;

            const response = await apiService.getShipments(
                shopId, 
                currentPage, 
                20, 
                currentSearch || undefined
            );

            renderShipmentsTable(response);
            updatePagination(response);
            
        } catch (error) {
            console.error('Error loading shipments:', error);
            Toast.error('Ошибка загрузки отгрузок');
            
            const tableContainer = content.querySelector('#shipments-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">Ошибка загрузки</div>
                    <div class="empty-state-description">Не удалось загрузить список отгрузок</div>
                    <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
                </div>
            `;
        }
    }

    function renderShipmentsTable(response: PageResponse<Shipment>): void {
        const tableContainer = content.querySelector('#shipments-table') as HTMLElement;
        
        if (response.content.length === 0) {
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🚚</div>
                    <div class="empty-state-title">${currentSearch ? 'Ничего не найдено' : 'Нет отгрузок'}</div>
                    <div class="empty-state-description">
                        ${currentSearch 
                            ? `По запросу "${currentSearch}" ничего не найдено`
                            : 'В магазине пока нет отгрузок'
                        }
                    </div>
                </div>
            `;
            return;
        }

        tableContainer.innerHTML = `
            <div class="table-container">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Номер отгрузки</th>
                            <th>Дата отгрузки</th>
                            <th>Принято всего</th>
                            <th>Товаров</th>
                            <th>Создана</th>
                            <th>Обновлена</th>
                            <th>Действия</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${response.content.map(shipment => `
                            <tr class="shipment-row cursor-pointer hover:bg-gray-50" data-shipment-id="${shipment.id}">
                                <td>
                                    <span class="font-medium">${shipment.shipmentNumber}</span>
                                </td>
                                <td>
                                    ${formatDate(shipment.shipmentDate)}
                                </td>
                                <td>
                                    <span class="font-medium text-blue-600">${formatNumber(shipment.totalQuantity)}</span>
                                </td>
                                <td>
                                    <span class="text-sm">${formatNumber(shipment.itemsCount)}</span>
                                </td>
                                <td>
                                    <span class="text-sm text-gray-600">${formatDate(shipment.createdAt)}</span>
                                </td>
                                <td>
                                    <span class="text-sm text-gray-600">${formatDate(shipment.updatedAt)}</span>
                                </td>
                                <td>
                                    <button class="btn btn-danger btn-sm delete-shipment-btn" 
                                            data-shipment-id="${shipment.id}"
                                            data-shipment-number="${shipment.shipmentNumber}"
                                            title="Удалить">
                                        🗑️
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }

    function updatePagination(response: PageResponse<Shipment>): void {
        const paginationContainer = content.querySelector('#pagination-container') as HTMLElement;
        const prevBtn = content.querySelector('#prev-btn') as HTMLButtonElement;
        const nextBtn = content.querySelector('#next-btn') as HTMLButtonElement;
        const pageInfo = content.querySelector('#page-info') as HTMLElement;

        if (response.totalPages <= 1) {
            paginationContainer.classList.add('hidden');
            return;
        }

        paginationContainer.classList.remove('hidden');
        
        prevBtn.disabled = response.first;
        nextBtn.disabled = response.last;
        
        const start = response.number * response.size + 1;
        const end = Math.min(start + response.size - 1, response.totalElements);
        
        pageInfo.textContent = `${start}-${end} из ${response.totalElements}`;
    }

    function showCreateShipmentModal(): void {
        showImportModal();
    }

    function showImportModal(): void {
        const container = document.createElement('div');
        container.innerHTML = `
            <div class="form-group">
                <label class="form-label" for="excel-file">Выберите Excel файл</label>
                <input type="file" id="excel-file" class="form-input" accept=".xlsx,.xls">
                <div class="form-error" id="file-error"></div>
                <div class="text-xs text-gray-500 mt-1">
                    Поддерживаются форматы: .xlsx, .xls (максимум 10MB)
                </div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="shipment-date">Дата отгрузки *</label>
                <input type="date" id="shipment-date" class="form-input" required>
                <div class="form-error" id="date-error"></div>
            </div>
            
            <div id="mapping-section" class="hidden">
                <h4 class="font-medium mb-4">Настройка колонок</h4>
                <div class="grid grid-cols-2 gap-4">
                    <div class="form-group">
                        <label class="form-label" for="sku-column">Колонка SKU *</label>
                        <select id="sku-column" class="form-select">
                            <option value="">Выберите колонку</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="article-column">Колонка Артикул</label>
                        <select id="article-column" class="form-select">
                            <option value="">Выберите колонку</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="quantity-column">Колонка Количество *</label>
                        <select id="quantity-column" class="form-select">
                            <option value="">Выберите колонку</option>
                        </select>
                    </div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="start-row">Начать с строки</label>
                    <input type="number" id="start-row" class="form-input" value="1" min="1" max="1000">
                    <div class="text-xs text-gray-500 mt-1">
                        Номер строки, с которой начинать парсинг данных (по умолчанию: 1)
                    </div>
                </div>
                
                <div class="form-group">
                    <label class="form-label">
                        <input type="checkbox" id="has-header" checked> Первая строка содержит заголовки
                    </label>
                </div>
                
                <div class="form-group">
                    <label class="form-label">
                        <input type="checkbox" id="strict-mode" checked> Строгий режим (прервать при ошибке)
                    </label>
                </div>
            </div>
        `;

        const footer = document.createElement('div');
        footer.innerHTML = `
            <button type="button" class="btn btn-secondary" data-action="cancel">Отмена</button>
            <button type="button" class="btn btn-primary hidden" id="import-submit-btn">Создать отгрузку</button>
        `;

        Modal.show({
            title: 'Создать отгрузку из Excel',
            content: container,
            footer,
        });

        const fileInput = container.querySelector('#excel-file') as HTMLInputElement;
        const shipmentDateInput = container.querySelector('#shipment-date') as HTMLInputElement;
        const mappingSection = container.querySelector('#mapping-section') as HTMLElement;
        const importBtn = footer.querySelector('#import-submit-btn') as HTMLButtonElement;

        // Set default date to today
        shipmentDateInput.value = new Date().toISOString().split('T')[0];

        // Update start row when header checkbox changes
        const hasHeaderCheckbox = container.querySelector('#has-header') as HTMLInputElement;
        const startRowInput = container.querySelector('#start-row') as HTMLInputElement;
        
        hasHeaderCheckbox.addEventListener('change', () => {
            if (hasHeaderCheckbox.checked) {
                startRowInput.value = '2';
            } else {
                startRowInput.value = '1';
            }
        });

        // Set initial value based on header checkbox
        if (hasHeaderCheckbox.checked) {
            startRowInput.value = '2';
        }

        fileInput.addEventListener('change', async (e) => {
            const file = (e.target as HTMLInputElement).files?.[0];
            if (file) {
                // Validate file
                const validationError = validateExcelFile(file);
                if (validationError) {
                    Toast.error(validationError);
                    fileInput.value = '';
                    return;
                }

                try {
                    // Parse headers using existing utility
                    const { parseExcelHeaders } = await import('../utils/excel');
                    const preview = await parseExcelHeaders(file);
                    
                    // Populate column selects with actual headers
                    const selects = container.querySelectorAll('select');
                    selects.forEach(select => {
                        select.innerHTML = '<option value="">Выберите колонку</option>' +
                            preview.headers.map(header => `
                                <option value="${header.column}">
                                    ${header.column} - ${header.value}
                                </option>
                            `).join('');
                    });

                    // Show preview info
                    const existingPreview = container.querySelector('#file-preview');
                    if (existingPreview) {
                        existingPreview.remove();
                    }

                    const previewDiv = document.createElement('div');
                    previewDiv.id = 'file-preview';
                    previewDiv.className = 'mb-4 p-3 bg-blue-50 border border-blue-200 rounded';
                    previewDiv.innerHTML = `
                        <div class="text-sm">
                            <strong>Файл:</strong> ${file.name}<br>
                            <strong>Строк:</strong> ${preview.rowCount}<br>
                            <strong>Колонок:</strong> ${preview.headers.length}
                        </div>
                    `;
                    
                    mappingSection.insertBefore(previewDiv, mappingSection.firstChild);
                    mappingSection.classList.remove('hidden');
                    importBtn.classList.remove('hidden');
                    
                } catch (error: any) {
                    console.error('Error processing file:', error);
                    Toast.error('Ошибка обработки файла: ' + error);
                    fileInput.value = '';
                }
            }
        });

        footer.addEventListener('click', async (e) => {
            const target = e.target as HTMLElement;
            
            if (target.dataset.action === 'cancel') {
                Modal.close();
                return;
            }
            
            if (target.matches('#import-submit-btn')) {
                const file = fileInput.files?.[0];
                const shipmentDate = shipmentDateInput.value;
                
                if (!file) {
                    Toast.error('Выберите файл для импорта');
                    return;
                }
                
                if (!shipmentDate) {
                    Toast.error('Укажите дату отгрузки');
                    return;
                }

                const skuColumn = (container.querySelector('#sku-column') as HTMLSelectElement).value;
                const articleColumn = (container.querySelector('#article-column') as HTMLSelectElement).value;
                const quantityColumn = (container.querySelector('#quantity-column') as HTMLSelectElement).value;
                const startRowInput = (container.querySelector('#start-row') as HTMLInputElement);
                const startRow = startRowInput.value ? parseInt(startRowInput.value) : 1;
                const hasHeader = (container.querySelector('#has-header') as HTMLInputElement).checked;
                const strict = (container.querySelector('#strict-mode') as HTMLInputElement).checked;

                if (!skuColumn || !quantityColumn) {
                    Toast.error('Необходимо выбрать колонки SKU и Количество');
                    return;
                }
                
                if (startRow < 1 || startRow > 1000) {
                    Toast.error('Стартовая строка должна быть от 1 до 1000');
                    return;
                }

                const mapping = {
                    sku: skuColumn,
                    article: articleColumn || '',
                    quantity: quantityColumn
                };

                (target as HTMLButtonElement).disabled = true;
                target.textContent = 'Создание...';

                try {
                    const result = await apiService.importShipment(shopId, file, mapping, shipmentDate, hasHeader, strict, startRow);
                    
                    let message = `Отгрузка создана: обработано ${result.rowsProcessed} строк, создано ${result.rowsCreated} товаров`;
                    if (result.errors.length > 0) {
                        message += `, ошибок: ${result.errors.length}`;
                    }
                    
                    Toast.success(message);
                    Modal.close();
                    
                    // Show detailed results if there were errors
                    if (result.errors.length > 0) {
                        showImportResults(result);
                    }
                    
                    // Navigate to shipment detail page
                    router.navigate(`/shops/${shopId}/shipments/${result.shipmentId}`);
                    
                } catch (error: any) {
                    console.error('Error importing file:', error);
                    
                    if (error.response?.data?.message) {
                        Toast.error(`Ошибка импорта: ${error.response.data.message}`);
                    } else {
                        Toast.error('Ошибка импорта файла');
                    }
                    
                    (target as HTMLButtonElement).disabled = false;
                    target.textContent = 'Создать отгрузку';
                }
            }
        });
    }

    function showImportResults(result: ShipmentImportResponse): void {
        const resultsHtml = `
            <div class="mb-4">
                <h4 class="font-medium mb-2">Результаты импорта</h4>
                <div class="grid grid-cols-3 gap-4 text-center">
                    <div>
                        <div class="text-2xl font-bold text-blue-600">${result.rowsProcessed}</div>
                        <div class="text-sm text-gray-500">Обработано строк</div>
                    </div>
                    <div>
                        <div class="text-2xl font-bold text-green-600">${result.rowsCreated}</div>
                        <div class="text-sm text-gray-500">Создано товаров</div>
                    </div>
                    <div>
                        <div class="text-2xl font-bold text-red-600">${result.errors.length}</div>
                        <div class="text-sm text-gray-500">Ошибок</div>
                    </div>
                </div>
            </div>
            
            ${result.errors.length > 0 ? `
                <div class="mb-4">
                    <h5 class="font-medium mb-2">Ошибки импорта:</h5>
                    <div class="max-h-60 overflow-y-auto">
                        <table class="table">
                            <thead>
                                <tr>
                                    <th>Строка</th>
                                    <th>Ошибка</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${result.errors.map(error => `
                                    <tr>
                                        <td>${error.row}</td>
                                        <td class="text-red-600">${error.error}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            ` : ''}
        `;

        const footer = document.createElement('div');
        footer.innerHTML = `
            <button type="button" class="btn btn-primary" data-action="close">Закрыть</button>
        `;

        Modal.show({
            title: 'Результаты импорта',
            content: resultsHtml,
            footer,
        });

        footer.addEventListener('click', (e) => {
            const target = e.target as HTMLElement;
            if (target.dataset.action === 'close') {
                Modal.close();
            }
        });
    }

    async function handleDeleteShipment(shipmentId: string, shipmentNumber: string): Promise<void> {
        const confirmed = await Modal.confirm(
            `Вы уверены, что хотите удалить отгрузку "${shipmentNumber}"? Это действие нельзя отменить.`,
            'Удаление отгрузки'
        );
        
        if (confirmed) {
            try {
                await apiService.deleteShipment(shopId, shipmentId);
                Toast.success('Отгрузка удалена');
                loadShipments();
                
            } catch (error) {
                console.error('Error deleting shipment:', error);
                Toast.error('Ошибка удаления отгрузки');
            }
        }
    }

    function validateExcelFile(file: File): string | null {
        if (!file) {
            return 'Файл не выбран';
        }
        
        if (file.size > 10 * 1024 * 1024) { // 10MB
            return 'Размер файла превышает 10MB';
        }
        
        const filename = file.name.toLowerCase();
        if (!filename.endsWith('.xlsx') && !filename.endsWith('.xls')) {
            return 'Поддерживаются только файлы Excel (.xlsx, .xls)';
        }
        
        if (filename.endsWith('.xlsm')) {
            return 'Файлы с макросами не поддерживаются по соображениям безопасности';
        }
        
        return null;
    }

    // Initial load
    loadShipments();

    return createLayout(content);
}
