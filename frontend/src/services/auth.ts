import { User, AppState } from '../types';

class AuthService {
    private state: AppState = {
        user: null,
        token: null,
        isAuthenticated: false,
        currentRoute: '/',
    };

    private listeners: Array<(state: AppState) => void> = [];

    constructor() {
        this.loadFromStorage();
    }

    private loadFromStorage(): void {
        const token = localStorage.getItem('token');
        const userStr = localStorage.getItem('user');

        if (token && userStr) {
            try {
                const user: User = JSON.parse(userStr);
                this.state = {
                    ...this.state,
                    user,
                    token,
                    isAuthenticated: true,
                };
            } catch (error) {
                console.error('Failed to parse user data from localStorage:', error);
                this.logout();
            }
        }
    }

    private saveToStorage(): void {
        if (this.state.token && this.state.user) {
            localStorage.setItem('token', this.state.token);
            localStorage.setItem('user', JSON.stringify(this.state.user));
        } else {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }
    }

    private notifyListeners(): void {
        this.listeners.forEach(listener => listener(this.state));
    }

    login(token: string, user: User): void {
        this.state = {
            ...this.state,
            user,
            token,
            isAuthenticated: true,
        };
        this.saveToStorage();
        this.notifyListeners();
    }

    logout(): void {
        this.state = {
            ...this.state,
            user: null,
            token: null,
            isAuthenticated: false,
        };
        this.saveToStorage();
        this.notifyListeners();
    }

    getState(): AppState {
        return { ...this.state };
    }

    isAuthenticated(): boolean {
        return this.state.isAuthenticated;
    }

    getUser(): User | null {
        return this.state.user;
    }

    getToken(): string | null {
        return this.state.token;
    }

    isAdmin(): boolean {
        return this.state.user?.role === 'ADMIN';
    }

    subscribe(listener: (state: AppState) => void): () => void {
        this.listeners.push(listener);
        
        // Return unsubscribe function
        return () => {
            const index = this.listeners.indexOf(listener);
            if (index > -1) {
                this.listeners.splice(index, 1);
            }
        };
    }

    setCurrentRoute(route: string): void {
        this.state = {
            ...this.state,
            currentRoute: route,
        };
        this.notifyListeners();
    }

    // Parse JWT token to get user info (basic implementation)
    private parseJWT(token: string): User | null {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(
                atob(base64)
                    .split('')
                    .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );

            const payload = JSON.parse(jsonPayload);
            
            return {
                id: payload.sub,
                email: payload.email,
                role: payload.role,
                createdAt: new Date(payload.iat * 1000).toISOString(),
                updatedAt: new Date(payload.iat * 1000).toISOString(),
            };
        } catch (error) {
            console.error('Failed to parse JWT:', error);
            return null;
        }
    }

    // Initialize user from token if user data is missing
    initializeFromToken(): void {
        if (this.state.token && !this.state.user) {
            const user = this.parseJWT(this.state.token);
            if (user) {
                this.state = {
                    ...this.state,
                    user,
                    isAuthenticated: true,
                };
                this.saveToStorage();
                this.notifyListeners();
            } else {
                this.logout();
            }
        }
    }
}

export const authService = new AuthService();
