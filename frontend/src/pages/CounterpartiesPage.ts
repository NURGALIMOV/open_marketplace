import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { router } from '../router';
import { CounterpartyContract, CreateCounterpartyContractRequest, UpdateCounterpartyContractRequest, PageResponse } from '../types';
import { Toast, Modal } from '../utils/ui';
import { formatDate, debounce } from '../utils/ui';

export function CounterpartiesPage(): HTMLElement {
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
            <h1 class="text-xl font-semibold">Контрагенты и договоры</h1>
            <p class="text-gray-600 mt-1">Управление контрагентами и договорами магазина</p>
        </div>

        <div class="card">
            <div class="card-header">
                <div class="search-box">
                    <input type="text" 
                           id="search-input" 
                           class="search-input" 
                           placeholder="Поиск по контрагенту или договору...">
                    <span class="search-icon">🔍</span>
                </div>
                <button class="btn btn-primary" id="create-counterparty-btn">
                    Создать контрагента
                </button>
            </div>
            <div class="card-body">
                <div id="counterparties-table">
                    <div class="loading">
                        <div class="loading-spinner"></div>
                        <p>Загрузка контрагентов...</p>
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
        loadCounterparties();
    }, 300);

    searchInput.addEventListener('input', (e) => {
        const query = (e.target as HTMLInputElement).value.trim();
        debouncedSearch(query);
    });

    // Event handlers
    content.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        if (target.matches('#create-counterparty-btn')) {
            showCreateCounterpartyModal();
        }
        
        if (target.matches('#prev-btn')) {
            if (currentPage > 0) {
                currentPage--;
                loadCounterparties();
            }
        }
        
        if (target.matches('#next-btn')) {
            currentPage++;
            loadCounterparties();
        }
        
        if (target.matches('.edit-counterparty-btn')) {
            const contractId = target.dataset.contractId;
            if (contractId) {
                showEditCounterpartyModal(contractId);
            }
        }
        
        if (target.matches('.delete-counterparty-btn')) {
            const contractId = target.dataset.contractId;
            const counterpartyName = target.dataset.counterpartyName;
            if (contractId && counterpartyName) {
                handleDeleteCounterparty(contractId, counterpartyName);
            }
        }
    });

    async function loadCounterparties(): Promise<void> {
        try {
            const tableContainer = content.querySelector('#counterparties-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка контрагентов...</p>
                </div>
            `;

            const response = await apiService.getCounterpartyContracts(
                shopId, 
                currentPage, 
                20, 
                currentSearch || undefined
            );

            renderCounterpartiesTable(response);
            updatePagination(response);
            
        } catch (error) {
            console.error('Error loading counterparties:', error);
            Toast.error('Ошибка загрузки контрагентов');
            
            const tableContainer = content.querySelector('#counterparties-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">Ошибка загрузки</div>
                    <div class="empty-state-description">Не удалось загрузить список контрагентов</div>
                    <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
                </div>
            `;
        }
    }

    function renderCounterpartiesTable(response: PageResponse<CounterpartyContract>): void {
        const tableContainer = content.querySelector('#counterparties-table') as HTMLElement;
        
        if (response.content.length === 0) {
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🤝</div>
                    <div class="empty-state-title">${currentSearch ? 'Ничего не найдено' : 'Нет контрагентов'}</div>
                    <div class="empty-state-description">
                        ${currentSearch 
                            ? `По запросу "${currentSearch}" ничего не найдено`
                            : 'В магазине пока нет контрагентов'
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
                            <th>Контрагент</th>
                            <th>Договор</th>
                            <th>Дата договора</th>
                            <th>Создан</th>
                            <th>Обновлен</th>
                            <th>Действия</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${response.content.map(contract => `
                            <tr>
                                <td>
                                    <span class="font-medium">${contract.counterparty}</span>
                                </td>
                                <td>
                                    <span class="font-mono text-sm">${contract.contract}</span>
                                </td>
                                <td>
                                    ${contract.contractDate ? formatDate(contract.contractDate) : '-'}
                                </td>
                                <td>
                                    <span class="text-sm text-gray-600">${formatDate(contract.createdAt)}</span>
                                </td>
                                <td>
                                    <span class="text-sm text-gray-600">${formatDate(contract.updatedAt)}</span>
                                </td>
                                <td>
                                    <div class="flex gap-2">
                                        <button class="btn btn-outline btn-sm edit-counterparty-btn" 
                                                data-contract-id="${contract.id}"
                                                title="Редактировать">
                                            ✏️
                                        </button>
                                        <button class="btn btn-danger btn-sm delete-counterparty-btn" 
                                                data-contract-id="${contract.id}"
                                                data-counterparty-name="${contract.counterparty}"
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

    function updatePagination(response: PageResponse<CounterpartyContract>): void {
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

    function showCreateCounterpartyModal(): void {
        const form = document.createElement('form');
        form.innerHTML = `
            <div class="form-group">
                <label class="form-label" for="counterparty">Контрагент *</label>
                <input type="text" id="counterparty" class="form-input" placeholder="Название контрагента" required>
                <div class="form-error" id="counterparty-error"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="contract">Договор *</label>
                <input type="text" id="contract" class="form-input" placeholder="Номер или название договора" required>
                <div class="form-error" id="contract-error"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="contract-date">Дата договора</label>
                <input type="date" id="contract-date" class="form-input">
                <div class="form-error" id="contract-date-error"></div>
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
            title: 'Создать контрагента',
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
            
            const counterpartyInput = form.querySelector('#counterparty') as HTMLInputElement;
            const contractInput = form.querySelector('#contract') as HTMLInputElement;
            const contractDateInput = form.querySelector('#contract-date') as HTMLInputElement;
            
            const counterparty = counterpartyInput.value.trim();
            const contract = contractInput.value.trim();
            const contractDate = contractDateInput.value || undefined;

            // Clear errors
            form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
            form.querySelectorAll('.form-input').forEach(el => el.classList.remove('error'));

            // Validate
            let hasErrors = false;

            if (!counterparty) {
                (form.querySelector('#counterparty-error') as HTMLElement).textContent = 'Контрагент обязателен';
                counterpartyInput.classList.add('error');
                hasErrors = true;
            }

            if (!contract) {
                (form.querySelector('#contract-error') as HTMLElement).textContent = 'Договор обязателен';
                contractInput.classList.add('error');
                hasErrors = true;
            }

            if (hasErrors) {
                return;
            }

            const submitBtn = form.querySelector('#submit-btn') as HTMLButtonElement;
            submitBtn.disabled = true;
            submitBtn.textContent = 'Создание...';

            try {
                const data: CreateCounterpartyContractRequest = { 
                    counterparty, 
                    contract,
                    contractDate
                };
                await apiService.createCounterpartyContract(shopId, data);
                
                Toast.success('Контрагент создан успешно');
                Modal.close();
                loadCounterparties();
                
            } catch (error: any) {
                console.error('Error creating counterparty:', error);
                
                if (error.response?.data?.message) {
                    Toast.error(`Ошибка создания контрагента: ${error.response.data.message}`);
                } else {
                    Toast.error('Ошибка создания контрагента');
                }
                
                submitBtn.disabled = false;
                submitBtn.textContent = 'Создать';
            }
        });

        // Focus on first input
        setTimeout(() => {
            const firstInput = form.querySelector('#counterparty') as HTMLInputElement;
            firstInput?.focus();
        }, 100);
    }

    async function showEditCounterpartyModal(contractId: string): Promise<void> {
        try {
            const contract = await apiService.getCounterpartyContract(shopId, contractId);
            
            const form = document.createElement('form');
            form.innerHTML = `
                <div class="form-group">
                    <label class="form-label" for="counterparty">Контрагент *</label>
                    <input type="text" id="counterparty" class="form-input" value="${contract.counterparty}" required>
                    <div class="form-error" id="counterparty-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="contract">Договор *</label>
                    <input type="text" id="contract" class="form-input" value="${contract.contract}" required>
                    <div class="form-error" id="contract-error"></div>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="contract-date">Дата договора</label>
                    <input type="date" id="contract-date" class="form-input" value="${contract.contractDate || ''}">
                    <div class="form-error" id="contract-date-error"></div>
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
                title: 'Редактировать контрагента',
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
                
                const counterpartyInput = form.querySelector('#counterparty') as HTMLInputElement;
                const contractInput = form.querySelector('#contract') as HTMLInputElement;
                const contractDateInput = form.querySelector('#contract-date') as HTMLInputElement;
                
                const counterparty = counterpartyInput.value.trim();
                const contractValue = contractInput.value.trim();
                const contractDate = contractDateInput.value || undefined;

                // Clear errors
                form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
                form.querySelectorAll('.form-input').forEach(el => el.classList.remove('error'));

                // Validate
                let hasErrors = false;

                if (!counterparty) {
                    (form.querySelector('#counterparty-error') as HTMLElement).textContent = 'Контрагент обязателен';
                    counterpartyInput.classList.add('error');
                    hasErrors = true;
                }

                if (!contractValue) {
                    (form.querySelector('#contract-error') as HTMLElement).textContent = 'Договор обязателен';
                    contractInput.classList.add('error');
                    hasErrors = true;
                }

                if (hasErrors) {
                    return;
                }

                const submitBtn = form.querySelector('#submit-btn') as HTMLButtonElement;
                submitBtn.disabled = true;
                submitBtn.textContent = 'Сохранение...';

                try {
                    const data: UpdateCounterpartyContractRequest = { 
                        counterparty, 
                        contract: contractValue,
                        contractDate
                    };
                    await apiService.updateCounterpartyContract(shopId, contractId, data);
                    
                    Toast.success('Контрагент обновлен успешно');
                    Modal.close();
                    loadCounterparties();
                    
                } catch (error: any) {
                    console.error('Error updating counterparty:', error);
                    
                    if (error.response?.data?.message) {
                        Toast.error(`Ошибка обновления контрагента: ${error.response.data.message}`);
                    } else {
                        Toast.error('Ошибка обновления контрагента');
                    }
                    
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Сохранить';
                }
            });
            
        } catch (error) {
            console.error('Error loading counterparty:', error);
            Toast.error('Ошибка загрузки данных контрагента');
        }
    }

    async function handleDeleteCounterparty(contractId: string, counterpartyName: string): Promise<void> {
        const confirmed = await Modal.confirm(
            `Вы уверены, что хотите удалить контрагента "${counterpartyName}"? Это действие нельзя отменить.`,
            'Удаление контрагента'
        );
        
        if (confirmed) {
            try {
                await apiService.deleteCounterpartyContract(shopId, contractId);
                Toast.success('Контрагент удален');
                loadCounterparties();
                
            } catch (error) {
                console.error('Error deleting counterparty:', error);
                Toast.error('Ошибка удаления контрагента');
            }
        }
    }

    // Initial load
    loadCounterparties();

    return createLayout(content);
}

