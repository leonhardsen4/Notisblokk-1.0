-- ============================================================
-- NOTISBLOKK 1.0 - SCHEMA DO BANCO DE DADOS
-- Sistema de gerenciamento com autenticação e sessões
-- ============================================================

-- Tabela de usuários
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK(role IN ('ADMIN', 'OPERATOR')),
    active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    foto_perfil TEXT,
    email_verificado BOOLEAN DEFAULT 0,
    token_verificacao TEXT,
    tentativas_login INTEGER DEFAULT 0,
    bloqueado_ate TIMESTAMP,
    data_alteracao_senha TIMESTAMP,
    senha_expira_em TIMESTAMP
);

-- Tabela de sessões (log de acessos)
CREATE TABLE IF NOT EXISTS sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'LOGGED_OUT', 'EXPIRED')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_login_time ON sessions(login_time);
CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions(status);

-- Trigger para atualizar updated_at automaticamente
CREATE TRIGGER IF NOT EXISTS update_users_timestamp
AFTER UPDATE ON users
BEGIN
    UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- ============================================================
-- TABELAS DE TAREFAS
-- ============================================================

-- Habilitar foreign keys no SQLite
PRAGMA foreign_keys = ON;

-- Tabela de Etiquetas (Tags para categorização)
CREATE TABLE IF NOT EXISTS etiquetas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL UNIQUE,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sessao_id INTEGER,
    usuario_id INTEGER,
    FOREIGN KEY (sessao_id) REFERENCES sessions(id) ON DELETE SET NULL,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Tabela de Status de Tarefas
CREATE TABLE IF NOT EXISTS status_tarefa (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL UNIQUE,
    cor_hex TEXT NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sessao_id INTEGER,
    usuario_id INTEGER,
    FOREIGN KEY (sessao_id) REFERENCES sessions(id) ON DELETE SET NULL,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Tabela de Tarefas
CREATE TABLE IF NOT EXISTS tarefas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    etiqueta_id INTEGER NOT NULL,
    status_id INTEGER NOT NULL,
    titulo TEXT NOT NULL,
    conteudo TEXT,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    prazo_final DATE NOT NULL,
    sessao_id INTEGER,
    usuario_id INTEGER,
    FOREIGN KEY (etiqueta_id) REFERENCES etiquetas(id) ON DELETE CASCADE,
    FOREIGN KEY (status_id) REFERENCES status_tarefa(id) ON DELETE RESTRICT,
    FOREIGN KEY (sessao_id) REFERENCES sessions(id) ON DELETE SET NULL,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Índices para performance nas tabelas de tarefas
CREATE INDEX IF NOT EXISTS idx_etiquetas_nome ON etiquetas(nome);
CREATE INDEX IF NOT EXISTS idx_status_tarefa_nome ON status_tarefa(nome);
CREATE INDEX IF NOT EXISTS idx_tarefas_etiqueta_id ON tarefas(etiqueta_id);
CREATE INDEX IF NOT EXISTS idx_tarefas_status_id ON tarefas(status_id);
CREATE INDEX IF NOT EXISTS idx_tarefas_prazo_final ON tarefas(prazo_final);
CREATE INDEX IF NOT EXISTS idx_tarefas_usuario_id ON tarefas(usuario_id);

-- Trigger para atualizar data_atualizacao automaticamente nas tarefas
CREATE TRIGGER IF NOT EXISTS update_tarefas_timestamp
AFTER UPDATE ON tarefas
BEGIN
    UPDATE tarefas SET data_atualizacao = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- ============================================================
-- TABELAS DE CONFIGURAÇÃO E ANEXOS
-- ============================================================

-- Tabela de Configurações do Sistema
CREATE TABLE IF NOT EXISTS configuracoes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER,
    chave TEXT NOT NULL,
    valor TEXT,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Tabela de Anexos de Tarefas
CREATE TABLE IF NOT EXISTS anexos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tarefa_id INTEGER NOT NULL,
    nome_arquivo TEXT NOT NULL,
    caminho_arquivo TEXT NOT NULL,
    tipo_mime TEXT,
    tamanho_bytes INTEGER,
    data_upload TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_id INTEGER,
    FOREIGN KEY (tarefa_id) REFERENCES tarefas(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Tabela de Histórico de Backups
CREATE TABLE IF NOT EXISTS backups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    caminho_arquivo TEXT NOT NULL,
    tipo TEXT NOT NULL CHECK(tipo IN ('AUTO', 'MANUAL', 'CSV')),
    tamanho_bytes INTEGER,
    data_backup TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_id INTEGER,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Tabela de Alertas Enviados (controle de duplicatas)
CREATE TABLE IF NOT EXISTS alertas_enviados (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL,
    tarefa_id INTEGER NOT NULL,
    nivel TEXT NOT NULL,
    dias_restantes INTEGER,
    data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (tarefa_id) REFERENCES tarefas(id) ON DELETE CASCADE
);

-- Índices para performance
CREATE UNIQUE INDEX IF NOT EXISTS idx_configuracoes_usuario_chave ON configuracoes(usuario_id, chave);
CREATE INDEX IF NOT EXISTS idx_configuracoes_usuario_id ON configuracoes(usuario_id);
CREATE INDEX IF NOT EXISTS idx_configuracoes_chave ON configuracoes(chave);
CREATE INDEX IF NOT EXISTS idx_anexos_tarefa_id ON anexos(tarefa_id);
CREATE INDEX IF NOT EXISTS idx_backups_data ON backups(data_backup);
CREATE INDEX IF NOT EXISTS idx_alertas_enviados_usuario_tarefa ON alertas_enviados(usuario_id, tarefa_id);
CREATE INDEX IF NOT EXISTS idx_alertas_enviados_data ON alertas_enviados(data_envio);

-- Trigger para atualizar data_atualizacao em configuracoes
CREATE TRIGGER IF NOT EXISTS update_configuracoes_timestamp
AFTER UPDATE ON configuracoes
BEGIN
    UPDATE configuracoes SET data_atualizacao = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- ============================================================
-- TABELAS DE FERRAMENTAS (CALCULADORA E BLOCO DE NOTAS)
-- ============================================================

-- Tabela de Histórico da Calculadora
CREATE TABLE IF NOT EXISTS historico_calculadora (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL,
    expressao TEXT NOT NULL,
    resultado REAL NOT NULL,
    tipo_operacao VARCHAR(20),
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sessao_id INTEGER,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (sessao_id) REFERENCES sessions(id) ON DELETE SET NULL
);

-- Tabela de Bloco de Notas (documento único por usuário)
CREATE TABLE IF NOT EXISTS bloco_notas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL UNIQUE,
    conteudo_markdown TEXT DEFAULT '',
    conteudo_html TEXT DEFAULT '',
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sessao_id INTEGER,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (sessao_id) REFERENCES sessions(id) ON DELETE SET NULL
);

-- Índices para performance das ferramentas
CREATE INDEX IF NOT EXISTS idx_historico_calc_usuario ON historico_calculadora(usuario_id);
CREATE INDEX IF NOT EXISTS idx_historico_calc_data ON historico_calculadora(data_criacao);
CREATE UNIQUE INDEX IF NOT EXISTS idx_bloco_notas_usuario ON bloco_notas(usuario_id);

-- Trigger para atualizar data_atualizacao automaticamente no bloco de notas
CREATE TRIGGER IF NOT EXISTS update_bloco_notas_timestamp
AFTER UPDATE ON bloco_notas
BEGIN
    UPDATE bloco_notas SET data_atualizacao = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;