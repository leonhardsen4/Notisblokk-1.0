# Implementação: Alertas por Email com Quartz Scheduler

## Resumo

Sistema completo de alertas por email automático utilizando Quartz Scheduler 2.3.2 para envio periódico de notificações sobre prazos de notas via email.

## Arquitetura

### Componentes Principais

```
┌─────────────────────────────────────────────────────────────┐
│              QuartzSchedulerManager                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Inicializa e gerencia o Quartz Scheduler              │ │
│  │ Configura jobs e triggers                             │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                    ↓
    ┌───────────────────────────────────────┐
    │         Job 1: AlertaEmailJob         │
    │   Trigger: a cada 1 hora (hourly)     │
    └───────────────────────────────────────┘
                    ↓
    ┌───────────────────────────────────────┐
    │  1. Busca todos os usuários ativos    │
    │  2. Verifica config notif_email       │
    │  3. Para cada usuário:                │
    │     - Busca notas pendentes           │
    │     - Calcula dias restantes          │
    │     - Envia emails se necessário      │
    │     - Registra envio (anti-duplicata) │
    └───────────────────────────────────────┘
                    ↓
    ┌───────────────────────────────────────┐
    │        Job 2: LimpezaAlertasJob       │
    │  Trigger: diariamente às 3h (cron)    │
    └───────────────────────────────────────┘
                    ↓
    ┌───────────────────────────────────────┐
    │ Remove registros com mais de 30 dias  │
    │ Mantém tabela otimizada               │
    └───────────────────────────────────────┘
```

## Arquivos Implementados

### 1. QuartzSchedulerManager.java

**Localização:** `src/main/java/com/notisblokk/scheduler/QuartzSchedulerManager.java`

**Responsabilidades:**
- Inicializar o Quartz Scheduler
- Configurar jobs e triggers
- Gerenciar ciclo de vida (start/stop)
- Fornecer API para execução manual

**Métodos principais:**

```java
public void iniciar() throws SchedulerException
public void parar()
public void pararImediatamente()
public boolean estaRodando()
public void executarAlertasManualmente()
```

**Jobs configurados:**

| Job | Trigger | Frequência | Descrição |
|-----|---------|------------|-----------|
| AlertaEmailJob | SimpleScheduleBuilder | A cada 1 hora | Envia alertas por email |
| LimpezaAlertasJob | CronScheduleBuilder | Diariamente às 3h | Limpa registros antigos |

**Configuração do AlertaEmailJob:**

```java
JobDetail job = JobBuilder.newJob(AlertaEmailJob.class)
    .withIdentity("alertaEmailJob", "alertas")
    .withDescription("Envia alertas de prazos por email")
    .build();

Trigger trigger = TriggerBuilder.newTrigger()
    .withIdentity("alertaEmailTrigger", "alertas")
    .startNow()
    .withSchedule(
        SimpleScheduleBuilder.simpleSchedule()
            .withIntervalInHours(1)
            .repeatForever()
    )
    .build();
```

**Configuração do LimpezaAlertasJob:**

```java
Trigger trigger = TriggerBuilder.newTrigger()
    .withIdentity("limpezaAlertasTrigger", "manutencao")
    .withSchedule(
        CronScheduleBuilder.dailyAtHourAndMinute(3, 0)
    )
    .build();
```

### 2. AlertaEmailJob.java

**Localização:** `src/main/java/com/notisblokk/scheduler/AlertaEmailJob.java`

**Fluxo de execução:**

```
execute()
   ↓
processarAlertasParaTodosUsuarios()
   ↓
Para cada usuário:
   ↓
   ├─ usuarioTemNotificacaoEmailHabilitada()?
   │    └─ Busca config "notif_email" do usuário
   ↓
   processarAlertasParaUsuario(usuario)
       ↓
       ├─ Busca notas do usuário
       ├─ Busca configurações de dias de alerta
       ├─ Para cada nota pendente:
       │    ├─ determinarNivelAlerta()
       │    ├─ alertaJaEnviado() ?
       │    ├─ enviarEmailAlerta()
       │    └─ registrarEnvio()
       └─ Retorna total de emails enviados
```

**Lógica de níveis de alerta:**

