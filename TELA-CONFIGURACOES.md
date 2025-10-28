# Tela de Configurações - Notisblokk 1.0

## 📱 Visão Geral

Criada a interface completa de configurações do sistema com múltiplas seções personalizáveis, permitindo que cada usuário ajuste o sistema de acordo com suas preferências.

**Rota:** `GET /configuracoes`
**Template:** `src/main/resources/templates/configuracoes/index.html`
**Controller:** `ConfiguracoesController.java`
**Service:** `ConfiguracaoService.java`

---

## 🎨 Seções de Configuração

### 1. 🎨 Preferências de Tema

Seleção visual do tema da interface com cards interativos.

**Opções:**
- **Claro** (☀️) - Tema padrão com fundo claro
- **Escuro** (🌙) - Tema com fundo escuro

**Implementação:**
```html
<label class="radio-card">
    <input type="radio" name="tema" value="light">
    <div class="radio-card-content">
        <svg>☀️</svg>
        <strong>Claro</strong>
        <small>Tema padrão com fundo claro</small>
    </div>
</label>
```

**Armazenamento:**
- Banco de dados: `configuracoes` (chave: `tema`)
- Sessão: `ctx.sessionAttribute("theme", "light")`
- Atualização imediata ao salvar

---

### 2. 🔔 Notificações e Alertas

Controle completo sobre notificações do sistema.

#### Tipos de Notificação

**Checkboxes:**
- ✅ **Notificações por E-mail** - Receber alertas de prazos por e-mail
- ✅ **Notificações no Sistema** - Mostrar alertas em tempo real (Toastify)

**Padrões:**
- E-mail: Habilitado
- Sistema: Habilitado

#### Antecedência dos Alertas

Configuração de dias de antecedência para cada nível de prioridade:

| Nível | Cor | Padrão | Mín | Máx |
|-------|-----|--------|-----|-----|
| 🔴 Crítico | Vermelho | 0 dias | 0 | 30 |
| 🟠 Urgente | Laranja | 3 dias | 0 | 30 |
| 🟡 Atenção | Amarelo | 5 dias | 0 | 30 |

**Lógica:**
```java
// Exemplo: Nota com prazo em 2 dias
if (diasRestantes <= notif_dias_critico) {
    nivel = "CRITICO";
} else if (diasRestantes <= notif_dias_urgente) {
    nivel = "URGENTE";
} else if (diasRestantes <= notif_dias_atencao) {
    nivel = "ATENCAO";
}
```

---

### 3. 🔐 Segurança da Conta

Configurações de política de senhas.

#### Expiração de Senha

**Dropdown:**
- Nunca expira
- 1 mês
- **3 meses** (padrão)
- 6 meses
- 12 meses

**Implementação:**
```java
// Ao alterar senha
LocalDateTime expiracao = LocalDateTime.now()
    .plusMonths(config.get("senha_expira_meses"));
user.setSenhaExpiraEm(expiracao);
```

#### Aviso de Expiração

**Input numérico:**
- Padrão: 10 dias
- Mínimo: 1 dia
- Máximo: 90 dias

**Lógica:**
```java
long diasRestantes = user.getDiasParaExpirarSenha();

if (diasRestantes <= aviso_antecedencia) {
    // Mostrar aviso ao usuário
    // Enviar e-mail de aviso
}
```

---

### 4. 💾 Backup Automático

Configuração de backups periódicos do banco de dados.

#### Habilitar Backup Automático

**Checkbox:**
- ✅ Criar backups automáticos periodicamente
- Padrão: Desabilitado

#### Periodicidade do Backup

**Dropdown:**
- Diário (1 dia)
- **Semanal** (7 dias - padrão)
- Quinzenal (15 dias)
- Mensal (30 dias)

**Implementação com Quartz Scheduler:**
```java
// CronTrigger baseado na periodicidade
// Semanal: "0 0 2 */7 * ?" (às 2h da manhã a cada 7 dias)
```

