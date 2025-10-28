/**
 * NOTISBLOKK 1.0 - THEME SWITCHER
 * Gerenciador de temas claro/escuro
 */

class ThemeSwitcher {
    constructor() {
        this.currentTheme = this.getStoredTheme() || 'light';
        this.init();
    }

    /**
     * Inicializa o theme switcher
     */
    init() {
        // Tema já foi aplicado pelo script inline no head
        // Apenas sincronizar com o que já está no DOM
        this.currentTheme = document.documentElement.getAttribute('data-theme') || this.currentTheme;

        // Atualizar apenas o botão de toggle
        this.updateToggleButton();

        // Adicionar listener ao botão de toggle
        const toggleBtn = document.getElementById('theme-toggle');
        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => this.toggle());
        }

        // Escutar mudanças de preferência do sistema
        this.watchSystemPreference();
    }

    /**
     * Obtém o tema armazenado no localStorage
     */
    getStoredTheme() {
        return localStorage.getItem('theme');
    }

    /**
     * Armazena o tema no localStorage
     */
    storeTheme(theme) {
        localStorage.setItem('theme', theme);
    }

    /**
     * Aplica o tema ao HTML
     */
    async applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        this.currentTheme = theme;
        this.storeTheme(theme);
        this.updateToggleButton();

        // Salvar tema na sessão do servidor
        try {
            await fetch('/api/theme', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: `theme=${theme}`
            });
        } catch (error) {
            console.error('Erro ao salvar tema no servidor:', error);
        }

        // Disparar evento customizado
        window.dispatchEvent(new CustomEvent('themeChanged', { detail: { theme } }));
    }

    /**
     * Alterna entre temas claro e escuro
     */
    toggle() {
        const newTheme = this.currentTheme === 'light' ? 'dark' : 'light';
        this.applyTheme(newTheme);
    }

    /**
     * Atualiza o ícone do botão de toggle
     */
    updateToggleButton() {
        const toggleBtn = document.getElementById('theme-toggle');
        if (!toggleBtn) return;

        const icon = toggleBtn.querySelector('i, svg, span');
        if (!icon) return;

        // Atualizar ícone ou texto do botão
        if (this.currentTheme === 'dark') {
            icon.textContent = '☀️'; // Sol para tema claro
            toggleBtn.setAttribute('title', 'Mudar para tema claro');
        } else {
            icon.textContent = '🌙'; // Lua para tema escuro
            toggleBtn.setAttribute('title', 'Mudar para tema escuro');
        }
    }

    /**
     * Observa mudanças na preferência de tema do sistema
     */
    watchSystemPreference() {
        const darkModeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

        darkModeMediaQuery.addEventListener('change', (e) => {
            // Só aplicar se não houver preferência salva
            if (!this.getStoredTheme()) {
                const systemTheme = e.matches ? 'dark' : 'light';
                this.applyTheme(systemTheme);
            }
        });
    }

    /**
     * Obtém o tema atual
     */
    getTheme() {
        return this.currentTheme;
    }
}

// Inicializar automaticamente quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
    window.themeSwitcher = new ThemeSwitcher();
});
