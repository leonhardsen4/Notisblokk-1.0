-- ============================================================================
-- SCHEMA SQL PARA O MÓDULO DE AUDIÊNCIAS JUDICIAIS
-- Notisblokk 1.0 - Sistema de Gestão de Audiências baseado no TJSP
-- ============================================================================
--
-- IMPORTANTE: Este schema segue as decisões técnicas definidas no PROGRESS.md:
-- - SQLite usa TEXT para datas (formato: dd/MM/yyyy)
-- - SQLite usa TEXT para horários (formato: HH:mm:ss)
-- - SQLite usa INTEGER para boolean (0=false, 1=true)
-- - Sempre executar PRAGMA foreign_keys = ON ao conectar
--
-- Total de tabelas: 8
-- Última atualização: 01/11/2025 16:00
-- ============================================================================

-- Habilitar foreign keys (CRÍTICO - SEMPRE EXECUTAR AO CONECTAR)
PRAGMA foreign_keys = ON;

-- ============================================================================
-- TABELA: VARA
-- Descrição: Varas judiciais onde as audiências são realizadas
-- ============================================================================
CREATE TABLE IF NOT EXISTS vara (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    comarca TEXT,
    endereco TEXT,
    telefone TEXT,
    email TEXT,
    observacoes TEXT,

    -- Validações
    CONSTRAINT chk_vara_nome_nao_vazio CHECK (length(trim(nome)) > 0)
);

-- ============================================================================
-- TABELA: JUIZ
-- Descrição: Magistrados responsáveis pelas audiências
-- ============================================================================
CREATE TABLE IF NOT EXISTS juiz (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    telefone TEXT,
    email TEXT,
    observacoes TEXT,

    -- Validações
    CONSTRAINT chk_juiz_nome_nao_vazio CHECK (length(trim(nome)) > 0)
);

-- ============================================================================
-- TABELA: PROMOTOR
-- Descrição: Promotores de Justiça que atuam nas audiências
-- ============================================================================
CREATE TABLE IF NOT EXISTS promotor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    telefone TEXT,
    email TEXT,
    observacoes TEXT,

    -- Validações
    CONSTRAINT chk_promotor_nome_nao_vazio CHECK (length(trim(nome)) > 0)
);

-- ============================================================================
-- TABELA: ADVOGADO
-- Descrição: Advogados que representam as partes nas audiências
-- ============================================================================
CREATE TABLE IF NOT EXISTS advogado (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    oab TEXT NOT NULL,
    telefone TEXT,
    email TEXT,
    observacoes TEXT,

    -- Validações
    CONSTRAINT chk_advogado_nome_nao_vazio CHECK (length(trim(nome)) > 0),
    CONSTRAINT chk_advogado_oab_nao_vazio CHECK (length(trim(oab)) > 0)
);

-- ============================================================================
-- TABELA: PESSOA
-- Descrição: Pessoas que participam das audiências (partes, testemunhas, etc.)
-- ============================================================================
CREATE TABLE IF NOT EXISTS pessoa (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    cpf TEXT,
    telefone TEXT,
    email TEXT,
    observacoes TEXT,

    -- Validações
    CONSTRAINT chk_pessoa_nome_nao_vazio CHECK (length(trim(nome)) > 0)
);

-- ============================================================================
-- TABELA: AUDIENCIA (PRINCIPAL)
-- Descrição: Audiências judiciais agendadas
-- ============================================================================
CREATE TABLE IF NOT EXISTS audiencia (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Dados básicos
    numero_processo TEXT NOT NULL,
    vara_id INTEGER NOT NULL,

    -- Data e horário (ATENÇÃO: formato brasileiro dd/MM/yyyy e HH:mm:ss)
    data_audiencia TEXT NOT NULL,      -- Formato: dd/MM/yyyy
    horario_inicio TEXT NOT NULL,      -- Formato: HH:mm:ss
    duracao INTEGER NOT NULL,          -- Duração em minutos
    horario_fim TEXT,                  -- Formato: HH:mm:ss (calculado)
    dia_semana TEXT,                   -- Ex: "Segunda-feira" (calculado)

    -- Tipo e formato
    tipo_audiencia TEXT NOT NULL,      -- Enum: TipoAudiencia
    formato TEXT NOT NULL,             -- Enum: FormatoAudiencia
    competencia TEXT NOT NULL,         -- Enum: Competencia
    status TEXT NOT NULL,              -- Enum: StatusAudiencia

    -- Informações adicionais
    artigo TEXT,
    observacoes TEXT,

    -- Flags especiais (0=false, 1=true)
    reu_preso INTEGER DEFAULT 0,
    agendamento_teams INTEGER DEFAULT 0,
    reconhecimento INTEGER DEFAULT 0,
    depoimento_especial INTEGER DEFAULT 0,

    -- Responsáveis (opcionais)
    juiz_id INTEGER,
    promotor_id INTEGER,

    -- Auditoria
    criacao TEXT DEFAULT (datetime('now', 'localtime')),
    atualizacao TEXT DEFAULT (datetime('now', 'localtime')),

    -- Foreign Keys
    FOREIGN KEY (vara_id) REFERENCES vara(id) ON DELETE RESTRICT,
    FOREIGN KEY (juiz_id) REFERENCES juiz(id) ON DELETE SET NULL,
    FOREIGN KEY (promotor_id) REFERENCES promotor(id) ON DELETE SET NULL,

    -- Validações
    CONSTRAINT chk_audiencia_numero_processo CHECK (length(trim(numero_processo)) > 0),
    CONSTRAINT chk_audiencia_duracao CHECK (duracao >= 15),
    CONSTRAINT chk_audiencia_reu_preso CHECK (reu_preso IN (0, 1)),
    CONSTRAINT chk_audiencia_teams CHECK (agendamento_teams IN (0, 1)),
    CONSTRAINT chk_audiencia_reconhecimento CHECK (reconhecimento IN (0, 1)),
    CONSTRAINT chk_audiencia_depoimento CHECK (depoimento_especial IN (0, 1))
);

