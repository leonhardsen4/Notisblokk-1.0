# 📋 TAREFAS - MELHORIAS NO SISTEMA DE ANOTAÇÕES

**Branch:** `feature/melhorias-anotacoes`
**Data de Início:** 02/12/2025
**Objetivo:** Implementar melhorias de usabilidade, performance e funcionalidades no sistema de anotações do Notisblokk

---

## 📊 PROGRESSO GERAL

**Total de Tarefas:** 22
**Concluídas:** 0
**Em Progresso:** 0
**Pendentes:** 22

**Progresso:** ███░░░░░░░ 0%

---

## 🎯 FASE 1: EXPORTAÇÃO DE PDF (PRIORIDADE ALTA)

### ✅ Tarefa 1.1: Adicionar Botão de PDF na Lista de Notas
- [x] **Status:** Concluído
- **Arquivo:** `src/main/resources/templates/notas/index.html`
- **Descrição:** Adicionar botão "📄 PDF" na coluna de ações da tabela
- **Localização:** Linha 117 (coluna de ações)
- **Tempo Estimado:** 10 min
- **Checklist:**
  - [x] Adicionar botão HTML com ícone 📄
  - [x] Adicionar atributo `@click="gerarPDF(nota.id, nota.titulo)"`
  - [x] Estilizar botão (classe `btn btn-sm btn-success`)
  - [ ] Testar clique no botão (aguardando implementação da função JS)
  - [x] Verificar integração com backend (endpoint já existe)
  - [x] Adicionar comentários em português

---

### ✅ Tarefa 1.2: Implementar Função JavaScript de Exportação Individual
- [x] **Status:** Concluído
- **Arquivo:** `src/main/resources/templates/notas/index.html` (seção `<script>`)
- **Descrição:** Criar função JavaScript para chamar endpoint de PDF
- **Tempo Estimado:** 5 min
- **Checklist:**
  - [x] Criar função `gerarPDF(notaId, tituloNota)`
  - [x] Implementar chamada ao endpoint `/api/notas/{id}/pdf`
  - [x] Adicionar feedback visual (logs console por enquanto)
  - [x] Tratar erros com mensagem amigável
  - [x] Documentar função em português (JSDoc completo)
  - [ ] Testar com nota existente (aguardando deploy)

---

### ✅ Tarefa 1.3: Adicionar Botão de PDF no Formulário de Edição
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/resources/templates/notas/form.html`
- **Descrição:** Adicionar botão "📄 Exportar PDF" no cabeçalho do formulário
- **Localização:** Linha ~134 (próximo ao botão "Voltar")
- **Tempo Estimado:** 8 min
- **Checklist:**
  - [ ] Adicionar botão com condicional `x-show="notaId"` (só aparece na edição)
  - [ ] Adicionar função `exportarPDF()` no script inline ou externo
  - [ ] Estilizar botão consistente com design atual
  - [ ] Testar exportação durante edição
  - [ ] Verificar que não aparece na criação de nova nota
  - [ ] Documentar código

---

### ✅ Tarefa 1.4: Implementar Seleção Múltipla de Notas
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/resources/templates/notas/index.html`
- **Descrição:** Adicionar checkboxes para seleção de múltiplas notas
- **Tempo Estimado:** 20 min
- **Checklist:**
  - [ ] Adicionar coluna de checkbox na tabela
  - [ ] Criar variável Alpine.js `notasSelecionadas: []`
  - [ ] Adicionar checkbox de "Selecionar Todas" no header
  - [ ] Implementar lógica de seleção/desseleção
  - [ ] Atualizar contador de notas selecionadas
  - [ ] Documentar componente
  - [ ] Testar seleção individual e em massa

---

