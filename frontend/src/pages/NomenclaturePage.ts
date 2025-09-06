import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { router } from '../router';
import { NomenclatureItem, PageResponse } from '../types';
import { Toast } from '../utils/ui';
import { formatDate, formatNumber, debounce } from '../utils/ui';

export function NomenclaturePage(): HTMLElement {
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
            <h1 class="text-xl font-semibold">Номенклатура</h1>
        </div>

        <div class="card">
            <div class="card-header">
                <div class="search-box">
                    <input type="text" 
                           id="search-input" 
                           class="search-input" 
                           placeholder="Поиск по артикулу или SKU...">
                    <span class="search-icon">🔍</span>
                </div>
                <div class="flex gap-2">
                    <button class="btn btn-outline btn-sm" id="show-updated-only">
                        Только обновленные
                    </button>
                    <button class="btn btn-outline btn-sm" id="show-all">
                        Показать все
                    </button>
                </div>
            </div>
            <div class="card-body">
                <div id="nomenclature-table">
                    <div class="loading">
                        <div class="loading-spinner"></div>
                        <p>Загрузка номенклатуры...</p>
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
    let showUpdatedOnly = false;

    // Search handler with debounce
    const searchInput = content.querySelector('#search-input') as HTMLInputElement;
    const debouncedSearch = debounce((query: string) => {
        currentSearch = query;
        currentPage = 0;
        loadNomenclature();
    }, 300);

    searchInput.addEventListener('input', (e) => {
        const query = (e.target as HTMLInputElement).value.trim();
        debouncedSearch(query);
    });

    // Event handlers
    content.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        if (target.matches('#show-updated-only')) {
            showUpdatedOnly = true;
            currentPage = 0;
            loadNomenclature();
            
            target.classList.add('btn-primary');
            target.classList.remove('btn-outline');
            
            const showAllBtn = content.querySelector('#show-all') as HTMLElement;
            showAllBtn.classList.add('btn-outline');
            showAllBtn.classList.remove('btn-primary');
        }
        
        if (target.matches('#show-all')) {
            showUpdatedOnly = false;
            currentPage = 0;
            loadNomenclature();
            
            target.classList.add('btn-primary');
            target.classList.remove('btn-outline');
            
            const showUpdatedBtn = content.querySelector('#show-updated-only') as HTMLElement;
            showUpdatedBtn.classList.add('btn-outline');
            showUpdatedBtn.classList.remove('btn-primary');
        }
        
        if (target.matches('#prev-btn')) {
            if (currentPage > 0) {
                currentPage--;
                loadNomenclature();
            }
        }
        
        if (target.matches('#next-btn')) {
            currentPage++;
            loadNomenclature();
        }
    });

    async function loadNomenclature(): Promise<void> {
        try {
            const tableContainer = content.querySelector('#nomenclature-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка номенклатуры...</p>
                </div>
            `;

            const response = await apiService.getNomenclature(
                shopId, 
                currentPage, 
                50, 
                currentSearch || undefined
            );

            renderNomenclatureTable(response);
            updatePagination(response);
            
        } catch (error) {
            console.error('Error loading nomenclature:', error);
            Toast.error('Ошибка загрузки номенклатуры');
            
            const tableContainer = content.querySelector('#nomenclature-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">Ошибка загрузки</div>
                    <div class="empty-state-description">Не удалось загрузить номенклатуру</div>
                    <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
                </div>
            `;
        }
    }

    function renderNomenclatureTable(response: PageResponse<NomenclatureItem>): void {
        const tableContainer = content.querySelector('#nomenclature-table') as HTMLElement;
        
        if (response.content.length === 0) {
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📦</div>
                    <div class="empty-state-title">${currentSearch ? 'Ничего не найдено' : 'Нет товаров'}</div>
                    <div class="empty-state-description">
                        ${currentSearch 
                            ? `По запросу "${currentSearch}" ничего не найдено`
                            : 'В номенклатуре пока нет товаров'
                        }
                    </div>
                </div>
            `;
            return;
        }

        // Filter updated items if needed
        const items = showUpdatedOnly 
            ? response.content.filter(item => item.updated)
            : response.content;

        tableContainer.innerHTML = `
            <div class="table-container">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Артикул</th>
                            <th>SKU</th>
                            <th>Вес</th>
                            <th>Создан</th>
                            <th>Обновлен</th>
                            <th>Примечания</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${items.map(item => `
                            <tr class="${item.updated ? 'row-updated' : ''}">
                                <td>
                                    <span class="font-mono text-sm">${item.article || '-'}</span>
                                </td>
                                <td>
                                    <span class="font-mono font-medium">${formatNumber(item.sku)}</span>
                                </td>
                                <td>
                                    ${item.weight ? `${item.weight} кг` : '-'}
                                </td>
                                <td>
                                    <span class="text-sm text-gray-600">${formatDate(item.createdAt)}</span>
                                </td>
                                <td>
                                    <span class="text-sm ${item.updated ? 'text-orange-600 font-medium' : 'text-gray-600'}">
                                        ${formatDate(item.updatedAt)}
                                    </span>
                                </td>
                                <td>
                                    ${item.note ? `
                                        <span class="text-sm text-orange-600">${item.note}</span>
                                    ` : '-'}
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }

    function updatePagination(response: PageResponse<NomenclatureItem>): void {
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

    // Initial load
    loadNomenclature();

    return createLayout(content);
}
