import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { router } from '../router';
import { Shop, NomenclatureStats, NomenclatureItem, PageResponse } from '../types';
import { Toast, Loading } from '../utils/ui';
import { formatDate, formatNumber } from '../utils/ui';

export function ShopDetailPage(): HTMLElement {
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
            <button class="btn btn-outline btn-sm mb-4" onclick="router.back()">← Назад</button>
            <div id="shop-header">
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка информации о магазине...</p>
                </div>
            </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-2">
                <div class="card">
                    <div class="card-header">
                        <h2 class="card-title">Номенклатура</h2>
                        <div class="flex gap-2">
                            <button class="btn btn-primary btn-sm" id="update-nomenclature-btn" disabled>
                                🔄 Обновить
                            </button>
                            <button class="btn btn-outline btn-sm" id="view-nomenclature-btn">
                                📋 Просмотреть все
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <div id="nomenclature-stats">
                            <div class="loading">
                                <div class="loading-spinner"></div>
                                <p>Загрузка статистики...</p>
                            </div>
                        </div>
                        
                        <div id="nomenclature-preview" class="mt-6">
                            <div class="loading">
                                <div class="loading-spinner"></div>
                                <p>Загрузка номенклатуры...</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="lg:col-span-1">
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">Настройки</h3>
                    </div>
                    <div class="card-body">
                        <div class="space-y-4">
                            <button class="btn btn-outline btn-full" id="edit-shop-btn">
                                ⚙️ Редактировать магазин
                            </button>
                            <button class="btn btn-outline btn-full" id="export-nomenclature-btn">
                                📤 Экспорт номенклатуры
                            </button>
                            <button class="btn btn-danger btn-full" id="delete-shop-btn">
                                🗑️ Удалить магазин
                            </button>
                        </div>
                    </div>
                </div>

                <div class="card mt-6">
                    <div class="card-header">
                        <h3 class="card-title">Последние обновления</h3>
                    </div>
                    <div class="card-body">
                        <div id="recent-updates">
                            <p class="text-gray-500 text-sm">Информация о последних обновлениях будет отображаться здесь</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Event handlers
    content.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        if (target.matches('#update-nomenclature-btn')) {
            handleUpdateNomenclature(shopId);
        }
        
        if (target.matches('#view-nomenclature-btn')) {
            router.navigate(`/shops/${shopId}/nomenclature`);
        }
        
        if (target.matches('#edit-shop-btn')) {
            Toast.info('Редактирование магазина будет реализовано в следующей версии');
        }
        
        if (target.matches('#export-nomenclature-btn')) {
            Toast.info('Экспорт номенклатуры будет реализован в следующей версии');
        }
        
        if (target.matches('#delete-shop-btn')) {
            Toast.info('Удаление магазина доступно на странице списка магазинов');
        }
    });

    // Load data
    loadShopData(content, shopId);

    return createLayout(content);
}

