/**
 * NOTISBLOKK 1.0 - MAIN.JS
 * Funções e utilitários principais da aplicação
 */

/**
 * Gerenciador da Sidebar
 */
class SidebarManager {
    constructor() {
        this.sidebar = document.querySelector('.app-sidebar');
        this.toggleBtn = document.querySelector('.hamburger-btn');
        this.isCollapsed = this.getCollapsedState();

        this.init();
    }

    init() {
        if (!this.sidebar || !this.toggleBtn) return;

        // Aplicar estado inicial
        if (this.isCollapsed) {
            this.sidebar.classList.add('collapsed');
        }

        // Adicionar listener ao botão
        this.toggleBtn.addEventListener('click', () => this.toggle());

        // Marcar item ativo baseado na URL atual
        this.setActiveMenuItem();
    }

    toggle() {
        this.isCollapsed = !this.isCollapsed;
        this.sidebar.classList.toggle('collapsed');
        this.storeCollapsedState(this.isCollapsed);
    }

    getCollapsedState() {
        const stored = localStorage.getItem('sidebarCollapsed');
        return stored === 'true';
    }

    storeCollapsedState(collapsed) {
        localStorage.setItem('sidebarCollapsed', collapsed);
    }

    setActiveMenuItem() {
        const currentPath = window.location.pathname;
        const menuItems = document.querySelectorAll('.sidebar-menu-item');

        menuItems.forEach(item => {
            const link = item.getAttribute('href') || item.dataset.href;
            if (link && currentPath.startsWith(link)) {
                item.classList.add('active');
            }
        });
    }
}

/**
 * Gerenciador de Alertas
 */
class AlertManager {
    static show(message, type = 'info', duration = 5000) {
        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        alert.textContent = message;
        alert.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            z-index: 9999;
            min-width: 300px;
            max-width: 500px;
            animation: slideInRight 0.3s ease;
        `;

        document.body.appendChild(alert);

        // Auto-remover após duration
        setTimeout(() => {
            alert.style.animation = 'slideOutRight 0.3s ease';
            setTimeout(() => alert.remove(), 300);
        }, duration);

        return alert;
    }

    static success(message, duration) {
        return this.show(message, 'success', duration);
    }

    static error(message, duration) {
        return this.show(message, 'error', duration);
    }

    static warning(message, duration) {
        return this.show(message, 'warning', duration);
    }

    static info(message, duration) {
        return this.show(message, 'info', duration);
    }
}

/**
 * Gerenciador de Modais
 */
class ModalManager {
    static show(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';

        // Adicionar listener para fechar ao clicar fora
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                this.hide(modalId);
            }
        });
    }

    static hide(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        modal.style.display = 'none';
        document.body.style.overflow = '';
    }

    static toggle(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        if (modal.style.display === 'flex') {
            this.hide(modalId);
        } else {
            this.show(modalId);
        }
    }
}

/**
 * Formatadores
 */
class Formatters {
    static date(dateString, format = 'dd/MM/yyyy') {
        if (!dateString) return '-';
        // Assuming dateString is already in correct format from server
        return dateString;
    }

    static currency(value) {
        return new Intl.NumberFormat('pt-BR', {
            style: 'currency',
            currency: 'BRL'
        }).format(value);
    }

    static number(value, decimals = 0) {
        return new Intl.NumberFormat('pt-BR', {
            minimumFractionDigits: decimals,
            maximumFractionDigits: decimals
        }).format(value);
    }

    static fileSize(bytes) {
        if (bytes === 0) return '0 Bytes';

        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }
}

/**
 * Utilitários de Validação (Client-side)
 */
class Validators {
    static email(email) {
        const pattern = /^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/;
        return pattern.test(email);
    }

    static required(value) {
        return value !== null && value !== undefined && value.trim() !== '';
    }

    static minLength(value, min) {
        return value && value.length >= min;
    }

    static maxLength(value, max) {
        return value && value.length <= max;
    }

    static password(password) {
        // Mínimo 8 caracteres, 1 maiúscula, 1 minúscula, 1 número, 1 especial
        const pattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
        return pattern.test(password);
    }
}

/**
 * Utilitários HTTP
 */
class HTTP {
    static async get(url) {
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    }

    static async post(url, data) {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    }

    static async put(url, data) {
        const response = await fetch(url, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    }

    static async delete(url) {
        const response = await fetch(url, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    }
}

/**
 * Adicionar animações CSS
 */
const animationStyles = `
@keyframes slideInRight {
    from {
        transform: translateX(100%);
        opacity: 0;
    }
    to {
        transform: translateX(0);
        opacity: 1;
    }
}

@keyframes slideOutRight {
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

// Adicionar estilos de animação ao head
const styleSheet = document.createElement('style');
styleSheet.textContent = animationStyles;
document.head.appendChild(styleSheet);

/**
 * Inicialização
 */
document.addEventListener('DOMContentLoaded', () => {
    // Inicializar sidebar
    window.sidebarManager = new SidebarManager();

    // Expor utilitários globalmente
    window.AlertManager = AlertManager;
    window.ModalManager = ModalManager;
    window.Formatters = Formatters;
    window.Validators = Validators;
    window.HTTP = HTTP;

    // Adicionar listener para fechar modais com ESC
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal-overlay').forEach(modal => {
                modal.style.display = 'none';
                document.body.style.overflow = '';
            });
        }
    });

    // Adicionar confirmação para links de logout
    const logoutLinks = document.querySelectorAll('a[href*="/auth/logout"]');
    logoutLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            if (!confirm('Deseja realmente sair?')) {
                e.preventDefault();
            }
        });
    });

    console.log('🚀 Notisblokk 1.0 iniciado com sucesso!');
});
