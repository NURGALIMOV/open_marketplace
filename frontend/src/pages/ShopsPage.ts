import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { router } from '../router';
import { Shop, CreateShopRequest } from '../types';
import { Toast, Modal, Loading } from '../utils/ui';
import { formatDate, formatNumber, validateEmail } from '../utils/ui';

export function ShopsPage(): HTMLElement {
    const content = document.createElement('div');
    content.innerHTML = `
        <div class="flex justify-between items-center mb-6">
            <div>
                <h1 class="text-xl font-semibold">Мои магазины</h1>
                <p class="text-gray-600 mt-1">Управление магазинами и их настройками</p>
            </div>
            <button class="btn btn-primary" id="create-shop-btn">
                Создать магазин
            </button>
        </div>

        <div class="card">
            <div class="card-body">
                <div id="shops-container">
                    <div class="loading">
                        <div class="loading-spinner"></div>
                        <p>Загрузка магазинов...</p>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Event handlers
    content.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        if (target.matches('#create-shop-btn')) {
            showCreateShopModal();
        }
        
        if (target.matches('.shop-card')) {
            const shopId = target.dataset.shopId;
            if (shopId) {
                router.navigate(`/shops/${shopId}`);
            }
        }
        
        if (target.matches('.edit-shop-btn')) {
            e.stopPropagation();
            const shopId = target.dataset.shopId;
            if (shopId) {
                showEditShopModal(shopId);
            }
        }
        
        if (target.matches('.delete-shop-btn')) {
            e.stopPropagation();
            const shopId = target.dataset.shopId;
            const shopName = target.dataset.shopName;
            if (shopId && shopName) {
                handleDeleteShop(shopId, shopName);
            }
        }
    });

    // Load shops
    loadShops(content);

    return createLayout(content);
}

async function loadShops(container: HTMLElement): Promise<void> {
    try {
        const shops = await apiService.getShops();
        renderShops(container, shops);
    } catch (error) {
        console.error('Error loading shops:', error);
        Toast.error('Ошибка загрузки магазинов');
        
        const shopsContainer = container.querySelector('#shops-container') as HTMLElement;
        shopsContainer.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-title">Ошибка загрузки</div>
                <div class="empty-state-description">Не удалось загрузить список магазинов</div>
                <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
            </div>
        `;
    }
}

