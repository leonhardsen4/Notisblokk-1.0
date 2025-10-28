# Guia de Testes: Sistema de Alertas por Email

## Pré-requisitos

Antes de iniciar os testes, verifique:

1. ✅ Projeto compilado sem erros (`mvn clean compile`)
2. ✅ Banco de dados inicializado (tabela `alertas_enviados` criada)
3. ✅ Configurações SMTP em `application.properties` ou variáveis de ambiente
4. ✅ Usuário admin criado e ativo

## Configuração SMTP para Testes

### Opção 1: Gmail (Desenvolvimento)

**application.properties:**
```properties
email.smtp.host=smtp.gmail.com
email.smtp.port=587
email.smtp.auth=true
email.smtp.starttls.enable=true
email.username=seu-email@gmail.com
email.password=sua-senha-de-app
email.from=seu-email@gmail.com
email.from.name=Notisblokk Sistema
```

**Como obter senha de app do Gmail:**
1. Acesse https://myaccount.google.com/security
2. Ative "Verificação em duas etapas"
3. Acesse "Senhas de app"
4. Gere uma senha para "Outro (nome personalizado)"
5. Use essa senha no `email.password`

### Opção 2: Mailtrap (Testes sem envio real)

```properties
email.smtp.host=smtp.mailtrap.io
email.smtp.port=2525
email.smtp.auth=true
email.smtp.starttls.enable=true
email.username=seu-username-mailtrap
email.password=sua-senha-mailtrap
email.from=test@notisblokk.com
email.from.name=Notisblokk Test
```

Cadastre-se em https://mailtrap.io (gratuito) e obtenha credenciais.

## Roteiro de Testes

### Teste 1: Verificação Inicial do Scheduler

**Objetivo:** Confirmar que o Quartz Scheduler iniciou corretamente.

**Passos:**

1. Inicie a aplicação:
```bash
mvn clean compile exec:java
```

2. Verifique os logs de inicialização:
```
[main] INFO  QuartzSchedulerManager - Inicializando Quartz Scheduler...
[main] INFO  QuartzSchedulerManager - 📧 Job de alertas por email agendado: executa a cada 1 hora
[main] INFO  QuartzSchedulerManager - 🧹 Job de limpeza de alertas agendado: executa diariamente às 3h
[main] INFO  QuartzSchedulerManager - ✅ Quartz Scheduler iniciado com sucesso
```

**Resultado esperado:**
✅ Logs mostram inicialização bem-sucedida
✅ Nenhuma exceção relacionada ao Quartz
✅ Aplicação inicia normalmente

---

### Teste 2: Verificação da Tabela alertas_enviados

**Objetivo:** Confirmar que a tabela foi criada no banco.

**Passos:**

1. Com a aplicação rodando, conecte ao banco de dados:
```bash
sqlite3 notisblokk.db
```

2. Verifique a tabela:
```sql
.tables
-- Deve aparecer: alertas_enviados

.schema alertas_enviados
-- Deve mostrar estrutura completa
```

3. Verifique índices:
```sql
SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='alertas_enviados';
-- Deve retornar:
-- idx_alertas_enviados_usuario_nota
-- idx_alertas_enviados_data
```

**Resultado esperado:**
✅ Tabela existe
✅ Campos corretos (id, usuario_id, nota_id, nivel, dias_restantes, data_envio)
✅ Índices criados

---

### Teste 3: Configuração de Notificações do Usuário

**Objetivo:** Habilitar alertas por email para o usuário admin.

**Passos:**

1. Faça login na aplicação:
   - URL: http://localhost:7070
   - Email: admin@notisblokk.com
   - Senha: admin123

2. Acesse **Configurações** no menu lateral

3. Seção "Notificações e Alertas":
   - ✅ Marque "Notificações por E-mail"
   - ✅ Marque "Notificações Toast (em tempo real)"
   - Configure dias:
     - Alerta Crítico: **0** dias
     - Alerta Urgente: **3** dias
     - Alerta de Atenção: **5** dias

