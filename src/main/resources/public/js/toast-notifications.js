/**
 * Sistema de Notificações Toast em Tempo Real
 * Usa Toastify.js para exibir alertas de prazos
 */

class ToastNotificationManager {
    constructor() {
        this.checkInterval = 60000; // 1 minuto (padrão)
        this.intervalId = null;
        this.notifiedAlerts = new Set(); // IDs já notificados nesta sessão
        this.enabled = true;
        this.debugMode = false;
    }

    /**
     * Inicializa o sistema de notificações
     */
    async init() {
        console.log('📱 Toast Notification Manager inicializado');

        // Buscar configurações do usuário
        await this.loadUserConfig();

        // Se notificações estão desabilitadas, não iniciar
        if (!this.enabled) {
            console.log('⏸️ Sistema de notificações não inicializado (desabilitado nas configurações)');
            return;
        }

        // Verificar alertas imediatamente
        this.checkAlerts();

        // Iniciar verificação periódica
        this.startPeriodicCheck();

        // Debug: mostrar toast de teste se em modo debug
        if (this.debugMode) {
            this.showTestToast();
        }
    }

    /**
     * Carrega configurações do usuário
     */
    async loadUserConfig() {
        try {
            // Buscar configurações do usuário no backend
            const response = await fetch('/api/configuracoes', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                console.warn('Não foi possível carregar configurações do usuário');
                this.enabled = false;
                return;
            }

            const data = await response.json();

            if (data.success && data.dados) {
                // Verificar se notificações toast estão habilitadas
                const notifToast = data.dados.notif_toast;
                this.enabled = (notifToast === 'true' || notifToast === true);

                console.log('⚙️ Notificações toast:', this.enabled ? 'HABILITADAS' : 'DESABILITADAS');

                if (!this.enabled) {
                    console.log('🔕 Notificações desabilitadas pelo usuário');
                    this.stopPeriodicCheck();
                }
            } else {
                this.enabled = false;
            }

            // Intervalo de verificação pode ser configurável
            // Por enquanto usa padrão de 1 minuto
            if (this.enabled) {
                console.log('⚙️ Configurações carregadas: verificação a cada', this.checkInterval / 1000, 'segundos');
            }
        } catch (error) {
            console.error('Erro ao carregar configurações:', error);
            this.enabled = false;
        }
    }

