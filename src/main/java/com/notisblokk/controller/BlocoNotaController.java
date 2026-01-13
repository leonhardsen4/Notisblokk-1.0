package com.notisblokk.controller;

import com.notisblokk.model.BlocoNota;
import com.notisblokk.service.BlocoNotaService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller responsável pelo gerenciamento do bloco de notas.
 *
 * <p>Gerencia os endpoints REST para operações do bloco de notas:</p>
 * <ul>
 *   <li>GET /api/bloco-notas - Obter documento do usuário</li>
 *   <li>POST /api/bloco-notas/salvar - Salvar documento</li>
 *   <li>GET /api/bloco-notas/exportar/txt - Exportar como TXT</li>
 *   <li>GET /api/bloco-notas/exportar/md - Exportar como Markdown</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class BlocoNotaController {

    private static final Logger logger = LoggerFactory.getLogger(BlocoNotaController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final BlocoNotaService blocoNotaService;

    /**
     * Construtor padrão.
     */
    public BlocoNotaController() {
        this.blocoNotaService = new BlocoNotaService();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param blocoNotaService serviço de bloco de notas
     */
    public BlocoNotaController(BlocoNotaService blocoNotaService) {
        this.blocoNotaService = blocoNotaService;
    }

    /**
     * GET /api/bloco-notas
     * Retorna o documento de bloco de notas do usuário (cria se não existir).
     */
    public void obter(Context ctx) {
        try {
            // Obter usuário e sessão
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);

            if (usuarioId == null) {
                ctx.status(401);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não autenticado"
                ));
                return;
            }

            // Obter documento
            BlocoNota nota = blocoNotaService.obter(usuarioId, sessaoId);

            // Preparar resposta
            Map<String, Object> dados = new HashMap<>();
            dados.put("id", nota.getId());
            dados.put("conteudoMarkdown", nota.getConteudoMarkdown() != null ? nota.getConteudoMarkdown() : "");

            if (nota.getDataCriacao() != null) {
                dados.put("dataCriacao", nota.getDataCriacao().format(FORMATTER));
            }

            if (nota.getDataAtualizacao() != null) {
                dados.put("dataAtualizacao", nota.getDataAtualizacao().format(FORMATTER));
            }

            ctx.json(Map.of(
                "success", true,
                "dados", dados
            ));

            logger.debug("Bloco de notas retornado para usuário ID {}", usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao obter bloco de notas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao obter bloco de notas: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/bloco-notas/salvar
     * Salva o conteúdo do bloco de notas.
     *
     * Payload esperado:
     * {
     *   "conteudo": "# Meu texto em Markdown..."
     * }
     */
    public void salvar(Context ctx) {
        try {
            // Obter sessão e usuário
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            if (sessaoId == null || usuarioId == null) {
                ctx.status(401);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não autenticado"
                ));
                return;
            }

            // Obter conteúdo do body
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String conteudo = (String) body.get("conteudo");

            // Salvar (aceita null/vazio)
            BlocoNota nota = blocoNotaService.salvar(conteudo, usuarioId, sessaoId);

            // Preparar resposta
            Map<String, Object> dados = new HashMap<>();
            dados.put("id", nota.getId());
            dados.put("conteudoMarkdown", nota.getConteudoMarkdown());

            if (nota.getDataAtualizacao() != null) {
                dados.put("dataAtualizacao", nota.getDataAtualizacao().format(FORMATTER));
            }

            ctx.json(Map.of(
                "success", true,
                "message", "Bloco de notas salvo com sucesso",
                "dados", dados
            ));

            logger.info("Bloco de notas salvo para usuário ID {}", usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao salvar bloco de notas", e);

            // Determinar código de status HTTP baseado na mensagem de erro
            int status = e.getMessage().contains("muito grande") ? 400 : 500;

            ctx.status(status);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/bloco-notas/exportar/txt
     * Exporta o bloco de notas como arquivo TXT.
     */
    public void exportarTxt(Context ctx) {
        try {
            // Obter usuário
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            if (usuarioId == null) {
                ctx.status(401);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não autenticado"
                ));
                return;
            }

            // Exportar
            byte[] conteudo = blocoNotaService.exportarTxt(usuarioId);

            // Configurar headers para download
            ctx.contentType("text/plain; charset=UTF-8");
            ctx.header("Content-Disposition", "attachment; filename=\"bloco-notas.txt\"");
            ctx.result(conteudo);

            logger.info("Bloco de notas exportado como TXT para usuário ID {}", usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao exportar TXT", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao exportar TXT: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/bloco-notas/exportar/md
     * Exporta o bloco de notas como arquivo Markdown.
     */
    public void exportarMarkdown(Context ctx) {
        try {
            // Obter usuário
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            if (usuarioId == null) {
                ctx.status(401);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não autenticado"
                ));
                return;
            }

            // Exportar
            byte[] conteudo = blocoNotaService.exportarMarkdown(usuarioId);

            // Configurar headers para download
            ctx.contentType("text/markdown; charset=UTF-8");
            ctx.header("Content-Disposition", "attachment; filename=\"bloco-notas.md\"");
            ctx.result(conteudo);

            logger.info("Bloco de notas exportado como MD para usuário ID {}", usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao exportar MD", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao exportar Markdown: " + e.getMessage()
            ));
        }
    }
}
