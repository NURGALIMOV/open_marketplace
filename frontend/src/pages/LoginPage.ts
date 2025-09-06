import { apiService } from '../services/api';
import { authService } from '../services/auth';
import { router } from '../router';
import { Toast } from '../utils/ui';
import { validateEmail, validatePassword } from '../utils/ui';

export function LoginPage(): HTMLElement {
    const container = document.createElement('div');
    container.className = 'app';

    container.innerHTML = `
        <div class="main">
            <form class="form" id="login-form">
                <h1 class="form-title">Вход в систему</h1>
                
                <div class="form-group">
                    <label class="form-label" for="email">Email</label>
                    <input 
                        type="email" 
                        id="email" 
                        class="form-input" 
                        placeholder="Введите email"
                        required
                    />
                    <div class="form-error" id="email-error"></div>
                </div>

                <div class="form-group">
                    <label class="form-label" for="password">Пароль</label>
                    <input 
                        type="password" 
                        id="password" 
                        class="form-input" 
                        placeholder="Введите пароль"
                        required
                    />
                    <div class="form-error" id="password-error"></div>
                </div>

                <button type="submit" class="btn btn-primary btn-full" id="login-btn">
                    Войти
                </button>

                <div class="form-error" id="form-error"></div>
            </form>
        </div>
    `;

    const form = container.querySelector('#login-form') as HTMLFormElement;
    const emailInput = container.querySelector('#email') as HTMLInputElement;
    const passwordInput = container.querySelector('#password') as HTMLInputElement;
    const loginBtn = container.querySelector('#login-btn') as HTMLButtonElement;
    const emailError = container.querySelector('#email-error') as HTMLElement;
    const passwordError = container.querySelector('#password-error') as HTMLElement;
    const formError = container.querySelector('#form-error') as HTMLElement;

    // Clear errors on input
    emailInput.addEventListener('input', () => {
        emailError.textContent = '';
        emailInput.classList.remove('error');
        formError.textContent = '';
    });

    passwordInput.addEventListener('input', () => {
        passwordError.textContent = '';
        passwordInput.classList.remove('error');
        formError.textContent = '';
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const email = emailInput.value.trim();
        const password = passwordInput.value;

        // Clear previous errors
        emailError.textContent = '';
        passwordError.textContent = '';
        formError.textContent = '';
        emailInput.classList.remove('error');
        passwordInput.classList.remove('error');

        // Validate email
        if (!email) {
            emailError.textContent = 'Email обязателен';
            emailInput.classList.add('error');
            emailInput.focus();
            return;
        }

        if (!validateEmail(email)) {
            emailError.textContent = 'Введите корректный email';
            emailInput.classList.add('error');
            emailInput.focus();
            return;
        }

        // Validate password
        if (!password) {
            passwordError.textContent = 'Пароль обязателен';
            passwordInput.classList.add('error');
            passwordInput.focus();
            return;
        }

        // Disable form during login
        loginBtn.disabled = true;
        loginBtn.textContent = 'Вход...';

        try {
            const response = await apiService.login({ email, password });
            
            // Parse user from JWT token
            const tokenParts = response.accessToken.split('.');
            const payload = JSON.parse(atob(tokenParts[1]));
            
            const user = {
                id: payload.sub,
                email: payload.email,
                role: payload.role,
                createdAt: new Date(payload.iat * 1000).toISOString(),
                updatedAt: new Date(payload.iat * 1000).toISOString(),
            };

            authService.login(response.accessToken, user);
            Toast.success('Вход выполнен успешно');
            router.navigate('/dashboard');
            
        } catch (error: any) {
            console.error('Login error:', error);
            
            if (error.response?.status === 401) {
                formError.textContent = 'Неверный email или пароль';
            } else {
                formError.textContent = 'Произошла ошибка. Попробуйте еще раз.';
            }
            
            loginBtn.disabled = false;
            loginBtn.textContent = 'Войти';
        }
    });

    // Focus on email input
    setTimeout(() => emailInput.focus(), 100);

    return container;
}