async function loadShopData(container: HTMLElement, shopId: string): Promise<void> {
    try {
        const [shop, stats, nomenclature] = await Promise.all([
            apiService.getShop(shopId),
            apiService.getNomenclatureStats(shopId),
            apiService.getNomenclature(shopId, 0, 10) // Load first 10 items for preview
        ]);
        
        renderShopHeader(container, shop);
        renderNomenclatureStats(container, stats);
        renderNomenclaturePreview(container, nomenclature, shopId);
        
        // Enable update button if shop has API token
        const updateBtn = container.querySelector('#update-nomenclature-btn') as HTMLButtonElement;
        updateBtn.disabled = !shop.hasToken;
        
        if (!shop.hasToken) {
            updateBtn.title = 'Для обновления номенклатуры необходимо настроить API ключ';
        }
        
    } catch (error) {
        console.error('Error loading shop data:', error);
        Toast.error('Ошибка загрузки данных магазина');
        
        const shopHeader = container.querySelector('#shop-header') as HTMLElement;
        shopHeader.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-title">Ошибка загрузки</div>
                <div class="empty-state-description">Не удалось загрузить информацию о магазине</div>
                <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
            </div>
        `;
    }
}

function renderShopHeader(container: HTMLElement, shop: Shop): void {
    const shopHeader = container.querySelector('#shop-header') as HTMLElement;
    
    shopHeader.innerHTML = `
        <div class="flex justify-between items-start">
            <div>
                <h1 class="text-2xl font-semibold mb-2">${shop.name}</h1>
                ${shop.externalId ? `
                    <div class="mb-2">
                        <span class="text-gray-500">External ID:</span>
                        <span class="font-mono text-sm bg-gray-100 px-2 py-1 rounded">${shop.externalId}</span>
                    </div>
                ` : ''}
                <div class="flex gap-4 text-sm text-gray-600">
                    <span>Создан: ${formatDate(shop.createdAt)}</span>
                    <span>Обновлен: ${formatDate(shop.updatedAt)}</span>
                </div>
            </div>
            <div class="text-right">
                <div class="text-3xl font-bold mb-1">${formatNumber(shop.nomenclatureCount)}</div>
                <div class="text-sm text-gray-500 mb-2">товаров в номенклатуре</div>
                <div class="text-sm ${shop.hasToken ? 'text-green-500' : 'text-red-500'}">
                    ${shop.hasToken ? '✓ API настроен' : '⚠ API не настроен'}
                </div>
            </div>
        </div>
    `;
}

function renderNomenclatureStats(container: HTMLElement, stats: NomenclatureStats): void {
    const statsContainer = container.querySelector('#nomenclature-stats') as HTMLElement;
    
    statsContainer.innerHTML = `
        <div class="grid grid-cols-3 gap-4 mb-6">
            <div class="text-center">
                <div class="text-2xl font-bold text-blue-600">${formatNumber(stats.totalCount)}</div>
                <div class="text-sm text-gray-500">Всего товаров</div>
            </div>
            <div class="text-center">
                <div class="text-2xl font-bold text-green-600">${formatNumber(stats.newCount)}</div>
                <div class="text-sm text-gray-500">Новых товаров</div>
            </div>
            <div class="text-center">
                <div class="text-2xl font-bold text-orange-600">${formatNumber(stats.updatedCount)}</div>
                <div class="text-sm text-gray-500">Обновленных товаров</div>
            </div>
        </div>
        
        ${stats.updatedCount > 0 ? `
            <div class="bg-orange-50 border border-orange-200 rounded-lg p-4">
                <div class="flex items-center gap-2 text-orange-800">
                    <span>⚠️</span>
                    <span class="font-medium">Внимание!</span>
                </div>
                <p class="text-orange-700 text-sm mt-1">
                    У вас есть ${formatNumber(stats.updatedCount)} обновленных товаров, которые требуют внимания.
                </p>
            </div>
        ` : ''}
    `;
}

async function handleUpdateNomenclature(shopId: string): Promise<void> {
    Loading.show('Обновление номенклатуры...');
    
    try {
        const result = await apiService.updateNomenclature(shopId);
        
        Toast.success(`Номенклатура обновлена: новых - ${result.new}, обновленных - ${result.updated}`);
        
        // Reload shop data
        const mainContainer = document.querySelector('.main') as HTMLElement;
        if (mainContainer) {
            loadShopData(mainContainer, shopId);
        }
        
    } catch (error: any) {
        console.error('Error updating nomenclature:', error);
        
        if (error.response?.data?.error) {
            Toast.error(`Ошибка обновления: ${error.response.data.error}`);
        } else {
            Toast.error('Ошибка обновления номенклатуры');
        }
    } finally {
        Loading.hide();
    }
}

function renderNomenclaturePreview(container: HTMLElement, nomenclatureResponse: PageResponse<NomenclatureItem>, shopId: string): void {
    const previewContainer = container.querySelector('#nomenclature-preview') as HTMLElement;
    
    if (nomenclatureResponse.content.length === 0) {
        previewContainer.innerHTML = `
            <div class="text-center py-8">
                <div class="text-gray-400 text-4xl mb-4">📦</div>
                <h3 class="text-lg font-medium text-gray-900 mb-2">Номенклатура пуста</h3>
                <p class="text-gray-500 mb-4">В этом магазине пока нет товаров</p>
                ${container.querySelector('#update-nomenclature-btn')?.hasAttribute('disabled') ? 
                    '<p class="text-sm text-orange-600">Настройте API ключ для автоматического обновления</p>' :
                    '<button class="btn btn-primary" onclick="document.getElementById(\'update-nomenclature-btn\').click()">Обновить номенклатуру</button>'
                }
            </div>
        `;
        return;
    }

    previewContainer.innerHTML = `
        <div class="mb-4">
            <div class="flex justify-between items-center">
                <h3 class="text-lg font-medium">Последние товары</h3>
                <button class="btn btn-outline btn-sm" onclick="router.navigate('/shops/${shopId}/nomenclature')">
                    Показать все (${formatNumber(nomenclatureResponse.totalElements)})
                </button>
            </div>
        </div>
        
        <div class="overflow-x-auto">
            <table class="table">
                <thead>
                    <tr>
                        <th>Артикул</th>
                        <th>SKU</th>
                        <th>Вес</th>
                        <th>Статус</th>
                        <th>Обновлен</th>
                    </tr>
                </thead>
                <tbody>
                    ${nomenclatureResponse.content.map(item => `
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
                                ${item.updated ? 
                                    '<span class="text-xs px-2 py-1 bg-orange-100 text-orange-800 rounded">Обновлен</span>' :
                                    '<span class="text-xs px-2 py-1 bg-green-100 text-green-800 rounded">Новый</span>'
                                }
                            </td>
                            <td>
                                <span class="text-sm text-gray-600">${formatDate(item.updatedAt)}</span>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
        
        ${nomenclatureResponse.totalElements > 10 ? `
            <div class="text-center mt-4">
                <button class="btn btn-outline" onclick="router.navigate('/shops/${shopId}/nomenclature')">
                    Показать все ${formatNumber(nomenclatureResponse.totalElements)} товаров
                </button>
            </div>
        ` : ''}
    `;
}
