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

// Counterparty Contract types
export interface CounterpartyContract {
    id: string;
    counterparty: string;
    contract: string;
    contractDate?: string;
    createdAt: string;
    updatedAt: string;
}

export interface CreateCounterpartyContractRequest {
    counterparty: string;
    contract: string;
    contractDate?: string;
}

export interface UpdateCounterpartyContractRequest {
    counterparty?: string;
    contract?: string;
    contractDate?: string;
}

// Receipt types
export interface Receipt {
    id: string;
    name: string;
    requestNumber?: string;
    receiptDate?: string;
    counterpartyContractId?: string;
    counterpartyName?: string;
    contract?: string;
    totalCost: number;
    itemsCount: number;
    createdAt: string;
    updatedAt: string;
}

export interface CreateReceiptRequest {
    name: string;
    requestNumber?: string;
    receiptDate?: string;
    counterpartyContractId?: string;
}

export interface UpdateReceiptRequest {
    name?: string;
    requestNumber?: string;
    receiptDate?: string;
    counterpartyContractId?: string;
}

// Receipt Item types
export interface ReceiptItem {
    id: string;
    sku?: string;
    article?: string;
    quantity?: number;
    cost?: number;
    totalCost: number;
    createdAt: string;
    updatedAt: string;
}

export interface CreateReceiptItemRequest {
    sku?: string;
    article?: string;
    quantity?: number;
    cost?: number;
}

export interface UpdateReceiptItemRequest {
    sku?: string;
    article?: string;
    quantity?: number;
    cost?: number;
}

// Excel Import types
export interface ExcelImportRequest {
    mapping: Record<string, string>; // field -> column mapping
    hasHeader?: boolean;
    strict?: boolean;
}

export interface ExcelImportResponse {
    rowsProcessed: number;
    rowsCreated: number;
    totalCostDelta: number;
    errors: ImportError[];
}

export interface ImportError {
    row: number;
    column?: string;
    error: string;
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

// Shipment types
export interface Shipment {
    id: string;
    shipmentNumber: string;
    shipmentDate: string;
    totalQuantity: number;
    itemsCount: number;
    createdAt: string;
    updatedAt: string;
}

export interface CreateShipmentRequest {
    shipmentNumber: string;
    shipmentDate: string;
}

export interface UpdateShipmentRequest {
    shipmentNumber?: string;
    shipmentDate?: string;
}

// Shipment Item types
export interface ShipmentItem {
    id: string;
    sku: string;
    article?: string;
    quantity: number;
    createdAt: string;
    updatedAt: string;
}

export interface CreateShipmentItemRequest {
    sku: string;
    article?: string;
    quantity: number;
}

export interface UpdateShipmentItemRequest {
    sku?: string;
    article?: string;
    quantity?: number;
}

// Shipment Import types
export interface ShipmentImportRequest {
    mapping: Record<string, string>; // field -> column mapping
    hasHeader?: boolean;
    strict?: boolean;
    shipmentDate: string;
    startRow?: number; // Starting row for parsing (1-based index)
}

export interface ShipmentImportResponse {
    shipmentId: string;
    rowsProcessed: number;
    rowsCreated: number;
    totalQuantityDelta: number;
    errors: ImportError[];
}

export interface ShipmentApiImportResponse {
    shipmentsAdded: number;
    shipmentsSkipped: number;
    errors: ShipmentImportError[];
}

export interface ShipmentImportError {
    shipmentNumber: string;
    error: string;
}

// Application State
export interface AppState {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    currentRoute: string;
}
