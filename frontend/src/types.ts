// API Types
export interface LoginRequest {
    email: string;
    password: string;
}

export interface LoginResponse {
    accessToken: string;
    expiresIn: number;
    tokenType: string;
}

export interface User {
    id: string;
    email: string;
    role: 'ADMIN' | 'USER';
    createdAt: string;
    updatedAt: string;
}

export interface CreateUserRequest {
    email: string;
    password: string;
    role: 'ADMIN' | 'USER';
}

export interface Shop {
    id: string;
    name: string;
    externalId?: string;
    hasToken: boolean;
    createdAt: string;
    updatedAt: string;
    nomenclatureCount: number;
}

export interface CreateShopRequest {
    name: string;
    externalId?: string;
    token?: string;
}

export interface UpdateShopRequest {
    name?: string;
    externalId?: string;
    token?: string;
}

export interface NomenclatureItem {
    id: string;
    article?: string;
    sku: number;
    weight?: number;
    createdAt: string;
    updatedAt: string;
    note?: string;
    updated: boolean;
}

export interface NomenclatureStats {
    totalCount: number;
    updatedCount: number;
    newCount: number;
}

export interface UpdateResult {
    processed: number;
    new: number;
    updated: number;
    duration: number;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
}

// UI Types
export interface Route {
    path: string;
    component: () => HTMLElement;
    requiresAuth?: boolean;
    requiresAdmin?: boolean;
}

export interface ToastOptions {
    type: 'success' | 'error' | 'warning' | 'info';
    message: string;
    duration?: number;
}

export interface ModalOptions {
    title: string;
    content: HTMLElement | string;
    footer?: HTMLElement;
    onClose?: () => void;
}

// Application State
export interface AppState {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    currentRoute: string;
}