-- ============================================================================
-- TABELA: PARTICIPACAO_AUDIENCIA
-- Descrição: Relacionamento entre pessoas e audiências
-- ============================================================================
CREATE TABLE IF NOT EXISTS participacao_audiencia (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    audiencia_id INTEGER NOT NULL,
    pessoa_id INTEGER NOT NULL,
    tipo TEXT NOT NULL,                -- Enum: TipoParticipacao
    intimado INTEGER DEFAULT 0,        -- 0=false, 1=true
    observacoes TEXT,

    -- Foreign Keys
    FOREIGN KEY (audiencia_id) REFERENCES audiencia(id) ON DELETE CASCADE,
    FOREIGN KEY (pessoa_id) REFERENCES pessoa(id) ON DELETE RESTRICT,

    -- Validações
    CONSTRAINT chk_participacao_intimado CHECK (intimado IN (0, 1))
);

-- ============================================================================
-- TABELA: REPRESENTACAO_ADVOGADO
-- Descrição: Relacionamento entre advogados, clientes e audiências
-- ============================================================================
CREATE TABLE IF NOT EXISTS representacao_advogado (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    audiencia_id INTEGER NOT NULL,
    advogado_id INTEGER NOT NULL,
    cliente_id INTEGER NOT NULL,       -- Pessoa sendo representada
    tipo TEXT NOT NULL,                -- Enum: TipoRepresentacao

    -- Foreign Keys
    FOREIGN KEY (audiencia_id) REFERENCES audiencia(id) ON DELETE CASCADE,
    FOREIGN KEY (advogado_id) REFERENCES advogado(id) ON DELETE RESTRICT,
    FOREIGN KEY (cliente_id) REFERENCES pessoa(id) ON DELETE RESTRICT
);

-- ============================================================================
-- ÍNDICES PARA PERFORMANCE
-- ============================================================================

-- Índices principais da tabela AUDIENCIA
CREATE INDEX IF NOT EXISTS idx_audiencia_data
    ON audiencia(data_audiencia);

CREATE INDEX IF NOT EXISTS idx_audiencia_vara
    ON audiencia(vara_id);

CREATE INDEX IF NOT EXISTS idx_audiencia_status
    ON audiencia(status);

CREATE INDEX IF NOT EXISTS idx_audiencia_processo
    ON audiencia(numero_processo);

CREATE INDEX IF NOT EXISTS idx_audiencia_juiz
    ON audiencia(juiz_id);

CREATE INDEX IF NOT EXISTS idx_audiencia_promotor
    ON audiencia(promotor_id);

-- Índice composto para busca de conflitos de horário
CREATE INDEX IF NOT EXISTS idx_audiencia_data_vara_horario
    ON audiencia(data_audiencia, vara_id, horario_inicio);

-- Índices da tabela PARTICIPACAO_AUDIENCIA
CREATE INDEX IF NOT EXISTS idx_participacao_audiencia
    ON participacao_audiencia(audiencia_id);

CREATE INDEX IF NOT EXISTS idx_participacao_pessoa
    ON participacao_audiencia(pessoa_id);

CREATE INDEX IF NOT EXISTS idx_participacao_tipo
    ON participacao_audiencia(tipo);