### ✅ Tarefa 1.5: Adicionar Botão de Exportação em Massa
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/resources/templates/notas/index.html`
- **Descrição:** Criar botão para exportar PDF com múltiplas notas selecionadas
- **Tempo Estimado:** 15 min
- **Checklist:**
  - [ ] Adicionar botão "📊 Exportar Selecionadas (X)"
  - [ ] Desabilitar quando nenhuma nota estiver selecionada
  - [ ] Implementar função `exportarSelecionadas()`
  - [ ] Fazer POST para `/api/notas/pdf/relatorio` com IDs
  - [ ] Baixar PDF gerado automaticamente
  - [ ] Limpar seleção após exportação bem-sucedida
  - [ ] Adicionar tratamento de erros
  - [ ] Documentar código

---

## 🎨 FASE 2: MELHORIAS DE USABILIDADE (PRIORIDADE ALTA)

### ✅ Tarefa 2.1: Implementar Sistema de Toast Notifications
- [ ] **Status:** Pendente
- **Arquivos:**
  - `src/main/resources/templates/notas/index.html`
  - `src/main/resources/templates/notas/form.html`
  - `src/main/resources/public/css/notas.css`
- **Descrição:** Adicionar biblioteca de toast e feedback visual para ações
- **Tempo Estimado:** 25 min
- **Checklist:**
  - [ ] Adicionar Toastify.js via CDN ou criar solução custom
  - [ ] Criar função helper `mostrarToast(mensagem, tipo)`
  - [ ] Adicionar toasts em: criar nota, editar nota, deletar nota, exportar PDF
  - [ ] Estilizar toasts com tema do Notisblokk
  - [ ] Testar em tema claro e escuro
  - [ ] Adicionar toasts de erro e sucesso
  - [ ] Documentar uso dos toasts
  - [ ] Verificar compatibilidade mobile

---

### ✅ Tarefa 2.2: Criar Modal de Preview de Nota
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/resources/templates/notas/index.html`
- **Descrição:** Implementar modal read-only para visualização rápida de notas
- **Tempo Estimado:** 35 min
- **Checklist:**
  - [ ] Criar estrutura HTML do modal de preview
  - [ ] Adicionar Alpine.js state: `modalPreview: false, notaPreview: null`
  - [ ] Implementar função `visualizarNota(id)`
  - [ ] Buscar dados da nota via API
  - [ ] Renderizar conteúdo HTML do Quill corretamente
  - [ ] Adicionar botão "Editar" que redireciona para formulário
  - [ ] Adicionar botão "Exportar PDF" no modal
  - [ ] Estilizar modal consistente com design
  - [ ] Tornar título clicável na tabela
  - [ ] Testar abertura e fechamento
  - [ ] Documentar componente

---

### ✅ Tarefa 2.3: Melhorar Feedback Visual de Deletar Nota
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/resources/templates/notas/index.html`
- **Descrição:** Melhorar modal de confirmação de exclusão
- **Tempo Estimado:** 15 min
- **Checklist:**
  - [ ] Criar modal de confirmação customizado (substituir confirm())
  - [ ] Mostrar título da nota sendo deletada
  - [ ] Adicionar ícone de alerta
  - [ ] Botões claros: "Cancelar" e "Deletar Permanentemente"
  - [ ] Toast de sucesso após deletar
  - [ ] Toast de erro se falhar
  - [ ] Documentar função
  - [ ] Testar fluxo completo

---

### ✅ Tarefa 2.4: Melhorar Destaque de Alertas Urgentes
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/resources/templates/notas/index.html`
- **Descrição:** Tornar botão de alertas mais proativo e visível
- **Tempo Estimado:** 12 min
- **Checklist:**
  - [ ] Adicionar animação pulsante quando houver alertas urgentes (< 3 dias)
  - [ ] Adicionar badge vermelho com número
  - [ ] Abrir modal automaticamente se houver notas vencidas
  - [ ] Adicionar som de notificação (opcional, com toggle)
  - [ ] Melhorar CSS do botão de alertas
  - [ ] Testar com diferentes quantidades de alertas
  - [ ] Documentar comportamento

---

## ⚡ FASE 3: MELHORIAS DE PERFORMANCE (PRIORIDADE MÉDIA)

