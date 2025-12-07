# 📋 Checklist de Implementação: Sistema de Processos e Audiências

## Legenda
- ⬜ Não iniciado
- 🔄 Em andamento
- ✅ Concluído
- ⚠️ Bloqueado/Problema

---

## FASE 1: Modelagem de Dados (Backend)

### 1.1 Modelos e Enums
- ⬜ Criar `Processo.java` (8 campos)
- ⬜ Criar `StatusProcesso.java` enum (6 valores)
- ⬜ Criar `StatusIntimacao.java` enum (3 valores)
- ⬜ Criar `StatusOitiva.java` enum (3 valores)
- ⬜ Criar `ProcessoParticipante.java`
- ⬜ Criar `AudienciaParticipante.java` (10 campos)
- ⬜ Modificar `Audiencia.java` (remover 4 campos, adicionar 2)

### 1.2 Database Schema
- ⬜ Criar tabela `processo` (8 colunas + 3 índices)
- ⬜ Criar tabela `processo_participante` (5 colunas + 2 índices)
- ⬜ Criar tabela `audiencia_participante` (10 colunas + 3 índices)
- ⬜ Modificar tabela `audiencia` (adicionar coluna `processo_id`)
- ⬜ Modificar tabela `representacao_advogado` (trocar `audiencia_id` por `processo_id`)
- ⬜ Atualizar `DatabaseConfig.java` com novos schemas
- ⬜ Testar criação do banco de dados do zero

---

## FASE 2: Camada de Repositório

### 2.1 ProcessoRepository
- ⬜ Criar `ProcessoRepository.java`
- ⬜ Implementar `buscarTodos()` com JOIN Vara
- ⬜ Implementar `buscarPorId()` (eager load)
- ⬜ Implementar `buscarPorNumero()`
- ⬜ Implementar `buscarPorVara()`
- ⬜ Implementar `buscarPorStatus()`
- ⬜ Implementar `salvar()`
- ⬜ Implementar `atualizar()`
- ⬜ Implementar `deletar()`

### 2.2 ProcessoParticipanteRepository
- ⬜ Criar `ProcessoParticipanteRepository.java`
- ⬜ Implementar `buscarPorProcesso()` (eager load Pessoa)
- ⬜ Implementar `salvar()`
- ⬜ Implementar `deletar()`
- ⬜ Implementar `existeParticipacao()`

### 2.3 AudienciaParticipanteRepository
- ⬜ Criar `AudienciaParticipanteRepository.java`
- ⬜ Implementar `buscarPorAudiencia()` (eager load)
- ⬜ Implementar `salvar()`
- ⬜ Implementar `atualizar()`
- ⬜ Implementar `deletar()`
- ⬜ Implementar `buscarNaoIntimados()`
- ⬜ Implementar `buscarAguardandoIntimacao()`

### 2.4 Modificações em Repositórios Existentes
- ⬜ Modificar `AudienciaRepository` (eager load Processo)
- ⬜ Adicionar método `buscarPorProcesso()` em AudienciaRepository
- ⬜ Modificar `RepresentacaoAdvogadoRepository` (queries com processo_id)
- ⬜ Adicionar método `buscarPorProcesso()` em RepresentacaoAdvogadoRepository

---

## FASE 3: Camada de Serviço e DTOs

### 3.1 DTOs
- ⬜ Criar `ProcessoRequestDTO.java`
- ⬜ Criar `ProcessoDTO.java`
- ⬜ Criar `ProcessoParticipanteDTO.java`
- ⬜ Criar `AudienciaParticipanteDTO.java`
- ⬜ Criar `AudienciaAlertaDTO.java` (11 campos incluindo criticidade, cor, ícone)
- ⬜ Criar `AudienciaResumoDTO.java` (para lista dentro de ProcessoDTO)
- ⬜ Modificar `AudienciaRequestDTO` (adicionar processoId, remover 4 campos, campos opcionais)

