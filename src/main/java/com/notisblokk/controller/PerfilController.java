package com.notisblokk.controller;

import com.notisblokk.model.User;
import com.notisblokk.repository.UserRepository;
import com.notisblokk.service.FileUploadService;
import com.notisblokk.service.SecurityService;
import com.notisblokk.service.UserService;
import com.notisblokk.util.SessionUtil;
import com.notisblokk.util.ValidationUtil;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller responsável pelo perfil do usuário.
 *
 * <p>Gerencia:</p>
 * <ul>
 *   <li>Visualização e edição do perfil</li>
 *   <li>Alteração de senha</li>
 *   <li>Alteração de email</li>
 *   <li>Upload de foto de perfil</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class PerfilController {

    private static final Logger logger = LoggerFactory.getLogger(PerfilController.class);
    private final UserService userService;
    private final UserRepository userRepository;
    private final SecurityService securityService;
    private final FileUploadService fileUploadService;

    public PerfilController() {
        this.userService = new UserService();
        this.userRepository = new UserRepository();
        this.securityService = new SecurityService();
        this.fileUploadService = new FileUploadService();
    }

    /**
     * GET /perfil
     * Exibe a página de perfil do usuário.
     */
    public void index(Context ctx) {
        User currentUser = SessionUtil.getCurrentUser(ctx);

        // Verificar se usuário está autenticado
        if (currentUser == null) {
            ctx.redirect("/auth/login");
            return;
        }

        Map<String, Object> model = new HashMap<>();

        // Adicionar atributos de sessão (inclui isAuthenticated, user, theme, etc)
        model.putAll(SessionUtil.getSessionAttributes(ctx));

        model.put("title", "Meu Perfil - Notisblokk");
        model.put("user", currentUser);
        model.put("theme", SessionUtil.getTheme(ctx));

        // Mensagens
        String success = ctx.sessionAttribute("perfilSuccess");
        String error = ctx.sessionAttribute("perfilError");

        if (success != null) {
            model.put("success", success);
            ctx.sessionAttribute("perfilSuccess", null);
        }
        if (error != null) {
            model.put("error", error);
            ctx.sessionAttribute("perfilError", null);
        }

        ctx.contentType("text/html; charset=utf-8");
        ctx.render("perfil/index", model);
    }

    /**
     * POST /perfil/senha
     * Atualiza a senha do usuário.
     */
    public void alterarSenha(Context ctx) {
        try {
            User currentUser = SessionUtil.getCurrentUser(ctx);

            // Verificar se usuário está autenticado
            if (currentUser == null) {
                ctx.redirect("/auth/login");
                return;
            }

            String senhaAtual = ctx.formParam("senhaAtual");
            String novaSenha = ctx.formParam("novaSenha");
            String confirmaSenha = ctx.formParam("confirmaSenha");

            // Validar campos
            if (ValidationUtil.isNullOrEmpty(senhaAtual) ||
                ValidationUtil.isNullOrEmpty(novaSenha) ||
                ValidationUtil.isNullOrEmpty(confirmaSenha)) {

                ctx.sessionAttribute("perfilError", "Preencha todos os campos");
                ctx.redirect("/perfil");
                return;
            }

            // Validar senha atual
            if (!BCrypt.checkpw(senhaAtual, currentUser.getPasswordHash())) {
                ctx.sessionAttribute("perfilError", "Senha atual incorreta");
                ctx.redirect("/perfil");
                return;
            }

            // Validar confirmação
            if (!novaSenha.equals(confirmaSenha)) {
                ctx.sessionAttribute("perfilError", "As senhas não coincidem");
                ctx.redirect("/perfil");
                return;
            }

            // Atualizar senha
            String passwordHash = BCrypt.hashpw(novaSenha, BCrypt.gensalt(12));
            userService.atualizarSenha(currentUser.getId(), passwordHash);

            logger.info("Senha alterada para usuário: {}", currentUser.getUsername());

            ctx.sessionAttribute("perfilSuccess", "Senha alterada com sucesso!");
            ctx.redirect("/perfil");

        } catch (Exception e) {
            logger.error("Erro ao alterar senha", e);
            ctx.sessionAttribute("perfilError", "Erro ao alterar senha: " + e.getMessage());
            ctx.redirect("/perfil");
        }
    }

    /**
     * POST /perfil/email
     * Atualiza o email do usuário.
     */
    public void alterarEmail(Context ctx) {
        try {
            User currentUser = SessionUtil.getCurrentUser(ctx);

            // Verificar se usuário está autenticado
            if (currentUser == null) {
                ctx.redirect("/auth/login");
                return;
            }

            String novoEmail = ctx.formParam("novoEmail");
            String senha = ctx.formParam("senha");

            // Validar campos
            if (ValidationUtil.isNullOrEmpty(novoEmail) || ValidationUtil.isNullOrEmpty(senha)) {
                ctx.sessionAttribute("perfilError", "Preencha todos os campos");
                ctx.redirect("/perfil");
                return;
            }

            // Validar formato de email
            if (!ValidationUtil.isValidEmail(novoEmail)) {
                ctx.sessionAttribute("perfilError", "Email inválido");
                ctx.redirect("/perfil");
                return;
            }

            // Validar senha
            if (!BCrypt.checkpw(senha, currentUser.getPasswordHash())) {
                ctx.sessionAttribute("perfilError", "Senha incorreta");
                ctx.redirect("/perfil");
                return;
            }

            // Atualizar email
            userRepository.atualizarEmail(currentUser.getId(), novoEmail);

            // Enviar email de confirmação
            currentUser.setEmail(novoEmail);
            try {
                securityService.enviarEmailConfirmacao(currentUser);
            } catch (Exception emailError) {
                logger.error("Erro ao enviar email de confirmação", emailError);
            }

            logger.info("Email alterado para usuário: {} - novo email: {}", currentUser.getUsername(), novoEmail);

            ctx.sessionAttribute("perfilSuccess",
                "Email alterado com sucesso! Verifique seu novo email para confirmar.");
            ctx.redirect("/perfil");

        } catch (Exception e) {
            logger.error("Erro ao alterar email", e);
            ctx.sessionAttribute("perfilError", "Erro ao alterar email: " + e.getMessage());
            ctx.redirect("/perfil");
        }
    }

    /**
     * POST /perfil/foto
     * Faz upload da foto de perfil.
     */
    public void uploadFoto(Context ctx) {
        try {
            User currentUser = SessionUtil.getCurrentUser(ctx);

            // Verificar se usuário está autenticado
            if (currentUser == null) {
                ctx.redirect("/auth/login");
                return;
            }

            UploadedFile file = ctx.uploadedFile("foto");

            if (file == null) {
                ctx.sessionAttribute("perfilError", "Nenhuma foto selecionada");
                ctx.redirect("/perfil");
                return;
            }

            // Upload do arquivo (método específico para foto de perfil)
            String caminhoFoto = fileUploadService.uploadFotoPerfil(
                file.filename(),
                file.content()
            );

            // Atualizar caminho da foto no banco de dados
            userRepository.atualizarFotoPerfil(currentUser.getId(), caminhoFoto);

            // Recarregar usuário do banco para ter dados atualizados
            User usuarioAtualizado = userRepository.buscarPorId(currentUser.getId())
                .orElse(currentUser);

            // Atualizar sessão com usuário recarregado
            ctx.sessionAttribute("currentUser", usuarioAtualizado);

            logger.info("Foto de perfil atualizada para usuário: {}", currentUser.getUsername());

            ctx.sessionAttribute("perfilSuccess", "Foto de perfil atualizada com sucesso!");
            ctx.redirect("/perfil");

        } catch (Exception e) {
            logger.error("Erro ao fazer upload de foto", e);
            ctx.sessionAttribute("perfilError", "Erro ao fazer upload da foto: " + e.getMessage());
            ctx.redirect("/perfil");
        }
    }

    /**
     * POST /perfil/foto/remover
     * Remove a foto de perfil do usuário.
     */
    public void removerFoto(Context ctx) {
        try {
            User currentUser = SessionUtil.getCurrentUser(ctx);

            // Verificar se usuário está autenticado
            if (currentUser == null) {
                ctx.redirect("/auth/login");
                return;
            }

            // Deletar arquivo físico se existir
            if (currentUser.getFotoPerfil() != null) {
                try {
                    java.io.File fotoAntiga = new java.io.File(currentUser.getFotoPerfil());
                    if (fotoAntiga.exists()) {
                        fotoAntiga.delete();
                        logger.info("Arquivo de foto removido: {}", currentUser.getFotoPerfil());
                    }
                } catch (Exception fileError) {
                    logger.warn("Não foi possível deletar arquivo de foto: {}", fileError.getMessage());
                }
            }

            // Atualizar banco de dados (setar foto_perfil como NULL)
            userRepository.atualizarFotoPerfil(currentUser.getId(), null);

            // Recarregar usuário do banco para ter dados atualizados
            User usuarioAtualizado = userRepository.buscarPorId(currentUser.getId())
                .orElse(currentUser);

            // Atualizar sessão com usuário recarregado
            ctx.sessionAttribute("currentUser", usuarioAtualizado);

            logger.info("Foto de perfil removida para usuário: {}", currentUser.getUsername());

            ctx.sessionAttribute("perfilSuccess", "Foto de perfil removida com sucesso!");
            ctx.redirect("/perfil");

        } catch (Exception e) {
            logger.error("Erro ao remover foto", e);
            ctx.sessionAttribute("perfilError", "Erro ao remover foto: " + e.getMessage());
            ctx.redirect("/perfil");
        }
    }
}
