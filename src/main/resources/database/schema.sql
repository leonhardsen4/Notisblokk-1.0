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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
-- TABELAS DE ANOTAÇÕES
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

-- Tabela de Status de Notas
CREATE TABLE IF NOT EXISTS status_nota (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL UNIQUE,
    cor_hex TEXT NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sessao_id INTEGER,
    usuario_id INTEGER,
    FOREIGN KEY (sessao_id) REFERENCES sessions(id) ON DELETE SET NULL,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Tabela de Notas
CREATE TABLE IF NOT EXISTS notas (
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
    FOREIGN KEY (status_id) REFERENCES status_nota(id) ON DELETE RESTRICT,
    FOREIGN KEY (sessao_id) REFERENCES sessions(id) ON DELETE SET NULL,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Índices para performance nas tabelas de anotações
CREATE INDEX IF NOT EXISTS idx_etiquetas_nome ON etiquetas(nome);
CREATE INDEX IF NOT EXISTS idx_status_nota_nome ON status_nota(nome);
CREATE INDEX IF NOT EXISTS idx_notas_etiqueta_id ON notas(etiqueta_id);
CREATE INDEX IF NOT EXISTS idx_notas_status_id ON notas(status_id);
CREATE INDEX IF NOT EXISTS idx_notas_prazo_final ON notas(prazo_final);
CREATE INDEX IF NOT EXISTS idx_notas_usuario_id ON notas(usuario_id);

-- Trigger para atualizar data_atualizacao automaticamente nas notas
CREATE TRIGGER IF NOT EXISTS update_notas_timestamp
AFTER UPDATE ON notas
BEGIN
    UPDATE notas SET data_atualizacao = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;