### 3.2 ProcessoService
- ⬜ Criar `ProcessoService.java`
- ⬜ Implementar `listar()` → List<ProcessoDTO>
- ⬜ Implementar `buscarPorId()` → ProcessoDTO
- ⬜ Implementar `buscarAvancado(String termo)` - busca em número, vara, competência, artigo
- ⬜ Implementar `criar()` com validações
- ⬜ Implementar `atualizar()`
- ⬜ Implementar `deletar()`
- ⬜ Implementar `deletarMultiplos(List<Long>)` - exclusão em massa
- ⬜ Implementar `mudarStatusEmMassa(List<Long>, StatusProcesso)`
- ⬜ Implementar `adicionarParticipante()`
- ⬜ Implementar `removerParticipante()`
- ⬜ Implementar `listarParticipantes()`
- ⬜ Implementar validação: número processo único
- ⬜ Implementar validação: vara existe
- ⬜ Implementar validação: status válido
- ⬜ Implementar conversão entidade ↔ DTO

### 3.3 Modificações em AudienciaService
- ⬜ Adicionar validação de `processoId` existe
- ⬜ Remover validação de `numeroProcesso`
- ⬜ Tornar campos opcionais: Juiz, Promotor, Participantes, Tipo, Formato
- ⬜ Não bloquear salvamento por falta de informações
- ⬜ Implementar `vincularParticipantes()`
- ⬜ Implementar `atualizarStatusIntimacao()`
- ⬜ Implementar `atualizarStatusOitiva()`
- ⬜ Implementar `registrarDesistenciaOitiva()`
- ⬜ Implementar `registrarOitivaAnterior()`
- ⬜ Implementar `verificarPendenciasAudiencia()` → List<String> alertas
- ⬜ Implementar `buscarAudienciasComAlertas(int dias)` → List<AudienciaAlertaDTO>
- ⬜ Modificar verificação de conflito de horário (usar vara do processo)

---

## FASE 4: Camada de Controller (API)

### 4.1 ProcessoController
- ⬜ Criar `ProcessoController.java`
- ⬜ `GET /api/processos` - listar todos
- ⬜ `GET /api/processos/{id}` - buscar por ID
- ⬜ `GET /api/processos/buscar?q={termo}` - busca avançada (número, vara, competência, artigo)
- ⬜ `POST /api/processos` - criar
- ⬜ `PUT /api/processos/{id}` - atualizar
- ⬜ `DELETE /api/processos/{id}` - deletar um único
- ⬜ `POST /api/processos/deletar-multiplos` - deletar múltiplos (recebe List<Long>)
- ⬜ `PUT /api/processos/mudar-status` - mudar status em massa (List<Long> + novo status)
- ⬜ `POST /api/processos/pdf/resumo` - gerar PDF resumo (List<Long>)
- ⬜ `GET /api/processos/{id}/participantes` - listar participantes
- ⬜ `POST /api/processos/{id}/participantes` - adicionar participante
- ⬜ `DELETE /api/processos/{id}/participantes/{participanteId}` - remover
- ⬜ `GET /api/processos/{id}/audiencias` - listar audiências
- ⬜ Registrar rotas no `Main.java`

### 4.2 Modificações em AudienciaController
- ⬜ `POST /api/audiencias/{id}/participantes` - vincular participantes
- ⬜ `PUT /api/audiencias/participantes/{id}/intimacao` - atualizar status intimação
- ⬜ `PUT /api/audiencias/participantes/{id}/oitiva` - atualizar status oitiva
- ⬜ `POST /api/audiencias/participantes/{id}/desistencia` - registrar desistência
- ⬜ `POST /api/audiencias/participantes/{id}/oitiva-anterior` - registrar oitiva anterior
- ⬜ `GET /api/audiencias/{id}/alertas` - buscar alertas (retorna AudienciaAlertaDTO)
- ⬜ `GET /api/audiencias/alertas/proximas?dias={n}` - audiências com alertas
- ⬜ `GET /api/audiencias/buscar?q={termo}` - busca avançada (processo, vara, competência, juiz, promotor)
- ⬜ `POST /api/audiencias/deletar-multiplas` - deletar múltiplas (List<Long>)
- ⬜ `PUT /api/audiencias/mudar-status` - mudar status em massa (List<Long> + novo status)
- ⬜ `POST /api/audiencias/pdf/pauta` - gerar PDF de pauta (List<Long>)
- ⬜ Registrar novas rotas no `Main.java`

---

## FASE 5: Interface - Página de Processos

