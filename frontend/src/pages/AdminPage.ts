import { createLayout } from '../components/Layout';
import { apiService } from '../services/api';
import { User, CreateUserRequest, PageResponse } from '../types';
import { Toast, Modal, Loading } from '../utils/ui';
import { formatDate, validateEmail, validatePassword } from '../utils/ui';

export function AdminPage(): HTMLElement {
    const content = document.createElement('div');
    
    content.innerHTML = `
        <div class="mb-6">
            <h1 class="text-xl font-semibold">Администрирование</h1>
            <p class="text-gray-600 mt-1">Управление пользователями системы</p>
        </div>

        <div class="card">
            <div class="card-header">
                <h2 class="card-title">Пользователи</h2>
                <div class="flex gap-2">
                    <select class="form-select" id="role-filter">
                        <option value="">Все роли</option>
                        <option value="USER">Пользователи</option>
                        <option value="ADMIN">Администраторы</option>
                    </select>
                    <button class="btn btn-primary" id="create-user-btn">
                        Создать пользователя
                    </button>
                </div>
            </div>
            <div class="card-body">
                <div id="users-table">
                    <div class="loading">
                        <div class="loading-spinner"></div>
                        <p>Загрузка пользователей...</p>
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
    let currentRole = '';

    // Define loadUsers function in the scope
    async function loadUsers(): Promise<void> {
        try {
            const tableContainer = content.querySelector('#users-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="loading">
                    <div class="loading-spinner"></div>
                    <p>Загрузка пользователей...</p>
                </div>
            `;

            const response = await apiService.getUsers(
                currentPage, 
                20, 
                currentRole || undefined
            );

            renderUsersTable(response);
            updatePagination(response);
            
        } catch (error) {
            console.error('Error loading users:', error);
            Toast.error('Ошибка загрузки пользователей');
            
            const tableContainer = content.querySelector('#users-table') as HTMLElement;
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">Ошибка загрузки</div>
                    <div class="empty-state-description">Не удалось загрузить список пользователей</div>
                    <button class="btn btn-primary" onclick="location.reload()">Попробовать еще раз</button>
                </div>
            `;
        }
    }

    function renderUsersTable(response: PageResponse<User>): void {
        const tableContainer = content.querySelector('#users-table') as HTMLElement;
        
        if (response.content.length === 0) {
            tableContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">👥</div>
                    <div class="empty-state-title">Пользователи не найдены</div>
                    <div class="empty-state-description">
                        ${currentRole ? `Нет пользователей с ролью ${currentRole}` : 'В системе пока нет пользователей'}
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
                            <th>Email</th>
                            <th>Роль</th>
                            <th>Создан</th>
                            <th>Последнее обновление</th>
                            <th>Действия</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${response.content.map(user => `
                            <tr>
                                <td>
                                    <span class="font-medium">${user.email}</span>
                                </td>
                                <td>
                                    <span class="px-2 py-1 rounded text-xs font-medium ${
                                        user.role === 'ADMIN' 
                                            ? 'bg-red-100 text-red-800' 
                                            : 'bg-blue-100 text-blue-800'
                                    }">
                                        ${user.role}
                                    </span>
                                </td>
                                <td>
                                    <span class="text-sm text-gray-600">${formatDate(user.createdAt)}</span>
                                </td>
                                <td>
                                    <span class="text-sm text-gray-600">${formatDate(user.updatedAt)}</span>
                                </td>
                                <td>
                                    <button class="btn btn-outline btn-sm update-password-btn" 
                                            data-user-id="${user.id}"
                                            data-user-email="${user.email}"
                                            title="Изменить пароль">
                                        🔑 Пароль
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }

    function updatePagination(response: PageResponse<User>): void {
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

    // Event handlers
    content.addEventListener('change', (e) => {
        const target = e.target as HTMLSelectElement;
        
        if (target.matches('#role-filter')) {
            currentRole = target.value;
            currentPage = 0;
            loadUsers();
        }
    });

    content.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        if (target.matches('#create-user-btn')) {
            showCreateUserModal(loadUsers);
        }
        
        if (target.matches('#prev-btn')) {
            if (currentPage > 0) {
                currentPage--;
                loadUsers();
            }
        }
        
        if (target.matches('#next-btn')) {
            currentPage++;
            loadUsers();
        }
        
        if (target.matches('.update-password-btn')) {
            const userId = target.dataset.userId;
            const userEmail = target.dataset.userEmail;
            if (userId && userEmail) {
                showUpdatePasswordModal(userId, userEmail);
            }
        }
    });

    // Initial load
    loadUsers();

    return createLayout(content);
}