**Box Informativo:**
```
ℹ️ Os backups são armazenados na pasta configurada no sistema.
Você pode fazer backup manual a qualquer momento através do menu Backup.
```

---

### 5. ⚡ Preferências Gerais

Configurações gerais da interface.

#### Itens por Página

**Dropdown:**
- **10 itens** (padrão)
- 25 itens
- 50 itens
- 100 itens

**Uso:**
```javascript
// Na listagem de notas
GET /api/notas/paginado?tamanho=10
```

#### Idioma da Interface

**Dropdown:**
- **Português (Brasil)** (padrão)
- English (US)
- Español

**Nota:** Atualmente apenas pt-BR está implementado. Suporte para outros idiomas é futuro.

---

## 🔄 Fluxo de Funcionamento

### Carregamento da Página

```
1. Usuário acessa /configuracoes
   ↓
2. ConfiguracoesController::index()
   ↓
3. ConfiguracaoService.buscarConfiguracoes(userId)
   ↓
4. SELECT chave, valor FROM configuracoes WHERE usuario_id = ?
   ↓
5. Se não houver config, usa valores padrão (DEFAULTS)
   ↓
6. Monta model com configurações
   ↓
7. Renderiza template com valores atuais
```

### Salvamento de Configurações

```
1. Usuário altera configurações e clica em "Salvar"
   ↓
2. POST /configuracoes/salvar
   ↓
3. ConfiguracoesController::salvar()
   ↓
4. Extrai todos os form params
   ↓
5. Monta Map<String, String> configuracoes
   ↓
6. ConfiguracaoService.salvarConfiguracoes(userId, config)
   ↓
7. UPSERT em batch na tabela configuracoes
   ↓
8. Se tema mudou, atualiza sessão
   ↓
9. Flash message: "Configurações salvas com sucesso!"
   ↓
10. Redirect /configuracoes
```

### Restaurar Padrão

```
1. Usuário clica em "Restaurar Padrão"
   ↓
2. Confirmação JavaScript: "Tem certeza?"
   ↓
3. POST /configuracoes/resetar
   ↓
4. ConfiguracoesController::resetar()
   ↓
5. DELETE FROM configuracoes WHERE usuario_id = ?
   ↓
6. Reseta tema na sessão para "light"
   ↓
7. Flash message: "Configurações restauradas!"
   ↓
8. Redirect /configuracoes
```

---

## 💾 Armazenamento no Banco de Dados

### Tabela: configuracoes

```sql
CREATE TABLE IF NOT EXISTS configuracoes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER,
    chave TEXT NOT NULL,
    valor TEXT,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(usuario_id, chave)  -- Constraint única por usuário/chave
);
```

### Exemplo de Registros

| id | usuario_id | chave | valor | data_criacao | data_atualizacao |
|----|------------|-------|-------|--------------|------------------|
| 1  | 1          | tema  | dark  | 2025-10-26   | 2025-10-26       |
| 2  | 1          | notif_email | true | 2025-10-26 | 2025-10-26     |
| 3  | 1          | notif_dias_urgente | 5 | 2025-10-26 | 2025-10-26 |
| 4  | 1          | paginacao_padrao | 25 | 2025-10-26 | 2025-10-26   |

### UPSERT Pattern

**SQL usado:**
```sql
INSERT INTO configuracoes (usuario_id, chave, valor, data_criacao, data_atualizacao)
VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT(usuario_id, chave) DO UPDATE SET
    valor = excluded.valor,
    data_atualizacao = CURRENT_TIMESTAMP
```

**Vantagens:**
- ✅ Cria se não existir
- ✅ Atualiza se já existir
- ✅ Uma query por configuração
- ✅ Executado em batch para performance

---

## 🎨 Componentes Visuais

### Radio Card (Seleção de Tema)

