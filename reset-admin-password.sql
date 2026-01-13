-- Script para resetar a senha do usuário admin
-- Nova senha: 12345
-- Hash BCrypt gerado com cost factor 12

UPDATE users
SET password_hash = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYILSBL9C4m',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@notisblokk.com';

-- Verificar se a atualização foi bem sucedida
SELECT 'Senha do admin resetada com sucesso!' as resultado,
       username,
       email,
       role,
       active
FROM users
WHERE email = 'admin@notisblokk.com';