### 5.1 HTML Template
- ⬜ Criar `processos/index.html`
- ⬜ Implementar layout base com Thymeleaf
- ⬜ Campo de pesquisa textual com ícone 🔍 "Pesquisar por número, vara, competência, artigo..."
- ⬜ Filtros com badges (Vara=azul, Status=colorido, Competência=laranja) - multi-seleção
- ⬜ Tabela de processos:
  - ⬜ Coluna checkbox de seleção
  - ⬜ Checkbox "Selecionar todos" no cabeçalho
  - ⬜ Colunas: ☑️ | Número | Vara | Competência | Artigo | Status | Criado Em | Ações
- ⬜ Botão "⚙️ Configurar Colunas"
- ⬜ Controle de colunas visíveis
- ⬜ Cabeçalhos clicáveis para ordenação
- ⬜ Ícones de ordenação (⇅, ↑, ↓)
- ⬜ Botões de ação individual (Editar ✏️, Deletar 🗑️, Ver Audiências 📅)
- ⬜ Barra flutuante de ações em massa (aparece quando há selecionados):
  - ⬜ Texto "X processos selecionados"
  - ⬜ Botão "Imprimir Resumo (PDF)"
  - ⬜ Botão "Mudar Status" com dropdown
  - ⬜ Botão "Excluir Selecionados"
  - ⬜ Botão "Limpar Seleção"
- ⬜ Modal de novo processo
- ⬜ Modal de editar processo
- ⬜ Modal de confirmação de exclusão (individual e massa)
- ⬜ Modal de mudança de status em massa
- ⬜ Empty state (sem processos)
- ⬜ Toast notifications

### 5.2 JavaScript (Alpine.js)
- ⬜ Criar `processos.js`
- ⬜ Implementar `processosApp()` com Alpine.js
- ⬜ Estado: processos, processosProcessados, filtros, colunas visíveis, selecionados
- ⬜ Função `carregar()` - buscar processos da API
- ⬜ Função `pesquisar()` - busca avançada (número, vara, competência, artigo) com debounce 300ms
- ⬜ Função `filtrarPorBadge()` - filtros de vara/status/competência (multi-seleção)
- ⬜ Função `ordenarPor(coluna)` - alternar asc/desc
- ⬜ Computed property `processosProcessados` (filtragem + ordenação)
- ⬜ Função `selecionarTodos()` - toggle checkbox de seleção
- ⬜ Função `toggleSelecao(processoId)` - selecionar/desselecionar individual
- ⬜ Computed property `temSelecionados` - verifica se há seleção
- ⬜ Função `abrirFormulario()`
- ⬜ Função `salvar()` - POST ou PUT
- ⬜ Função `deletar()` - DELETE com confirmação
- ⬜ Função `deletarSelecionados()` - DELETE em massa com confirmação
- ⬜ Função `mudarStatusSelecionados(novoStatus)` - PUT em massa
- ⬜ Função `gerarPDFResumo()` - POST /api/processos/pdf/resumo
- ⬜ Função `limparSelecao()`
- ⬜ Função `mostrarToast(tipo, titulo, mensagem)`
- ⬜ Função `mostrarLoading()` e `ocultarLoading()`
- ⬜ Integração com toast notifications
- ⬜ Loading states em todas operações assíncronas

### 5.3 CSS
- ⬜ Criar seção de estilos em `processos.css` ou usar `audiencias.css`
- ⬜ Estilos para badges de filtro (cores: vara=azul, status=variável, competência=laranja)
- ⬜ Estilos para tabela (seguir padrão de notas)
- ⬜ Estilos para controle de colunas
- ⬜ Estilos para ícones de ordenação
- ⬜ Estilos para modais
- ⬜ Estilos para toast notifications (se não existir)
- ⬜ Estilos para loading overlay
- ⬜ Responsividade mobile

---

## FASE 6: Interface - Detalhes do Processo

