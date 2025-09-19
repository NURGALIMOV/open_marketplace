import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { router } from '../router';
import { Receipt, ReceiptItem, CreateReceiptItemRequest, UpdateReceiptItemRequest, CounterpartyContract, PageResponse, ExcelImportResponse } from '../types';
import { Toast, Modal, Loading } from '../utils/ui';
import { formatDate, formatNumber, debounce } from '../utils/ui';
import { parseExcelHeaders, validateExcelFile, ExcelPreview } from '../utils/excel';

export function ReceiptDetailPage(): HTMLElement {
    const content = document.createElement('div');
    const params = router.getParams(router.getCurrentPath());
    const shopId = params.shopId;
    const receiptId = params.receiptId;

    if (!shopId || !receiptId) {
        content.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-title">Приемка не найдена</div>
                <button class="btn btn-primary" onclick="router.back()">Назад</button>
            </div>
        `;
        return createLayout(content);
    }

    content.innerHTML = `
        <div class="mb-6">
            <button class="btn btn-outline btn-sm mb-4" onclick="router.navigate('/shops/${shopId}/receipts')">← К списку приемок</button>
            <div id="receipt-header">
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка информации о приемке...</p>
                </div>
            </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-2">
                <div class="card">
                    <div class="card-header">
                        <h2 class="card-title">Единицы приемки</h2>
                        <div class="flex gap-2">
                            <button class="btn btn-outline btn-sm" id="import-btn">
                                📤 Импорт из Excel
                            </button>
                            <button class="btn btn-primary btn-sm" id="add-item-btn">
                                ➕ Добавить единицу
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <div class="search-box mb-4">
                            <input type="text" 
                                   id="search-input" 
                                   class="search-input" 
                                   placeholder="Поиск по SKU или артикулу...">
                            <span class="search-icon">🔍</span>
                        </div>
                        
                        <div id="items-table">
                            <div class="loading">
                                <div class="loading-spinner"></div>
                                <p>Загрузка единиц приемки...</p>
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
            </div>

            <div class="lg:col-span-1">
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">Действия</h3>
                    </div>
                    <div class="card-body">
                        <div class="space-y-4">
                            <button class="btn btn-outline btn-full" id="edit-receipt-btn">
                                ✏️ Редактировать приемку
                            </button>
                            <button class="btn btn-danger btn-full" id="delete-receipt-btn">
                                🗑️ Удалить приемку
                            </button>
                        </div>
                    </div>
                </div>

                <div class="card mt-6">
                    <div class="card-header">
                        <h3 class="card-title">Общая стоимость</h3>
                    </div>
                    <div class="card-body">
                        <div id="total-cost-display" class="text-center">
                            <div class="text-3xl font-bold text-blue-600">-</div>
                            <div class="text-sm text-gray-500">рублей</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;

    let currentPage = 0;
    let currentSearch = '';
    let currentReceipt: Receipt | null = null;

    // Search handler with debounce
    const searchInput = content.querySelector('#search-input') as HTMLInputElement;
    const debouncedSearch = debounce((query: string) => {
        currentSearch = query;
        currentPage = 0;
        loadReceiptItems();
    }, 300);

    searchInput.addEventListener('input', (e) => {
        const query = (e.target as HTMLInputElement).value.trim();
        debouncedSearch(query);
    });

    // Event handlers
    content.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        if (target.matches('#add-item-btn')) {
            showCreateItemModal();
        }
        
        if (target.matches('#import-btn')) {
            showImportModal();
        }
        
        if (target.matches('#edit-receipt-btn')) {
            showEditReceiptModal();
        }
        
        if (target.matches('#delete-receipt-btn')) {
            handleDeleteReceipt();
        }
        
        if (target.matches('#prev-btn')) {
            if (currentPage > 0) {
                currentPage--;
                loadReceiptItems();
            }
        }
        
        if (target.matches('#next-btn')) {
            currentPage++;
            loadReceiptItems();
        }
        
        if (target.matches('.edit-item-btn')) {
            const itemId = target.dataset.itemId;
            if (itemId) {
                showEditItemModal(itemId);
            }
        }
        
        if (target.matches('.delete-item-btn')) {
            const itemId = target.dataset.itemId;
            const itemSku = target.dataset.itemSku;
            if (itemId) {
                handleDeleteItem(itemId, itemSku || 'товар');
            }
        }
    });

    async function loadReceiptData(): Promise<void> {
        try {
            currentReceipt = await apiService.getReceipt(shopId, receiptId);
            renderReceiptHeader(currentReceipt);
            updateTotalCostDisplay(currentReceipt.totalCost);
        } catch (error) {
            console.error('Error loading receipt:', error);
            Toast.error('Ошибка загрузки данных приемки');
            
            const headerContainer = content.querySelector('#receipt-header') as HTMLElement;
            headerContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">Ошибка загрузки</div>
                    <div class="empty-state-description">Не удалось загрузить информацию о приемке</div>
                    <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
                </div>
            `;
        }
    }

    async function loadReceiptItems(): Promise<void> {
        try {
            const tableContainer = content.querySelector('#items-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка единиц приемки...</p>
                </div>
            `;

            const response = await apiService.getReceiptItems(
                shopId, 
                receiptId,
                currentPage, 
                50, 
                currentSearch || undefined
            );

            renderItemsTable(response);
            updatePagination(response);
            
        } catch (error) {
            console.error('Error loading receipt items:', error);
            Toast.error('Ошибка загрузки единиц приемки');
            
            const tableContainer = content.querySelector('#items-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">Ошибка загрузки</div>
                    <div class="empty-state-description">Не удалось загрузить единицы приемки</div>
                    <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
                </div>
            `;
        }
    }

    function renderReceiptHeader(receipt: Receipt): void {
        const headerContainer = content.querySelector('#receipt-header') as HTMLElement;
        
        headerContainer.innerHTML = `
            <div class="flex justify-between items-start">
                <div>
                    <h1 class="text-2xl font-semibold mb-2">${receipt.name}</h1>
                    ${receipt.requestNumber ? `
                        <div class="mb-2">
                            <span class="text-gray-500">Номер заявки:</span>
                            <span class="font-mono text-sm bg-gray-100 px-2 py-1 rounded">${receipt.requestNumber}</span>
                        </div>
                    ` : ''}
                    <div class="flex gap-4 text-sm text-gray-600">
                        <span>Создана: ${formatDate(receipt.createdAt)}</span>
                        <span>Обновлена: ${formatDate(receipt.updatedAt)}</span>
                        ${receipt.receiptDate ? `<span>Дата приемки: ${formatDate(receipt.receiptDate)}</span>` : ''}
                    </div>
                    ${receipt.counterpartyName ? `
                        <div class="mt-2">
                            <span class="text-gray-500">Контрагент:</span>
                            <span class="font-medium">${receipt.counterpartyName}</span>
                        </div>
                    ` : ''}
                    ${receipt.contract ? `
                        <div class="mt-1">
                            <span class="text-gray-500">Договор:</span>
                            <span class="font-mono text-sm">${receipt.contract}</span>
                        </div>
                    ` : ''}
                </div>
                <div class="text-right">
                    <div class="text-3xl font-bold mb-1">${formatNumber(receipt.itemsCount)}</div>
                    <div class="text-sm text-gray-500">единиц в приемке</div>
                </div>
            </div>
        `;
    }

    function renderItemsTable(response: PageResponse<ReceiptItem>): void {
        const tableContainer = content.querySelector('#items-table') as HTMLElement;
        
        if (response.content.length === 0) {
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📦</div>
                    <div class="empty-state-title">${currentSearch ? 'Ничего не найдено' : 'Нет единиц приемки'}</div>
                    <div class="empty-state-description">
                        ${currentSearch 
                            ? `По запросу "${currentSearch}" ничего не найдено`
                            : 'В приемке пока нет единиц товара'
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
                            <th>SKU</th>
                            <th>Артикул</th>
                            <th>Количество</th>
                            <th>Себестоимость</th>
                            <th>Сумма</th>
                            <th>Создана</th>
                            <th>Действия</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${response.content.map(item => `
                            <tr>
                                <td>
                                    <span class="font-mono text-sm">${item.sku || '-'}</span>
                                </td>
                                <td>
                                    <span class="text-sm">${item.article || '-'}</span>
                                </td>
                                <td>
                                    <span class="font-medium">${formatNumber(item.quantity || 0)}</span>
                                </td>
                                <td>
                                    <span class="font-medium">${formatNumber(item.cost || 0)} ₽</span>
                                </td>
                                <td>
                                    <span class="font-bold text-blue-600">${formatNumber(item.totalCost)} ₽</span>
                                </td>
                                <td>
                                    <span class="text-sm text-gray-600">${formatDate(item.createdAt)}</span>
                                </td>
                                <td>
                                    <div class="flex gap-2">
                                        <button class="btn btn-outline btn-sm edit-item-btn" 
                                                data-item-id="${item.id}"
                                                title="Редактировать">
                                            ✏️
                                        </button>
                                        <button class="btn btn-danger btn-sm delete-item-btn" 
                                                data-item-id="${item.id}"
                                                data-item-sku="${item.sku || ''}"
                                                title="Удалить">
                                            🗑️
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }

    function updatePagination(response: PageResponse<ReceiptItem>): void {
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
        
        pageInfo.textContent = `${start}-${end} из ${formatNumber(response.totalElements)}`;
    }

    function updateTotalCostDisplay(totalCost: number): void {
        const totalCostDisplay = content.querySelector('#total-cost-display .text-3xl') as HTMLElement;
        if (totalCostDisplay) {
            totalCostDisplay.textContent = formatNumber(totalCost);
        }
    }

    function showCreateItemModal(): void {
        const form = document.createElement('form');
        form.innerHTML = `
            <div class="form-group">
                <label class="form-label" for="sku">SKU</label>
                <input type="text" id="sku" class="form-input" placeholder="SKU товара">
                <div class="form-error" id="sku-error"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="article">Артикул</label>
                <input type="text" id="article" class="form-input" placeholder="Артикул товара">
                <div class="form-error" id="article-error"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="quantity">Количество</label>
                <input type="number" id="quantity" class="form-input" placeholder="0" min="0">
                <div class="form-error" id="quantity-error"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="cost">Себестоимость (₽)</label>
                <input type="number" id="cost" class="form-input" placeholder="0.00" min="0" step="0.01">
                <div class="form-error" id="cost-error"></div>
            </div>
        `;

        const formSubmitSection = document.createElement('div');
        formSubmitSection.className = 'mt-6';
        formSubmitSection.innerHTML = `
            <button type="submit" class="btn btn-primary btn-full" id="submit-btn">Добавить</button>
        `;
        form.appendChild(formSubmitSection);

        const footer = document.createElement('div');
        footer.innerHTML = `
            <button type="button" class="btn btn-secondary" data-action="cancel">Отмена</button>
        `;

        Modal.show({
            title: 'Добавить единицу приемки',
            content: form,
            footer,
        });

        footer.addEventListener('click', (e) => {
            const target = e.target as HTMLElement;
            if (target.dataset.action === 'cancel') {
                Modal.close();
            }
        });

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const skuInput = form.querySelector('#sku') as HTMLInputElement;
            const articleInput = form.querySelector('#article') as HTMLInputElement;
            const quantityInput = form.querySelector('#quantity') as HTMLInputElement;
            const costInput = form.querySelector('#cost') as HTMLInputElement;
            
            const sku = skuInput.value.trim() || undefined;
            const article = articleInput.value.trim() || undefined;
            const quantity = quantityInput.value ? parseInt(quantityInput.value) : undefined;
            const cost = costInput.value ? parseFloat(costInput.value) : undefined;

            // Clear errors
            form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
            form.querySelectorAll('.form-input').forEach(el => el.classList.remove('error'));

            const submitBtn = form.querySelector('#submit-btn') as HTMLButtonElement;
            submitBtn.disabled = true;
            submitBtn.textContent = 'Добавление...';

            try {
                const data: CreateReceiptItemRequest = { sku, article, quantity, cost };
                await apiService.createReceiptItem(shopId, receiptId, data);
                
                Toast.success('Единица приемки добавлена');
                Modal.close();
                
                // Reload data
                await Promise.all([loadReceiptData(), loadReceiptItems()]);
                
            } catch (error: any) {
                console.error('Error creating receipt item:', error);
                
                if (error.response?.data?.message) {
                    Toast.error(`Ошибка добавления единицы: ${error.response.data.message}`);
                } else {
                    Toast.error('Ошибка добавления единицы');
                }
                
                submitBtn.disabled = false;
                submitBtn.textContent = 'Добавить';
            }
        });

        // Focus on first input
        setTimeout(() => {
            const firstInput = form.querySelector('#sku') as HTMLInputElement;
            firstInput?.focus();
        }, 100);
    }

    async function showEditItemModal(itemId: string): Promise<void> {
        try {
            const item = await apiService.getReceiptItem(shopId, receiptId, itemId);
            
            const form = document.createElement('form');
            form.innerHTML = `
                <div class="form-group">
                    <label class="form-label" for="sku">SKU</label>
                    <input type="text" id="sku" class="form-input" value="${item.sku || ''}">
                    <div class="form-error" id="sku-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="article">Артикул</label>
                    <input type="text" id="article" class="form-input" value="${item.article || ''}">
                    <div class="form-error" id="article-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="quantity">Количество</label>
                    <input type="number" id="quantity" class="form-input" value="${item.quantity || ''}" min="0">
                    <div class="form-error" id="quantity-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="cost">Себестоимость (₽)</label>
                    <input type="number" id="cost" class="form-input" value="${item.cost || ''}" min="0" step="0.01">
                    <div class="form-error" id="cost-error"></div>
                </div>
            `;

            const formSubmitSection = document.createElement('div');
            formSubmitSection.className = 'mt-6';
            formSubmitSection.innerHTML = `
                <button type="submit" class="btn btn-primary btn-full" id="submit-btn">Сохранить</button>
            `;
            form.appendChild(formSubmitSection);

            const footer = document.createElement('div');
            footer.innerHTML = `
                <button type="button" class="btn btn-secondary" data-action="cancel">Отмена</button>
            `;

            Modal.show({
                title: 'Редактировать единицу приемки',
                content: form,
                footer,
            });

            footer.addEventListener('click', (e) => {
                const target = e.target as HTMLElement;
                if (target.dataset.action === 'cancel') {
                    Modal.close();
                }
            });

            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                
                const skuInput = form.querySelector('#sku') as HTMLInputElement;
                const articleInput = form.querySelector('#article') as HTMLInputElement;
                const quantityInput = form.querySelector('#quantity') as HTMLInputElement;
                const costInput = form.querySelector('#cost') as HTMLInputElement;
                
                const sku = skuInput.value.trim() || undefined;
                const article = articleInput.value.trim() || undefined;
                const quantity = quantityInput.value ? parseInt(quantityInput.value) : undefined;
                const cost = costInput.value ? parseFloat(costInput.value) : undefined;

                // Clear errors
                form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
                form.querySelectorAll('.form-input').forEach(el => el.classList.remove('error'));

                const submitBtn = form.querySelector('#submit-btn') as HTMLButtonElement;
                submitBtn.disabled = true;
                submitBtn.textContent = 'Сохранение...';

                try {
                    const data: UpdateReceiptItemRequest = { sku, article, quantity, cost };
                    await apiService.updateReceiptItem(shopId, receiptId, itemId, data);
                    
                    Toast.success('Единица приемки обновлена');
                    Modal.close();
                    
                    // Reload data
                    await Promise.all([loadReceiptData(), loadReceiptItems()]);
                    
                } catch (error: any) {
                    console.error('Error updating receipt item:', error);
                    
                    if (error.response?.data?.message) {
                        Toast.error(`Ошибка обновления единицы: ${error.response.data.message}`);
                    } else {
                        Toast.error('Ошибка обновления единицы');
                    }
                    
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Сохранить';
                }
            });
            
        } catch (error) {
            console.error('Error loading receipt item:', error);
            Toast.error('Ошибка загрузки данных единицы');
        }
    }

    async function showEditReceiptModal(): Promise<void> {
        if (!currentReceipt) return;

        try {
            // Load counterparty contracts for dropdown
            const counterparties = await apiService.getCounterpartyContractsList(shopId);
            
            const form = document.createElement('form');
            form.innerHTML = `
                <div class="form-group">
                    <label class="form-label" for="name">Название приемки *</label>
                    <input type="text" id="name" class="form-input" value="${currentReceipt.name}" required>
                    <div class="form-error" id="name-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="request-number">Номер заявки</label>
                    <input type="text" id="request-number" class="form-input" value="${currentReceipt.requestNumber || ''}">
                    <div class="form-error" id="request-number-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="receipt-date">Дата приемки</label>
                    <input type="date" id="receipt-date" class="form-input" value="${currentReceipt.receiptDate || ''}">
                    <div class="form-error" id="receipt-date-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="counterparty-contract">Контрагент</label>
                    <select id="counterparty-contract" class="form-select">
                        <option value="">Выберите контрагента</option>
                        ${counterparties.map(contract => `
                            <option value="${contract.id}" ${contract.id === currentReceipt?.counterpartyContractId ? 'selected' : ''}>
                                ${contract.counterparty} - ${contract.contract}
                            </option>
                        `).join('')}
                    </select>
                    <div class="form-error" id="counterparty-contract-error"></div>
                </div>
            `;

            const formSubmitSection = document.createElement('div');
            formSubmitSection.className = 'mt-6';
            formSubmitSection.innerHTML = `
                <button type="submit" class="btn btn-primary btn-full" id="submit-btn">Сохранить</button>
            `;
            form.appendChild(formSubmitSection);

            const footer = document.createElement('div');
            footer.innerHTML = `
                <button type="button" class="btn btn-secondary" data-action="cancel">Отмена</button>
            `;

            Modal.show({
                title: 'Редактировать приемку',
                content: form,
                footer,
            });

            footer.addEventListener('click', (e) => {
                const target = e.target as HTMLElement;
                if (target.dataset.action === 'cancel') {
                    Modal.close();
                }
            });

            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                
                const nameInput = form.querySelector('#name') as HTMLInputElement;
                const requestNumberInput = form.querySelector('#request-number') as HTMLInputElement;
                const receiptDateInput = form.querySelector('#receipt-date') as HTMLInputElement;
                const counterpartyContractSelect = form.querySelector('#counterparty-contract') as HTMLSelectElement;
                
                const name = nameInput.value.trim();
                const requestNumber = requestNumberInput.value.trim() || undefined;
                const receiptDate = receiptDateInput.value || undefined;
                const counterpartyContractId = counterpartyContractSelect.value || undefined;

                // Clear errors
                form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
                form.querySelectorAll('.form-input, .form-select').forEach(el => el.classList.remove('error'));

                // Validate
                if (!name) {
                    (form.querySelector('#name-error') as HTMLElement).textContent = 'Название обязательно';
                    nameInput.classList.add('error');
                    nameInput.focus();
                    return;
                }

                const submitBtn = form.querySelector('#submit-btn') as HTMLButtonElement;
                submitBtn.disabled = true;
                submitBtn.textContent = 'Сохранение...';

                try {
                    const data = { name, requestNumber, receiptDate, counterpartyContractId };
                    await apiService.updateReceipt(shopId, receiptId, data);
                    
                    Toast.success('Приемка обновлена');
                    Modal.close();
                    
                    // Reload receipt data
                    await loadReceiptData();
                    
                } catch (error: any) {
                    console.error('Error updating receipt:', error);
                    
                    if (error.response?.data?.message) {
                        Toast.error(`Ошибка обновления приемки: ${error.response.data.message}`);
                    } else {
                        Toast.error('Ошибка обновления приемки');
                    }
                    
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Сохранить';
                }
            });
            
        } catch (error) {
            console.error('Error loading counterparties:', error);
            Toast.error('Ошибка загрузки контрагентов');
        }
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
            
            <div id="mapping-section" class="hidden">
                <h4 class="font-medium mb-4">Настройка колонок</h4>
                <div class="grid grid-cols-2 gap-4">
                    <div class="form-group">
                        <label class="form-label" for="sku-column">Колонка SKU</label>
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
                        <label class="form-label" for="quantity-column">Колонка Количество</label>
                        <select id="quantity-column" class="form-select">
                            <option value="">Выберите колонку</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="cost-column">Колонка Себестоимость</label>
                        <select id="cost-column" class="form-select">
                            <option value="">Выберите колонку</option>
                        </select>
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
            <button type="button" class="btn btn-primary hidden" id="import-submit-btn">Импортировать</button>
        `;

        Modal.show({
            title: 'Импорт из Excel',
            content: container,
            footer,
        });

        const fileInput = container.querySelector('#excel-file') as HTMLInputElement;
        const mappingSection = container.querySelector('#mapping-section') as HTMLElement;
        const importBtn = footer.querySelector('#import-submit-btn') as HTMLButtonElement;

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
                    Loading.show('Анализ Excel файла...');
                    
                    // Parse headers using SheetJS
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
                    
                } catch (error) {
                    console.error('Error processing file:', error);
                    Toast.error('Ошибка обработки файла: ' + error);
                    fileInput.value = '';
                } finally {
                    Loading.hide();
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
                if (!file) {
                    Toast.error('Выберите файл для импорта');
                    return;
                }

                const skuColumn = (container.querySelector('#sku-column') as HTMLSelectElement).value;
                const articleColumn = (container.querySelector('#article-column') as HTMLSelectElement).value;
                const quantityColumn = (container.querySelector('#quantity-column') as HTMLSelectElement).value;
                const costColumn = (container.querySelector('#cost-column') as HTMLSelectElement).value;
                const hasHeader = (container.querySelector('#has-header') as HTMLInputElement).checked;
                const strict = (container.querySelector('#strict-mode') as HTMLInputElement).checked;

                if (!skuColumn || !articleColumn || !quantityColumn || !costColumn) {
                    Toast.error('Необходимо выбрать все колонки');
                    return;
                }

                const mapping = {
                    sku: skuColumn,
                    article: articleColumn,
                    quantity: quantityColumn,
                    cost: costColumn
                };

                (target as HTMLButtonElement).disabled = true;
                target.textContent = 'Импорт...';

                try {
                    const result = await apiService.importReceiptItems(shopId, receiptId, file, mapping, hasHeader, strict);
                    
                    let message = `Импорт завершен: обработано ${result.rowsProcessed} строк, создано ${result.rowsCreated} единиц`;
                    if (result.errors.length > 0) {
                        message += `, ошибок: ${result.errors.length}`;
                    }
                    
                    Toast.success(message);
                    Modal.close();
                    
                    // Show detailed results if there were errors
                    if (result.errors.length > 0) {
                        showImportResults(result);
                    }
                    
                    // Reload data
                    await Promise.all([loadReceiptData(), loadReceiptItems()]);
                    
                } catch (error: any) {
                    console.error('Error importing file:', error);
                    
                    if (error.response?.data?.message) {
                        Toast.error(`Ошибка импорта: ${error.response.data.message}`);
                    } else {
                        Toast.error('Ошибка импорта файла');
                    }
                    
                    (target as HTMLButtonElement).disabled = false;
                    target.textContent = 'Импортировать';
                }
            }
        });
    }

    function showImportResults(result: ExcelImportResponse): void {
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
                        <div class="text-sm text-gray-500">Создано единиц</div>
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

    async function handleDeleteItem(itemId: string, itemSku: string): Promise<void> {
        const confirmed = await Modal.confirm(
            `Вы уверены, что хотите удалить единицу "${itemSku}"? Это действие нельзя отменить.`,
            'Удаление единицы'
        );
        
        if (confirmed) {
            try {
                await apiService.deleteReceiptItem(shopId, receiptId, itemId);
                Toast.success('Единица удалена');
                
                // Reload data
                await Promise.all([loadReceiptData(), loadReceiptItems()]);
                
            } catch (error) {
                console.error('Error deleting receipt item:', error);
                Toast.error('Ошибка удаления единицы');
            }
        }
    }

    async function handleDeleteReceipt(): Promise<void> {
        if (!currentReceipt) return;

        const confirmed = await Modal.confirm(
            `Вы уверены, что хотите удалить приемку "${currentReceipt.name}"? Это действие нельзя отменить.`,
            'Удаление приемки'
        );
        
        if (confirmed) {
            try {
                await apiService.deleteReceipt(shopId, receiptId);
                Toast.success('Приемка удалена');
                router.navigate(`/shops/${shopId}/receipts`);
                
            } catch (error) {
                console.error('Error deleting receipt:', error);
                Toast.error('Ошибка удаления приемки');
            }
        }
    }

    // Initial load
    Promise.all([loadReceiptData(), loadReceiptItems()]);

    return createLayout(content);
}
