import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { router } from '../router';
import { Receipt, CreateReceiptRequest, CounterpartyContract, PageResponse } from '../types';
import { Toast, Modal } from '../utils/ui';
import { formatDate, formatNumber, debounce } from '../utils/ui';

export function ReceiptsPage(): HTMLElement {
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
            <h1 class="text-xl font-semibold">Приемки</h1>
            <p class="text-gray-600 mt-1">Управление приемками товаров</p>
        </div>

        <div class="card">
            <div class="card-header">
                <div class="search-box">
                    <input type="text" 
                           id="search-input" 
                           class="search-input" 
                           placeholder="Поиск по названию или номеру заявки...">
                    <span class="search-icon">🔍</span>
                </div>
                <button class="btn btn-primary" id="create-receipt-btn">
                    Создать приемку
                </button>
            </div>
            <div class="card-body">
                <div id="receipts-table">
                    <div class="loading">
                        <div class="loading-spinner"></div>
                        <p>Загрузка приемок...</p>
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
        loadReceipts();
    }, 300);

    searchInput.addEventListener('input', (e) => {
        const query = (e.target as HTMLInputElement).value.trim();
        debouncedSearch(query);
    });

    // Event handlers
    content.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        if (target.matches('#create-receipt-btn')) {
            showCreateReceiptModal();
        }
        
        if (target.matches('#prev-btn')) {
            if (currentPage > 0) {
                currentPage--;
                loadReceipts();
            }
        }
        
        if (target.matches('#next-btn')) {
            currentPage++;
            loadReceipts();
        }
        
        if (target.matches('.receipt-row')) {
            const receiptId = target.dataset.receiptId;
            if (receiptId) {
                router.navigate(`/shops/${shopId}/receipts/${receiptId}`);
            }
        }
        
        if (target.matches('.delete-receipt-btn')) {
            e.stopPropagation();
            const receiptId = target.dataset.receiptId;
            const receiptName = target.dataset.receiptName;
            if (receiptId && receiptName) {
                handleDeleteReceipt(receiptId, receiptName);
            }
        }
    });

    async function loadReceipts(): Promise<void> {
        try {
            const tableContainer = content.querySelector('#receipts-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка приемок...</p>
                </div>
            `;

            const response = await apiService.getReceipts(
                shopId, 
                currentPage, 
                20, 
                currentSearch || undefined
            );

            renderReceiptsTable(response);
            updatePagination(response);
            
        } catch (error) {
            console.error('Error loading receipts:', error);
            Toast.error('Ошибка загрузки приемок');
            
            const tableContainer = content.querySelector('#receipts-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">Ошибка загрузки</div>
                    <div class="empty-state-description">Не удалось загрузить список приемок</div>
                    <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
                </div>
            `;
        }
    }

    function renderReceiptsTable(response: PageResponse<Receipt>): void {
        const tableContainer = content.querySelector('#receipts-table') as HTMLElement;
        
        if (response.content.length === 0) {
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📦</div>
                    <div class="empty-state-title">${currentSearch ? 'Ничего не найдено' : 'Нет приемок'}</div>
                    <div class="empty-state-description">
                        ${currentSearch 
                            ? `По запросу "${currentSearch}" ничего не найдено`
                            : 'В магазине пока нет приемок'
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
                            <th>Название</th>
                            <th>Номер заявки</th>
                            <th>Дата приемки</th>
                            <th>Контрагент</th>
                            <th>Общая стоимость</th>
                            <th>Товаров</th>
                            <th>Обновлена</th>
                            <th>Действия</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${response.content.map(receipt => `
                            <tr class="receipt-row cursor-pointer hover:bg-gray-50" data-receipt-id="${receipt.id}">
                                <td>
                                    <span class="font-medium">${receipt.name}</span>
                                </td>
                                <td>
                                    ${receipt.requestNumber ? `<span class="font-mono text-sm">${receipt.requestNumber}</span>` : '-'}
                                </td>
                                <td>
                                    ${receipt.receiptDate ? formatDate(receipt.receiptDate) : '-'}
                                </td>
                                <td>
                                    ${receipt.counterpartyName || '-'}
                                </td>
                                <td>
                                    <span class="font-medium">${formatNumber(receipt.totalCost)} ₽</span>
                                </td>
                                <td>
                                    <span class="text-sm">${formatNumber(receipt.itemsCount)}</span>
                                </td>
                                <td>
                                    <span class="text-sm text-gray-600">${formatDate(receipt.updatedAt)}</span>
                                </td>
                                <td>
                                    <button class="btn btn-danger btn-sm delete-receipt-btn" 
                                            data-receipt-id="${receipt.id}"
                                            data-receipt-name="${receipt.name}"
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

    function updatePagination(response: PageResponse<Receipt>): void {
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

    async function showCreateReceiptModal(): Promise<void> {
        try {
            // Load counterparty contracts for dropdown
            const counterparties = await apiService.getCounterpartyContractsList(shopId);
            
            const form = document.createElement('form');
            form.innerHTML = `
                <div class="form-group">
                    <label class="form-label" for="name">Название приемки *</label>
                    <input type="text" id="name" class="form-input" placeholder="Название приемки" required>
                    <div class="form-error" id="name-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="request-number">Номер заявки</label>
                    <input type="text" id="request-number" class="form-input" placeholder="Номер заявки">
                    <div class="form-error" id="request-number-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="receipt-date">Дата приемки</label>
                    <input type="date" id="receipt-date" class="form-input">
                    <div class="form-error" id="receipt-date-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="counterparty-contract">Контрагент</label>
                    <select id="counterparty-contract" class="form-select">
                        <option value="">Выберите контрагента</option>
                        ${counterparties.map(contract => `
                            <option value="${contract.id}">${contract.counterparty} - ${contract.contract}</option>
                        `).join('')}
                    </select>
                    <div class="form-error" id="counterparty-contract-error"></div>
                </div>
            `;

            const formSubmitSection = document.createElement('div');
            formSubmitSection.className = 'mt-6';
            formSubmitSection.innerHTML = `
                <button type="submit" class="btn btn-primary btn-full" id="submit-btn">Создать</button>
            `;
            form.appendChild(formSubmitSection);

            const footer = document.createElement('div');
            footer.innerHTML = `
                <button type="button" class="btn btn-secondary" data-action="cancel">Отмена</button>
            `;

            Modal.show({
                title: 'Создать приемку',
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
                submitBtn.textContent = 'Создание...';

                try {
                    const data: CreateReceiptRequest = { 
                        name, 
                        requestNumber,
                        receiptDate,
                        counterpartyContractId
                    };
                    const receipt = await apiService.createReceipt(shopId, data);
                    
                    Toast.success('Приемка создана успешно');
                    Modal.close();
                    
                    // Navigate to receipt detail page
                    router.navigate(`/shops/${shopId}/receipts/${receipt.id}`);
                    
                } catch (error: any) {
                    console.error('Error creating receipt:', error);
                    
                    if (error.response?.data?.message) {
                        Toast.error(`Ошибка создания приемки: ${error.response.data.message}`);
                    } else {
                        Toast.error('Ошибка создания приемки');
                    }
                    
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Создать';
                }
            });

            // Focus on first input
            setTimeout(() => {
                const firstInput = form.querySelector('#name') as HTMLInputElement;
                firstInput?.focus();
            }, 100);
            
        } catch (error) {
            console.error('Error loading counterparties:', error);
            Toast.error('Ошибка загрузки контрагентов');
        }
    }

    async function handleDeleteReceipt(receiptId: string, receiptName: string): Promise<void> {
        const confirmed = await Modal.confirm(
            `Вы уверены, что хотите удалить приемку "${receiptName}"? Это действие нельзя отменить.`,
            'Удаление приемки'
        );
        
        if (confirmed) {
            try {
                await apiService.deleteReceipt(shopId, receiptId);
                Toast.success('Приемка удалена');
                loadReceipts();
                
            } catch (error) {
                console.error('Error deleting receipt:', error);
                Toast.error('Ошибка удаления приемки');
            }
        }
    }

    // Initial load
    loadReceipts();

    return createLayout(content);
}

