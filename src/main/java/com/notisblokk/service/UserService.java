package com.notisblokk.service;

import com.notisblokk.model.User;
import com.notisblokk.model.UserRole;
import com.notisblokk.repository.UserRepository;
import com.notisblokk.util.PasswordUtil;
import com.notisblokk.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável pela lógica de negócio relacionada a usuários.
 *
 * <p>Coordena operações entre controllers e repository, implementando
 * regras de negócio para gerenciamento de usuários.</p>
 *
 * <p><b>Funcionalidades:</b></p>
 * <ul>
 *   <li>Criar novo usuário com validações</li>
 *   <li>Atualizar dados de usuário</li>
 *   <li>Listar usuários</li>
 *   <li>Ativar/desativar usuários</li>
 *   <li>Alterar senha</li>
 *   <li>Validar dados de entrada</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    /**
     * Construtor padrão.
     */
    public UserService() {
        this.userRepository = new UserRepository();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param userRepository repositório de usuários
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Cria um novo usuário no sistema.
     *
     * <p>Valida todos os dados de entrada antes de criar o usuário.
     * A senha é automaticamente criptografada com BCrypt.</p>
     *
     * @param username nome de usuário
     * @param email email do usuário
     * @param password senha em texto plano
     * @param fullName nome completo
     * @param role papel do usuário (ADMIN ou OPERATOR)
     * @return User usuário criado
     * @throws Exception se houver erro de validação ou ao criar usuário
     */
    public User criarUsuario(String username, String email, String password,
                            String fullName, UserRole role) throws Exception {

        logger.info("Criando novo usuário: {}", username);

        // Validações
        validarDadosUsuario(username, email, password, fullName);

        // Verificar se username já existe
        if (userRepository.buscarPorUsername(username).isPresent()) {
            throw new Exception("Username '" + username + "' já está em uso");
        }

        // Verificar se email já existe
        if (userRepository.buscarPorEmail(email).isPresent()) {
            throw new Exception("Email '" + email + "' já está em uso");
        }

        try {
            // Criar usuário
            User user = new User();
            user.setUsername(ValidationUtil.sanitize(username));
            user.setEmail(ValidationUtil.sanitize(email));
            user.setPasswordHash(PasswordUtil.hashPassword(password));
            user.setFullName(ValidationUtil.sanitize(fullName));
            user.setRole(role != null ? role : UserRole.OPERATOR);
            user.setActive(true);

            user = userRepository.salvar(user);

            logger.info("Usuário criado com sucesso: {} (ID: {})", user.getUsername(), user.getId());
            return user;

        } catch (SQLException e) {
            logger.error("Erro ao criar usuário: {}", username, e);
            throw new Exception("Erro ao criar usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza dados de um usuário existente.
     *
     * <p>Se a senha for fornecida (não nula/vazia), ela será atualizada.
     * Caso contrário, a senha atual é mantida.</p>
     *
     * @param id ID do usuário
     * @param username novo username
     * @param email novo email
     * @param password nova senha (opcional, null para não alterar)
     * @param fullName novo nome completo
     * @param role novo papel
     * @return User usuário atualizado
     * @throws Exception se houver erro de validação ou ao atualizar
     */
    public User atualizarUsuario(Long id, String username, String email, String password,
                                String fullName, UserRole role) throws Exception {

        logger.info("Atualizando usuário ID {}", id);

        // Buscar usuário existente
        User user = userRepository.buscarPorId(id)
            .orElseThrow(() -> new Exception("Usuário não encontrado"));

        // Validar dados básicos
        validarDadosUsuarioParaAtualizacao(username, email, fullName);

        // Verificar se username mudou e se o novo já existe
        if (!user.getUsername().equals(username)) {
            if (userRepository.buscarPorUsername(username).isPresent()) {
                throw new Exception("Username '" + username + "' já está em uso");
            }
        }

        // Verificar se email mudou e se o novo já existe
        if (!user.getEmail().equals(email)) {
            if (userRepository.buscarPorEmail(email).isPresent()) {
                throw new Exception("Email '" + email + "' já está em uso");
            }
        }

        try {
            // Atualizar dados
            user.setUsername(ValidationUtil.sanitize(username));
            user.setEmail(ValidationUtil.sanitize(email));
            user.setFullName(ValidationUtil.sanitize(fullName));
            user.setRole(role);

            // Atualizar senha apenas se fornecida
            if (password != null && !password.trim().isEmpty()) {
                PasswordUtil.ValidationResult passwordValidation =
                    PasswordUtil.validatePasswordStrength(password);

                if (!passwordValidation.isValid()) {
                    throw new Exception(passwordValidation.getMessage());
                }

                user.setPasswordHash(PasswordUtil.hashPassword(password));
            }

            userRepository.atualizar(user);

            logger.info("Usuário atualizado com sucesso: {} (ID: {})", user.getUsername(), user.getId());
            return user;

        } catch (SQLException e) {
            logger.error("Erro ao atualizar usuário ID {}", id, e);
            throw new Exception("Erro ao atualizar usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Altera a senha de um usuário.
     *
     * @param userId ID do usuário
     * @param novaSenha nova senha em texto plano
     * @throws Exception se houver erro ao alterar senha
     */
    public void alterarSenha(Long userId, String novaSenha) throws Exception {
        logger.info("Alterando senha do usuário ID {}", userId);

        // Validar senha
        PasswordUtil.ValidationResult validation = PasswordUtil.validatePasswordStrength(novaSenha);
        if (!validation.isValid()) {
            throw new Exception(validation.getMessage());
        }

        try {
            User user = userRepository.buscarPorId(userId)
                .orElseThrow(() -> new Exception("Usuário não encontrado"));

            user.setPasswordHash(PasswordUtil.hashPassword(novaSenha));
            userRepository.atualizar(user);

            logger.info("Senha alterada com sucesso para usuário ID {}", userId);

        } catch (SQLException e) {
            logger.error("Erro ao alterar senha do usuário ID {}", userId, e);
            throw new Exception("Erro ao alterar senha: " + e.getMessage(), e);
        }
    }

    /**
     * Ativa ou desativa um usuário (soft delete).
     *
     * @param id ID do usuário
     * @param ativo true para ativar, false para desativar
     * @throws Exception se houver erro ao alterar status
     */
    public void alterarStatus(Long id, boolean ativo) throws Exception {
        logger.info("Alterando status do usuário ID {} para: {}", id, ativo ? "ATIVO" : "INATIVO");

        try {
            userRepository.alterarStatus(id, ativo);
            logger.info("Status do usuário ID {} alterado com sucesso", id);

        } catch (SQLException e) {
            logger.error("Erro ao alterar status do usuário ID {}", id, e);
            throw new Exception("Erro ao alterar status: " + e.getMessage(), e);
        }
    }

    /**
     * Remove um usuário do sistema (hard delete).
     *
     * @param id ID do usuário a ser removido
     * @throws Exception se houver erro ao deletar
     */
    public void deletarUsuario(Long id) throws Exception {
        logger.warn("Deletando usuário ID {} (hard delete)", id);

        try {
            userRepository.deletar(id);
            logger.info("Usuário ID {} deletado com sucesso", id);

        } catch (SQLException e) {
            logger.error("Erro ao deletar usuário ID {}", id, e);
            throw new Exception("Erro ao deletar usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um usuário por ID.
     *
     * @param id ID do usuário
     * @return Optional<User> usuário encontrado ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<User> buscarPorId(Long id) throws Exception {
        try {
            return userRepository.buscarPorId(id);

        } catch (SQLException e) {
            logger.error("Erro ao buscar usuário ID {}", id, e);
            throw new Exception("Erro ao buscar usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um usuário por username.
     *
     * @param username username do usuário
     * @return Optional<User> usuário encontrado ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<User> buscarPorUsername(String username) throws Exception {
        try {
            return userRepository.buscarPorUsername(username);

        } catch (SQLException e) {
            logger.error("Erro ao buscar usuário por username: {}", username, e);
            throw new Exception("Erro ao buscar usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um usuário por email.
     *
     * @param email email do usuário
     * @return Optional<User> usuário encontrado ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<User> buscarPorEmail(String email) throws Exception {
        try {
            return userRepository.buscarPorEmail(email);

        } catch (SQLException e) {
            logger.error("Erro ao buscar usuário por email: {}", email, e);
            throw new Exception("Erro ao buscar usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos os usuários do sistema.
     *
     * @return List<User> lista de todos os usuários
     * @throws Exception se houver erro ao listar
     */
    public List<User> listarTodos() throws Exception {
        try {
            return userRepository.buscarTodos();

        } catch (SQLException e) {
            logger.error("Erro ao listar usuários", e);
            throw new Exception("Erro ao listar usuários: " + e.getMessage(), e);
        }
    }

    /**
     * Conta o total de usuários no sistema.
     *
     * @return long total de usuários
     * @throws Exception se houver erro ao contar
     */
    public long contarTotal() throws Exception {
        try {
            return userRepository.contarTotal();

        } catch (SQLException e) {
            logger.error("Erro ao contar usuários", e);
            throw new Exception("Erro ao contar usuários: " + e.getMessage(), e);
        }
    }

    /**
     * Conta usuários ativos no sistema.
     *
     * @return long total de usuários ativos
     * @throws Exception se houver erro ao contar
     */
    public long contarAtivos() throws Exception {
        try {
            return userRepository.contarAtivos();

        } catch (SQLException e) {
            logger.error("Erro ao contar usuários ativos", e);
            throw new Exception("Erro ao contar usuários ativos: " + e.getMessage(), e);
        }
    }

    /**
     * Valida dados de usuário para criação.
     *
     * @param username username a validar
     * @param email email a validar
     * @param password senha a validar
     * @param fullName nome completo a validar
     * @throws Exception se alguma validação falhar
     */
    private void validarDadosUsuario(String username, String email, String password, String fullName)
            throws Exception {

        // Validar username
        ValidationUtil.ValidationResult usernameValidation = ValidationUtil.validateUsername(username);
        if (!usernameValidation.isValid()) {
            throw new Exception(usernameValidation.getMessage());
        }

        // Validar email
        ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(email);
        if (!emailValidation.isValid()) {
            throw new Exception(emailValidation.getMessage());
        }

        // Validar senha
        PasswordUtil.ValidationResult passwordValidation = PasswordUtil.validatePasswordStrength(password);
        if (!passwordValidation.isValid()) {
            throw new Exception(passwordValidation.getMessage());
        }

        // Validar nome completo
        ValidationUtil.ValidationResult nameValidation = ValidationUtil.validateFullName(fullName);
        if (!nameValidation.isValid()) {
            throw new Exception(nameValidation.getMessage());
        }
    }

    /**
     * Valida dados de usuário para atualização (sem validar senha).
     *
     * @param username username a validar
     * @param email email a validar
     * @param fullName nome completo a validar
     * @throws Exception se alguma validação falhar
     */
    private void validarDadosUsuarioParaAtualizacao(String username, String email, String fullName)
            throws Exception {

        // Validar username
        ValidationUtil.ValidationResult usernameValidation = ValidationUtil.validateUsername(username);
        if (!usernameValidation.isValid()) {
            throw new Exception(usernameValidation.getMessage());
        }

        // Validar email
        ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(email);
        if (!emailValidation.isValid()) {
            throw new Exception(emailValidation.getMessage());
        }

        // Validar nome completo
        ValidationUtil.ValidationResult nameValidation = ValidationUtil.validateFullName(fullName);
        if (!nameValidation.isValid()) {
            throw new Exception(nameValidation.getMessage());
        }
    }
}