-- Índices da tabela REPRESENTACAO_ADVOGADO
CREATE INDEX IF NOT EXISTS idx_representacao_audiencia
    ON representacao_advogado(audiencia_id);

CREATE INDEX IF NOT EXISTS idx_representacao_advogado
    ON representacao_advogado(advogado_id);

CREATE INDEX IF NOT EXISTS idx_representacao_cliente
    ON representacao_advogado(cliente_id);

-- Índices para busca por nome
CREATE INDEX IF NOT EXISTS idx_vara_nome
    ON vara(nome);

CREATE INDEX IF NOT EXISTS idx_juiz_nome
    ON juiz(nome);

CREATE INDEX IF NOT EXISTS idx_promotor_nome
    ON promotor(nome);

CREATE INDEX IF NOT EXISTS idx_advogado_nome
    ON advogado(nome);

CREATE INDEX IF NOT EXISTS idx_advogado_oab
    ON advogado(oab);

CREATE INDEX IF NOT EXISTS idx_pessoa_nome
    ON pessoa(nome);

CREATE INDEX IF NOT EXISTS idx_pessoa_cpf
    ON pessoa(cpf);

-- ============================================================================
-- TRIGGERS PARA AUDITORIA
-- ============================================================================

-- Trigger para atualizar campo 'atualizacao' automaticamente
CREATE TRIGGER IF NOT EXISTS trg_audiencia_atualizacao
AFTER UPDATE ON audiencia
FOR EACH ROW
BEGIN
    UPDATE audiencia
    SET atualizacao = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

-- ============================================================================
-- TABELA: PROCESSO (NOVA - ENTIDADE CENTRAL)
-- Descrição: Processo judicial, entidade central que agrega audiências
-- ============================================================================
CREATE TABLE IF NOT EXISTS processo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero_processo TEXT NOT NULL UNIQUE,
    competencia TEXT NOT NULL,         -- Enum: Competencia
    artigo TEXT,
    vara_id INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'EM_ANDAMENTO',  -- Enum: StatusProcesso
    observacoes TEXT,

    -- Auditoria
    criado_em TEXT DEFAULT (datetime('now', 'localtime')),
    atualizado_em TEXT DEFAULT (datetime('now', 'localtime')),

    -- Foreign Keys
    FOREIGN KEY (vara_id) REFERENCES vara(id) ON DELETE RESTRICT,

    -- Validações
    CONSTRAINT chk_processo_numero CHECK (length(trim(numero_processo)) > 0)
);

-- ============================================================================
-- TABELA: PROCESSO_PARTICIPANTE
-- Descrição: Participantes vinculados ao processo (não à audiência individual)
-- ============================================================================
CREATE TABLE IF NOT EXISTS processo_participante (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    processo_id INTEGER NOT NULL,
    pessoa_id INTEGER NOT NULL,
    tipo_participacao TEXT NOT NULL,   -- Enum: TipoParticipacao
    observacoes TEXT,
    criado_em TEXT DEFAULT (datetime('now', 'localtime')),

    -- Foreign Keys
    FOREIGN KEY (processo_id) REFERENCES processo(id) ON DELETE CASCADE,
    FOREIGN KEY (pessoa_id) REFERENCES pessoa(id) ON DELETE CASCADE,

    -- Prevenir duplicatas
    UNIQUE(processo_id, pessoa_id, tipo_participacao)
);

-- ============================================================================
-- TABELA: AUDIENCIA_PARTICIPANTE
-- Descrição: Participantes do processo selecionados para audiência específica
--            com controle de intimação e oitiva
-- ============================================================================
CREATE TABLE IF NOT EXISTS audiencia_participante (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    audiencia_id INTEGER NOT NULL,
    processo_participante_id INTEGER NOT NULL,

    -- Status de controle
    status_intimacao TEXT NOT NULL DEFAULT 'NAO_INTIMADA',  -- Enum: StatusIntimacao
    status_oitiva TEXT NOT NULL DEFAULT 'AGUARDANDO',       -- Enum: StatusOitiva

    -- Informações sobre desistência
    observacoes_desistencia TEXT,      -- Ex: "Desistência da Defesa", "Desistência do MP"

    -- Informações sobre oitiva anterior
    data_oitiva_anterior TEXT,         -- Formato: dd/MM/yyyy
    observacoes_oitiva TEXT,           -- Detalhes sobre oitiva anterior

    -- Presença e observações
    presente INTEGER DEFAULT 0,
    observacoes TEXT,

    -- Auditoria
    criado_em TEXT DEFAULT (datetime('now', 'localtime')),
    atualizado_em TEXT DEFAULT (datetime('now', 'localtime')),

    -- Foreign Keys
    FOREIGN KEY (audiencia_id) REFERENCES audiencia(id) ON DELETE CASCADE,
    FOREIGN KEY (processo_participante_id) REFERENCES processo_participante(id) ON DELETE CASCADE,

    -- Prevenir duplicatas
    UNIQUE(audiencia_id, processo_participante_id),

    -- Validações
    CONSTRAINT chk_aud_part_presente CHECK (presente IN (0, 1))
);