**HTML:**
```html
<label class="radio-card">
    <input type="radio" name="tema" value="light">
    <div class="radio-card-content">
        <!-- Ícone, título e descrição -->
    </div>
</label>
```

**CSS:**
```css
.radio-card-content {
    border: 2px solid var(--color-border);
    transition: all 0.3s ease;
}

.radio-card input:checked + .radio-card-content {
    border-color: var(--color-primary);
    background-color: rgba(74, 144, 226, 0.05);
}
```

**Estados:**
- **Normal:** Borda cinza
- **Selecionado:** Borda azul + fundo azul claro
- **Hover:** Transição suave

### Checkbox Item

**HTML:**
```html
<div class="checkbox-item">
    <label class="form-checkbox">
        <input type="checkbox" name="notif_email">
        <span>
            <strong>Notificações por E-mail</strong>
            <small>Receber alertas de prazos por e-mail</small>
        </span>
    </label>
</div>
```

**CSS:**
```css
.checkbox-item {
    padding: 1rem;
    background-color: var(--color-bg-secondary);
    border-radius: 8px;
    margin-bottom: 0.5rem;
}
```

### Grid Layout

**Responsivo:**
```css
.grid-cols-2 {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
}

@media (max-width: 768px) {
    .grid-cols-2 {
        grid-template-columns: 1fr;
    }
}
```

### Box Informativo

**Exemplo (Backup):**
```html
<div style="border-left: 4px solid var(--color-info);">
    <svg>ℹ️</svg>
    <p>Os backups são armazenados na pasta...</p>
</div>
```

---

## 📡 Integração Backend

### ConfiguracaoService

**Valores Padrão:**
```java
private static final Map<String, String> DEFAULTS = Map.ofEntries(
    Map.entry("tema", "light"),
    Map.entry("notif_email", "true"),
    Map.entry("notif_toast", "true"),
    Map.entry("notif_dias_critico", "0"),
    Map.entry("notif_dias_urgente", "3"),
    Map.entry("notif_dias_atencao", "5"),
    Map.entry("senha_expira_meses", "3"),
    Map.entry("senha_aviso_antecedencia", "10"),
    Map.entry("backup_auto", "false"),
    Map.entry("backup_periodicidade", "7"),
    Map.entry("paginacao_padrao", "10"),
    Map.entry("idioma_interface", "pt-BR")
);
```

**Métodos Principais:**
- `buscarConfiguracoes(userId)` - Retorna Map com todas as configs
- `buscarConfiguracao(userId, chave)` - Retorna config específica
- `salvarConfiguracoes(userId, Map)` - Salva múltiplas configs
- `salvarConfiguracao(userId, chave, valor)` - Salva config única
- `resetarConfiguracoes(userId)` - Deleta todas e volta ao padrão
- `obterPadroes()` - Retorna Map com valores padrão

### ConfiguracoesController

**Endpoints:**

| Método | Rota | Função |
|--------|------|--------|
| GET | /configuracoes | Exibir página de configurações |
| POST | /configuracoes/salvar | Salvar todas as configurações |
| POST | /configuracoes/resetar | Restaurar configurações padrão |

**Fluxo de Salvamento:**
```java
public void salvar(Context ctx) {
    // 1. Obter usuário atual
    User currentUser = SessionUtil.getCurrentUser(ctx);

    // 2. Extrair form params
    Map<String, String> configuracoes = new HashMap<>();
    String tema = ctx.formParam("tema");
    String notifEmail = ctx.formParam("notif_email");
    // ...

    // 3. Converter checkboxes (null = false)
    configuracoes.put("notif_email", notifEmail != null ? "true" : "false");

    // 4. Salvar no banco
    configuracaoService.salvarConfiguracoes(currentUser.getId(), configuracoes);

    // 5. Atualizar sessão (tema)
    if (tema != null) {
        ctx.sessionAttribute("theme", tema);
    }

    // 6. Flash message + redirect
    ctx.sessionAttribute("configSuccess", "Configurações salvas com sucesso!");
    ctx.redirect("/configuracoes");
}
```