### 6.1 HTML Template
- ⬜ Criar `processos/detalhes.html`
- ⬜ Seção: Dados do Processo (card com informações)
- ⬜ Seção: Participantes (tabela)
- ⬜ Tabela de participantes (nome, CPF, tipo, ações)
- ⬜ Botão "Adicionar Participante"
- ⬜ Modal de adicionar participante (buscar pessoa ou criar)
- ⬜ Modal de editar participante
- ⬜ Botão remover participante
- ⬜ Seção: Advogados (tabela)
- ⬜ Tabela de representações (advogado, cliente)
- ⬜ Botão "Adicionar Representação"
- ⬜ Modal de adicionar representação
- ⬜ Seção: Audiências do Processo (tabela)
- ⬜ Tabela de audiências (data, hora, tipo, status)
- ⬜ Botão "Nova Audiência" (redireciona para form com processo pré-selecionado)
- ⬜ Links para editar cada audiência
- ⬜ Botão "Editar Processo"
- ⬜ Botão "Excluir Processo"
- ⬜ Dropdown "Mudar Status"

### 6.2 JavaScript
- ⬜ Criar `processoDetalhes.js`
- ⬜ Função `carregarProcesso(id)` - GET /api/processos/{id}
- ⬜ Função `carregarParticipantes()`
- ⬜ Função `adicionarParticipante()`
- ⬜ Função `removerParticipante(id)`
- ⬜ Função `carregarRepresentacoes()`
- ⬜ Função `adicionarRepresentacao()`
- ⬜ Função `carregarAudiencias()`
- ⬜ Função `mudarStatus(novoStatus)`
- ⬜ Função `editarProcesso()`
- ⬜ Função `excluirProcesso()` com confirmação
- ⬜ Toast notifications
- ⬜ Loading states

---

## FASE 7: Interface - Modificar Audiências

### 7.1 Modificar `audiencias/index.html`
- ⬜ Campo de pesquisa avançada: "Pesquisar por processo, vara, competência, juiz, promotor, tipo..."
- ⬜ Tabela de Audiências:
  - ⬜ Coluna checkbox de seleção
  - ⬜ Checkbox "Selecionar todos" no cabeçalho
  - ⬜ Colunas: ☑️ | Alertas | Processo | Data | Horário | Vara | Competência | Juiz | Promotor | Tipo | Status | Ações
- ⬜ Coluna "Alertas" (nova):
  - ⬜ Badge com ícone de criticidade (🔴/🟠/🟡)
  - ⬜ Tooltip com detalhes: "Falta: Juiz, 2 não intimados (em 5 dias)"
  - ⬜ Contador de pendências no badge
- ⬜ Filtros com badges (multi-seleção):
  - ⬜ Por Processo (badges azuis)
  - ⬜ Por Vara (badges verdes)
  - ⬜ Por Competência (badges laranjas)
  - ⬜ Por Status (badges coloridos)
  - ⬜ Por Tipo (badges roxos)
  - ⬜ Por Criticidade (badges: vermelho/laranja/amarelo)
- ⬜ Widget de alertas (card flutuante):
  - ⬜ Título: "Audiências com Pendências"
  - ⬜ Abas: "Próximos 3 dias" | "Próximos 7 dias" | "Próximos 15 dias"
  - ⬜ Lista de alertas agrupados por criticidade
  - ⬜ Itens clicáveis (redireciona para audiência)
  - ⬜ Detalhes: "Processo X - Audiência 15/12 - Falta: Juiz, 2 não intimados"
- ⬜ Barra flutuante de ações em massa (aparece quando há selecionados):
  - ⬜ Texto "X audiências selecionadas"
  - ⬜ Botão "Imprimir Pauta (PDF)"
  - ⬜ Botão "Mudar Status" com dropdown
  - ⬜ Botão "Excluir Selecionadas"
  - ⬜ Botão "Limpar Seleção"
- ⬜ Controle de colunas visíveis (⚙️)
- ⬜ Ordenação em todas as colunas

