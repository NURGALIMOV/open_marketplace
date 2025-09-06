import { ToastOptions, ModalOptions } from '../types';

// Toast notifications
export class Toast {
    private static container: HTMLElement | null = null;

    private static getContainer(): HTMLElement {
        if (!this.container) {
            this.container = document.getElementById('toast-container');
            if (!this.container) {
                this.container = document.createElement('div');
                this.container.id = 'toast-container';
                document.body.appendChild(this.container);
            }
        }
        return this.container;
    }

    static show(options: ToastOptions): void {
        const { type, message, duration = 5000 } = options;
        const container = this.getContainer();

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `
            ${message}
            <button class="toast-close" aria-label="Close">&times;</button>
        `;

        const closeBtn = toast.querySelector('.toast-close') as HTMLElement;
        closeBtn.addEventListener('click', () => this.remove(toast));

        container.appendChild(toast);

        // Auto remove after duration
        setTimeout(() => this.remove(toast), duration);
    }

    private static remove(toast: HTMLElement): void {
        toast.style.animation = 'slideOut 0.3s ease-in forwards';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }

    static success(message: string, duration?: number): void {
        this.show({ type: 'success', message, duration });
    }

    static error(message: string, duration?: number): void {
        this.show({ type: 'error', message, duration });
    }

    static warning(message: string, duration?: number): void {
        this.show({ type: 'warning', message, duration });
    }

    static info(message: string, duration?: number): void {
        this.show({ type: 'info', message, duration });
    }
}

// Modal dialogs
export class Modal {
    private static activeModal: HTMLElement | null = null;

    static show(options: ModalOptions): HTMLElement {
        const { title, content, footer, onClose } = options;

        // Close existing modal
        this.close();

        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';

        const modal = document.createElement('div');
        modal.className = 'modal';

        const header = document.createElement('div');
        header.className = 'modal-header';
        header.innerHTML = `
            <h3 class="modal-title">${title}</h3>
            <button class="modal-close" aria-label="Close">&times;</button>
        `;

        const body = document.createElement('div');
        body.className = 'modal-body';
        if (typeof content === 'string') {
            body.innerHTML = content;
        } else {
            body.appendChild(content);
        }

        modal.appendChild(header);
        modal.appendChild(body);

        if (footer) {
            const footerEl = document.createElement('div');
            footerEl.className = 'modal-footer';
            footerEl.appendChild(footer);
            modal.appendChild(footerEl);
        }

        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        // Close handlers
        const closeModal = () => {
            this.close();
            if (onClose) onClose();
        };

        header.querySelector('.modal-close')?.addEventListener('click', closeModal);
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) closeModal();
        });

        // ESC key handler
        const escHandler = (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                closeModal();
                document.removeEventListener('keydown', escHandler);
            }
        };
        document.addEventListener('keydown', escHandler);

        this.activeModal = overlay;
        return modal;
    }

    static close(): void {
        if (this.activeModal) {
            document.body.removeChild(this.activeModal);
            this.activeModal = null;
        }
    }

    static confirm(message: string, title = 'Подтверждение'): Promise<boolean> {
        return new Promise((resolve) => {
            const footer = document.createElement('div');
            footer.innerHTML = `
                <button class="btn btn-secondary" data-action="cancel">Отмена</button>
                <button class="btn btn-danger" data-action="confirm">Подтвердить</button>
            `;

            footer.addEventListener('click', (e) => {
                const target = e.target as HTMLElement;
                if (target.dataset.action === 'confirm') {
                    resolve(true);
                    this.close();
                } else if (target.dataset.action === 'cancel') {
                    resolve(false);
                    this.close();
                }
            });

            this.show({
                title,
                content: `<p>${message}</p>`,
                footer,
                onClose: () => resolve(false),
            });
        });
    }
}

// Loading spinner
export class Loading {
    private static overlay: HTMLElement | null = null;

    static show(message = 'Загрузка...'): void {
        this.hide(); // Hide existing

        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.style.backgroundColor = 'rgba(255, 255, 255, 0.8)';

        const loading = document.createElement('div');
        loading.className = 'loading';
        loading.innerHTML = `
            <div class="loading-spinner"></div>
            <p>${message}</p>
        `;

        overlay.appendChild(loading);
        document.body.appendChild(overlay);

        this.overlay = overlay;
    }

    static hide(): void {
        if (this.overlay) {
            document.body.removeChild(this.overlay);
            this.overlay = null;
        }
    }
}

// Utility functions
export function formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
}

export function formatNumber(num: number): string {
    return num.toLocaleString('ru-RU');
}

export function formatDuration(ms: number): string {
    if (ms < 1000) return `${ms}мс`;
    if (ms < 60000) return `${(ms / 1000).toFixed(1)}с`;
    return `${(ms / 60000).toFixed(1)}мин`;
}

export function debounce<T extends (...args: any[]) => void>(
    func: T,
    delay: number
): (...args: Parameters<T>) => void {
    let timeoutId: NodeJS.Timeout;
    return (...args: Parameters<T>) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => func(...args), delay);
    };
}

export function createElement(
    tag: string,
    className?: string,
    innerHTML?: string
): HTMLElement {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (innerHTML) element.innerHTML = innerHTML;
    return element;
}

export function validateEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

export function validatePassword(password: string): string | null {
    if (password.length < 8) {
        return 'Пароль должен содержать минимум 8 символов';
    }
    if (!/\d/.test(password)) {
        return 'Пароль должен содержать минимум одну цифру';
    }
    if (!/[a-z]/.test(password)) {
        return 'Пароль должен содержать минимум одну строчную букву';
    }
    if (!/[A-Z]/.test(password)) {
        return 'Пароль должен содержать минимум одну заглавную букву';
    }
    return null;
}

// Add CSS for slideOut animation
const style = document.createElement('style');
style.textContent = `
@keyframes slideOut {
    from {
        transform: translateX(0);
        opacity: 1;
    }
    to {
        transform: translateX(100%);
        opacity: 0;
    }
}
`;
document.head.appendChild(style);