function showCreateUserModal(reloadCallback: () => void): void {
    const form = document.createElement('form');
    form.innerHTML = `
        <div class="form-group">
            <label class="form-label" for="user-email">Email *</label>
            <input type="email" id="user-email" class="form-input" placeholder="Введите email" required>
            <div class="form-error" id="email-error"></div>
        </div>
        
        <div class="form-group">
            <label class="form-label" for="user-password">Пароль *</label>
            <input type="password" id="user-password" class="form-input" placeholder="Введите пароль" required>
            <div class="form-error" id="password-error"></div>
            <div class="text-xs text-gray-500 mt-1">
                Минимум 8 символов, должен содержать цифры, строчные и заглавные буквы
            </div>
        </div>
        
        <div class="form-group">
            <label class="form-label" for="user-role">Роль *</label>
            <select id="user-role" class="form-select" required>
                <option value="">Выберите роль</option>
                <option value="USER">Пользователь</option>
                <option value="ADMIN">Администратор</option>
            </select>
            <div class="form-error" id="role-error"></div>
        </div>
    `;

    // Add submit button inside the form
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
        title: 'Создать пользователя',
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
        
        const emailInput = form.querySelector('#user-email') as HTMLInputElement;
        const passwordInput = form.querySelector('#user-password') as HTMLInputElement;
        const roleSelect = form.querySelector('#user-role') as HTMLSelectElement;
        
        const email = emailInput.value.trim();
        const password = passwordInput.value;
        const role = roleSelect.value as 'USER' | 'ADMIN';

        // Clear errors
        form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
        form.querySelectorAll('.form-input, .form-select').forEach(el => el.classList.remove('error'));

        // Validate
        let hasErrors = false;

        if (!email) {
            (form.querySelector('#email-error') as HTMLElement).textContent = 'Email обязателен';
            emailInput.classList.add('error');
            hasErrors = true;
        } else if (!validateEmail(email)) {
            (form.querySelector('#email-error') as HTMLElement).textContent = 'Введите корректный email';
            emailInput.classList.add('error');
            hasErrors = true;
        }

        if (!password) {
            (form.querySelector('#password-error') as HTMLElement).textContent = 'Пароль обязателен';
            passwordInput.classList.add('error');
            hasErrors = true;
        } else {
            const passwordError = validatePassword(password);
            if (passwordError) {
                (form.querySelector('#password-error') as HTMLElement).textContent = passwordError;
                passwordInput.classList.add('error');
                hasErrors = true;
            }
        }

        if (!role) {
            (form.querySelector('#role-error') as HTMLElement).textContent = 'Роль обязательна';
            roleSelect.classList.add('error');
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        const submitBtn = form.querySelector('#submit-btn') as HTMLButtonElement;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Создание...';

        try {
            const userData: CreateUserRequest = { email, password, role };
            await apiService.createUser(userData);
            
            Toast.success('Пользователь создан успешно');
            Modal.close();
            
            // Reload users
            reloadCallback();
            
        } catch (error: any) {
            console.error('Error creating user:', error);
            
            if (error.response?.data?.message) {
                Toast.error(`Ошибка создания пользователя: ${error.response.data.message}`);
            } else {
                Toast.error('Ошибка создания пользователя');
            }
            
            submitBtn.disabled = false;
            submitBtn.textContent = 'Создать';
        }
    });

    // Focus on first input
    setTimeout(() => {
        const firstInput = form.querySelector('#user-email') as HTMLInputElement;
        firstInput?.focus();
    }, 100);
}

function showUpdatePasswordModal(userId: string, userEmail: string): void {
    const form = document.createElement('form');
    form.innerHTML = `
        <div class="mb-4">
            <p class="text-gray-600">Изменение пароля для пользователя: <strong>${userEmail}</strong></p>
        </div>
        
        <div class="form-group">
            <label class="form-label" for="new-password">Новый пароль *</label>
            <input type="password" id="new-password" class="form-input" placeholder="Введите новый пароль" required>
            <div class="form-error" id="password-error"></div>
            <div class="text-xs text-gray-500 mt-1">
                Минимум 8 символов, должен содержать цифры, строчные и заглавные буквы
            </div>
        </div>
        
        <div class="form-group">
            <label class="form-label" for="confirm-password">Подтвердите пароль *</label>
            <input type="password" id="confirm-password" class="form-input" placeholder="Повторите пароль" required>
            <div class="form-error" id="confirm-error"></div>
        </div>
    `;

    // Add submit button inside the form
    const formSubmitSection = document.createElement('div');
    formSubmitSection.className = 'mt-6';
    formSubmitSection.innerHTML = `
        <button type="submit" class="btn btn-primary btn-full" id="password-submit-btn">Изменить пароль</button>
    `;
    form.appendChild(formSubmitSection);

    const footer = document.createElement('div');
    footer.innerHTML = `
        <button type="button" class="btn btn-secondary" data-action="cancel">Отмена</button>
    `;

    Modal.show({
        title: 'Изменить пароль пользователя',
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
        
        const passwordInput = form.querySelector('#new-password') as HTMLInputElement;
        const confirmInput = form.querySelector('#confirm-password') as HTMLInputElement;
        
        const password = passwordInput.value;
        const confirmPassword = confirmInput.value;

        // Clear errors
        form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
        form.querySelectorAll('.form-input').forEach(el => el.classList.remove('error'));

        // Validate
        let hasErrors = false;

        if (!password) {
            (form.querySelector('#password-error') as HTMLElement).textContent = 'Пароль обязателен';
            passwordInput.classList.add('error');
            hasErrors = true;
        } else {
            const passwordError = validatePassword(password);
            if (passwordError) {
                (form.querySelector('#password-error') as HTMLElement).textContent = passwordError;
                passwordInput.classList.add('error');
                hasErrors = true;
            }
        }

        if (password !== confirmPassword) {
            (form.querySelector('#confirm-error') as HTMLElement).textContent = 'Пароли не совпадают';
            confirmInput.classList.add('error');
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        const submitBtn = form.querySelector('#password-submit-btn') as HTMLButtonElement;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Изменение...';

        try {
            await apiService.updateUserPassword(userId, password);
            
            Toast.success(`Пароль для пользователя ${userEmail} успешно изменен`);
            Modal.close();
            
        } catch (error: any) {
            console.error('Error updating password:', error);
            
            if (error.response?.data?.message) {
                Toast.error(`Ошибка изменения пароля: ${error.response.data.message}`);
            } else {
                Toast.error('Ошибка изменения пароля');
            }
            
            submitBtn.disabled = false;
            submitBtn.textContent = 'Изменить пароль';
        }
    });

    // Focus on password input
    setTimeout(() => {
        const passwordInput = form.querySelector('#new-password') as HTMLInputElement;
        passwordInput?.focus();
    }, 100);
}