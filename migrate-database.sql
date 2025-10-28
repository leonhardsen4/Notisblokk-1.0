-- Script de migração para adicionar colunas faltantes
-- Execute este script se quiser manter os dados existentes

-- Adicionar colunas na tabela users
ALTER TABLE users ADD COLUMN foto_perfil TEXT;
ALTER TABLE users ADD COLUMN email_verificado BOOLEAN DEFAULT 0;
ALTER TABLE users ADD COLUMN token_verificacao TEXT;
ALTER TABLE users ADD COLUMN tentativas_login INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN bloqueado_ate TIMESTAMP;
ALTER TABLE users ADD COLUMN data_alteracao_senha TIMESTAMP;
ALTER TABLE users ADD COLUMN senha_expira_em TIMESTAMP;

-- Criar tabela de alertas enviados
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

-- Criar índices para alertas_enviados
CREATE INDEX IF NOT EXISTS idx_alertas_enviados_usuario_nota ON alertas_enviados(usuario_id, nota_id);
CREATE INDEX IF NOT EXISTS idx_alertas_enviados_data ON alertas_enviados(data_envio);

-- Criar índice único para configuracoes (se não existir)
DROP INDEX IF EXISTS idx_configuracoes_usuario_chave;
CREATE UNIQUE INDEX idx_configuracoes_usuario_chave ON configuracoes(usuario_id, chave);

SELECT 'Migração concluída com sucesso!' as resultado;
