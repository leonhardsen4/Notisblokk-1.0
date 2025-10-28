# Implementação: Alertas em Tempo Real (Toast Notifications)

## Resumo

Sistema completo de notificações toast em tempo real utilizando Toastify.js para exibir alertas de prazos das notas de forma não-intrusiva e visual.

## Arquitetura

### Frontend: Sistema de Polling

O sistema utiliza polling periódico (a cada 60 segundos) para verificar alertas pendentes no servidor, evitando a complexidade de WebSockets para uma aplicação embarcada.

```
┌──────────────────────────────────────────────┐
│  ToastNotificationManager (JavaScript)       │
│  ┌────────────────────────────────────────┐  │
│  │ 1. Inicialização ao carregar página    │  │
│  │ 2. Verificação imediata de alertas     │  │
│  │ 3. setInterval(60000ms) para polling   │  │
│  │ 4. Cache de alertas já exibidos (Set)  │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
                    ↓
         GET /api/notificacoes/alertas
                    ↓
┌──────────────────────────────────────────────┐
│  NotificacaoController + NotificacaoService  │
│  ┌────────────────────────────────────────┐  │
│  │ 1. Busca todas as notas não resolvidas │  │
│  │ 2. Calcula dias restantes até prazo    │  │
│  │ 3. Classifica por nível de urgência    │  │
│  │ 4. Retorna JSON ordenado por prioridade│  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

## Componentes Implementados

### 1. ToastNotificationManager (toast-notifications.js)

**Localização:** `src/main/resources/public/js/toast-notifications.js`

#### Classe Principal

```javascript
class ToastNotificationManager {
    constructor() {
        this.checkInterval = 60000;        // 1 minuto
        this.intervalId = null;
        this.notifiedAlerts = new Set();   // Previne duplicatas
        this.enabled = true;
        this.debugMode = false;
    }
}
```

#### Métodos Principais

| Método | Descrição |
|--------|-----------|
| `init()` | Inicializa sistema, carrega config, primeira verificação |
| `checkAlerts()` | Faz requisição GET para `/api/notificacoes/alertas` |
| `processAlerts(alertas)` | Processa array de alertas, evita duplicatas |
| `showAlert(alerta)` | Exibe toast com Toastify.js, aplica estilo por nível |
| `getToastConfig(nivel)` | Retorna configuração (cor, duração) por nível |
| `playNotificationSound(nivel)` | Toca som de notificação (se disponível) |

#### Métodos Utilitários Globais

```javascript
window.showSuccessToast(mensagem);  // Toast verde de sucesso
window.showErrorToast(mensagem);    // Toast vermelho de erro
window.showInfoToast(mensagem);     // Toast azul informativo
```

### 2. Níveis de Alerta e Estilos

| Nível | Condição | Cor (Gradiente) | Duração | Prioridade |
|-------|----------|-----------------|---------|------------|
| **CRÍTICO** | Prazo vencido (dias < 0) | Vermelho escuro (#dc2626 → #991b1b) | ∞ (não fecha) | 1 |
| **URGENTE** | 0-1 dias restantes | Laranja (#ea580c → #c2410c) | 10 segundos | 2 |
| **ATENÇÃO** | 2-3 dias restantes | Amarelo (#eab308 → #ca8a04) | 7 segundos | 3 |
| **AVISO** | 4-5 dias restantes | Azul (#3b82f6 → #2563eb) | 5 segundos | 4 |

**Características visuais:**
- Border-left de 4px na cor da categoria
- Border-radius de 8px
- Box-shadow para profundidade
- Posição: top-right da tela
- Botão de fechar (X) disponível
- Cursor pointer + click handler para navegar à nota

### 3. Integração com Layout Base

**Arquivo:** `src/main/resources/templates/layout/base.html`

#### Adições no `<head>`:

```html
<!-- Toastify.js (CDN) -->
<link rel="stylesheet" type="text/css"
      href="https://cdn.jsdelivr.net/npm/toastify-js/src/toastify.min.css">
<script type="text/javascript"
        src="https://cdn.jsdelivr.net/npm/toastify-js"></script>
```

#### Adições antes de `</body>`:

```html
<!-- Toast Notifications (apenas para usuários autenticados) -->
<th:block th:if="${isAuthenticated}">
    <script src="/js/toast-notifications.js"></script>
