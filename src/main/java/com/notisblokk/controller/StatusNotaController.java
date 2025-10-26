package com.notisblokk.controller;

import com.notisblokk.model.StatusNota;
import com.notisblokk.repository.StatusNotaRepository;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

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
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class StatusNotaController {

    private static final Logger logger = LoggerFactory.getLogger(StatusNotaController.class);
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([0-9a-fA-F]{6})$");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final StatusNotaRepository statusRepository;

    /**
     * Construtor padrão.
     */
    public StatusNotaController() {
        this.statusRepository = new StatusNotaRepository();
    }

    /**
     * GET /api/status
     * Lista todos os status, incluindo contador de notas.
     */
    public void listar(Context ctx) {
        try {
            List<StatusNota> statusList = statusRepository.buscarTodos();

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
                        long totalNotas = statusRepository.contarNotasPorStatus(status.getId());
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
     * Busca um status por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Optional<StatusNota> statusOpt = statusRepository.buscarPorId(id);

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
     * Cria um novo status.
     */
    public void criar(Context ctx) {
        try {
            // Obter dados do JSON
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String nome = (String) body.get("nome");
            String corHex = (String) body.get("corHex");

            // Validações
            if (nome == null || nome.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nome do status é obrigatório"
                ));
                return;
            }

            if (corHex == null || !HEX_COLOR_PATTERN.matcher(corHex).matches()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Cor em formato hexadecimal (#RRGGBB) é obrigatória"
                ));
                return;
            }

            // Verificar se já existe
            if (statusRepository.buscarPorNome(nome).isPresent()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Já existe um status com este nome"
                ));
                return;
            }

            // Obter sessão e usuário atual
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            // Criar status
            StatusNota status = new StatusNota();
            status.setNome(nome.trim());
            status.setCorHex(corHex.toUpperCase());

            status = statusRepository.salvar(status, sessaoId, usuarioId);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Status criado com sucesso",
                "dados", status
            ));

            logger.info("Status criado: {} por usuário {}", nome, usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao criar status", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar status: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/status/{id}
     * Atualiza um status existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Obter dados do JSON
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String nome = (String) body.get("nome");
            String corHex = (String) body.get("corHex");

            // Validações
            if (nome == null || nome.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nome do status é obrigatório"
                ));
                return;
            }

            if (corHex == null || !HEX_COLOR_PATTERN.matcher(corHex).matches()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Cor em formato hexadecimal (#RRGGBB) é obrigatória"
                ));
                return;
            }

            // Buscar status existente
            Optional<StatusNota> statusOpt = statusRepository.buscarPorId(id);
            if (statusOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Status não encontrado"
                ));
                return;
            }

            StatusNota status = statusOpt.get();

            // Verificar se o novo nome já existe em outro status
            Optional<StatusNota> existente = statusRepository.buscarPorNome(nome);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Já existe outro status com este nome"
                ));
                return;
            }

            // Atualizar
            status.setNome(nome.trim());
            status.setCorHex(corHex.toUpperCase());
            statusRepository.atualizar(status);

            ctx.json(Map.of(
                "success", true,
                "message", "Status atualizado com sucesso",
                "dados", status
            ));

            logger.info("Status ID {} atualizado para: {}", id, nome);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao atualizar status", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar status: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/status/{id}
     * Deleta um status.
     * ATENÇÃO: Não será possível deletar se houver notas com esse status (RESTRICT).
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Verificar se status existe
            Optional<StatusNota> statusOpt = statusRepository.buscarPorId(id);
            if (statusOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Status não encontrado"
                ));
                return;
            }

            // Verificar se há notas com esse status
            long totalNotas = statusRepository.contarNotasPorStatus(id);
            if (totalNotas > 0) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", String.format("Não é possível deletar este status pois há %d nota(s) vinculada(s)", totalNotas)
                ));
                return;
            }

            // Deletar status
            statusRepository.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Status deletado com sucesso"
            ));

            logger.warn("Status ID {} deletado", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao deletar status", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar status: " + e.getMessage()
            ));
        }
    }
}
