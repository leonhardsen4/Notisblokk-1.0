package com.notisblokk.controller;

import com.notisblokk.model.Etiqueta;
import com.notisblokk.service.EtiquetaService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller responsável pelo gerenciamento de etiquetas.
 *
 * <p>Gerencia os endpoints REST para operações CRUD de etiquetas:</p>
 * <ul>
 *   <li>GET /api/etiquetas - Listar todas as etiquetas</li>
 *   <li>GET /api/etiquetas/{id} - Buscar etiqueta por ID</li>
 *   <li>POST /api/etiquetas - Criar nova etiqueta</li>
 *   <li>PUT /api/etiquetas/{id} - Atualizar etiqueta</li>
 *   <li>DELETE /api/etiquetas/{id} - Deletar etiqueta</li>
 * </ul>
 *
 * <p><b>OTIMIZADO:</b> Utiliza EtiquetaService com cache em memória.</p>
 *
 * @author Notisblokk Team
 * @version 1.1
 * @since 2025-01-26
 */
public class EtiquetaController {

    private static final Logger logger = LoggerFactory.getLogger(EtiquetaController.class);
    private final EtiquetaService etiquetaService;

    /**
     * Construtor padrão.
     */
    public EtiquetaController() {
        this.etiquetaService = new EtiquetaService();
    }

    /**
     * GET /api/etiquetas
     * Lista todas as etiquetas, incluindo contador de notas (com cache).
     */
    public void listar(Context ctx) {
        try {
            List<Etiqueta> etiquetas = etiquetaService.listarTodas();

            // Adicionar contador de notas para cada etiqueta
            List<Map<String, Object>> etiquetasComContador = etiquetas.stream()
                .map(etiqueta -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", etiqueta.getId());
                    map.put("nome", etiqueta.getNome());
                    map.put("dataCriacao", etiqueta.getDataCriacao());

                    try {
                        long totalTarefas = etiquetaService.contarTarefasPorEtiqueta(etiqueta.getId());
                        map.put("totalTarefas", totalTarefas);
                    } catch (Exception e) {
                        logger.error("Erro ao contar tarefas da etiqueta {}", etiqueta.getId(), e);
                        map.put("totalTarefas", 0);
                    }

                    return map;
                })
                .toList();

            ctx.json(Map.of(
                "success", true,
                "dados", etiquetasComContador
            ));

            logger.debug("Listadas {} etiquetas", etiquetas.size());

        } catch (Exception e) {
            logger.error("Erro ao listar etiquetas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar etiquetas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/etiquetas/{id}
     * Busca uma etiqueta por ID (com cache).
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Etiqueta> etiquetaOpt = etiquetaService.buscarPorId(id);

            if (etiquetaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Etiqueta não encontrada"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", etiquetaOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao buscar etiqueta", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar etiqueta: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/etiquetas
     * Cria uma nova etiqueta (invalida cache).
     */
    public void criar(Context ctx) {
        try {
            // Obter dados do JSON
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String nome = (String) body.get("nome");

            // Obter sessão e usuário atual
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            // Criar via service (validações e cache são gerenciados lá)
            Etiqueta etiqueta = etiquetaService.criar(nome, sessaoId, usuarioId);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Etiqueta criada com sucesso",
                "dados", etiqueta
            ));

        } catch (Exception e) {
            logger.error("Erro ao criar etiqueta", e);

            // Determinar código de status HTTP baseado na mensagem de erro
            int status = e.getMessage().contains("obrigatório") ||
                         e.getMessage().contains("já existe") ||
                         e.getMessage().contains("máximo") ? 400 : 500;

            ctx.status(status);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/etiquetas/{id}
     * Atualiza uma etiqueta existente (invalida cache).
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Obter dados do JSON
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String nome = (String) body.get("nome");

            // Atualizar via service (validações e cache são gerenciados lá)
            Etiqueta etiqueta = etiquetaService.atualizar(id, nome);

            ctx.json(Map.of(
                "success", true,
                "message", "Etiqueta atualizada com sucesso",
                "dados", etiqueta
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao atualizar etiqueta", e);

            // Determinar código de status HTTP baseado na mensagem de erro
            int status = e.getMessage().contains("não encontrada") ? 404 :
                         (e.getMessage().contains("obrigatório") ||
                          e.getMessage().contains("já existe") ||
                          e.getMessage().contains("máximo")) ? 400 : 500;

            ctx.status(status);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/etiquetas/{id}
     * Deleta uma etiqueta (invalida cache).
     * ATENÇÃO: Cascata irá deletar todas as notas associadas!
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Deletar via service (retorna quantidade de notas deletadas)
            long totalNotas = etiquetaService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", String.format("Etiqueta deletada com sucesso (%d nota(s) também foram deletadas)", totalNotas)
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao deletar etiqueta", e);

            // Determinar código de status HTTP baseado na mensagem de erro
            int status = e.getMessage().contains("não encontrada") ? 404 : 500;

            ctx.status(status);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