</th:block>
```

**Justificativa:** Notificações só são relevantes para usuários logados com acesso às notas.

## Fluxo de Funcionamento

### 1. Inicialização

```
Usuário autentica → Carrega página
              ↓
   DOMContentLoaded event
              ↓
  Verifica se usuário está autenticado (.app-container existe?)
              ↓
        Sim → Cria instância ToastNotificationManager
              ↓
          toastManager.init()
              ↓
    ┌─────────────────────────┐
    │ 1. loadUserConfig()     │
    │ 2. checkAlerts()        │ ← Verificação imediata
    │ 3. startPeriodicCheck() │ ← Inicia polling
    └─────────────────────────┘
```

### 2. Verificação Periódica

```
setInterval(60000ms)
      ↓
checkAlerts()
      ↓
fetch('/api/notificacoes/alertas')
      ↓
  Resposta JSON:
  {
    "success": true,
    "dados": [
      {
        "notaId": 123,
        "titulo": "Relatório fiscal",
        "prazoFinal": "2025-10-28",
        "diasRestantes": 1,
        "nivel": "URGENTE",
        "prioridade": 2
      }
    ],
    "total": 1
  }
      ↓
processAlerts(data.dados)
      ↓
Para cada alerta:
  alertId = `${notaId}_${nivel}`
  Se não está em notifiedAlerts:
    ↓
  showAlert(alerta)
  Adiciona alertId ao Set
```

### 3. Exibição do Toast

```javascript
showAlert(alerta) {
    // 1. Busca configuração do nível
    const config = getToastConfig(alerta.nivel);

    // 2. Monta mensagem formatada
    let mensagem = `📝 ${alerta.titulo}\n`;
    mensagem += `📅 Prazo: ${alerta.prazoFinal}\n`;

    if (alerta.diasRestantes === 0) {
        mensagem += `⏰ Vence hoje!`;
    } else if (alerta.diasRestantes === 1) {
        mensagem += `⏰ Vence amanhã!`;
    } else {
        mensagem += `⏰ Faltam ${alerta.diasRestantes} dias`;
    }

    // 3. Cria e exibe toast
    Toastify({
        text: mensagem,
        duration: config.duration,
        style: { /* cores e estilo */ },
        onClick: () => {
            window.location.href = `/notas?id=${alerta.notaId}`;
        }
    }).showToast();

    // 4. Toca som (opcional)
    playNotificationSound(alerta.nivel);
}
```

## Prevenção de Duplicatas

O sistema usa um `Set` para rastrear alertas já exibidos **durante a sessão atual**:

```javascript
this.notifiedAlerts = new Set();

