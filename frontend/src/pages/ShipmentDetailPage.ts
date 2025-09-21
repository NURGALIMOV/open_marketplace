import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { router } from '../router';
import { Shipment, ShipmentItem, CreateShipmentItemRequest, UpdateShipmentItemRequest, PageResponse } from '../types';
import { Toast, Modal, Loading } from '../utils/ui';
import { formatDate, formatNumber, debounce } from '../utils/ui';

export function ShipmentDetailPage(): HTMLElement {
    const content = document.createElement('div');
    const params = router.getParams(router.getCurrentPath());
    const shopId = params.shopId;
    const shipmentId = params.shipmentId;

    if (!shopId || !shipmentId) {
        content.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-title">Отгрузка не найдена</div>
                <button class="btn btn-primary" onclick="router.back()">Назад</button>
            </div>
        `;
        return createLayout(content);
    }

    content.innerHTML = `
        <div class="mb-6">
            <button class="btn btn-outline btn-sm mb-4" onclick="router.navigate('/shops/${shopId}/shipments')">← К списку отгрузок</button>
            <div id="shipment-header">
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка информации об отгрузке...</p>
                </div>
            </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-2">
                <div class="card">
                    <div class="card-header">
                        <h2 class="card-title">Товары отгрузки</h2>
                        <div class="flex gap-2">
                            <button class="btn btn-primary btn-sm" id="add-item-btn">
                                ➕ Добавить товар
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
                                <p>Загрузка товаров отгрузки...</p>
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
                            <button class="btn btn-outline btn-full" id="edit-shipment-btn">
                                ✏️ Редактировать отгрузку
                            </button>
                            <button class="btn btn-danger btn-full" id="delete-shipment-btn">
                                🗑️ Удалить отгрузку
                            </button>
                        </div>
                    </div>
                </div>

                <div class="card mt-6">
                    <div class="card-header">
                        <h3 class="card-title">Принято всего</h3>
                    </div>
                    <div class="card-body">
                        <div id="total-quantity-display" class="text-center">
                            <div class="text-3xl font-bold text-blue-600">-</div>
                            <div class="text-sm text-gray-500">единиц</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;

    let currentPage = 0;
    let currentSearch = '';
    let currentShipment: Shipment | null = null;

    // Search handler with debounce
    const searchInput = content.querySelector('#search-input') as HTMLInputElement;
    const debouncedSearch = debounce((query: string) => {
        currentSearch = query;
        currentPage = 0;
        loadShipmentItems();
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
        
        if (target.matches('#edit-shipment-btn')) {
            showEditShipmentModal();
        }
        
        if (target.matches('#delete-shipment-btn')) {
            handleDeleteShipment();
        }
        
        if (target.matches('#prev-btn')) {
            if (currentPage > 0) {
                currentPage--;
                loadShipmentItems();
            }
        }
        
        if (target.matches('#next-btn')) {
            currentPage++;
            loadShipmentItems();
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

    async function loadShipmentData(): Promise<void> {
        try {
            currentShipment = await apiService.getShipment(shopId, shipmentId);
            renderShipmentHeader(currentShipment);
            updateTotalQuantityDisplay(currentShipment.totalQuantity);
        } catch (error) {
            console.error('Error loading shipment:', error);
            Toast.error('Ошибка загрузки данных отгрузки');
            
            const headerContainer = content.querySelector('#shipment-header') as HTMLElement;
            headerContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">Ошибка загрузки</div>
                    <div class="empty-state-description">Не удалось загрузить информацию об отгрузке</div>
                    <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
                </div>
            `;
        }
    }

    async function loadShipmentItems(): Promise<void> {
        try {
            const tableContainer = content.querySelector('#items-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка товаров отгрузки...</p>
                </div>
            `;

            const response = await apiService.getShipmentItems(
                shopId, 
                shipmentId,
                currentPage, 
                50, 
                currentSearch || undefined
            );

            renderItemsTable(response);
            updatePagination(response);
            
        } catch (error) {
            console.error('Error loading shipment items:', error);
            Toast.error('Ошибка загрузки товаров отгрузки');
            
            const tableContainer = content.querySelector('#items-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">Ошибка загрузки</div>
                    <div class="empty-state-description">Не удалось загрузить товары отгрузки</div>
                    <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
                </div>
            `;
        }
    }

    function renderShipmentHeader(shipment: Shipment): void {
        const headerContainer = content.querySelector('#shipment-header') as HTMLElement;
        
        headerContainer.innerHTML = `
            <div class="flex justify-between items-start">
                <div>
                    <h1 class="text-2xl font-semibold mb-2">${shipment.shipmentNumber}</h1>
                    <div class="flex gap-4 text-sm text-gray-600">
                        <span>Создана: ${formatDate(shipment.createdAt)}</span>
                        <span>Обновлена: ${formatDate(shipment.updatedAt)}</span>
                        <span>Дата отгрузки: ${formatDate(shipment.shipmentDate)}</span>
                    </div>
                </div>
                <div class="text-right">
                    <div class="text-3xl font-bold mb-1">${formatNumber(shipment.itemsCount)}</div>
                    <div class="text-sm text-gray-500">товаров в отгрузке</div>
                </div>
            </div>
        `;
    }

    function renderItemsTable(response: PageResponse<ShipmentItem>): void {
        const tableContainer = content.querySelector('#items-table') as HTMLElement;
        
        if (response.content.length === 0) {
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📦</div>
                    <div class="empty-state-title">${currentSearch ? 'Ничего не найдено' : 'Нет товаров'}</div>
                    <div class="empty-state-description">
                        ${currentSearch 
                            ? `По запросу "${currentSearch}" ничего не найдено`
                            : 'В отгрузке пока нет товаров'
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
                            <th>Создан</th>
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

    function updatePagination(response: PageResponse<ShipmentItem>): void {
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

    function updateTotalQuantityDisplay(totalQuantity: number): void {
        const totalQuantityDisplay = content.querySelector('#total-quantity-display .text-3xl') as HTMLElement;
        if (totalQuantityDisplay) {
            totalQuantityDisplay.textContent = formatNumber(totalQuantity);
        }
    }

    function showCreateItemModal(): void {
        const form = document.createElement('form');
        form.innerHTML = `
            <div class="form-group">
                <label class="form-label" for="sku">SKU *</label>
                <input type="text" id="sku" class="form-input" placeholder="SKU товара" required>
                <div class="form-error" id="sku-error"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="article">Артикул</label>
                <input type="text" id="article" class="form-input" placeholder="Артикул товара">
                <div class="form-error" id="article-error"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="quantity">Количество *</label>
                <input type="number" id="quantity" class="form-input" placeholder="0" min="1" required>
                <div class="form-error" id="quantity-error"></div>
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
            title: 'Добавить товар в отгрузку',
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
            
            const sku = skuInput.value.trim();
            const article = articleInput.value.trim() || undefined;
            const quantity = quantityInput.value ? parseInt(quantityInput.value) : 0;

            // Clear errors
            form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
            form.querySelectorAll('.form-input').forEach(el => el.classList.remove('error'));

            // Validate
            let hasErrors = false;
            
            if (!sku) {
                (form.querySelector('#sku-error') as HTMLElement).textContent = 'SKU обязателен';
                skuInput.classList.add('error');
                hasErrors = true;
            }
            
            if (quantity <= 0) {
                (form.querySelector('#quantity-error') as HTMLElement).textContent = 'Количество должно быть больше 0';
                quantityInput.classList.add('error');
                hasErrors = true;
            }

            if (hasErrors) {
                return;
            }

            const submitBtn = form.querySelector('#submit-btn') as HTMLButtonElement;
            submitBtn.disabled = true;
            submitBtn.textContent = 'Добавление...';

            try {
                const data: CreateShipmentItemRequest = { sku, article, quantity };
                await apiService.createShipmentItem(shopId, shipmentId, data);
                
                Toast.success('Товар добавлен в отгрузку');
                Modal.close();
                
                // Reload data
                await Promise.all([loadShipmentData(), loadShipmentItems()]);
                
            } catch (error: any) {
                console.error('Error creating shipment item:', error);
                
                if (error.response?.data?.message) {
                    Toast.error(`Ошибка добавления товара: ${error.response.data.message}`);
                } else {
                    Toast.error('Ошибка добавления товара');
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
            const item = await apiService.getShipmentItem(shopId, shipmentId, itemId);
            
            const form = document.createElement('form');
            form.innerHTML = `
                <div class="form-group">
                    <label class="form-label" for="sku">SKU *</label>
                    <input type="text" id="sku" class="form-input" value="${item.sku || ''}" required>
                    <div class="form-error" id="sku-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="article">Артикул</label>
                    <input type="text" id="article" class="form-input" value="${item.article || ''}">
                    <div class="form-error" id="article-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="quantity">Количество *</label>
                    <input type="number" id="quantity" class="form-input" value="${item.quantity || ''}" min="1" required>
                    <div class="form-error" id="quantity-error"></div>
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
                title: 'Редактировать товар отгрузки',
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
                
                const sku = skuInput.value.trim();
                const article = articleInput.value.trim() || undefined;
                const quantity = quantityInput.value ? parseInt(quantityInput.value) : 0;

                // Clear errors
                form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
                form.querySelectorAll('.form-input').forEach(el => el.classList.remove('error'));

                // Validate
                let hasErrors = false;
                
                if (!sku) {
                    (form.querySelector('#sku-error') as HTMLElement).textContent = 'SKU обязателен';
                    skuInput.classList.add('error');
                    hasErrors = true;
                }
                
                if (quantity <= 0) {
                    (form.querySelector('#quantity-error') as HTMLElement).textContent = 'Количество должно быть больше 0';
                    quantityInput.classList.add('error');
                    hasErrors = true;
                }

                if (hasErrors) {
                    return;
                }

                const submitBtn = form.querySelector('#submit-btn') as HTMLButtonElement;
                submitBtn.disabled = true;
                submitBtn.textContent = 'Сохранение...';

                try {
                    const data: UpdateShipmentItemRequest = { sku, article, quantity };
                    await apiService.updateShipmentItem(shopId, shipmentId, itemId, data);
                    
                    Toast.success('Товар отгрузки обновлен');
                    Modal.close();
                    
                    // Reload data
                    await Promise.all([loadShipmentData(), loadShipmentItems()]);
                    
                } catch (error: any) {
                    console.error('Error updating shipment item:', error);
                    
                    if (error.response?.data?.message) {
                        Toast.error(`Ошибка обновления товара: ${error.response.data.message}`);
                    } else {
                        Toast.error('Ошибка обновления товара');
                    }
                    
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Сохранить';
                }
            });
            
        } catch (error) {
            console.error('Error loading shipment item:', error);
            Toast.error('Ошибка загрузки данных товара');
        }
    }

    async function showEditShipmentModal(): Promise<void> {
        if (!currentShipment) return;

        const form = document.createElement('form');
        form.innerHTML = `
            <div class="form-group">
                <label class="form-label" for="shipment-number">Номер отгрузки *</label>
                <input type="text" id="shipment-number" class="form-input" value="${currentShipment.shipmentNumber}" required>
                <div class="form-error" id="shipment-number-error"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="shipment-date">Дата отгрузки *</label>
                <input type="date" id="shipment-date" class="form-input" value="${currentShipment.shipmentDate}" required>
                <div class="form-error" id="shipment-date-error"></div>
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
            title: 'Редактировать отгрузку',
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
            
            const shipmentNumberInput = form.querySelector('#shipment-number') as HTMLInputElement;
            const shipmentDateInput = form.querySelector('#shipment-date') as HTMLInputElement;
            
            const shipmentNumber = shipmentNumberInput.value.trim();
            const shipmentDate = shipmentDateInput.value;

            // Clear errors
            form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
            form.querySelectorAll('.form-input').forEach(el => el.classList.remove('error'));

            // Validate
            if (!shipmentNumber) {
                (form.querySelector('#shipment-number-error') as HTMLElement).textContent = 'Номер отгрузки обязателен';
                shipmentNumberInput.classList.add('error');
                shipmentNumberInput.focus();
                return;
            }
            
            if (!shipmentDate) {
                (form.querySelector('#shipment-date-error') as HTMLElement).textContent = 'Дата отгрузки обязательна';
                shipmentDateInput.classList.add('error');
                shipmentDateInput.focus();
                return;
            }

            const submitBtn = form.querySelector('#submit-btn') as HTMLButtonElement;
            submitBtn.disabled = true;
            submitBtn.textContent = 'Сохранение...';

            try {
                const data = { shipmentNumber, shipmentDate };
                await apiService.updateShipment(shopId, shipmentId, data);
                
                Toast.success('Отгрузка обновлена');
                Modal.close();
                
                // Reload shipment data
                await loadShipmentData();
                
            } catch (error: any) {
                console.error('Error updating shipment:', error);
                
                if (error.response?.data?.message) {
                    Toast.error(`Ошибка обновления отгрузки: ${error.response.data.message}`);
                } else {
                    Toast.error('Ошибка обновления отгрузки');
                }
                
                submitBtn.disabled = false;
                submitBtn.textContent = 'Сохранить';
            }
        });
    }

    async function handleDeleteItem(itemId: string, itemSku: string): Promise<void> {
        const confirmed = await Modal.confirm(
            `Вы уверены, что хотите удалить товар "${itemSku}"? Это действие нельзя отменить.`,
            'Удаление товара'
        );
        
        if (confirmed) {
            try {
                await apiService.deleteShipmentItem(shopId, shipmentId, itemId);
                Toast.success('Товар удален');
                
                // Reload data
                await Promise.all([loadShipmentData(), loadShipmentItems()]);
                
            } catch (error) {
                console.error('Error deleting shipment item:', error);
                Toast.error('Ошибка удаления товара');
            }
        }
    }

    async function handleDeleteShipment(): Promise<void> {
        if (!currentShipment) return;

        const confirmed = await Modal.confirm(
            `Вы уверены, что хотите удалить отгрузку "${currentShipment.shipmentNumber}"? Это действие нельзя отменить.`,
            'Удаление отгрузки'
        );
        
        if (confirmed) {
            try {
                await apiService.deleteShipment(shopId, shipmentId);
                Toast.success('Отгрузка удалена');
                router.navigate(`/shops/${shopId}/shipments`);
                
            } catch (error) {
                console.error('Error deleting shipment:', error);
                Toast.error('Ошибка удаления отгрузки');
            }
        }
    }

    // Initial load
    Promise.all([loadShipmentData(), loadShipmentItems()]);

    return createLayout(content);
}
