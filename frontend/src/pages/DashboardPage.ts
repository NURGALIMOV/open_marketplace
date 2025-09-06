import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { authService } from '../services/auth';
import { router } from '../router';
import { Shop } from '../types';
import { Toast, Loading } from '../utils/ui';
import { formatNumber } from '../utils/ui';

export function DashboardPage(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
        <div class="mb-6">
            <h1 class="text-xl font-semibold mb-4">Панель управления</h1>
            <p class="text-gray-600">Добро пожаловать в систему учёта для интернет-магазинов</p>
        </div>

        <div class="stats" id="stats-container">
            <div class="stat-card">
                <div class="stat-value" id="shops-count">-</div>
                <div class="stat-label">Магазинов</div>
            </div>
            <div class="stat-card">
                <div class="stat-value" id="total-nomenclature">-</div>
                <div class="stat-label">Товаров в номенклатуре</div>
            </div>
            <div class="stat-card">
                <div class="stat-value" id="active-shops">-</div>
                <div class="stat-label">Активных магазинов</div>
            </div>
            <div class="stat-card">
                <div class="stat-value" id="updated-items">-</div>
                <div class="stat-label">Обновленных товаров</div>
            </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2">
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">Мои магазины</h2>
                    <button class="btn btn-primary btn-sm" id="create-shop-btn">Создать магазин</button>
                </div>
                <div class="card-body">
                    <div id="shops-list">
                        <div class="loading">
                            <div class="loading-spinner"></div>
                            <p>Загрузка магазинов...</p>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">Быстрые действия</h2>
                </div>
                <div class="card-body">
                    <div class="flex flex-col gap-4">
                        <button class="btn btn-outline" id="view-all-shops">
                            📊 Просмотреть все магазины
                        </button>
                        <button class="btn btn-outline" id="update-all-nomenclature">
                            🔄 Обновить всю номенклатуру
                        </button>
                        ${authService.isAdmin() ? `
                        <button class="btn btn-outline" id="admin-panel">
                            ⚙️ Панель администратора
                        </button>
                        ` : ''}
                    </div>
                </div>
            </div>
        </div>
    `;

    // Event handlers
    content.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        console.log('Click detected on:', target.className, target.id);
        
        if (target.matches('#create-shop-btn')) {
            router.navigate('/shops');
            return;
        }
        
        if (target.matches('#view-all-shops')) {
            router.navigate('/shops');
            return;
        }
        
        if (target.matches('#admin-panel')) {
            router.navigate('/admin');
            return;
        }
        
        if (target.matches('#update-all-nomenclature')) {
            handleUpdateAllNomenclature();
            return;
        }
        
        // Check if click was on shop item or its children
        const shopElement = target.closest('.shop-item') as HTMLElement;
        if (shopElement) {
            const shopId = shopElement.dataset.shopId;
            console.log('Shop clicked:', shopId, shopElement);
            if (shopId) {
                e.preventDefault();
                e.stopPropagation();
                router.navigate(`/shops/${shopId}`);
            }
        }
    });

    // Load data
    loadDashboardData(content);

    return createLayout(content);
}

async function loadDashboardData(container: HTMLElement): Promise<void> {
    try {
        const shops = await apiService.getShops();
        updateStats(container, shops);
        renderShopsList(container, shops);
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        Toast.error('Ошибка загрузки данных панели');
        
        const shopsList = container.querySelector('#shops-list') as HTMLElement;
        shopsList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-title">Ошибка загрузки</div>
                <div class="empty-state-description">Не удалось загрузить данные</div>
                <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
            </div>
        `;
    }
}

function updateStats(container: HTMLElement, shops: Shop[]): void {
    const shopsCount = container.querySelector('#shops-count') as HTMLElement;
    const totalNomenclature = container.querySelector('#total-nomenclature') as HTMLElement;
    const activeShops = container.querySelector('#active-shops') as HTMLElement;
    const updatedItems = container.querySelector('#updated-items') as HTMLElement;

    shopsCount.textContent = formatNumber(shops.length);
    
    const totalItems = shops.reduce((sum, shop) => sum + shop.nomenclatureCount, 0);
    totalNomenclature.textContent = formatNumber(totalItems);
    
    const activeShopsCount = shops.filter(shop => shop.hasToken).length;
    activeShops.textContent = formatNumber(activeShopsCount);
    
    // For updated items, we would need additional API call
    updatedItems.textContent = '-';
}

function renderShopsList(container: HTMLElement, shops: Shop[]): void {
    const shopsList = container.querySelector('#shops-list') as HTMLElement;
    
    if (shops.length === 0) {
        shopsList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">🏪</div>
                <div class="empty-state-title">Нет магазинов</div>
                <div class="empty-state-description">Создайте свой первый магазин для начала работы</div>
                <button class="btn btn-primary" onclick="router.navigate('/shops')">Создать магазин</button>
            </div>
        `;
        return;
    }

    const recentShops = shops.slice(0, 5); // Show only first 5 shops
    
    shopsList.innerHTML = `
        <div class="space-y-3">
            ${recentShops.map(shop => `
                <div class="shop-item" 
                     style="padding: 12px; border: 1px solid #e5e7eb; border-radius: 8px; cursor: pointer; transition: background-color 0.2s; margin-bottom: 12px;"
                     data-shop-id="${shop.id}"
                     onmouseover="this.style.backgroundColor='#f9fafb'"
                     onmouseout="this.style.backgroundColor='white'">
                    <div class="flex justify-between items-start">
                        <div>
                            <h3 class="font-medium text-gray-900">${shop.name}</h3>
                            ${shop.externalId ? `<p class="text-sm text-gray-500">ID: ${shop.externalId}</p>` : ''}
                        </div>
                        <div class="text-right">
                            <div class="text-sm font-medium">${formatNumber(shop.nomenclatureCount)} товаров</div>
                            <div class="text-xs ${shop.hasToken ? 'text-green-500' : 'text-red-500'}">
                                ${shop.hasToken ? '✓ Настроен' : '⚠ Требует настройки'}
                            </div>
                        </div>
                    </div>
                </div>
            `).join('')}
            ${shops.length > 5 ? `
                <div class="text-center pt-2">
                    <button class="btn btn-outline btn-sm" onclick="router.navigate('/shops')">
                        Показать все (${shops.length})
                    </button>
                </div>
            ` : ''}
        </div>
    `;
}

async function handleUpdateAllNomenclature(): Promise<void> {
    const shops = await apiService.getShops();
    const activeShops = shops.filter(shop => shop.hasToken);
    
    if (activeShops.length === 0) {
        Toast.warning('Нет магазинов с настроенными API ключами');
        return;
    }

    Loading.show('Обновление номенклатуры для всех магазинов...');
    
    let successCount = 0;
    let errorCount = 0;
    
    for (const shop of activeShops) {
        try {
            await apiService.updateNomenclature(shop.id);
            successCount++;
        } catch (error) {
            errorCount++;
            console.error(`Error updating nomenclature for shop ${shop.name}:`, error);
        }
    }
    
    Loading.hide();
    
    if (errorCount === 0) {
        Toast.success(`Номенклатура обновлена для ${successCount} магазинов`);
    } else {
        Toast.warning(`Обновлено: ${successCount}, ошибок: ${errorCount}`);
    }
    
    // Reload data
    loadDashboardData(document.querySelector('.main') as HTMLElement);
}
