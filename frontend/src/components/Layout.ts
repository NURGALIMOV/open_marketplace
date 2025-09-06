import { authService } from '../services/auth';
import { router } from '../router';
import { Modal } from '../utils/ui';

export function createLayout(content: HTMLElement): HTMLElement {
    const container = document.createElement('div');
    container.className = 'app';

    const user = authService.getUser();
    if (!user) {
        return content;
    }

    container.innerHTML = `
        <header class="header">
            <div class="header-left">
                <div class="logo">Open Market Placer</div>
                <nav class="nav">
                    <a href="/dashboard" class="nav-link">Панель</a>
                    <a href="/shops" class="nav-link">Магазины</a>
                    ${user.role === 'ADMIN' ? '<a href="/admin" class="nav-link">Администрирование</a>' : ''}
                </nav>
            </div>
            <div class="header-right">
                <div class="user-info">
                    <span>${user.email}</span>
                    <span class="text-gray-500">(${user.role})</span>
                </div>
                <button class="btn btn-outline btn-sm" id="logout-btn">Выйти</button>
            </div>
        </header>
        <main class="main" id="main-content">
        </main>
    `;

    const mainContent = container.querySelector('#main-content') as HTMLElement;
    mainContent.appendChild(content);

    // Handle navigation clicks
    container.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        
        if (target.matches('.nav-link')) {
            e.preventDefault();
            const href = target.getAttribute('href');
            if (href) {
                router.navigate(href);
            }
        }
        
        if (target.matches('#logout-btn')) {
            handleLogout();
        }
    });

    return container;
}

async function handleLogout(): Promise<void> {
    const confirmed = await Modal.confirm(
        'Вы уверены, что хотите выйти из системы?',
        'Выход'
    );
    
    if (confirmed) {
        authService.logout();
        router.navigate('/login');
    }
}
