import axios, { AxiosInstance, AxiosResponse } from 'axios';
import {
    LoginRequest,
    LoginResponse,
    User,
    CreateUserRequest,
    Shop,
    CreateShopRequest,
    UpdateShopRequest,
    NomenclatureItem,
    NomenclatureStats,
    UpdateResult,
    PageResponse,
    CounterpartyContract,
    CreateCounterpartyContractRequest,
    UpdateCounterpartyContractRequest,
    Receipt,
    CreateReceiptRequest,
    UpdateReceiptRequest,
    ReceiptItem,
    CreateReceiptItemRequest,
    UpdateReceiptItemRequest,
    ExcelImportResponse,
    Shipment,
    CreateShipmentRequest,
    UpdateShipmentRequest,
    ShipmentItem,
    CreateShipmentItemRequest,
    UpdateShipmentItemRequest,
    ShipmentImportResponse
} from '../types';

class ApiService {
    private api: AxiosInstance;

    constructor() {
        this.api = axios.create({
            baseURL: '/api',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        // Request interceptor to add auth token
        this.api.interceptors.request.use((config) => {
            const token = localStorage.getItem('token');
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
            return config;
        });

        // Response interceptor to handle auth errors
        this.api.interceptors.response.use(
            (response) => response,
            (error) => {
                if (error.response?.status === 401) {
                    localStorage.removeItem('token');
                    localStorage.removeItem('user');
                    window.location.href = '/login';
                }
                return Promise.reject(error);
            }
        );
    }

    // Auth endpoints
    async login(credentials: LoginRequest): Promise<LoginResponse> {
        const response = await this.api.post<LoginResponse>('/auth/login', credentials);
        return response.data;
    }

    // Admin endpoints
    async createUser(userData: CreateUserRequest): Promise<User> {
        const response = await this.api.post<User>('/admin/users', userData);
        return response.data;
    }

    async getUsers(page = 0, size = 20, role?: string): Promise<PageResponse<User>> {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
        });
        if (role) params.append('role', role);

        const response = await this.api.get<PageResponse<User>>(`/admin/users?${params}`);
        return response.data;
    }

    async updateUserPassword(userId: string, newPassword: string): Promise<User> {
        const response = await this.api.put<User>(`/admin/users/${userId}/password`, {
            newPassword
        });
        return response.data;
    }

    // Shop endpoints
    async getShops(): Promise<Shop[]> {
        const response = await this.api.get<Shop[]>('/shops');
        return response.data;
    }

    async getShop(id: string): Promise<Shop> {
        const response = await this.api.get<Shop>(`/shops/${id}`);
        return response.data;
    }

    async createShop(shopData: CreateShopRequest): Promise<Shop> {
        const response = await this.api.post<Shop>('/shops', shopData);
        return response.data;
    }

    async updateShop(id: string, shopData: UpdateShopRequest): Promise<Shop> {
        const response = await this.api.put<Shop>(`/shops/${id}`, shopData);
        return response.data;
    }

    async deleteShop(id: string): Promise<void> {
        await this.api.delete(`/shops/${id}`);
    }