### ✅ Tarefa 3.1: Otimizar Queries N+1 - Repository
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/java/com/notisblokk/repository/NotaRepository.java`
- **Descrição:** Modificar query para fazer JOIN e evitar N+1 queries
- **Tempo Estimado:** 30 min
- **Checklist:**
  - [ ] Criar método `buscarTodasComRelacionamentos()`
  - [ ] Implementar LEFT JOIN com etiquetas e status_nota
  - [ ] Mapear ResultSet para DTOs diretamente
  - [ ] Adicionar JavaDoc em português
  - [ ] Testar query no SQLite
  - [ ] Comparar performance (antes/depois)
  - [ ] Verificar se não quebrou funcionalidades existentes
  - [ ] Adicionar logs de debug

---

### ✅ Tarefa 3.2: Otimizar Service para Usar Nova Query
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/java/com/notisblokk/service/NotaService.java`
- **Descrição:** Atualizar método `listarTodas()` para usar query otimizada
- **Tempo Estimado:** 15 min
- **Checklist:**
  - [ ] Modificar `listarTodas()` para usar novo método do repository
  - [ ] Remover método `converterParaDTOs()` antigo (se não usado)
  - [ ] Atualizar JavaDoc
  - [ ] Executar testes manuais
  - [ ] Verificar paginação continua funcionando
  - [ ] Documentar mudanças

---

### ✅ Tarefa 3.3: Implementar Cache Simples de Etiquetas
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/java/com/notisblokk/service/EtiquetaService.java`
- **Descrição:** Adicionar cache em memória com TTL para etiquetas
- **Tempo Estimado:** 40 min
- **Checklist:**
  - [ ] Criar classe `SimpleCache<K, V>` genérica
  - [ ] Implementar TTL (Time To Live) de 5 minutos
  - [ ] Adicionar cache no `EtiquetaService`
  - [ ] Invalidar cache ao criar/editar/deletar etiqueta
  - [ ] Adicionar logs de cache hit/miss
  - [ ] Documentar classe e uso
  - [ ] Testar invalidação funciona corretamente
  - [ ] Verificar thread-safety (ConcurrentHashMap)

---

### ✅ Tarefa 3.4: Implementar Cache de Status
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/java/com/notisblokk/service/StatusNotaService.java`
- **Descrição:** Adicionar cache para status de notas (similar a etiquetas)
- **Tempo Estimado:** 20 min
- **Checklist:**
  - [ ] Adicionar cache usando classe `SimpleCache`
  - [ ] Invalidar ao criar/editar/deletar status
  - [ ] Documentar uso
  - [ ] Testar funcionalidade
  - [ ] Verificar performance melhorou

---

## 🎨 FASE 4: MELHORIAS DE UI/UX (PRIORIDADE MÉDIA)