-- ============================================================================
-- MIGRAÇÃO: Adicionar processo_id à tabela AUDIENCIA
-- Remover campos que agora pertencem ao Processo
-- ============================================================================
-- IMPORTANTE: Esta migração será aplicada apenas uma vez
-- Tabela audiencia agora referencia processo ao invés de duplicar dados

-- Adicionar coluna processo_id (temporariamente NULL para compatibilidade)
ALTER TABLE audiencia ADD COLUMN processo_id INTEGER;

-- Criar índice para processo_id
CREATE INDEX IF NOT EXISTS idx_audiencia_processo_id ON audiencia(processo_id);

-- ============================================================================
-- MIGRAÇÃO: Modificar REPRESENTACAO_ADVOGADO para referenciar PROCESSO
-- ============================================================================
-- Advogados representam clientes no processo todo, não em audiências individuais

-- Criar nova tabela com estrutura correta
CREATE TABLE IF NOT EXISTS representacao_advogado_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    processo_id INTEGER NOT NULL,
    advogado_id INTEGER NOT NULL,
    cliente_id INTEGER NOT NULL,       -- Pessoa sendo representada
    tipo TEXT NOT NULL,                -- Enum: TipoRepresentacao

    -- Foreign Keys
    FOREIGN KEY (processo_id) REFERENCES processo(id) ON DELETE CASCADE,
    FOREIGN KEY (advogado_id) REFERENCES advogado(id) ON DELETE RESTRICT,
    FOREIGN KEY (cliente_id) REFERENCES pessoa(id) ON DELETE RESTRICT
);

-- Copiar dados existentes (se houver) - será feito em migração futura
-- Por enquanto, deixar tabela nova vazia

-- ============================================================================
-- ÍNDICES PARA AS NOVAS TABELAS
-- ============================================================================

-- Índices da tabela PROCESSO
CREATE INDEX IF NOT EXISTS idx_processo_numero ON processo(numero_processo);
CREATE INDEX IF NOT EXISTS idx_processo_vara ON processo(vara_id);
CREATE INDEX IF NOT EXISTS idx_processo_status ON processo(status);

-- Índices da tabela PROCESSO_PARTICIPANTE
CREATE INDEX IF NOT EXISTS idx_proc_part_processo ON processo_participante(processo_id);
CREATE INDEX IF NOT EXISTS idx_proc_part_pessoa ON processo_participante(pessoa_id);

-- Índices da tabela AUDIENCIA_PARTICIPANTE
CREATE INDEX IF NOT EXISTS idx_aud_part_audiencia ON audiencia_participante(audiencia_id);
CREATE INDEX IF NOT EXISTS idx_aud_part_intimacao ON audiencia_participante(status_intimacao);
CREATE INDEX IF NOT EXISTS idx_aud_part_oitiva ON audiencia_participante(status_oitiva);

-- Índices da nova tabela REPRESENTACAO_ADVOGADO
CREATE INDEX IF NOT EXISTS idx_repr_new_processo ON representacao_advogado_new(processo_id);
CREATE INDEX IF NOT EXISTS idx_repr_new_advogado ON representacao_advogado_new(advogado_id);
CREATE INDEX IF NOT EXISTS idx_repr_new_cliente ON representacao_advogado_new(cliente_id);

-- ============================================================================
-- TRIGGERS PARA AUDITORIA DAS NOVAS TABELAS
-- ============================================================================

-- Trigger para atualizar campo 'atualizado_em' em processo
CREATE TRIGGER IF NOT EXISTS trg_processo_atualizacao
AFTER UPDATE ON processo
FOR EACH ROW
BEGIN
    UPDATE processo
    SET atualizado_em = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

-- Trigger para atualizar campo 'atualizado_em' em audiencia_participante
CREATE TRIGGER IF NOT EXISTS trg_aud_part_atualizacao
AFTER UPDATE ON audiencia_participante
FOR EACH ROW
BEGIN
    UPDATE audiencia_participante
    SET atualizado_em = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

-- ============================================================================
-- FIM DO SCHEMA
-- ============================================================================
-- Total de tabelas criadas: 11 (8 antigas + 3 novas)
-- Total de índices criados: 28 (18 antigos + 10 novos)
-- Total de triggers criados: 3 (1 antigo + 2 novos)
-- ============================================================================