### 7.2 Modificar `audiencias.js`
- ⬜ Estado: adicionar `alertas`, `selecionados`, `abaAtiva`
- ⬜ Função `carregarAlertas(dias)` - GET /api/audiencias/alertas/proximas
- ⬜ Função `pesquisarAvancada()` - buscar em processo, vara, competência, juiz, promotor (debounce 300ms)
- ⬜ Computed property para contadores de alertas (por criticidade)
- ⬜ Modificar `audienciasProcessadas` para incluir dados de alertas
- ⬜ Função `calcularAlerta(audiencia)` - retorna objeto com criticidade, informações faltantes
- ⬜ Função `selecionarTodos()` - toggle checkbox
- ⬜ Função `toggleSelecao(audienciaId)`
- ⬜ Computed property `temSelecionados`
- ⬜ Função `deletarSelecionadas()` - DELETE em massa
- ⬜ Função `mudarStatusSelecionadas(novoStatus)` - PUT em massa
- ⬜ Função `gerarPDFPauta()` - POST /api/audiencias/pdf/pauta
- ⬜ Função `limparSelecao()`
- ⬜ Adicionar filtros por processo (multi-seleção)
- ⬜ Adicionar filtros por criticidade
- ⬜ Renderizar widget de alertas com abas
- ⬜ Implementar tooltip com detalhes dos alertas

---

## FASE 8: Interface - Modificar Formulário de Audiência

### 8.1 Modificar `audiencias/form.html`

#### Seção 1: Dados do Processo
- ⬜ Adicionar dropdown "Selecionar Processo"
- ⬜ Carregar lista de processos na API
- ⬜ Ao selecionar processo, carregar dados (vara, competência, artigo, status)
- ⬜ Exibir campos readonly: Vara, Competência, Artigo, Status
- ⬜ Remover campos antigos do form (número processo, vara, competência, artigo)

#### Seção 2: Dados da Audiência
- ⬜ Manter campos existentes (data, horário, duração, tipo, formato, etc.)
- ⬜ Garantir que validações funcionem
- ⬜ Integrar com processo selecionado

#### Seção 3: Participantes da Audiência (NOVA SEÇÃO)
- ⬜ Criar tabela de participantes
- ⬜ Colunas: Participante, Tipo (badge), Status Intimação (dropdown), Status Oitiva (dropdown), Ações
- ⬜ Dropdown Status Intimação:
  - ✅ Intimada (verde)
  - ⏳ Aguardando (amarelo)
  - ❌ Não Intimada (vermelho)
- ⬜ Dropdown Status Oitiva:
  - ⏳ Aguardando (azul)
  - ❌ Desistência (cinza)
  - ✅ Já Ouvida (verde)
- ⬜ Modal para "Desistência de Oitiva":
  - Campo texto: "Quem desistiu?" (Defesa/MP/Ambos)
  - Campo observações
  - Botão Salvar/Cancelar
- ⬜ Modal para "Oitiva Anterior":
  - Campo data: "Quando foi ouvida?"
  - Campo observações: "Detalhes"
  - Botão Salvar/Cancelar
- ⬜ Botão "⚙️" para editar detalhes de participante
- ⬜ Botão "+ Adicionar Participante" (buscar da lista do processo)
- ⬜ Modal de seleção de participante (checkboxes)
- ⬜ Botão "Remover Selecionados"

#### Alertas em Tempo Real
- ⬜ Exibir alerta acima da tabela: "🔴 X pessoas não intimadas"
- ⬜ Exibir alerta: "🟡 X pessoas aguardando intimação"
- ⬜ Atualizar alertas conforme usuário muda dropdowns (reativo)
- ⬜ Calcular alertas automaticamente

### 8.2 Modificar JavaScript da Audiência
- ⬜ Função `carregarProcessos()` - para dropdown
- ⬜ Função `selecionarProcesso(processoId)` - carregar dados + participantes
- ⬜ Função `carregarParticipantesDoProcesso(processoId)`
- ⬜ Estado `participantes` (array de AudienciaParticipanteDTO)
- ⬜ Função `adicionarParticipante()` - modal de seleção
- ⬜ Função `removerParticipantes()` - remover selecionados
- ⬜ Função `atualizarStatusIntimacao(participanteId, status)` - PUT API
- ⬜ Função `atualizarStatusOitiva(participanteId, status)` - PUT API
- ⬜ Função `abrirModalDesistencia(participanteId)`
- ⬜ Função `salvarDesistencia()` - POST /api/audiencias/participantes/{id}/desistencia
- ⬜ Função `abrirModalOitivaAnterior(participanteId)`
- ⬜ Função `salvarOitivaAnterior()` - POST /api/audiencias/participantes/{id}/oitiva-anterior
- ⬜ Computed property `alertasParticipantes` - calcular em tempo real
- ⬜ Watch em dropdowns para atualizar alertas
- ⬜ Validação: não salvar audiência se faltam informações críticas