### ✅ Tarefa 4.1: Criar Filtros Visuais com Badges
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/resources/templates/notas/index.html`
- **Descrição:** Substituir dropdowns por badges clicáveis com contadores
- **Tempo Estimado:** 45 min
- **Checklist:**
  - [ ] Criar seção de filtros com badges
  - [ ] Calcular contadores para cada etiqueta/status
  - [ ] Tornar badges clicáveis (toggle filtro)
  - [ ] Mostrar badge ativo com destaque
  - [ ] Permitir múltiplos filtros ativos
  - [ ] Atualizar contadores ao pesquisar
  - [ ] Estilizar com cores das etiquetas/status
  - [ ] Adicionar botão "Limpar Filtros"
  - [ ] Documentar componente
  - [ ] Testar combinações de filtros

---

### ✅ Tarefa 4.2: Implementar Atalhos de Teclado
- [ ] **Status:** Pendente
- **Arquivos:**
  - `src/main/resources/templates/notas/index.html`
  - `src/main/resources/templates/notas/form.html`
- **Descrição:** Adicionar atalhos de teclado para ações comuns
- **Tempo Estimado:** 30 min
- **Checklist:**
  - [ ] `Ctrl+N` ou `Alt+N`: Nova nota
  - [ ] `Ctrl+S`: Salvar nota (no formulário)
  - [ ] `Esc`: Fechar modais
  - [ ] `/`: Focar campo de busca
  - [ ] `Ctrl+P` ou `Alt+P`: Exportar PDF
  - [ ] Criar função `setupKeyboardShortcuts()`
  - [ ] Prevenir conflitos com atalhos do navegador
  - [ ] Adicionar tooltip mostrando atalhos disponíveis
  - [ ] Documentar atalhos
  - [ ] Testar em diferentes navegadores

---

### ✅ Tarefa 4.3: Adicionar Indicador de Loading
- [ ] **Status:** Pendente
- **Arquivos:**
  - `src/main/resources/templates/notas/index.html`
  - `src/main/resources/templates/notas/form.html`
- **Descrição:** Mostrar spinner durante operações assíncronas
- **Tempo Estimado:** 20 min
- **Checklist:**
  - [ ] Criar componente de loading overlay
  - [ ] Adicionar variável Alpine.js `carregando: false`
  - [ ] Mostrar loading ao: listar notas, exportar PDF, salvar
  - [ ] Estilizar spinner consistente com tema
  - [ ] Funcionar em tema claro e escuro
  - [ ] Documentar uso
  - [ ] Testar em conexões lentas

---

## 🔍 FASE 5: FUNCIONALIDADES AVANÇADAS (PRIORIDADE BAIXA)

### ✅ Tarefa 5.1: Implementar Busca em Conteúdo
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/java/com/notisblokk/repository/NotaRepository.java`
- **Descrição:** Expandir busca para incluir conteúdo das notas
- **Tempo Estimado:** 25 min
- **Checklist:**
  - [ ] Modificar query de busca para incluir `LIKE` em conteúdo
  - [ ] Usar LOWER() para busca case-insensitive
  - [ ] Atualizar método `buscarPorTexto(String termo)`
  - [ ] Testar com notas que têm HTML no conteúdo
  - [ ] Adicionar highlight dos resultados (opcional)
  - [ ] Documentar método
  - [ ] Verificar performance com muitas notas

---

### ✅ Tarefa 5.2: Adicionar Filtro por Intervalo de Datas
- [ ] **Status:** Pendente
- **Arquivos:**
  - `src/main/resources/templates/notas/index.html`
  - `src/main/java/com/notisblokk/repository/NotaRepository.java`
- **Descrição:** Permitir filtrar notas por intervalo de prazo final
- **Tempo Estimado:** 30 min
- **Checklist:**
  - [ ] Adicionar campos de data: "De" e "Até"
  - [ ] Criar método `buscarPorIntervaloPrazo(LocalDate inicio, LocalDate fim)`
  - [ ] Integrar com filtros existentes
  - [ ] Adicionar presets (Esta semana, Este mês, Últimos 30 dias)
  - [ ] Documentar funcionalidade
  - [ ] Testar diferentes intervalos

---

