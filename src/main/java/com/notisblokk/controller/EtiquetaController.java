package com.notisblokk.controller;

import com.notisblokk.model.Etiqueta;
import com.notisblokk.repository.EtiquetaRepository;
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
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class EtiquetaController {

    private static final Logger logger = LoggerFactory.getLogger(EtiquetaController.class);
    private final EtiquetaRepository etiquetaRepository;

    /**
     * Construtor padrão.
     */
    public EtiquetaController() {
        this.etiquetaRepository = new EtiquetaRepository();
    }

    /**
     * GET /api/etiquetas
     * Lista todas as etiquetas, incluindo contador de notas.
     */
    public void listar(Context ctx) {
        try {
            List<Etiqueta> etiquetas = etiquetaRepository.buscarTodos();

            // Adicionar contador de notas para cada etiqueta
            List<Map<String, Object>> etiquetasComContador = etiquetas.stream()
                .map(etiqueta -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", etiqueta.getId());
                    map.put("nome", etiqueta.getNome());
                    map.put("dataCriacao", etiqueta.getDataCriacao());

                    try {
                        long totalNotas = etiquetaRepository.contarNotasPorEtiqueta(etiqueta.getId());
                        map.put("totalNotas", totalNotas);
                    } catch (Exception e) {
                        logger.error("Erro ao contar notas da etiqueta {}", etiqueta.getId(), e);
                        map.put("totalNotas", 0);
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
     * Busca uma etiqueta por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Etiqueta> etiquetaOpt = etiquetaRepository.buscarPorId(id);

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
     * Cria uma nova etiqueta.
     */
    public void criar(Context ctx) {
        try {
            // Obter dados do JSON
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String nome = (String) body.get("nome");

            // Validações
            if (nome == null || nome.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nome da etiqueta é obrigatório"
                ));
                return;
            }

            // Verificar se já existe
            if (etiquetaRepository.buscarPorNome(nome).isPresent()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Já existe uma etiqueta com este nome"
                ));
                return;
            }

            // Obter sessão e usuário atual
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            // Criar etiqueta
            Etiqueta etiqueta = new Etiqueta();
            etiqueta.setNome(nome.trim());

            etiqueta = etiquetaRepository.salvar(etiqueta, sessaoId, usuarioId);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Etiqueta criada com sucesso",
                "dados", etiqueta
            ));

            logger.info("Etiqueta criada: {} por usuário {}", nome, usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao criar etiqueta", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar etiqueta: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/etiquetas/{id}
     * Atualiza uma etiqueta existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Obter dados do JSON
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String nome = (String) body.get("nome");

            // Validações
            if (nome == null || nome.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nome da etiqueta é obrigatório"
                ));
                return;
            }

            // Buscar etiqueta existente
            Optional<Etiqueta> etiquetaOpt = etiquetaRepository.buscarPorId(id);
            if (etiquetaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Etiqueta não encontrada"
                ));
                return;
            }

            Etiqueta etiqueta = etiquetaOpt.get();

            // Verificar se o novo nome já existe em outra etiqueta
            Optional<Etiqueta> existente = etiquetaRepository.buscarPorNome(nome);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Já existe outra etiqueta com este nome"
                ));
                return;
            }

            // Atualizar
            etiqueta.setNome(nome.trim());
            etiquetaRepository.atualizar(etiqueta);

            ctx.json(Map.of(
                "success", true,
                "message", "Etiqueta atualizada com sucesso",
                "dados", etiqueta
            ));

            logger.info("Etiqueta ID {} atualizada para: {}", id, nome);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao atualizar etiqueta", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar etiqueta: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/etiquetas/{id}
     * Deleta uma etiqueta.
     * ATENÇÃO: Cascata irá deletar todas as notas associadas!
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Verificar se etiqueta existe
            Optional<Etiqueta> etiquetaOpt = etiquetaRepository.buscarPorId(id);
            if (etiquetaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Etiqueta não encontrada"
                ));
                return;
            }

            // Verificar quantas notas serão deletadas
            long totalNotas = etiquetaRepository.contarNotasPorEtiqueta(id);

            // Deletar etiqueta
            etiquetaRepository.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", String.format("Etiqueta deletada com sucesso (%d nota(s) também foram deletadas)", totalNotas)
            ));

            logger.warn("Etiqueta ID {} deletada (cascata: {} notas)", id, totalNotas);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao deletar etiqueta", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar etiqueta: " + e.getMessage()
            ));
        }
    }
}