    /**
     * Inicia verificação periódica de alertas
     */
    startPeriodicCheck() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
        }

        this.intervalId = setInterval(() => {
            this.checkAlerts();
        }, this.checkInterval);

        console.log('⏱️ Verificação periódica iniciada');
    }

    /**
     * Para a verificação periódica
     */
    stopPeriodicCheck() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = null;
            console.log('⏱️ Verificação periódica parada');
        }
    }

    /**
     * Verifica alertas pendentes no servidor
     */
    async checkAlerts() {
        if (!this.enabled) {
            return;
        }

        try {
            const response = await fetch('/api/notificacoes/alertas', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Erro ao buscar alertas');
            }

            const data = await response.json();

            if (data.success && data.dados && data.dados.length > 0) {
                this.processAlerts(data.dados);
            }

        } catch (error) {
            console.error('Erro ao verificar alertas:', error);
        }
    }

    /**
     * Processa lista de alertas e exibe toasts
     */
    processAlerts(alertas) {
        alertas.forEach(alerta => {
            const alertId = `${alerta.notaId}_${alerta.nivel}`;

            // Evitar notificar novamente o mesmo alerta nesta sessão
            if (!this.notifiedAlerts.has(alertId)) {
                this.showAlert(alerta);
                this.notifiedAlerts.add(alertId);
            }
        });
    }

    /**
     * Exibe um alerta como toast
     */
    showAlert(alerta) {
        const config = this.getToastConfig(alerta.nivel);

        // Criar mensagem
        let mensagem = `📝 ${alerta.titulo}\n`;
        mensagem += `📅 Prazo: ${alerta.prazoFinal}\n`;

        if (alerta.diasRestantes === 0) {
            mensagem += `⏰ Vence hoje!`;
        } else if (alerta.diasRestantes === 1) {
            mensagem += `⏰ Vence amanhã!`;
        } else {
            mensagem += `⏰ Faltam ${alerta.diasRestantes} dias`;
        }

        // Exibir toast
        Toastify({
            text: mensagem,
            duration: config.duration,
            close: true,
            gravity: "top",
            position: "right",
            stopOnFocus: true,
            style: {
                background: config.background,
                color: config.color,
                borderLeft: `4px solid ${config.borderColor}`,
                borderRadius: "8px",
                boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
                padding: "16px",
                fontSize: "14px",
                fontWeight: "500",
                whiteSpace: "pre-line"
            },
            onClick: function() {
                // Redirecionar para a nota
                window.location.href = `/notas?id=${alerta.notaId}`;
            }
        }).showToast();

        console.log(`🔔 Alerta exibido: ${alerta.titulo} (${alerta.nivel})`);

        // Tocar som de notificação se disponível
        this.playNotificationSound(alerta.nivel);
    }

    /**
     * Retorna configuração de toast baseado no nível
     */
    getToastConfig(nivel) {
        const configs = {
            'CRITICO': {
                background: 'linear-gradient(135deg, #dc2626, #991b1b)',
                color: '#ffffff',
                borderColor: '#7f1d1d',
                duration: -1 // Não fecha automaticamente
            },
            'URGENTE': {
                background: 'linear-gradient(135deg, #ea580c, #c2410c)',
                color: '#ffffff',
                borderColor: '#9a3412',
                duration: 10000 // 10 segundos
            },
            'ATENCAO': {
                background: 'linear-gradient(135deg, #eab308, #ca8a04)',
                color: '#ffffff',
                borderColor: '#a16207',
                duration: 7000 // 7 segundos
            },
            'AVISO': {
                background: 'linear-gradient(135deg, #3b82f6, #2563eb)',
                color: '#ffffff',
                borderColor: '#1e40af',
                duration: 5000 // 5 segundos
            }
        };

        return configs[nivel] || configs['AVISO'];
    }

    /**
     * Toca som de notificação (se disponível)
     */
    playNotificationSound(nivel) {
        try {
            // Usar Audio API do navegador
            const audio = new Audio('/sounds/notification.mp3');
            audio.volume = 0.3;
            audio.play().catch(err => {
                // Ignorar erro se som não estiver disponível ou navegador bloquear
                console.log('Som de notificação não disponível');
            });
        } catch (error) {
            // Ignorar
        }
    }

    /**
     * Exibe toast de sucesso personalizado
     */
    showSuccess(mensagem) {
        Toastify({
            text: mensagem,
            duration: 3000,
            close: true,
            gravity: "top",
            position: "right",
            stopOnFocus: true,
            style: {
                background: "linear-gradient(135deg, #22c55e, #16a34a)",
                color: "#ffffff",
                borderLeft: "4px solid #15803d",
                borderRadius: "8px",
                boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
                padding: "16px"
            }
        }).showToast();
    }

    /**
     * Exibe toast de erro personalizado
     */
    showError(mensagem) {
        Toastify({
            text: mensagem,
            duration: 5000,
            close: true,
            gravity: "top",
            position: "right",
            stopOnFocus: true,
            style: {
                background: "linear-gradient(135deg, #ef4444, #dc2626)",
                color: "#ffffff",
                borderLeft: "4px solid #991b1b",
                borderRadius: "8px",
                boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
                padding: "16px"
            }
        }).showToast();
    }

    /**
     * Exibe toast de informação personalizado
     */
    showInfo(mensagem) {
        Toastify({
            text: mensagem,
            duration: 4000,
            close: true,
            gravity: "top",
            position: "right",
            stopOnFocus: true,
            style: {
                background: "linear-gradient(135deg, #3b82f6, #2563eb)",
                color: "#ffffff",
                borderLeft: "4px solid #1e40af",
                borderRadius: "8px",
                boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
                padding: "16px"
            }
        }).showToast();
    }

    /**
     * Exibe toast de teste (debug)
     */
    showTestToast() {
        Toastify({
            text: "🧪 Toast de teste - Sistema de notificações ativo!",
            duration: 3000,
            close: true,
            gravity: "top",
            position: "right",
            style: {
                background: "linear-gradient(135deg, #8b5cf6, #7c3aed)",
                color: "#ffffff",
                borderRadius: "8px",
                padding: "16px"
            }
        }).showToast();
    }

    /**
     * Limpa cache de alertas notificados
     */
    clearNotifiedCache() {
        this.notifiedAlerts.clear();
        console.log('🗑️ Cache de alertas notificados limpo');
    }

    /**
     * Destroi o gerenciador (cleanup)
     */
    destroy() {
        this.stopPeriodicCheck();
        this.clearNotifiedCache();
        console.log('💥 Toast Notification Manager destruído');
    }
}

// Instância global
let toastManager = null;

// Inicializar quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', function() {
    // Verificar se estamos em uma página autenticada
    const isAuthenticated = document.querySelector('.app-container') !== null;

    if (isAuthenticated) {
        toastManager = new ToastNotificationManager();
        toastManager.init();

        // Expor globalmente para uso em outros scripts
        window.toastManager = toastManager;
    }
});

// Cleanup ao sair da página
window.addEventListener('beforeunload', function() {
    if (toastManager) {
        toastManager.destroy();
    }
});

// Expor métodos utilitários globalmente
window.showSuccessToast = function(mensagem) {
    if (toastManager) {
        toastManager.showSuccess(mensagem);
    }
};

window.showErrorToast = function(mensagem) {
    if (toastManager) {
        toastManager.showError(mensagem);
    }
};

window.showInfoToast = function(mensagem) {
    if (toastManager) {
        toastManager.showInfo(mensagem);
    }
};