    // Nomenclature endpoints
    async getNomenclature(
        shopId: string,
        page = 0,
        size = 50,
        search?: string
    ): Promise<PageResponse<NomenclatureItem>> {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
        });
        if (search) params.append('search', search);

        const response = await this.api.get<PageResponse<NomenclatureItem>>(
            `/shops/${shopId}/nomenclature?${params}`
        );
        return response.data;
    }

    async getNomenclatureStats(shopId: string): Promise<NomenclatureStats> {
        const response = await this.api.get<NomenclatureStats>(
            `/shops/${shopId}/nomenclature/stats`
        );
        return response.data;
    }

    async updateNomenclature(shopId: string): Promise<UpdateResult> {
        const response = await this.api.post<UpdateResult>(
            `/shops/${shopId}/nomenclature/update`
        );
        return response.data;
    }

    async getNomenclatureItem(shopId: string, itemId: string): Promise<NomenclatureItem> {
        const response = await this.api.get<NomenclatureItem>(
            `/shops/${shopId}/nomenclature/${itemId}`
        );
        return response.data;
    }

    // Counterparty Contract endpoints
    async getCounterpartyContracts(
        shopId: string,
        page = 0,
        size = 20,
        search?: string
    ): Promise<PageResponse<CounterpartyContract>> {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
        });
        if (search) params.append('search', search);

        const response = await this.api.get<PageResponse<CounterpartyContract>>(
            `/shops/${shopId}/counterparties?${params}`
        );
        return response.data;
    }

    async getCounterpartyContractsList(shopId: string): Promise<CounterpartyContract[]> {
        const response = await this.api.get<CounterpartyContract[]>(
            `/shops/${shopId}/counterparties/list`
        );
        return response.data;
    }

    async getCounterpartyContract(shopId: string, contractId: string): Promise<CounterpartyContract> {
        const response = await this.api.get<CounterpartyContract>(
            `/shops/${shopId}/counterparties/${contractId}`
        );
        return response.data;
    }

    async createCounterpartyContract(shopId: string, data: CreateCounterpartyContractRequest): Promise<CounterpartyContract> {
        const response = await this.api.post<CounterpartyContract>(
            `/shops/${shopId}/counterparties`,
            data
        );
        return response.data;
    }

    async updateCounterpartyContract(
        shopId: string,
        contractId: string,
        data: UpdateCounterpartyContractRequest
    ): Promise<CounterpartyContract> {
        const response = await this.api.put<CounterpartyContract>(
            `/shops/${shopId}/counterparties/${contractId}`,
            data
        );
        return response.data;
    }

    async deleteCounterpartyContract(shopId: string, contractId: string): Promise<void> {
        await this.api.delete(`/shops/${shopId}/counterparties/${contractId}`);
    }

    // Receipt endpoints
    async getReceipts(
        shopId: string,
        page = 0,
        size = 20,
        search?: string,
        startDate?: string,
        endDate?: string,
        counterpartyContractId?: string
    ): Promise<PageResponse<Receipt>> {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
        });
        if (search) params.append('search', search);
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        if (counterpartyContractId) params.append('counterpartyContractId', counterpartyContractId);

        const response = await this.api.get<PageResponse<Receipt>>(
            `/shops/${shopId}/receipts?${params}`
        );
        return response.data;
    }

    async getReceipt(shopId: string, receiptId: string): Promise<Receipt> {
        const response = await this.api.get<Receipt>(
            `/shops/${shopId}/receipts/${receiptId}`
        );
        return response.data;
    }

    async createReceipt(shopId: string, data: CreateReceiptRequest): Promise<Receipt> {
        const response = await this.api.post<Receipt>(
            `/shops/${shopId}/receipts`,
            data
        );
        return response.data;
    }

    async updateReceipt(shopId: string, receiptId: string, data: UpdateReceiptRequest): Promise<Receipt> {
        const response = await this.api.put<Receipt>(
            `/shops/${shopId}/receipts/${receiptId}`,
            data
        );
        return response.data;
    }

    async deleteReceipt(shopId: string, receiptId: string): Promise<void> {
        await this.api.delete(`/shops/${shopId}/receipts/${receiptId}`);
    }

    // Receipt Item endpoints
    async getReceiptItems(
        shopId: string,
        receiptId: string,
        page = 0,
        size = 50,
        search?: string
    ): Promise<PageResponse<ReceiptItem>> {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
        });
        if (search) params.append('search', search);

        const response = await this.api.get<PageResponse<ReceiptItem>>(
            `/shops/${shopId}/receipts/${receiptId}/items?${params}`
        );
        return response.data;
    }

    async getReceiptItem(shopId: string, receiptId: string, itemId: string): Promise<ReceiptItem> {
        const response = await this.api.get<ReceiptItem>(
            `/shops/${shopId}/receipts/${receiptId}/items/${itemId}`
        );
        return response.data;
    }

    async createReceiptItem(
        shopId: string,
        receiptId: string,
        data: CreateReceiptItemRequest
    ): Promise<ReceiptItem> {
        const response = await this.api.post<ReceiptItem>(
            `/shops/${shopId}/receipts/${receiptId}/items`,
            data
        );
        return response.data;
    }

    async updateReceiptItem(
        shopId: string,
        receiptId: string,
        itemId: string,
        data: UpdateReceiptItemRequest
    ): Promise<ReceiptItem> {
        const response = await this.api.put<ReceiptItem>(
            `/shops/${shopId}/receipts/${receiptId}/items/${itemId}`,
            data
        );
        return response.data;
    }

    async deleteReceiptItem(shopId: string, receiptId: string, itemId: string): Promise<void> {
        await this.api.delete(`/shops/${shopId}/receipts/${receiptId}/items/${itemId}`);
    }

    // Excel Import endpoint
    async importReceiptItems(
        shopId: string,
        receiptId: string,
        file: File,
        mapping: Record<string, string>,
        hasHeader = true,
        strict = true
    ): Promise<ExcelImportResponse> {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('mapping', JSON.stringify({ mapping, hasHeader }));
        formData.append('strict', strict.toString());

        const response = await this.api.post<ExcelImportResponse>(
            `/shops/${shopId}/receipts/${receiptId}/items/import`,
            formData,
            {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            }
        );
        return response.data;
    }

    // Shipment endpoints
    async getShipments(
        shopId: string,
        page = 0,
        size = 20,
        search?: string,
        startDate?: string,
        endDate?: string
    ): Promise<PageResponse<Shipment>> {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
        });
        if (search) params.append('search', search);
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);

        const response = await this.api.get<PageResponse<Shipment>>(
            `/shops/${shopId}/shipments?${params}`
        );
        return response.data;
    }

    async getShipment(shopId: string, shipmentId: string): Promise<Shipment> {
        const response = await this.api.get<Shipment>(
            `/shops/${shopId}/shipments/${shipmentId}`
        );
        return response.data;
    }

    async createShipment(shopId: string, data: CreateShipmentRequest): Promise<Shipment> {
        const response = await this.api.post<Shipment>(
            `/shops/${shopId}/shipments`,
            data
        );
        return response.data;
    }

    async updateShipment(shopId: string, shipmentId: string, data: UpdateShipmentRequest): Promise<Shipment> {
        const response = await this.api.put<Shipment>(
            `/shops/${shopId}/shipments/${shipmentId}`,
            data
        );
        return response.data;
    }

    async deleteShipment(shopId: string, shipmentId: string): Promise<void> {
        await this.api.delete(`/shops/${shopId}/shipments/${shipmentId}`);
    }

    // Shipment Item endpoints
    async getShipmentItems(
        shopId: string,
        shipmentId: string,
        page = 0,
        size = 50,
        search?: string
    ): Promise<PageResponse<ShipmentItem>> {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
        });
        if (search) params.append('search', search);

        const response = await this.api.get<PageResponse<ShipmentItem>>(
            `/shops/${shopId}/shipments/${shipmentId}/items?${params}`
        );
        return response.data;
    }

    async getShipmentItem(shopId: string, shipmentId: string, itemId: string): Promise<ShipmentItem> {
        const response = await this.api.get<ShipmentItem>(
            `/shops/${shopId}/shipments/${shipmentId}/items/${itemId}`
        );
        return response.data;
    }

    async createShipmentItem(
        shopId: string,
        shipmentId: string,
        data: CreateShipmentItemRequest
    ): Promise<ShipmentItem> {
        const response = await this.api.post<ShipmentItem>(
            `/shops/${shopId}/shipments/${shipmentId}/items`,
            data
        );
        return response.data;
    }

    async updateShipmentItem(
        shopId: string,
        shipmentId: string,
        itemId: string,
        data: UpdateShipmentItemRequest
    ): Promise<ShipmentItem> {
        const response = await this.api.put<ShipmentItem>(
            `/shops/${shopId}/shipments/${shipmentId}/items/${itemId}`,
            data
        );
        return response.data;
    }

    async deleteShipmentItem(shopId: string, shipmentId: string, itemId: string): Promise<void> {
        await this.api.delete(`/shops/${shopId}/shipments/${shipmentId}/items/${itemId}`);
    }

    // Shipment Excel Import endpoint
    async importShipment(
        shopId: string,
        file: File,
        mapping: Record<string, string>,
        shipmentDate: string,
        hasHeader = true,
        strict = true,
        startRow = 1
    ): Promise<ShipmentImportResponse> {
        const importRequest = {
            mapping,
            hasHeader,
            strict,
            shipmentDate,
            startRow
        };

        const formData = new FormData();
        formData.append('file', file);
        
        // Add JSON data as a separate part with correct content type
        const importRequestBlob = new Blob([JSON.stringify(importRequest)], {
            type: 'application/json'
        });
        formData.append('importRequest', importRequestBlob);

        const response = await this.api.post<ShipmentImportResponse>(
            `/shops/${shopId}/shipments/import`,
            formData,
            {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            }
        );
        return response.data;
    }
}

export const apiService = new ApiService();