---

## FASE 9: Interface - Dashboard e Sidebar

### 9.1 Modificar Sidebar
- ⬜ Adicionar link "Processos" em `layout/sidebar.html`
- ⬜ Ícone: 📁 (pasta/documento)
- ⬜ Ordem: Dashboard → Anotações → **Processos** → Audiências → Pessoas
- ⬜ Verificar active state no link

### 9.2 Modificar Dashboard
- ⬜ Criar widget "Audiências com Pendências"
- ⬜ Card com título "🔔 Audiências com Pendências"
- ⬜ Contadores:
  - Badge vermelho: "X audiências com pessoas não intimadas"
  - Badge amarelo: "X audiências aguardando intimação"
- ⬜ Lista de alertas (próximas audiências com pendências)
- ⬜ Links clicáveis para cada audiência
- ⬜ Função `carregarAlertasAudiencias()` no dashboard.js
- ⬜ Integração com API `/api/audiencias/alertas/proximas?dias=30`
- ⬜ Estilos do widget (seguir padrão de alertas de notas)

---

## FASE 10: Testes e Validações

### 10.1 Testes de Repositório
- ⬜ Testar criação de Processo
- ⬜ Testar busca por ID, número, vara, status
- ⬜ Testar cascade delete (deletar processo deleta audiências)
- ⬜ Testar unicidade de número do processo
- ⬜ Testar criação de ProcessoParticipante
- ⬜ Testar criação de AudienciaParticipante
- ⬜ Testar atualização de status de intimação
- ⬜ Testar atualização de status de oitiva

### 10.2 Testes de Serviço
- ⬜ Testar validação de número processo único
- ⬜ Testar validação de vara existente
- ⬜ Testar criação de processo com participantes
- ⬜ Testar criação de audiência vinculada a processo
- ⬜ Testar verificação de pendências de audiência
- ⬜ Testar busca de audiências com alertas
- ⬜ Testar registro de desistência de oitiva
- ⬜ Testar registro de oitiva anterior

### 10.3 Testes de API
- ⬜ Testar todos os endpoints de ProcessoController
- ⬜ Testar todos os novos endpoints de AudienciaController
- ⬜ Testar validações de request (campos obrigatórios)
- ⬜ Testar respostas de erro (400, 404, 500)
- ⬜ Testar formato JSON das respostas
- ⬜ Testar paginação (se implementada)

### 10.4 Testes de UI
- ⬜ Testar workflow completo: Criar Processo → Adicionar Participantes → Criar Audiência
- ⬜ Testar filtros de processos (vara, status, competência)
- ⬜ Testar pesquisa textual em processos
- ⬜ Testar ordenação de colunas em processos
- ⬜ Testar controle de colunas visíveis
- ⬜ Testar criação/edição/exclusão de processo
- ⬜ Testar página de detalhes do processo
- ⬜ Testar adição/remoção de participantes
- ⬜ Testar adição de representação de advogado
- ⬜ Testar criação de audiência com processo pré-selecionado
- ⬜ Testar dropdowns de status de intimação
- ⬜ Testar dropdowns de status de oitiva
- ⬜ Testar modal de desistência de oitiva
- ⬜ Testar modal de oitiva anterior
- ⬜ Testar alertas em tempo real no formulário
- ⬜ Testar widget de alertas na listagem de audiências
- ⬜ Testar widget de alertas no dashboard
- ⬜ Testar toast notifications em todas as operações
- ⬜ Testar loading states
- ⬜ Testar responsividade mobile
- ⬜ Testar navegação entre páginas

### 10.5 Testes de Integridade
- ⬜ Verificar integridade referencial (FKs funcionando)
- ⬜ Testar cascade delete em todas as relações
- ⬜ Testar constraints UNIQUE
- ⬜ Verificar indexes (performance de queries)
- ⬜ Testar transações (rollback em caso de erro)

---

## FASE 11: Refinamentos e Polimento

### 11.1 Performance
- ⬜ Otimizar queries (evitar N+1)
- ⬜ Adicionar índices faltantes (se necessário)
- ⬜ Testar performance com muitos processos/audiências
- ⬜ Implementar paginação (se necessário)
- ⬜ Cache de dados estáticos (varas, status, etc.)