---

## 🔄 Uso das Configurações

### Exemplo 1: Aplicar Tema

**No layout base:**
```html
<html th:data-theme="${theme}">
```

**No login:**
```java
String tema = configuracaoService.buscarConfiguracao(userId, "tema");
ctx.sessionAttribute("theme", tema);
```

### Exemplo 2: Paginação

**No controller de notas:**
```java
int tamanhoPadrao = Integer.parseInt(
    configuracaoService.buscarConfiguracao(userId, "paginacao_padrao")
);

int tamanho = ctx.queryParamAsClass("tamanho", Integer.class)
    .getOrDefault(tamanhoPadrao);
```

### Exemplo 3: Alertas de Prazo

**No serviço de notificações:**
```java
Map<String, String> config = configuracaoService.buscarConfiguracoes(userId);

int diasCritico = Integer.parseInt(config.get("notif_dias_critico"));
int diasUrgente = Integer.parseInt(config.get("notif_dias_urgente"));
int diasAtencao = Integer.parseInt(config.get("notif_dias_atencao"));

// Calcular dias restantes
long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), nota.getPrazoFinal());

String nivel;
if (diasRestantes <= diasCritico) {
    nivel = "CRITICO";
} else if (diasRestantes <= diasUrgente) {
    nivel = "URGENTE";
} else if (diasRestantes <= diasAtencao) {
    nivel = "ATENCAO";
} else {
    nivel = "NORMAL";
}

// Enviar notificação se habilitado
boolean enviarEmail = Boolean.parseBoolean(config.get("notif_email"));
if (enviarEmail && nivel.equals("CRITICO")) {
    emailService.enviarAlerta(user, nota);
}
```

### Exemplo 4: Backup Automático

**No scheduler:**
```java
boolean backupAuto = Boolean.parseBoolean(
    configuracaoService.buscarConfiguracao(userId, "backup_auto")
);

if (backupAuto) {
    int periodicidade = Integer.parseInt(
        configuracaoService.buscarConfiguracao(userId, "backup_periodicidade")
    );

    // Criar CronTrigger
    String cron = String.format("0 0 2 */%d * ?", periodicidade);
    // Agendar backup
}
```

---

## 🧪 Testes Sugeridos

### Testes Funcionais

1. **Carregar Configurações:**
   - [ ] Acessa /configuracoes pela primeira vez (deve usar padrões)
   - [ ] Valores padrão são exibidos corretamente
   - [ ] Tema atual da sessão está selecionado

2. **Salvar Configurações:**
   - [ ] Alterar tema de claro para escuro
   - [ ] Desabilitar notificações por e-mail
   - [ ] Mudar paginação para 25 itens
   - [ ] Clicar em "Salvar"
   - [ ] Verificar mensagem de sucesso
   - [ ] Recarregar página e verificar valores salvos

3. **Restaurar Padrão:**
   - [ ] Alterar várias configurações
   - [ ] Clicar em "Restaurar Padrão"
   - [ ] Confirmar diálogo
   - [ ] Verificar que todas voltaram ao padrão

4. **Validações:**
   - [ ] Dias de alerta não podem ser negativos
   - [ ] Dias de alerta máximo 30
   - [ ] Todas as opções de dropdown funcionam

### Testes de Persistência

1. **Banco de Dados:**
   - [ ] Configurações são salvas na tabela
   - [ ] UPSERT funciona (cria se não existe, atualiza se existe)
   - [ ] DELETE funciona ao restaurar padrão
   - [ ] Constraint UNIQUE funciona (usuario_id, chave)

2. **Sessão:**
   - [ ] Tema é atualizado na sessão ao salvar
   - [ ] Tema persiste durante navegação
   - [ ] Logout limpa sessão mas mantém banco

### Testes de UI