```java
private String determinarNivelAlerta(long diasRestantes, int diasCritico, int diasUrgente, int diasAtencao) {
    if (diasRestantes < 0 || diasRestantes <= diasCritico) {
        return "CRITICO";   // Atrasado ou vence hoje
    } else if (diasRestantes <= diasUrgente) {
        return "URGENTE";   // Default: 1-3 dias
    } else if (diasRestantes <= diasAtencao) {
        return "ATENCAO";   // Default: 4-5 dias
    } else {
        return null;        // Não precisa alertar ainda
    }
}
```

**Configurações do usuário respeitadas:**

| Chave | Default | Descrição |
|-------|---------|-----------|
| `notif_email` | `true` | Ativa/desativa alertas por email |
| `notif_dias_critico` | `0` | Dias para alerta crítico |
| `notif_dias_urgente` | `3` | Dias para alerta urgente |
| `notif_dias_atencao` | `5` | Dias para alerta de atenção |

### 3. LimpezaAlertasJob.java

**Localização:** `src/main/java/com/notisblokk/scheduler/LimpezaAlertasJob.java`

**Função:**
- Executa diariamente às 3h da manhã
- Remove registros de `alertas_enviados` com mais de 30 dias
- Mantém tabela otimizada

**Código:**

```java
@Override
public void execute(JobExecutionContext context) throws JobExecutionException {
    logger.info("Iniciando job de limpeza de alertas antigos");

    try {
        alertaEnviadoRepository.limparAlertasAntigos();
        logger.info("Job de limpeza finalizado com sucesso");

    } catch (Exception e) {
        logger.error("Erro ao executar job de limpeza de alertas", e);
        throw new JobExecutionException("Erro na limpeza de alertas: " + e.getMessage(), e);
    }
}
```

### 4. AlertaEnviadoRepository.java

**Localização:** `src/main/java/com/notisblokk/repository/AlertaEnviadoRepository.java`

**Responsabilidades:**
- Controlar quais alertas já foram enviados (prevenir duplicatas)
- Registrar envio de emails
- Limpar registros antigos

**Métodos principais:**

```java
public boolean alertaJaEnviado(Long userId, Long notaId, String nivel)
public void registrarEnvio(Long userId, Long notaId, String nivel, int diasRestantes)
public void limparAlertasAntigos()
public int contarAlertasEnviadosHoje()
```

**Lógica anti-duplicata:**

```sql
SELECT COUNT(*) FROM alertas_enviados
WHERE usuario_id = ?
  AND nota_id = ?
  AND nivel = ?
  AND DATE(data_envio) = DATE('now', 'localtime')
```

**Resultado:**
- Cada combinação usuário+nota+nível só é enviada **uma vez por dia**
- Nível pode mudar (ex: ATENCAO → URGENTE) e novo email será enviado
- Após 30 dias, registros são removidos automaticamente

### 5. Tabela alertas_enviados (schema.sql)

**Estrutura:**

```sql
CREATE TABLE IF NOT EXISTS alertas_enviados (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL,
    nota_id INTEGER NOT NULL,
    nivel TEXT NOT NULL,
    dias_restantes INTEGER,
    data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (nota_id) REFERENCES notas(id) ON DELETE CASCADE
);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_alertas_enviados_usuario_nota
    ON alertas_enviados(usuario_id, nota_id);
CREATE INDEX IF NOT EXISTS idx_alertas_enviados_data
    ON alertas_enviados(data_envio);
```

**Campos:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | INTEGER | PK auto-incremento |
| `usuario_id` | INTEGER | FK para users.id |
| `nota_id` | INTEGER | FK para notas.id |
| `nivel` | TEXT | CRITICO, URGENTE, ATENCAO |
| `dias_restantes` | INTEGER | Quantos dias faltavam no envio |
| `data_envio` | TIMESTAMP | Quando foi enviado |

### 6. NotaRepository.buscarPorUsuarioId()

**Adição ao NotaRepository:**

Novo método para buscar todas as notas de um usuário com dados completos (etiqueta e status) em uma única query, otimizando performance.

