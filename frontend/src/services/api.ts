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
    PageResponse
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
}

export const apiService = new ApiService();
