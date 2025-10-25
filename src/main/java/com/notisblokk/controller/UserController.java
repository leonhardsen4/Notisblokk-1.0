package com.notisblokk.controller;

import com.notisblokk.model.User;
import com.notisblokk.model.UserRole;
import com.notisblokk.service.UserService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller responsável pelo gerenciamento de usuários.
 *
 * <p>Gerencia os endpoints de administração de usuários (apenas para ADMIN):</p>
 * <ul>
 *   <li>Listar todos os usuários</li>
 *   <li>Criar novo usuário</li>
 *   <li>Editar usuário existente</li>
 *   <li>Ativar/desativar usuário</li>
 *   <li>Deletar usuário</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    /**
     * Construtor padrão.
     */
    public UserController() {
        this.userService = new UserService();
    }

    /**
     * GET /admin/users
     * Lista todos os usuários do sistema.
     */
    public void list(Context ctx) {
        try {
            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Gerenciar Usuários - Notisblokk");

            // Listar todos os usuários
            List<User> users = userService.listarTodos();
            model.put("users", users);

            // Estatísticas
            model.put("totalUsuarios", users.size());
            model.put("usuariosAtivos", users.stream().filter(User::isActive).count());
            model.put("usuariosInativos", users.stream().filter(u -> !u.isActive()).count());

            // Mensagens de feedback
            String success = ctx.sessionAttribute("userSuccess");
            String error = ctx.sessionAttribute("userError");

            if (success != null) {
                model.put("success", success);
                ctx.sessionAttribute("userSuccess", null);
            }

            if (error != null) {
                model.put("error", error);
                ctx.sessionAttribute("userError", null);
            }

            logger.debug("Lista de usuários acessada por: {}", SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("admin/users", model);

        } catch (Exception e) {
            logger.error("Erro ao listar usuários", e);
            ctx.status(500);
            ctx.result("Erro ao listar usuários: " + e.getMessage());
        }
    }

    /**
     * POST /admin/users
     * Cria um novo usuário.
     */
    public void create(Context ctx) {
        try {
            // Obter dados do formulário
            String username = ctx.formParam("username");
            String email = ctx.formParam("email");
            String password = ctx.formParam("password");
            String fullName = ctx.formParam("fullName");
            String roleStr = ctx.formParam("role");
            String activeStr = ctx.formParam("active");

            // Parse role
            UserRole role = UserRole.fromString(roleStr != null ? roleStr : "OPERATOR");

            // Parse active
            boolean active = activeStr != null && activeStr.equals("on");

            // Criar usuário
            User user = userService.criarUsuario(username, email, password, fullName, role);

            // Se foi criado como inativo, atualizar
            if (!active) {
                userService.alterarStatus(user.getId(), false);
            }

            logger.info("Usuário criado por admin: {} (Criado por: {})",
                       user.getUsername(), SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.sessionAttribute("userSuccess", "Usuário criado com sucesso!");
            ctx.redirect("/admin/users");

        } catch (Exception e) {
            logger.error("Erro ao criar usuário", e);
            ctx.sessionAttribute("userError", e.getMessage());
            ctx.redirect("/admin/users");
        }
    }

    /**
     * PUT /admin/users/:id
     * Atualiza um usuário existente.
     */
    public void update(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Obter dados do formulário
            String username = ctx.formParam("username");
            String email = ctx.formParam("email");
            String password = ctx.formParam("password"); // Opcional
            String fullName = ctx.formParam("fullName");
            String roleStr = ctx.formParam("role");
            String activeStr = ctx.formParam("active");

            // Parse role
            UserRole role = UserRole.fromString(roleStr);

            // Parse active
            boolean active = activeStr != null && activeStr.equals("on");

            // Atualizar usuário
            userService.atualizarUsuario(id, username, email, password, fullName, role);

            // Atualizar status se necessário
            Optional<User> userOpt = userService.buscarPorId(id);
            if (userOpt.isPresent() && userOpt.get().isActive() != active) {
                userService.alterarStatus(id, active);
            }

            logger.info("Usuário atualizado por admin: ID {} (Atualizado por: {})",
                       id, SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.sessionAttribute("userSuccess", "Usuário atualizado com sucesso!");
            ctx.redirect("/admin/users");

        } catch (Exception e) {
            logger.error("Erro ao atualizar usuário", e);
            ctx.sessionAttribute("userError", e.getMessage());
            ctx.redirect("/admin/users");
        }
    }

    /**
     * PATCH /admin/users/:id/toggle
     * Ativa ou desativa um usuário.
     */
    public void toggleStatus(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Buscar usuário
            Optional<User> userOpt = userService.buscarPorId(id);

            if (userOpt.isEmpty()) {
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não encontrado"
                ));
                return;
            }

            User user = userOpt.get();

            // Inverter status
            boolean newStatus = !user.isActive();
            userService.alterarStatus(id, newStatus);

            logger.info("Status do usuário alterado: {} -> {} (Por: {})",
                       user.getUsername(),
                       newStatus ? "ATIVO" : "INATIVO",
                       SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.json(Map.of(
                "success", true,
                "message", "Status alterado com sucesso",
                "active", newStatus
            ));

        } catch (Exception e) {
            logger.error("Erro ao alterar status do usuário", e);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /admin/users/:id
     * Remove um usuário do sistema.
     */
    public void delete(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Verificar se não está tentando deletar a si mesmo
            Long currentUserId = SessionUtil.getCurrentUserId(ctx);
            if (id.equals(currentUserId)) {
                ctx.json(Map.of(
                    "success", false,
                    "message", "Você não pode deletar seu próprio usuário"
                ));
                return;
            }

            // Buscar usuário antes de deletar (para log)
            Optional<User> userOpt = userService.buscarPorId(id);

            if (userOpt.isEmpty()) {
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não encontrado"
                ));
                return;
            }

            String username = userOpt.get().getUsername();

            // Deletar usuário
            userService.deletarUsuario(id);

            logger.warn("Usuário deletado: {} (ID: {}, Por: {})",
                       username, id, SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.json(Map.of(
                "success", true,
                "message", "Usuário deletado com sucesso"
            ));

        } catch (Exception e) {
            logger.error("Erro ao deletar usuário", e);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /admin/users/:id
     * Retorna dados de um usuário em JSON (para AJAX/modal).
     */
    public void getUser(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            Optional<User> userOpt = userService.buscarPorId(id);

            if (userOpt.isEmpty()) {
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não encontrado"
                ));
                return;
            }

            User user = userOpt.get();

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("fullName", user.getFullName());
            userData.put("role", user.getRole().name());
            userData.put("active", user.isActive());
            userData.put("createdAt", user.getFormattedCreatedAt());
            userData.put("updatedAt", user.getFormattedUpdatedAt());

            ctx.json(Map.of(
                "success", true,
                "user", userData
            ));

        } catch (Exception e) {
            logger.error("Erro ao buscar usuário", e);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/users
     * Lista todos os usuários em formato JSON.
     */
    public void listJson(Context ctx) {
        try {
            List<User> users = userService.listarTodos();

            ctx.json(Map.of(
                "success", true,
                "users", users,
                "total", users.size()
            ));

        } catch (Exception e) {
            logger.error("Erro ao listar usuários (JSON)", e);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