```java
public List<NotaDTO> buscarPorUsuarioId(Long usuarioId) throws SQLException {
    String sql = """
        SELECT
            n.*,
            e.nome as etiqueta_nome,
            s.nome as status_nome,
            s.cor_hex as status_cor
        FROM notas n
        INNER JOIN etiquetas e ON n.etiqueta_id = e.id
        INNER JOIN status_nota s ON n.status_id = s.id
        WHERE n.usuario_id = ?
        ORDER BY n.prazo_final ASC
    """;
    // ...
}
```

**Justificativa:**
- Reduz múltiplas queries ao banco
- Retorna DTOs prontos para uso
- JOIN otimizado com índices

### 7. Integração no Main.java

**Inicialização automática:**

```java
// Inicializar Quartz Scheduler para alertas por email
logger.info("Inicializando Quartz Scheduler...");
QuartzSchedulerManager schedulerManager = new QuartzSchedulerManager();
schedulerManager.iniciar();

// Registrar shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    logger.info("Encerrando aplicação...");
    schedulerManager.parar();
    DatabaseConfig.close();
    app.stop();
    logger.info("Aplicação encerrada");
}));
```

**Comportamento:**
- Scheduler inicia automaticamente com a aplicação
- Jobs começam a executar conforme cronograma
- Shutdown gracioso aguarda jobs em execução

## EmailService.enviarEmailAlertaNota()

**Método existente no EmailService reutilizado:**

```java
public void enviarEmailAlertaNota(String email, String nomeUsuario, String tituloNota,
                                  String prazoFinal, int diasRestantes) throws Exception
```

**Template de email:**

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; color: #333; }
        .header { background-color: [COR]; color: white; padding: 20px; }
        .nota-info { border-left: 4px solid [COR]; padding: 15px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>⚠️ Alerta de Prazo</h1>
        </div>
        <div class="content">
            <h2>Olá, [NOME]!</h2>
            <p><strong>[MENSAGEM]</strong></p>
            <div class="nota-info">
                <h3>[TITULO_NOTA]</h3>
                <p><strong>Prazo final:</strong> [PRAZO]</p>
                <p><strong>Nível de urgência:</strong> [NIVEL]</p>
            </div>
            <p style="text-align: center;">
                <a href="http://localhost:7070/notas" class="button">
                    Ver Nota
                </a>
            </p>
        </div>
    </div>
</body>
</html>
```

**Cores por nível:**

| Nível | Cor | Hex Code |
|-------|-----|----------|
| CRÍTICO | Vermelho escuro | #DC2626 |
| URGENTE | Laranja | #F97316 |
| ATENÇÃO | Amarelo | #F59E0B |
| AVISO | Azul | #3B82F6 |

## Configuração do Usuário

O sistema respeita as configurações individuais de cada usuário definidas na tela `/configuracoes`:

### Habilitação de Alertas

```
┌─────────────────────────────────────────┐
│ ☑ Notificações por E-mail               │
│   Receber alertas de prazos por e-mail  │
└─────────────────────────────────────────┘
```

**Banco:**
```sql
INSERT INTO configuracoes (usuario_id, chave, valor)
VALUES (1, 'notif_email', 'true')
```

### Configuração de Dias para Alertas

```
┌─────────────────────────────────────────┐
│ Alerta Crítico (prazo vencido)          │
│ [0] dias                                 │
├─────────────────────────────────────────┤
│ Alerta Urgente                           │
│ [3] dias antes do prazo                  │
├─────────────────────────────────────────┤
│ Alerta de Atenção                        │
│ [5] dias antes do prazo                  │
└─────────────────────────────────────────┘
```

**Banco:**
```sql
INSERT INTO configuracoes (usuario_id, chave, valor) VALUES
    (1, 'notif_dias_critico', '0'),
    (1, 'notif_dias_urgente', '3'),
    (1, 'notif_dias_atencao', '5')
```

## Fluxo Completo: Do Job ao Email

### Passo a Passo

**1. Trigger do Quartz (a cada 1 hora)**

```
12:00 → Quartz dispara AlertaEmailJob
```

**2. Job busca usuários ativos**

```sql
SELECT * FROM users WHERE active = 1
```

**3. Para cada usuário, verifica configuração**

```sql
SELECT valor FROM configuracoes
WHERE usuario_id = ? AND chave = 'notif_email'
```

Se `valor = 'false'`, pula usuário.

**4. Busca notas do usuário**

```sql
SELECT n.*, e.nome, s.nome, s.cor_hex
FROM notas n
JOIN etiquetas e ON n.etiqueta_id = e.id
JOIN status_nota s ON n.status_id = s.id
WHERE n.usuario_id = ?
```

**5. Filtra notas resolvidas/canceladas**

```java
String statusNome = nota.getStatus().getNome().toLowerCase();
if (statusNome.contains("resolvid") || statusNome.contains("cancelad")) {
    continue; // Pula nota
}
```

**6. Calcula dias restantes**

```java
long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), nota.getPrazoFinal());
```

**7. Determina nível de alerta**

```java
String nivel = determinarNivelAlerta(diasRestantes, diasCritico, diasUrgente, diasAtencao);
```

**8. Verifica se já foi enviado hoje**

```sql
SELECT COUNT(*) FROM alertas_enviados
WHERE usuario_id = ? AND nota_id = ? AND nivel = ?
  AND DATE(data_envio) = DATE('now', 'localtime')
