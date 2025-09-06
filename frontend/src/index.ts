import './styles.css';
import { authService } from './services/auth';
import { router } from './router';

// Initialize application
class App {
    constructor() {
        this.init();
    }

    private init(): void {
        // Wait for DOM to be ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.start());
        } else {
            this.start();
        }
    }

    private start(): void {
        console.log('Starting Open Market Placer application...');

        // Initialize auth service
        authService.initializeFromToken();

        // Subscribe to auth state changes
        authService.subscribe((state) => {
            console.log('Auth state changed:', state.isAuthenticated);
        });

        // Initialize router
        router.init();

        console.log('Application started successfully');
    }
}

// Start the application
new App();

// Make services available globally for debugging
declare global {
    interface Window {
        authService: typeof authService;
        router: typeof router;
    }
}

if (process.env.NODE_ENV === 'development') {
    window.authService = authService;
    window.router = router;
}