### 11.2 UX
- ⬜ Verificar feedback visual em todas as ações
- ⬜ Adicionar animações suaves (transições)
- ⬜ Verificar acessibilidade (ARIA labels)
- ⬜ Testar atalhos de teclado (se implementados)
- ⬜ Verificar mensagens de erro (claras e úteis)
- ⬜ Verificar tooltips e hints
- ⬜ Garantir consistência visual com resto da app

### 11.3 Documentação
- ⬜ Documentar JavaDoc em todos os métodos públicos
- ⬜ Documentar JSDoc em funções JavaScript
- ⬜ Atualizar CLAUDE.md com novo sistema
- ⬜ Criar exemplos de uso da API
- ⬜ Documentar fluxos de trabalho (workflow)

### 11.4 Segurança
- ⬜ Verificar autorização em todos os endpoints
- ⬜ Validar inputs (sanitização)
- ⬜ Verificar SQL injection (PreparedStatements)
- ⬜ Verificar XSS (escapar outputs)
- ⬜ Verificar CSRF (se aplicável)

---

## FASE 12: Preparação para Merge

### 12.1 Code Review
- ⬜ Revisar todo o código implementado
- ⬜ Verificar convenções de código (Java, JS, HTML, CSS)
- ⬜ Remover código comentado ou debug
- ⬜ Verificar nomenclatura (variáveis, funções, classes)
- ⬜ Verificar consistência de formatação

### 12.2 Git
- ⬜ Fazer commits atômicos durante desenvolvimento
- ⬜ Escrever mensagens de commit descritivas
- ⬜ Squash commits relacionados (opcional)
- ⬜ Verificar que não há arquivos sensíveis commitados
- ⬜ Atualizar .gitignore se necessário

### 12.3 Testes Finais
- ⬜ Rodar todos os testes
- ⬜ Testar em ambiente limpo (banco zerado)
- ⬜ Testar em diferentes navegadores
- ⬜ Testar em diferentes resoluções
- ⬜ Verificar console do navegador (sem erros)
- ⬜ Verificar logs do servidor (sem warnings críticos)

### 12.4 Merge para Main
- ⬜ Atualizar branch com últimas mudanças do main
- ⬜ Resolver conflitos (se houver)
- ⬜ Fazer merge final
- ⬜ Push para remote
- ⬜ Criar Pull Request (se aplicável)
- ⬜ Adicionar descrição detalhada das mudanças
- ⬜ Adicionar screenshots (se aplicável)

---

## RESUMO DO PROGRESSO

**Total de Tarefas**: 295
**Concluídas**: 0
**Em Andamento**: 0
**Bloqueadas**: 0
**Progresso**: 0%

---

## NOTAS E OBSERVAÇÕES

### Decisões Técnicas:
- Usar formato brasileiro para datas/timestamps (padrão do sistema)
- Não incluir campo `dataAbertura` em Processo
- Seguir exatamente os padrões visuais de Anotações
- Usar badges coloridos para filtros (multi-seleção)
- Implementar alertas em tempo real com escala de criticidade
- Checkboxes para seleção múltipla em todas as tabelas
- Pesquisa avançada em múltiplos campos
- Campos opcionais na criação de audiências (não bloquear salvamento)
- Sistema de alertas detalhado (especificar o que falta)
- Ações em massa: deletar, mudar status, gerar PDF

### Prioridades:
1. Backend (Modelos, Repositórios, Serviços, Controllers)
2. UI de Processos (listagem e detalhes)
3. Modificações na UI de Audiências
4. Sistema de alertas
5. Dashboard e refinamentos

### Riscos Identificados:
- Complexidade da tabela de participantes (muitos campos e interações)
- Sistema de alertas com criticidade (lógica complexa)
- Cálculo de alertas em tempo real (reatividade Alpine.js)
- Performance com muitas audiências/processos selecionados
- Integridade referencial em cascata
- Geração de PDF em massa (pode ser lenta)
- Pesquisa avançada em múltiplos campos (performance)

### Próximos Passos:
1. Criar branch `feature/sistema-processos`
2. Iniciar Fase 1 (Modelagem de Dados)
3. Seguir ordem sequencial das fases
4. Testar continuamente durante desenvolvimento