4. Clique em "Salvar Configurações"

5. Verifique no banco:
```sql
SELECT chave, valor FROM configuracoes WHERE usuario_id = 1;
```

**Resultado esperado:**
✅ Mensagem: "Configurações salvas com sucesso"
✅ Registros no banco:
```
notif_email|true
notif_toast|true
notif_dias_critico|0
notif_dias_urgente|3
notif_dias_atencao|5
```

---

### Teste 4: Criar Nota com Prazo Próximo

**Objetivo:** Criar nota que deve gerar alerta urgente.

**Passos:**

1. Acesse **Anotações** no menu

2. Clique em "Nova Nota"

3. Preencha:
   - **Título:** Teste de Alerta - Urgente
   - **Etiqueta:** Trabalho (ou crie nova)
   - **Status:** Pendente
   - **Prazo:** [Data de amanhã no formato dd/MM/yyyy]
   - **Conteúdo:** Esta é uma nota de teste para alertas por email

4. Clique em "Salvar"

5. Verifique que a nota foi criada:
```sql
SELECT id, titulo, prazo_final FROM notas WHERE titulo LIKE '%Teste de Alerta%';
```

**Resultado esperado:**
✅ Nota criada com sucesso
✅ Prazo configurado para amanhã
✅ Status = Pendente

---

### Teste 5: Criar Nota Atrasada

**Objetivo:** Criar nota com prazo vencido (alerta crítico).

**Passos:**

1. Crie nova nota:
   - **Título:** Teste de Alerta - Crítico
   - **Etiqueta:** Urgente
   - **Status:** Pendente
   - **Prazo:** [Data de 2 dias atrás]
   - **Conteúdo:** Esta nota está atrasada

2. Salve a nota

**Resultado esperado:**
✅ Nota criada com prazo no passado
✅ Sistema permite criar nota atrasada

---

### Teste 6: Execução Manual do Job (Método 1 - Aguardar)

**Objetivo:** Aguardar a execução automática do job na próxima hora.

**Passos:**

1. Anote o horário atual (ex: 14:30)

2. Aguarde até o início da próxima hora (ex: 15:00)

3. Observe os logs:
```
[DefaultQuartzScheduler_Worker-1] INFO  AlertaEmailJob - ========================================
[DefaultQuartzScheduler_Worker-1] INFO  AlertaEmailJob - Iniciando job de envio de alertas por email
[DefaultQuartzScheduler_Worker-1] INFO  AlertaEmailJob - Processando alertas para X usuários
[DefaultQuartzScheduler_Worker-1] INFO  AlertaEmailJob - Email enviado: admin@notisblokk.com - Nota: Teste de Alerta - Urgente (1 dias)
[DefaultQuartzScheduler_Worker-1] INFO  AlertaEmailJob - Email enviado: admin@notisblokk.com - Nota: Teste de Alerta - Crítico (-2 dias)
[DefaultQuartzScheduler_Worker-1] INFO  AlertaEmailJob - Job finalizado com sucesso: 2 emails enviados
```

**Resultado esperado:**
✅ Job executa automaticamente a cada hora
✅ Logs aparecem no console
✅ Emails são enviados

---

### Teste 6 (Alternativo): Execução Manual via Código

**Objetivo:** Forçar execução imediata do job para testes rápidos.

**Opção A - Alterar intervalo para 1 minuto (temporário):**

1. Edite `QuartzSchedulerManager.java`:
```java
// TEMPORÁRIO: apenas para testes
.withIntervalInMinutes(1)  // Era: .withIntervalInHours(1)
```

2. Reinicie a aplicação

3. Aguarde 1 minuto e observe os logs

**Opção B - Criar endpoint de teste:**

1. Adicione em `Main.java` (após outras rotas):
```java
// TEMPORÁRIO: endpoint de teste para disparar job manualmente
app.get("/admin/test/trigger-alertas", ctx -> {
    schedulerManager.executarAlertasManualmente();
    ctx.result("Job de alertas disparado manualmente!");
});
```