1. **Radio Cards:**
   - [ ] Visual de selecionado aparece corretamente
   - [ ] Apenas um pode ser selecionado por vez
   - [ ] Hover funciona

2. **Checkboxes:**
   - [ ] Check/uncheck funciona
   - [ ] Estado persiste após salvar
   - [ ] Desabilitado quando carregando

3. **Inputs Numéricos:**
   - [ ] Min/max são respeitados
   - [ ] Apenas números são aceitos
   - [ ] Valor padrão aparece

4. **Responsividade:**
   - [ ] Grid adapta em mobile (1 coluna)
   - [ ] Cards são legíveis em telas pequenas
   - [ ] Botões são clicáveis em toque

---

## 🚀 Melhorias Futuras

### Funcionalidades

- [ ] **Exportar/Importar Configurações**
  - Exportar para JSON
  - Importar de arquivo
  - Compartilhar entre usuários

- [ ] **Configurações Avançadas**
  - Formato de data preferido
  - Fuso horário
  - Primeira página ao login
  - Atalhos de teclado personalizados

- [ ] **Temas Customizados**
  - Paleta de cores personalizada
  - Modo alto contraste
  - Tamanho de fonte ajustável

- [ ] **Notificações Granulares**
  - Por tipo de etiqueta
  - Por horário (não enviar à noite)
  - Frequência de resumo (diário, semanal)

- [ ] **Backup na Nuvem**
  - Integração com Google Drive
  - Integração com Dropbox
  - Backup criptografado

### UI/UX

- [ ] Preview ao vivo do tema
- [ ] Busca dentro de configurações
- [ ] Tabs para organizar seções
- [ ] Indicador de "configurações não salvas"
- [ ] Undo/Redo de alterações
- [ ] Tour guiado para novos usuários

### Segurança

- [ ] Auditoria de alterações
- [ ] Requer senha para alterações críticas
- [ ] Bloqueio de configurações por admin
- [ ] Políticas corporativas

---

## 📚 Arquivos Criados/Modificados

### Novos Arquivos

1. **ConfiguracoesController.java** - Controller de configurações
   - `index()` - GET /configuracoes
   - `salvar()` - POST /configuracoes/salvar
   - `resetar()` - POST /configuracoes/resetar

2. **ConfiguracaoService.java** - Service de gerenciamento
   - CRUD de configurações
   - Valores padrão (DEFAULTS)
   - UPSERT em batch

3. **templates/configuracoes/index.html** - Template completo
   - 5 seções de configurações
   - Radio cards, checkboxes, selects
   - Responsivo

### Arquivos Modificados

1. **Main.java**
   - Instanciado `ConfiguracoesController`
   - Adicionadas 3 rotas de configurações

2. **layout/sidebar.html**
   - Adicionado link "Configurações" com ícone de engrenagem

---

## 📊 Estatísticas

**Seções:** 5
**Configurações disponíveis:** 12
**Valores padrão:** 12
**Rotas:** 3
**Linhas de código:**
- Controller: ~150
- Service: ~200
- Template: ~450
- **Total:** ~800 linhas

---

## 📖 Exemplo de Uso Completo

```
1. Usuário loga no sistema
2. Clica em "Configurações" no sidebar
3. Página /configuracoes carrega com valores atuais
4. Altera:
   - Tema: Claro → Escuro
   - Notificações por E-mail: ✓ → ✗
   - Dias Urgente: 3 → 5
   - Paginação: 10 → 25
5. Clica em "Salvar Configurações"
6. Mensagem verde: "Configurações salvas com sucesso!"
7. Tema muda imediatamente para escuro
8. Navegaem para /notas
9. Listagem mostra 25 itens por página
10. Não recebe mais e-mails de alerta
11. Alertas urgentes aparecem 5 dias antes
```

---

**Implementado em:** 26/10/2025
**Status:** Totalmente funcional e pronto para uso
**Banco de Dados:** Tabela `configuracoes` criada no schema
**Compatibilidade:** Desktop e Mobile