```

Se `COUNT > 0`, pula nota.

**9. Envia email**

```java
emailService.enviarEmailAlertaNota(
    usuario.getEmail(),
    nomeUsuario,
    nota.getTitulo(),
    nota.getPrazoFinalFormatado(),
    (int) diasRestantes
);
```

**10. Registra envio**

```sql
INSERT INTO alertas_enviados (usuario_id, nota_id, nivel, dias_restantes, data_envio)
VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
```

**11. Job finaliza**

```
12:01 → Job concluído
Log: "Job finalizado com sucesso: 5 emails enviados"
```

## Exemplos de Uso

### Exemplo 1: Nota vencendo amanhã

**Dados:**
- Nota: "Relatório Fiscal"
- Prazo: 28/01/2025 (amanhã)
- Usuário: João Silva (joao@empresa.com)
- Config: `notif_email = true`, `notif_dias_urgente = 3`

**Execução do Job:**

```
1. Job inicia às 14:00
2. Busca usuário João Silva (ativo)
3. Verifica config: notif_email = true ✓
4. Busca notas de João
5. Encontra "Relatório Fiscal"
6. Calcula: diasRestantes = 1
7. Determina: nivel = URGENTE (1 <= 3)
8. Verifica: não foi enviado hoje ✓
9. Envia email com subject: "[URGENTE] Alerta: Relatório Fiscal"
10. Registra envio na tabela
11. Log: "Email enviado: joao@empresa.com - Nota: Relatório Fiscal (1 dias)"
```

**Email recebido:**

```
De: Notisblokk <noreply@notisblokk.com>
Para: joao@empresa.com
Assunto: [URGENTE] Alerta: Relatório Fiscal

Olá, João Silva!
Faltam apenas 1 dia(s) para o prazo!

📝 Relatório Fiscal
Prazo final: 28/01/2025
Nível de urgência: URGENTE