2. Reinicie a aplicação

3. Acesse: http://localhost:7070/admin/test/trigger-alertas

4. Observe os logs

**Resultado esperado:**
✅ Job executa imediatamente
✅ Emails são enviados

---

### Teste 7: Verificar Email Recebido

**Objetivo:** Confirmar recebimento e conteúdo dos emails.

**Passos:**

1. Verifique sua caixa de entrada (ou Mailtrap)

2. Procure por emails de "Notisblokk Sistema"

3. Verifique o email da nota urgente:
   - **Subject:** [URGENTE] Alerta: Teste de Alerta - Urgente
   - **Corpo contém:**
     - Nome do usuário
     - Título da nota
     - Prazo final
     - Mensagem: "Faltam apenas 1 dia(s) para o prazo!"
     - Botão "Ver Nota"
   - **Estilo:**
     - Cor laranja (#F97316)
     - Layout HTML profissional

4. Verifique o email da nota crítica:
   - **Subject:** [CRÍTICO - Nota Atrasada] Alerta: Teste de Alerta - Crítico
   - **Corpo contém:**
     - Mensagem: "Esta nota está atrasada há 2 dia(s)!"
   - **Estilo:**
     - Cor vermelha (#DC2626)

**Resultado esperado:**
✅ 2 emails recebidos
✅ Conteúdo correto e formatado
✅ Cores apropriadas por nível
✅ Botão "Ver Nota" presente

---

### Teste 8: Verificar Registros de Envio

**Objetivo:** Confirmar que envios foram registrados no banco.

**Passos:**

1. Consulte a tabela:
```sql
SELECT
    ae.id,
    u.username,
    n.titulo,
    ae.nivel,
    ae.dias_restantes,
    ae.data_envio
FROM alertas_enviados ae
JOIN users u ON ae.usuario_id = u.id
JOIN notas n ON ae.nota_id = n.id
ORDER BY ae.data_envio DESC;
```

**Resultado esperado:**
```
1|admin|Teste de Alerta - Urgente|URGENTE|1|2025-01-27 15:00:15
2|admin|Teste de Alerta - Crítico|CRITICO|-2|2025-01-27 15:00:16
```

✅ 2 registros criados
✅ Níveis corretos (URGENTE e CRITICO)
✅ Dias restantes corretos
✅ Data/hora de envio recente

---

### Teste 9: Verificar Anti-Duplicata (Mesmo Dia)

**Objetivo:** Confirmar que alertas não são reenviados no mesmo dia.

**Passos:**

1. Force uma nova execução do job:
   - Aguarde próxima hora OU
   - Use endpoint de teste OU
   - Reinicie aplicação (se intervalo for < 1 hora)

2. Observe os logs:
```
[AlertaEmailJob] DEBUG - Alerta para nota Teste de Alerta - Urgente (nível URGENTE) já foi enviado hoje
[AlertaEmailJob] INFO  - Job finalizado com sucesso: 0 emails enviados
```

3. Verifique sua caixa de entrada:
   - Nenhum email novo recebido

4. Verifique o banco:
```sql
SELECT COUNT(*) FROM alertas_enviados
WHERE DATE(data_envio) = DATE('now', 'localtime');
-- Deve retornar: 2 (sem novos registros)
```

**Resultado esperado:**
✅ Nenhum email duplicado enviado
✅ Log indica que alerta já foi enviado
✅ Nenhum novo registro na tabela

---

### Teste 10: Verificar Envio no Dia Seguinte

**Objetivo:** Confirmar que alertas são reenviados no dia seguinte.

**Passos:**

1. **Simular mudança de dia** (alterar registro no banco):
```sql
UPDATE alertas_enviados
SET data_envio = datetime('now', '-1 day')
WHERE nota_id IN (
    SELECT id FROM notas WHERE titulo LIKE '%Teste de Alerta%'
);
```

2. Force nova execução do job

3. Observe os logs:
```
[AlertaEmailJob] INFO - Email enviado: admin@notisblokk.com - Nota: Teste de Alerta - Urgente (0 dias)
```

4. Verifique caixa de entrada:
   - Novos emails recebidos

5. Verifique banco:
```sql
SELECT nota_id, COUNT(*) as total
FROM alertas_enviados
GROUP BY nota_id;
-- Deve mostrar 2 registros por nota
```

**Resultado esperado:**
✅ Emails reenviados após "mudança de dia"
✅ Novos registros criados
✅ Sistema funciona corretamente dia após dia

---

### Teste 11: Desabilitar Notificações

**Objetivo:** Confirmar que usuário pode desabilitar alertas.

**Passos:**

1. Acesse **Configurações**

2. **Desmarque** "Notificações por E-mail"

3. Salve

4. Simule mudança de dia:
```sql
UPDATE alertas_enviados
SET data_envio = datetime('now', '-1 day');
```

5. Force nova execução do job

6. Observe os logs:
```
[AlertaEmailJob] DEBUG - Usuário admin tem notificações por email desabilitadas
[AlertaEmailJob] INFO  - Job finalizado com sucesso: 0 emails enviados
```

7. Verifique caixa de entrada:
   - Nenhum email recebido

**Resultado esperado:**
✅ Nenhum email enviado
✅ Log indica que notificações estão desabilitadas
✅ Configuração do usuário respeitada

---

### Teste 12: Notas Resolvidas Não Geram Alertas

**Objetivo:** Confirmar que notas com status "Resolvido" não geram alertas.

**Passos:**

1. Acesse a lista de **Anotações**

2. Edite a nota "Teste de Alerta - Urgente"

3. Altere **Status** para "Resolvido"

4. Salve

5. Reabilite notificações em **Configurações**

6. Simule mudança de dia e force execução do job

7. Observe os logs:
```
[AlertaEmailJob] INFO - Job finalizado com sucesso: 1 emails enviados
```

8. Verifique caixa de entrada:
   - Apenas 1 email recebido (nota crítica)
   - Nota resolvida não gerou email

**Resultado esperado:**
✅ Nota resolvida ignorada
✅ Apenas nota crítica gera alerta
✅ Filtro de status funcionando

---

### Teste 13: Job de Limpeza de Alertas Antigos

**Objetivo:** Verificar remoção de registros com mais de 30 dias.

**Passos:**

1. Insira registro antigo:
```sql
INSERT INTO alertas_enviados (usuario_id, nota_id, nivel, dias_restantes, data_envio)
VALUES (1, 1, 'TESTE', 5, datetime('now', '-35 days'));
```

2. Verifique que registro existe:
```sql
SELECT * FROM alertas_enviados WHERE nivel = 'TESTE';
-- Deve retornar 1 registro
```

3. **Opção A - Aguardar até 3h da manhã** (horário do cron)

   **Opção B - Alterar horário do cron temporariamente:**

   Em `QuartzSchedulerManager.java`:
   ```java
   // Executar em 5 minutos
   .withSchedule(
       CronScheduleBuilder.cronSchedule("0 */5 * * * ?")
   )
   ```

   **Opção C - Executar job manualmente via código:**

   Adicione endpoint de teste:
   ```java
   app.get("/admin/test/trigger-limpeza", ctx -> {
       new LimpezaAlertasJob().execute(null);
       ctx.result("Job de limpeza executado!");
   });
   ```

4. Após execução, observe logs:
```
[LimpezaAlertasJob] INFO - Iniciando job de limpeza de alertas antigos
[AlertaEnviadoRepository] INFO - Limpeza de alertas antigos: 1 registros removidos
[LimpezaAlertasJob] INFO - Job de limpeza finalizado com sucesso
```

5. Verifique que registro foi removido:
```sql
SELECT * FROM alertas_enviados WHERE nivel = 'TESTE';
-- Deve retornar 0 registros
```

**Resultado esperado:**
✅ Registro antigo removido
✅ Registros recentes mantidos
✅ Log mostra quantidade de registros removidos

---

### Teste 14: Múltiplos Usuários

**Objetivo:** Confirmar que sistema processa múltiplos usuários corretamente.

**Passos:**

1. Crie novo usuário via interface ou SQL:
```sql
INSERT INTO users (username, email, password_hash, full_name, role, active, created_at, updated_at)
VALUES (
    'operador',
    'operador@notisblokk.com',
    '$2a$12$KIXvBN0q3xGfPHDuLZxdMeB5YfC8h8PdRvHWKjKPUFGpHCvXqk3Qi', -- senha: 123456
    'Operador Teste',
    'OPERATOR',
    1,
    datetime('now'),
    datetime('now')
);
```

2. Configure notificações para o novo usuário:
```sql
INSERT INTO configuracoes (usuario_id, chave, valor)
SELECT id, 'notif_email', 'true' FROM users WHERE username = 'operador';
```

3. Crie nota para o novo usuário:
   - Faça login com operador@notisblokk.com / 123456
   - Crie nota com prazo amanhã

4. Force execução do job

5. Observe logs:
```
[AlertaEmailJob] INFO - Processando alertas para 2 usuários
[AlertaEmailJob] INFO - Email enviado: admin@notisblokk.com - ...
[AlertaEmailJob] INFO - Email enviado: operador@notisblokk.com - ...
[AlertaEmailJob] INFO - Job finalizado com sucesso: X emails enviados
```

**Resultado esperado:**
✅ Job processa todos os usuários ativos
✅ Emails enviados para múltiplos destinatários
✅ Configurações individuais respeitadas

---

### Teste 15: Performance com Muitas Notas

**Objetivo:** Avaliar performance do job com volume maior de dados.

**Passos:**

1. Crie múltiplas notas via SQL:
```sql
INSERT INTO notas (etiqueta_id, status_id, titulo, conteudo, prazo_final, data_criacao, data_atualizacao, sessao_id, usuario_id)
SELECT
    1,
    1,
    'Nota Teste ' || seq,
    'Conteúdo de teste',
    date('now', '+' || (seq % 10) || ' days'),
    datetime('now'),
    datetime('now'),
    1,
    1
FROM (
    SELECT 1 as seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
);
```

2. Limpe registros antigos:
```sql
DELETE FROM alertas_enviados;
```

3. Force execução do job

4. Observe logs e meça tempo:
```
[AlertaEmailJob] INFO - Iniciando job...
[AlertaEmailJob] INFO - Verificando 20 notas do usuário admin
[AlertaEmailJob] INFO - Job finalizado com sucesso: X emails enviados
```

5. Anote tempo de execução

**Resultado esperado:**
✅ Job processa 20+ notas rapidamente (< 5 segundos)
✅ Nenhum timeout
✅ Queries otimizadas (com JOINs)

---

## Verificação Final: Checklist Completo

Após completar todos os testes, verifique:

### Funcionalidades Básicas
- [ ] Quartz Scheduler inicia com a aplicação
- [ ] Job de alertas executa a cada 1 hora
- [ ] Job de limpeza executa diariamente às 3h
- [ ] Tabela `alertas_enviados` criada corretamente

### Envio de Emails
- [ ] Emails são enviados para alertas críticos (prazo vencido)
- [ ] Emails são enviados para alertas urgentes (0-3 dias)
- [ ] Emails são enviados para alertas de atenção (4-5 dias)
- [ ] Emails NÃO são enviados para notas com mais de 5 dias
- [ ] HTML dos emails está formatado corretamente
- [ ] Cores corretas por nível de urgência

### Configurações do Usuário
- [ ] Usuário pode habilitar/desabilitar notificações
- [ ] Configuração `notif_email = false` impede envio
- [ ] Dias configuráveis para cada nível de alerta
- [ ] Configurações salvas no banco corretamente

### Anti-Duplicata
- [ ] Mesmo alerta não é enviado múltiplas vezes no mesmo dia
- [ ] Alertas são reenviados no dia seguinte
- [ ] Registros salvos na tabela `alertas_enviados`
- [ ] Índices funcionando corretamente

### Filtros e Lógica
- [ ] Notas resolvidas/canceladas não geram alertas
- [ ] Notas de usuários inativos não geram alertas
- [ ] Múltiplos usuários processados corretamente
- [ ] Nível de alerta muda conforme prazo se aproxima

### Performance e Manutenção
- [ ] Queries otimizadas (JOINs em vez de N+1)
- [ ] Job executa rapidamente mesmo com muitas notas
- [ ] Limpeza remove registros com mais de 30 dias
- [ ] Logs detalhados para monitoramento

### Shutdown
- [ ] Scheduler para graciosamente ao encerrar aplicação
- [ ] Jobs em execução completam antes do shutdown

---

## Troubleshooting Durante Testes

### Problema: "Nenhum email recebido"

**Verificações:**

1. Configuração SMTP está correta?
```bash
grep "email.smtp" application.properties
```

2. Usuário tem notificações habilitadas?
```sql
SELECT valor FROM configuracoes WHERE usuario_id = 1 AND chave = 'notif_email';
```

3. Nota tem prazo dentro do range?
```sql
SELECT
    titulo,
    prazo_final,
    julianday(prazo_final) - julianday('now') as dias
FROM notas
WHERE usuario_id = 1;
```

4. Nota está com status pendente?
```sql
SELECT n.titulo, s.nome
FROM notas n
JOIN status_nota s ON n.status_id = s.id
WHERE n.usuario_id = 1;
```

### Problema: "Job não executa"

**Verificações:**

1. Scheduler iniciou?
```
grep "Quartz Scheduler iniciado" logs
```

2. Há exceções nos logs?
```
grep "ERROR.*Quartz\|ERROR.*Job" logs
```

3. Trigger configurado corretamente?
```java
// Verificar em QuartzSchedulerManager.java
.withIntervalInHours(1)
```

### Problema: "Emails duplicados"

**Verificações:**

1. Método `alertaJaEnviado()` está funcionando?
```sql
-- Deve retornar registros de hoje
SELECT * FROM alertas_enviados
WHERE DATE(data_envio) = DATE('now', 'localtime');
```

2. Índice existe?
```sql
SELECT sql FROM sqlite_master
WHERE name = 'idx_alertas_enviados_usuario_nota';
```

---

## Próximos Passos Após Testes

Após validar todos os testes:

1. **Ajustar configurações de produção:**
   - Configurar SMTP real (SendGrid, AWS SES, etc.)
   - Ajustar intervalo do job se necessário
   - Configurar horário de envio (apenas horário comercial)

2. **Implementar monitoramento:**
   - Dashboard com métricas de emails enviados
   - Alertas se job falhar
   - Logs centralizados

3. **Melhorias opcionais:**
   - Digest diário (resumo de todas as notas)
   - Templates de email personalizáveis
   - Suporte a múltiplos idiomas
   - Relatório semanal para administradores

---

## Resumo

✅ **Sistema de alertas por email está completo e funcional!**

Componentes implementados:
- ✅ Quartz Scheduler com 2 jobs configurados
- ✅ AlertaEmailJob (executa a cada 1 hora)
- ✅ LimpezaAlertasJob (executa diariamente às 3h)
- ✅ Sistema anti-duplicata
- ✅ Configuração por usuário
- ✅ Níveis de alerta dinâmicos
- ✅ Emails HTML profissionais
- ✅ Integração completa com sistema existente
- ✅ Otimizações de performance
- ✅ Documentação completa

**Todas as 10 tarefas do projeto foram concluídas com sucesso!**

Para suporte ou dúvidas, consulte:
- `IMPLEMENTACAO-ALERTAS-EMAIL.md` - Documentação técnica completa
- Logs da aplicação em tempo real
- Código-fonte comentado
