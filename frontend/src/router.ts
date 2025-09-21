import { Route } from './types';
import { authService } from './services/auth';
import { 
    LoginPage, 
    DashboardPage, 
    ShopsPage, 
    ShopDetailPage, 
    NomenclaturePage, 
    AdminPage,
    CounterpartiesPage,
    ReceiptsPage,
    ReceiptDetailPage,
    ShipmentsPage,
    ShipmentDetailPage
} from './pages';

class Router {
    private routes: Route[] = [
        {
            path: '/login',
            component: LoginPage,
            requiresAuth: false,
        },
        {
            path: '/',
            component: DashboardPage,
            requiresAuth: true,
        },
        {
            path: '/dashboard',
            component: DashboardPage,
            requiresAuth: true,
        },
        {
            path: '/shops',
            component: ShopsPage,
            requiresAuth: true,
        },
        {
            path: '/shops/:id',
            component: ShopDetailPage,
            requiresAuth: true,
        },
        {
            path: '/shops/:id/nomenclature',
            component: NomenclaturePage,
            requiresAuth: true,
        },
        {
            path: '/admin',
            component: AdminPage,
            requiresAuth: true,
            requiresAdmin: true,
        },
        {
            path: '/shops/:id/counterparties',
            component: CounterpartiesPage,
            requiresAuth: true,
        },
        {
            path: '/shops/:id/receipts',
            component: ReceiptsPage,
            requiresAuth: true,
        },
        {
            path: '/shops/:shopId/receipts/:receiptId',
            component: ReceiptDetailPage,
            requiresAuth: true,
        },
        {
            path: '/shops/:id/shipments',
            component: ShipmentsPage,
            requiresAuth: true,
        },
        {
            path: '/shops/:shopId/shipments/:shipmentId',
            component: ShipmentDetailPage,
            requiresAuth: true,
        },
    ];

    private currentPath: string = '';

    constructor() {
        window.addEventListener('popstate', () => {
            this.navigate(window.location.pathname, false);
        });
    }

    init(): void {
        const path = window.location.pathname;
        this.navigate(path, false);
    }

    navigate(path: string, addToHistory = true): void {
        if (addToHistory) {
            window.history.pushState({}, '', path);
        }

        this.currentPath = path;
        authService.setCurrentRoute(path);

        const route = this.findRoute(path);
        if (!route) {
            this.navigate('/dashboard');
            return;
        }

        // Check authentication
        if (route.requiresAuth && !authService.isAuthenticated()) {
            this.navigate('/login');
            return;
        }

        // Check admin access
        if (route.requiresAdmin && !authService.isAdmin()) {
            this.navigate('/dashboard');
            return;
        }

        // Redirect authenticated users from login page
        if (path === '/login' && authService.isAuthenticated()) {
            this.navigate('/dashboard');
            return;
        }

        this.renderRoute(route, path);
    }

    private findRoute(path: string): Route | null {
        // Try exact match first
        const exactMatch = this.routes.find(route => route.path === path);
        if (exactMatch) return exactMatch;

        // Try pattern matching for dynamic routes
        for (const route of this.routes) {
            if (this.matchPath(route.path, path)) {
                return route;
            }
        }

        return null;
    }

    private matchPath(pattern: string, path: string): boolean {
        const patternParts = pattern.split('/');
        const pathParts = path.split('/');

        if (patternParts.length !== pathParts.length) {
            return false;
        }

        return patternParts.every((part, index) => {
            if (part.startsWith(':')) {
                return true; // Dynamic segment
            }
            return part === pathParts[index];
        });
    }

    private renderRoute(route: Route, path: string): void {
        const appElement = document.getElementById('app');
        if (!appElement) return;

        try {
            const component = route.component();
            appElement.innerHTML = '';
            appElement.appendChild(component);

            // Set active navigation
            this.updateNavigation(path);
        } catch (error) {
            console.error('Error rendering route:', error);
            appElement.innerHTML = `
                <div class="error-page">
                    <h1>Ошибка</h1>
                    <p>Произошла ошибка при загрузке страницы.</p>
                    <button class="btn btn-primary" onclick="location.reload()">Обновить</button>
                </div>
            `;
        }
    }

    private updateNavigation(currentPath: string): void {
        const navLinks = document.querySelectorAll('.nav-link');
        navLinks.forEach(link => {
            const href = (link as HTMLAnchorElement).getAttribute('href');
            if (href === currentPath || (href === '/' && currentPath === '/dashboard')) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    getParams(path: string): Record<string, string> {
        const route = this.findRoute(path);
        if (!route) return {};

        const patternParts = route.path.split('/');
        const pathParts = path.split('/');
        const params: Record<string, string> = {};

        patternParts.forEach((part, index) => {
            if (part.startsWith(':')) {
                const paramName = part.slice(1);
                params[paramName] = pathParts[index];
            }
        });

        return params;
    }

    getCurrentPath(): string {
        return this.currentPath;
    }

    back(): void {
        window.history.back();
    }
}

export const router = new Router();