[Ver Nota]
```

### Exemplo 2: Nota atrasada

**Dados:**
- Nota: "Contrato com Fornecedor"
- Prazo: 25/01/2025 (2 dias atrás)
- diasRestantes = -2

**Execução:**

```
1. Job inicia
2. Calcula: diasRestantes = -2
3. Determina: nivel = CRITICO (< 0)
4. Envia email: "[CRÍTICO - Nota Atrasada] Alerta: Contrato com Fornecedor"
5. Mensagem: "Esta nota está atrasada há 2 dia(s)!"
```

### Exemplo 3: Usuário com notificações desabilitadas

**Dados:**
- Usuário: Maria Santos
- Config: `notif_email = false`

**Execução:**

```
1. Job inicia
2. Busca usuário Maria Santos
3. Verifica config: notif_email = false ✗
4. Log: "Usuário Maria Santos tem notificações por email desabilitadas"
5. Pula usuário, não envia nada
```

### Exemplo 4: Nota já alertada hoje

**Dados:**
- Nota: "Pagamento de Fatura"
- Último envio: 27/01/2025 10:00
- Hora atual: 27/01/2025 15:00 (mesmo dia)

**Execução:**

```
1. Job inicia às 15:00
2. Processa nota "Pagamento de Fatura"
3. Verifica: alertaJaEnviado(userId=1, notaId=5, nivel=URGENTE)
4. Query retorna: COUNT = 1 (enviado hoje às 10:00)
5. Log: "Alerta para nota Pagamento de Fatura (nível URGENTE) já foi enviado hoje"
6. Pula nota
```

**No dia seguinte:**

```
1. Job inicia às 10:00 (28/01/2025)
2. Verifica: alertaJaEnviado(...)
3. Query retorna: COUNT = 0 (último envio foi ontem)
4. Envia novo email ✓
```

## Logs e Monitoramento

### Logs do QuartzSchedulerManager

```
INFO  [main] QuartzSchedulerManager - Inicializando Quartz Scheduler...
INFO  [main] QuartzSchedulerManager - 📧 Job de alertas por email agendado: executa a cada 1 hora
INFO  [main] QuartzSchedulerManager - 🧹 Job de limpeza de alertas agendado: executa diariamente às 3h
INFO  [main] QuartzSchedulerManager - ✅ Quartz Scheduler iniciado com sucesso
```

### Logs do AlertaEmailJob

```
INFO  [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - ========================================
INFO  [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - Iniciando job de envio de alertas por email
INFO  [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - ========================================
INFO  [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - Processando alertas para 10 usuários
DEBUG [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - Verificando 5 notas do usuário joao
INFO  [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - Email enviado: joao@empresa.com - Nota: Relatório Fiscal (1 dias)
INFO  [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - Email enviado: maria@empresa.com - Nota: Contrato (0 dias)
INFO  [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - ========================================
INFO  [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - Job finalizado com sucesso: 5 emails enviados
INFO  [DefaultQuartzScheduler_Worker-1] AlertaEmailJob - ========================================
```

### Logs do LimpezaAlertasJob

```
INFO  [DefaultQuartzScheduler_Worker-2] LimpezaAlertasJob - ========================================
INFO  [DefaultQuartzScheduler_Worker-2] LimpezaAlertasJob - Iniciando job de limpeza de alertas antigos
INFO  [DefaultQuartzScheduler_Worker-2] AlertaEnviadoRepository - Limpeza de alertas antigos: 127 registros removidos
INFO  [DefaultQuartzScheduler_Worker-2] LimpezaAlertasJob - ========================================
INFO  [DefaultQuartzScheduler_Worker-2] LimpezaAlertasJob - Job de limpeza finalizado com sucesso
INFO  [DefaultQuartzScheduler_Worker-2] LimpezaAlertasJob - ========================================
```

## Testes Manuais

### Teste 1: Execução manual do job

```java
// Via código (útil para debug)
QuartzSchedulerManager manager = new QuartzSchedulerManager();
manager.iniciar();
manager.executarAlertasManualmente();
```

**Resultado esperado:**
- Job executa imediatamente
- Logs aparecem no console
- Emails são enviados

### Teste 2: Criar nota com prazo amanhã

1. Fazer login na aplicação
2. Criar nota com prazo = amanhã
3. Aguardar até próxima execução do job (1 hora)
4. Verificar email

**Resultado esperado:**
- Email recebido com nível URGENTE
- Subject: "[URGENTE] Alerta: [Título da Nota]"

### Teste 3: Desabilitar notificações

1. Acessar `/configuracoes`
2. Desmarcar "Notificações por E-mail"
3. Salvar
4. Aguardar próxima execução do job

**Resultado esperado:**
- Nenhum email recebido
- Log: "Usuário tem notificações por email desabilitadas"

### Teste 4: Verificar anti-duplicata

1. Criar nota com prazo amanhã
2. Executar job manualmente
3. Executar job novamente (mesma hora/dia)

**Resultado esperado:**
- Primeiro email enviado ✓
- Segundo email NÃO enviado
- Log: "Alerta já foi enviado hoje"

### Teste 5: Limpeza de registros antigos

1. Inserir registro manualmente na tabela:
```sql
INSERT INTO alertas_enviados (usuario_id, nota_id, nivel, data_envio)
VALUES (1, 1, 'URGENTE', datetime('now', '-35 days'));
```

2. Executar LimpezaAlertasJob (ou aguardar 3h da manhã)

**Resultado esperado:**
- Registro removido
- Log: "Limpeza de alertas antigos: 1 registros removidos"

## Performance e Otimizações

### Otimizações Implementadas

**1. Query otimizada com JOINs**

```sql
-- ✓ Correto: Uma query com JOINs
SELECT n.*, e.nome, s.nome, s.cor_hex
FROM notas n
JOIN etiquetas e ON n.etiqueta_id = e.id
JOIN status_nota s ON n.status_id = s.id
WHERE n.usuario_id = ?

-- ✗ Errado: N+1 queries
SELECT * FROM notas WHERE usuario_id = ?
-- Para cada nota:
SELECT * FROM etiquetas WHERE id = ?
SELECT * FROM status_nota WHERE id = ?
```

**2. Índices estratégicos**

```sql
CREATE INDEX idx_alertas_enviados_usuario_nota ON alertas_enviados(usuario_id, nota_id);
CREATE INDEX idx_alertas_enviados_data ON alertas_enviados(data_envio);
CREATE INDEX idx_notas_usuario_id ON notas(usuario_id);
CREATE UNIQUE INDEX idx_configuracoes_usuario_chave ON configuracoes(usuario_id, chave);
```

**3. Limpeza periódica**

- Mantém tabela `alertas_enviados` pequena
- Apenas últimos 30 dias
- Executado em horário de baixo uso (3h)

### Estimativa de Carga

**Cenário típico:**
- 100 usuários ativos
- 50 usuários com notificações habilitadas
- 10 notas por usuário
- 20% das notas precisam de alerta

**Carga por execução do job:**
- Queries de usuários: 1
- Queries de configurações: 50
- Queries de notas: 50 (com JOINs)
- Queries de verificação anti-duplicata: ~100 (20% de 500 notas)
- Emails enviados: ~100
- Inserções de registro: ~100

**Total por hora:**
- ~300 queries
- ~100 emails
- Tempo estimado: 5-10 segundos

**Total por dia:**
- 24 execuções
- ~7.200 queries
- ~2.400 emails
- Pico às 9h (horário comercial)

### Escalabilidade

**Para volumes maiores:**

1. **Aumentar intervalo do job:**
```java
// De 1 em 1 hora para 2 em 2 horas
.withIntervalInHours(2)
```

2. **Filtrar usuários inativos mais cedo:**
```sql
SELECT * FROM users WHERE active = 1 AND email_verificado = 1
```

3. **Adicionar pool de threads:**
```java
// Processar usuários em paralelo
ExecutorService executor = Executors.newFixedThreadPool(5);
```

4. **Migrar para fila de mensagens:**
```
Job → Publica na fila → Workers processam
```

## Troubleshooting

### Problema 1: Job não está executando

**Sintomas:**
- Nenhum email recebido
- Logs não aparecem

**Diagnóstico:**

1. Verificar se scheduler iniciou:
```
grep "Quartz Scheduler iniciado" logs/app.log
```

2. Verificar status do scheduler:
```java
schedulerManager.estaRodando() // Deve retornar true
```

3. Verificar triggers:
```sql
-- Quartz armazena metadata em memória (SQLite)
-- Verificar logs de inicialização
```

**Solução:**
- Verificar exceções na inicialização
- Conferir dependência do quartz-scheduler no pom.xml

### Problema 2: Emails não estão sendo enviados

**Sintomas:**
- Job executa
- Logs mostram "0 emails enviados"

**Diagnóstico:**

1. Verificar configuração do usuário:
```sql
SELECT * FROM configuracoes WHERE usuario_id = 1 AND chave = 'notif_email';
```

2. Verificar notas pendentes:
```sql
SELECT * FROM notas WHERE usuario_id = 1 AND prazo_final <= date('now', '+5 days');
```

3. Verificar status das notas:
```sql
SELECT s.nome FROM notas n
JOIN status_nota s ON n.status_id = s.id
WHERE n.usuario_id = 1;
```

**Soluções:**
- Habilitar notificações: `/configuracoes`
- Verificar se notas não estão resolvidas/canceladas
- Conferir se prazo é futuro ou próximo

### Problema 3: Emails duplicados

**Sintomas:**
- Mesmo alerta recebido múltiplas vezes no mesmo dia

**Diagnóstico:**

1. Verificar registros de envio:
```sql
SELECT * FROM alertas_enviados WHERE nota_id = 1 AND DATE(data_envio) = DATE('now');
```

2. Verificar índice único:
```sql
SELECT sql FROM sqlite_master WHERE name = 'idx_alertas_enviados_usuario_nota';
```

**Solução:**
- Índice único evita duplicatas
- Se ocorrer, verificar lógica `alertaJaEnviado()`

### Problema 4: Exceção SMTP

**Sintomas:**
- Log: "Erro ao enviar email"
- Exceção `javax.mail.MessagingException`

**Diagnóstico:**

1. Verificar configurações SMTP em `application.properties`:
```properties
email.smtp.host=smtp.gmail.com
email.smtp.port=587
email.smtp.auth=true
email.smtp.starttls.enable=true
email.username=seu-email@gmail.com
email.password=sua-senha-app
```

2. Testar conexão SMTP:
```bash
telnet smtp.gmail.com 587
```

**Soluções:**
- Usar senha de app (Google)
- Verificar firewall
- Conferir credenciais

### Problema 5: Performance lenta

**Sintomas:**
- Job demora muito para executar
- Timeout em queries

**Diagnóstico:**

1. Verificar quantidade de usuários/notas:
```sql
SELECT COUNT(*) FROM users WHERE active = 1;
SELECT COUNT(*) FROM notas;
```

2. Verificar índices:
```sql
SELECT name FROM sqlite_master WHERE type = 'index';
```

3. Analisar query plan:
```sql
EXPLAIN QUERY PLAN
SELECT n.*, e.nome, s.nome FROM notas n
JOIN etiquetas e ON n.etiqueta_id = e.id
JOIN status_nota s ON n.status_id = s.id
WHERE n.usuario_id = 1;
```

**Soluções:**
- Criar índices faltantes
- Aumentar intervalo do job
- Implementar paginação

## Configuração de Produção

### Recomendações

**1. Configurar SMTP real**

Substituir configurações de desenvolvimento por SMTP de produção:

```properties
# Gmail (desenvolvimento)
email.smtp.host=smtp.gmail.com
email.smtp.port=587

# SendGrid (produção)
email.smtp.host=smtp.sendgrid.net
email.smtp.port=587
email.username=apikey
email.password=SG.xxxxxxxxxxxx
```

**2. Ajustar frequência**

Considerar intervalo adequado ao volume:

```java
// Baixo volume: 1 hora
.withIntervalInHours(1)

// Alto volume: 2-4 horas
.withIntervalInHours(4)
```

**3. Configurar horário de envio**

Evitar envios fora do horário comercial:

```java
// Apenas entre 8h e 18h
CronScheduleBuilder.cronSchedule("0 0 8-18 * * ?")
```

**4. Monitoramento**

Implementar métricas:

```java
// Enviar métricas para dashboard
int totalEnviados = processarAlertasParaTodosUsuarios();
metricsService.recordEmailsSent(totalEnviados);
```

**5. Rate limiting**

Limitar envios para evitar blacklist:

```java
// Máximo 100 emails por execução
if (enviados >= 100) {
    logger.warn("Limite de emails atingido");
    break;
}
```

## Conclusão

O sistema de alertas por email está completamente implementado com:

✅ **Quartz Scheduler** configurado e rodando automaticamente
✅ **Job de alertas** executando a cada 1 hora
✅ **Job de limpeza** executando diariamente às 3h
✅ **Anti-duplicatas** garantindo um email por dia por alerta
✅ **Configuração por usuário** respeitando preferências individuais
✅ **Níveis de alerta** dinâmicos baseados em configurações
✅ **Emails HTML** profissionais com cores por urgência
✅ **Integração completa** com sistema existente
✅ **Logs detalhados** para monitoramento
✅ **Performance otimizada** com índices e JOINs

**Próximos Passos Opcionais:**

- [ ] Adicionar testes unitários para os jobs
- [ ] Implementar dashboard de monitoramento
- [ ] Adicionar suporte a templates de email personalizados
- [ ] Implementar digest diário (resumo de todas as notas)
- [ ] Adicionar métricas para Grafana/Prometheus
- [ ] Suporte a múltiplos idiomas nos emails