function renderShops(container: HTMLElement, shops: Shop[]): void {
    const shopsContainer = container.querySelector('#shops-container') as HTMLElement;
    
    if (shops.length === 0) {
        shopsContainer.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">🏪</div>
                <div class="empty-state-title">Нет магазинов</div>
                <div class="empty-state-description">Создайте свой первый магазин для начала работы с системой</div>
                <button class="btn btn-primary" onclick="document.getElementById('create-shop-btn').click()">
                    Создать магазин
                </button>
            </div>
        `;
        return;
    }

    shopsContainer.innerHTML = `
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
            ${shops.map(shop => `
                <div class="shop-card card cursor-pointer hover:shadow-lg transition-shadow" data-shop-id="${shop.id}">
                    <div class="card-body">
                        <div class="flex justify-between items-start mb-4">
                            <h3 class="font-semibold text-lg">${shop.name}</h3>
                            <div class="flex gap-2">
                                <button class="btn btn-outline btn-sm edit-shop-btn" 
                                        data-shop-id="${shop.id}" 
                                        title="Редактировать">
                                    ✏️
                                </button>
                                <button class="btn btn-danger btn-sm delete-shop-btn" 
                                        data-shop-id="${shop.id}"
                                        data-shop-name="${shop.name}"
                                        title="Удалить">
                                    🗑️
                                </button>
                            </div>
                        </div>
                        
                        ${shop.externalId ? `
                            <div class="mb-2">
                                <span class="text-gray-500">External ID:</span>
                                <span class="font-mono text-sm">${shop.externalId}</span>
                            </div>
                        ` : ''}
                        
                        <div class="flex justify-between items-center mb-3">
                            <div>
                                <div class="text-2xl font-bold">${formatNumber(shop.nomenclatureCount)}</div>
                                <div class="text-sm text-gray-500">товаров</div>
                            </div>
                            <div class="text-right">
                                <div class="text-xs ${shop.hasToken ? 'text-green-500' : 'text-red-500'}">
                                    ${shop.hasToken ? '✓ API настроен' : '⚠ Требует настройки'}
                                </div>
                            </div>
                        </div>
                        
                        <div class="text-xs text-gray-400">
                            Создан: ${formatDate(shop.createdAt)}
                        </div>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
}

function showCreateShopModal(): void {
    const form = document.createElement('form');
    form.innerHTML = `
        <div class="form-group">
            <label class="form-label" for="shop-name">Название магазина *</label>
            <input type="text" id="shop-name" class="form-input" placeholder="Введите название" required>
            <div class="form-error" id="name-error"></div>
        </div>
        
        <div class="form-group">
            <label class="form-label" for="external-id">External ID (Client-Id)</label>
            <input type="text" id="external-id" class="form-input" placeholder="ID магазина в Ozon">
            <div class="form-error" id="external-id-error"></div>
        </div>
        
        <div class="form-group">
            <label class="form-label" for="api-token">API Token (Api-Key)</label>
            <textarea id="api-token" class="form-textarea" placeholder="API ключ для доступа к Ozon"></textarea>
            <div class="form-error" id="token-error"></div>
        </div>
    `;

    // Add submit button inside the form
    const formSubmitSection = document.createElement('div');
    formSubmitSection.className = 'mt-6';
    formSubmitSection.innerHTML = `
        <button type="submit" class="btn btn-primary btn-full" id="shop-submit-btn">Создать</button>
    `;
    form.appendChild(formSubmitSection);

    const footer = document.createElement('div');
    footer.innerHTML = `
        <button type="button" class="btn btn-secondary" data-action="cancel">Отмена</button>
    `;

    const modal = Modal.show({
        title: 'Создать магазин',
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
        
        const nameInput = form.querySelector('#shop-name') as HTMLInputElement;
        const externalIdInput = form.querySelector('#external-id') as HTMLInputElement;
        const tokenInput = form.querySelector('#api-token') as HTMLTextAreaElement;
        
        const name = nameInput.value.trim();
        const externalId = externalIdInput.value.trim();
        const token = tokenInput.value.trim();

        // Clear errors
        form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
        form.querySelectorAll('.form-input, .form-textarea').forEach(el => el.classList.remove('error'));

        // Validate
        if (!name) {
            (form.querySelector('#name-error') as HTMLElement).textContent = 'Название обязательно';
            nameInput.classList.add('error');
            nameInput.focus();
            return;
        }

        const submitBtn = form.querySelector('#shop-submit-btn') as HTMLButtonElement;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Создание...';

        try {
            const shopData: CreateShopRequest = { name };
            if (externalId) shopData.externalId = externalId;
            if (token) shopData.token = token;

            await apiService.createShop(shopData);
            Toast.success('Магазин создан успешно');
            Modal.close();
            
            // Reload shops
            loadShops(document.querySelector('.main') as HTMLElement);
            
        } catch (error: any) {
            console.error('Error creating shop:', error);
            Toast.error('Ошибка создания магазина');
            
            submitBtn.disabled = false;
            submitBtn.textContent = 'Создать';
        }
    });
}

function showEditShopModal(shopId: string): void {
    // Implementation similar to create modal but with pre-filled data
    // This would require additional API call to get shop details
    Toast.info('Редактирование магазина будет реализовано в следующей версии');
}

async function handleDeleteShop(shopId: string, shopName: string): Promise<void> {
    const confirmed = await Modal.confirm(
        `Вы уверены, что хотите удалить магазин "${shopName}"? Это действие нельзя отменить.`,
        'Удаление магазина'
    );
    
    if (confirmed) {
        Loading.show('Удаление магазина...');
        
        try {
            await apiService.deleteShop(shopId);
            Toast.success('Магазин удален');
            
            // Reload shops
            loadShops(document.querySelector('.main') as HTMLElement);
            
        } catch (error) {
            console.error('Error deleting shop:', error);
            Toast.error('Ошибка удаления магазина');
        } finally {
            Loading.hide();
        }
    }
}