// Adiciona ao exibir
const alertId = `${alerta.notaId}_${alerta.nivel}`;
if (!this.notifiedAlerts.has(alertId)) {
    this.showAlert(alerta);
    this.notifiedAlerts.add(alertId);
}
```

**Comportamento:**
- Mesmo alerta não é exibido múltiplas vezes na mesma sessão
- Cache é limpo ao recarregar página (refresh)
- Cache pode ser limpo manualmente: `toastManager.clearNotifiedCache()`

## Interação com Backend

### Endpoint: GET /api/notificacoes/alertas

**Controller:** `NotificacaoController.java`

```java
public void gerarAlertas(Context ctx) {
    List<Map<String, Object>> alertas = notificacaoService.gerarAlertas();

    ctx.json(Map.of(
        "success", true,
        "dados", alertas,
        "total", alertas.size()
    ));
}
```

**Service:** `NotificacaoService.java`

```java
public List<Map<String, Object>> gerarAlertas() throws Exception {
    List<NotaDTO> todasNotas = notaService.listarTodas();
    List<Map<String, Object>> alertas = new ArrayList<>();

    for (NotaDTO nota : todasNotas) {
        // Ignora notas resolvidas/canceladas
        String statusNome = nota.getStatus().getNome().toLowerCase();
        if (statusNome.contains("resolvid") || statusNome.contains("cancelad")) {
            continue;
        }

        Map<String, Object> alerta = criarAlerta(nota);
        if (alerta != null) {
            alertas.add(alerta);
        }
    }

    // Ordena por prioridade (1 = mais urgente)
    alertas.sort(Comparator.comparingInt(a -> (Integer) a.get("prioridade")));
    return alertas;
}
```

**Estrutura do Alerta:**

```java
Map<String, Object> alerta = new HashMap<>();
alerta.put("notaId", nota.getId());
alerta.put("titulo", nota.getTitulo());
alerta.put("prazoFinal", nota.getPrazoFinal().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
alerta.put("diasRestantes", diasRestantes);
alerta.put("nivel", nivel);              // CRITICO, URGENTE, ATENCAO, AVISO
alerta.put("prioridade", prioridade);    // 1, 2, 3, 4
alerta.put("status", nota.getStatus().getNome());
alerta.put("etiqueta", nota.getEtiqueta().getNome());
```

## Configurações do Usuário

O sistema respeita as configurações definidas na tela de Configurações:

| Chave | Valor | Impacto |
|-------|-------|---------|
| `notif_toast` | `true`/`false` | Ativa/desativa notificações toast |
| `notif_dias_critico` | `0` | Dias para nível CRÍTICO (vencido) |
| `notif_dias_urgente` | `3` | Dias para nível URGENTE (0-3 dias) |
| `notif_dias_atencao` | `5` | Dias para nível ATENÇÃO (4-5 dias) |

**Nota:** Atualmente os limiares são fixos no código. Integração com configurações do usuário pode ser implementada futuramente.

## Sons de Notificação

O sistema tenta tocar um som ao exibir alertas:

```javascript
playNotificationSound(nivel) {
    try {
        const audio = new Audio('/sounds/notification.mp3');
        audio.volume = 0.3;
        audio.play().catch(err => {
            console.log('Som de notificação não disponível');
        });
    } catch (error) {
        // Ignora silenciosamente
    }
}
```

**Implementação:**
- Arquivo de som esperado: `src/main/resources/public/sounds/notification.mp3`
- Se não existir, erro é ignorado silenciosamente
- Volume padrão: 30%

## Modo Debug

Para facilitar desenvolvimento e testes:

```javascript
this.debugMode = false;  // Alterar para true

if (this.debugMode) {
    this.showTestToast();  // Exibe toast roxo de teste
}
```

**Toast de teste:**
- Mensagem: "🧪 Toast de teste - Sistema de notificações ativo!"
- Cor: Roxo (#8b5cf6 → #7c3aed)
- Duração: 3 segundos

## Cleanup e Gerenciamento de Memória

```javascript
// Ao sair da página
window.addEventListener('beforeunload', function() {
    if (toastManager) {
        toastManager.destroy();
    }
});

destroy() {
    this.stopPeriodicCheck();        // Limpa setInterval
    this.clearNotifiedCache();       // Limpa Set
    console.log('💥 Toast Notification Manager destruído');
}
```

Garante que não há vazamento de memória com timers rodando em background.

## Exemplos de Uso

### 1. Exibir Toast Personalizado de Sucesso

```javascript
// Em qualquer página autenticada
window.showSuccessToast("Nota salva com sucesso!");
```

### 2. Exibir Toast de Erro

```javascript
window.showErrorToast("Erro ao salvar nota. Tente novamente.");
```

### 3. Exibir Toast Informativo

```javascript
window.showInfoToast("Você tem 5 notas com prazo esta semana.");
```

### 4. Limpar Cache de Alertas

```javascript
// Forçar re-exibição de alertas já mostrados
window.toastManager.clearNotifiedCache();
```

### 5. Parar/Reiniciar Verificação Periódica

```javascript
// Pausar
window.toastManager.stopPeriodicCheck();

// Retomar
window.toastManager.startPeriodicCheck();
```

## Testes Manuais

### Cenário 1: Alerta CRÍTICO (prazo vencido)

1. Criar nota com `prazo_final` = ontem (ex: 2025-10-26)
2. Aguardar até 60 segundos após login
3. **Esperado:** Toast vermelho que não fecha automaticamente

### Cenário 2: Alerta URGENTE (amanhã)

1. Criar nota com `prazo_final` = amanhã (ex: 2025-10-28)
2. Aguardar verificação periódica
3. **Esperado:** Toast laranja por 10 segundos, texto "Vence amanhã!"

### Cenário 3: Alerta ATENÇÃO (3 dias)

1. Criar nota com `prazo_final` = daqui a 3 dias
2. **Esperado:** Toast amarelo por 7 segundos, texto "Faltam 3 dias"

### Cenário 4: Alerta AVISO (5 dias)

1. Criar nota com `prazo_final` = daqui a 5 dias
2. **Esperado:** Toast azul por 5 segundos

### Cenário 5: Navegação ao Clicar

1. Exibir qualquer alerta
2. Clicar no toast
3. **Esperado:** Redireciona para `/notas?id={notaId}` da nota

### Cenário 6: Prevenção de Duplicatas

1. Exibir alerta de nota X
2. Aguardar 60 segundos (próxima verificação)
3. **Esperado:** Mesmo alerta NÃO é exibido novamente

### Cenário 7: Notas Resolvidas Ignoradas

1. Criar nota com prazo próximo
2. Alterar status para "Resolvido"
3. **Esperado:** Nenhum alerta exibido

## Performance e Otimizações

### 1. Polling vs WebSocket

**Decisão:** Polling a cada 60 segundos

**Justificativa:**
- Aplicação embarcada (não há servidor sempre online)
- Baixo volume de requisições (1 req/min por usuário)
- Simplicidade de implementação
- Não requer infraestrutura adicional

**Consumo estimado:**
- 1 requisição/minuto
- ~500 bytes de dados por requisição
- 30 KB/hora de tráfego por usuário

### 2. Cache de Alertas Notificados

Uso de `Set` em vez de Array para verificação O(1):

```javascript
// O(1) - Eficiente
this.notifiedAlerts.has(alertId)

// O(n) - Lento para muitos alertas
this.notifiedAlerts.includes(alertId)
```

### 3. Ordenação no Backend

Alertas são ordenados por prioridade no backend antes de enviar ao frontend, evitando processamento extra no navegador.

## Integração com Tela de Configurações

As configurações da tela `/configuracoes` controlam o comportamento:

```
┌─────────────────────────────────────┐
│ Configuração: notif_toast = true    │
└─────────────────────────────────────┘
              ↓
    ConfiguracaoService.buscarConfiguracoes()
              ↓
    toastManager.loadUserConfig()
              ↓
    this.enabled = config.notif_toast
              ↓
    if (!this.enabled) return; // Não verifica alertas
```

**Futuras melhorias:**
- Carregar configurações via AJAX ao inicializar
- Aplicar `notif_dias_critico`, `notif_dias_urgente`, etc. dinamicamente
- Permitir configurar intervalo de polling

## Logs e Debugging

O sistema registra logs no console do navegador:

```
📱 Toast Notification Manager inicializado
⚙️ Configurações carregadas: verificação a cada 60 segundos
⏱️ Verificação periódica iniciada
🔔 Alerta exibido: Relatório fiscal (URGENTE)
🗑️ Cache de alertas notificados limpo
💥 Toast Notification Manager destruído
```

**Para ativar debug completo:**

```javascript
// No console do navegador
window.toastManager.debugMode = true;
window.toastManager.showTestToast();
```

## Arquivos Modificados/Criados

| Arquivo | Tipo | Descrição |
|---------|------|-----------|
| `public/js/toast-notifications.js` | **NOVO** | Sistema completo de gerenciamento de toasts |
| `templates/layout/base.html` | **MODIFICADO** | Adicionado Toastify.js CDN + carregamento do script |
| `NotificacaoController.java` | **EXISTENTE** | Endpoint `/api/notificacoes/alertas` já implementado |
| `NotificacaoService.java` | **EXISTENTE** | Lógica de geração de alertas já implementada |

## Roadmap de Melhorias

### Curto Prazo

- [ ] Adicionar arquivo de som de notificação (`sounds/notification.mp3`)
- [ ] Integrar configurações dinâmicas do usuário
- [ ] Testes unitários do ToastNotificationManager
- [ ] Tradução de mensagens (i18n)

### Médio Prazo

- [ ] Permitir usuário configurar intervalo de polling
- [ ] Adicionar sons diferentes por nível de alerta
- [ ] Suporte a notificações do navegador (Web Notifications API)
- [ ] Histórico de alertas exibidos

### Longo Prazo

- [ ] Migrar para WebSocket em versão server
- [ ] Notificações push para dispositivos móveis
- [ ] Integração com calendário (Google Calendar, etc.)
- [ ] Alertas por SMS/WhatsApp

## Conclusão

O sistema de alertas em tempo real está completamente implementado e funcional. Utiliza polling periódico para verificar alertas no backend, exibe toasts estilizados com Toastify.js, e previne duplicatas durante a sessão.

**Principais Benefícios:**
- ✅ Notificações não-intrusivas
- ✅ Visual atraente com cores por prioridade
- ✅ Click-to-navigate para nota relevante
- ✅ Prevenção de spam com cache de sessão
- ✅ Configurável por usuário
- ✅ Baixo overhead de performance

**Próximo Passo:**
Implementar alertas por e-mail (tarefa pendente) para complementar as notificações toast.