### ✅ Tarefa 5.3: Implementar Ações em Massa - Deletar
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/resources/templates/notas/index.html`
- **Descrição:** Permitir deletar múltiplas notas de uma vez
- **Tempo Estimado:** 25 min
- **Checklist:**
  - [ ] Adicionar botão "🗑️ Deletar Selecionadas"
  - [ ] Modal de confirmação mostrando quantidade
  - [ ] Implementar função `deletarSelecionadas()`
  - [ ] Fazer requisições em paralelo ou batch endpoint
  - [ ] Toast de sucesso/erro
  - [ ] Limpar seleção após deleção
  - [ ] Atualizar lista automaticamente
  - [ ] Documentar código
  - [ ] Testar com diferentes quantidades

---

### ✅ Tarefa 5.4: Implementar Ações em Massa - Mudar Status
- [ ] **Status:** Pendente
- **Arquivo:** `src/main/resources/templates/notas/index.html`
- **Descrição:** Permitir mudar status de múltiplas notas simultaneamente
- **Tempo Estimado:** 30 min
- **Checklist:**
  - [ ] Adicionar botão "📊 Mudar Status"
  - [ ] Modal com dropdown de status disponíveis
  - [ ] Implementar função `mudarStatusSelecionadas(novoStatusId)`
  - [ ] Atualizar notas via API (PUT em lote ou um por vez)
  - [ ] Toast de progresso e conclusão
  - [ ] Recarregar lista após mudanças
  - [ ] Documentar código
  - [ ] Testar mudança de status

---

## 📚 FASE 6: DOCUMENTAÇÃO E FINALIZAÇÃO

### ✅ Tarefa 6.1: Atualizar CLAUDE.md com Novas Funcionalidades
- [ ] **Status:** Pendente
- **Arquivo:** `CLAUDE.md`
- **Descrição:** Documentar todas as melhorias implementadas
- **Tempo Estimado:** 20 min
- **Checklist:**
  - [ ] Adicionar seção sobre sistema de toast
  - [ ] Documentar atalhos de teclado
  - [ ] Explicar sistema de cache
  - [ ] Detalhar melhorias de performance
  - [ ] Adicionar exemplos de uso de PDF
  - [ ] Revisar português

---

### ✅ Tarefa 6.2: Criar Changelog das Melhorias
- [ ] **Status:** Pendente
- **Arquivo:** `CHANGELOG_MELHORIAS_ANOTACOES.md`
- **Descrição:** Documentar todas as mudanças realizadas
- **Tempo Estimado:** 15 min
- **Checklist:**
  - [ ] Listar todas as features adicionadas
  - [ ] Documentar melhorias de performance
  - [ ] Incluir breaking changes (se houver)
  - [ ] Adicionar screenshots/GIFs (opcional)
  - [ ] Revisar texto

---

### ✅ Tarefa 6.3: Verificação Final e Testes Integrados
- [ ] **Status:** Pendente
- **Descrição:** Teste completo de todas as funcionalidades implementadas
- **Tempo Estimado:** 45 min
- **Checklist:**
  - [ ] Testar exportação de PDF (individual e em massa)
  - [ ] Testar todos os toasts aparecem corretamente
  - [ ] Testar modal de preview
  - [ ] Testar filtros visuais
  - [ ] Testar atalhos de teclado
  - [ ] Testar busca expandida
  - [ ] Testar ações em massa
  - [ ] Verificar performance melhorou
  - [ ] Testar em tema claro e escuro
  - [ ] Testar responsividade mobile
  - [ ] Verificar console do navegador (sem erros)
  - [ ] Testar em diferentes navegadores (Chrome, Firefox, Edge)

---

### ✅ Tarefa 6.4: Commit e Push para GitHub
- [ ] **Status:** Pendente
- **Descrição:** Fazer commit final e push do branch
- **Tempo Estimado:** 10 min
- **Checklist:**
  - [ ] Revisar todos os arquivos modificados
  - [ ] Criar commit descritivo
  - [ ] Push do branch para o GitHub
  - [ ] Verificar CI/CD passou (se existir)
  - [ ] Marcar este arquivo de tarefas como 100% completo

---

## 📈 ESTIMATIVA TOTAL

**Tempo Total Estimado:** ~8.5 horas

**Distribuição por Fase:**
- Fase 1 (PDF): 58 min
- Fase 2 (Usabilidade): 87 min
- Fase 3 (Performance): 105 min
- Fase 4 (UI/UX): 95 min
- Fase 5 (Avançado): 110 min
- Fase 6 (Doc/Testes): 90 min

---

## 🔄 NOTAS DE DESENVOLVIMENTO

### Convenções de Código:
- **Comentários:** Sempre em português
- **JavaDoc:** Completo em todos os métodos públicos
- **Commits:** Mensagens descritivas em português
- **Testes:** Verificar manualmente cada funcionalidade após implementação

### Verificação de Código:
Após cada tarefa, verificar:
1. ✅ Código compila sem erros
2. ✅ Funcionalidade testada manualmente
3. ✅ Não quebrou funcionalidades existentes
4. ✅ Documentação adicionada
5. ✅ Console sem erros JavaScript
6. ✅ Funciona em tema claro e escuro

---

## 📝 LOG DE ALTERAÇÕES

### 02/12/2025 - Início do Projeto
- Branch `feature/melhorias-anotacoes` criado
- Arquivo de tarefas estruturado
- Pronto para iniciar implementação

---

**Última Atualização:** 02/12/2025
**Responsável:** Claude Code + Desenvolvedor
**Revisão:** Pendente
