package com.notisblokk.controller;

import com.notisblokk.model.StatusNota;
import com.notisblokk.service.StatusNotaService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller responsável pelo gerenciamento de status de notas.
 *
 * <p>Gerencia os endpoints REST para operações CRUD de status:</p>
 * <ul>
 *   <li>GET /api/status - Listar todos os status</li>
 *   <li>GET /api/status/{id} - Buscar status por ID</li>
 *   <li>POST /api/status - Criar novo status</li>
 *   <li>PUT /api/status/{id} - Atualizar status</li>
 *   <li>DELETE /api/status/{id} - Deletar status</li>
 * </ul>
 *
 * <p><b>OTIMIZADO:</b> Utiliza StatusNotaService com cache em memória.</p>
 *
 * @author Notisblokk Team
 * @version 1.1
 * @since 2025-01-26
 */
public class StatusNotaController {

    private static final Logger logger = LoggerFactory.getLogger(StatusNotaController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final StatusNotaService statusService;

    /**
     * Construtor padrão.
     */
    public StatusNotaController() {
        this.statusService = new StatusNotaService();
    }

    /**
     * GET /api/status
     * Lista todos os status, incluindo contador de notas (com cache).
     */
    public void listar(Context ctx) {
        try {
            List<StatusNota> statusList = statusService.listarTodos();

            // Adicionar contador de notas para cada status
            List<Map<String, Object>> statusComContador = statusList.stream()
                .map(status -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", status.getId());
                    map.put("nome", status.getNome());
                    map.put("corHex", status.getCorHex());

                    // Formatar data_criacao no formato brasileiro
                    if (status.getDataCriacao() != null) {
                        map.put("dataCriacao", status.getDataCriacao().format(FORMATTER));
                    }

                    try {
                        long totalNotas = statusService.contarNotasPorStatus(status.getId());
                        map.put("totalNotas", totalNotas);
                    } catch (Exception e) {
                        logger.error("Erro ao contar notas do status {}", status.getId(), e);
                        map.put("totalNotas", 0);
                    }

                    return map;
                })
                .toList();

            ctx.json(Map.of(
                "success", true,
                "dados", statusComContador
            ));

            logger.debug("Listados {} status", statusList.size());

        } catch (Exception e) {
            logger.error("Erro ao listar status", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar status: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/status/{id}
     * Busca um status por ID (com cache).
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Optional<StatusNota> statusOpt = statusService.buscarPorId(id);

            if (statusOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Status não encontrado"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", statusOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao buscar status", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar status: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/status
     * Cria um novo status (invalida cache).
     */
    public void criar(Context ctx) {
        try {
            // Obter dados do JSON
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String nome = (String) body.get("nome");
            String corHex = (String) body.get("corHex");

            // Obter sessão e usuário atual
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            // Criar via service (validações e cache são gerenciados lá)
            StatusNota status = statusService.criar(nome, corHex, sessaoId, usuarioId);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Status criado com sucesso",
                "dados", status
            ));

        } catch (Exception e) {
            logger.error("Erro ao criar status", e);

            // Determinar código de status HTTP baseado na mensagem de erro
            int status = e.getMessage().contains("obrigatório") ||
                         e.getMessage().contains("já existe") ||
                         e.getMessage().contains("máximo") ||
                         e.getMessage().contains("hexadecimal") ? 400 : 500;

            ctx.status(status);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/status/{id}
     * Atualiza um status existente (invalida cache).
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Obter dados do JSON
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String nome = (String) body.get("nome");
            String corHex = (String) body.get("corHex");

            // Atualizar via service (validações e cache são gerenciados lá)
            StatusNota status = statusService.atualizar(id, nome, corHex);

            ctx.json(Map.of(
                "success", true,
                "message", "Status atualizado com sucesso",
                "dados", status
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao atualizar status", e);

            // Determinar código de status HTTP baseado na mensagem de erro
            int status = e.getMessage().contains("não encontrado") ? 404 :
                         (e.getMessage().contains("obrigatório") ||
                          e.getMessage().contains("já existe") ||
                          e.getMessage().contains("máximo") ||
                          e.getMessage().contains("hexadecimal")) ? 400 : 500;

            ctx.status(status);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/status/{id}
     * Deleta um status (invalida cache).
     * ATENÇÃO: Não será possível deletar se houver notas com esse status (RESTRICT).
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Deletar via service (validações e cache são gerenciados lá)
            statusService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Status deletado com sucesso"
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao deletar status", e);

            // Determinar código de status HTTP baseado na mensagem de erro
            int status = e.getMessage().contains("não encontrado") ? 404 :
                         e.getMessage().contains("vinculada") ? 400 : 500;

            ctx.status(status);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
