package com.notisblokk.controller;

import com.notisblokk.model.Tarefa;
import com.notisblokk.model.TarefaDTO;
import com.notisblokk.model.PaginatedResponse;
import com.notisblokk.service.TarefaService;
import com.notisblokk.service.PDFService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller responsável pelo gerenciamento de tarefas.
 *
 * <p>Gerencia os endpoints REST para operações CRUD de tarefas:</p>
 * <ul>
 *   <li>GET /api/tarefas - Listar todas as tarefas</li>
 *   <li>GET /api/tarefas/{id} - Buscar tarefa por ID</li>
 *   <li>GET /api/tarefas/etiqueta/{etiquetaId} - Buscar tarefas por etiqueta</li>
 *   <li>POST /api/tarefas - Criar nova tarefa</li>
 *   <li>PUT /api/tarefas/{id} - Atualizar tarefa</li>
 *   <li>DELETE /api/tarefas/{id} - Deletar tarefa</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class TarefaController {

    private static final Logger logger = LoggerFactory.getLogger(TarefaController.class);
    private final TarefaService tarefaService;
    private final PDFService pdfService;

    /**
     * Construtor padrão.
     */
    public TarefaController() {
        this.tarefaService = new TarefaService();
        this.pdfService = new PDFService();
    }

    /**
     * Classe interna para receber dados da requisição de criar/atualizar tarefa.
     */
    private static class TarefaRequest {
        public Long etiquetaId;
        public Long statusId;
        public String titulo;
        public String conteudo;
        public String prazoFinal; // Formato: yyyy-MM-dd
    }

    /**
     * GET /api/tarefas
     * Lista todas as tarefas como DTOs completos.
     * Se query params "pagina" e "tamanho" estiverem presentes, retorna paginado.
     */
    public void listar(Context ctx) {
        try {
            // Verificar se há parâmetros de paginação
            String paginaParam = ctx.queryParam("pagina");
            String tamanhoParam = ctx.queryParam("tamanho");

            // Se tem parâmetros de paginação, usar endpoint paginado
            if (paginaParam != null || tamanhoParam != null) {
                listarPaginado(ctx);
                return;
            }

            // Caso contrário, retornar todas as tarefas
            List<TarefaDTO> tarefas = tarefaService.listarTodas();

            ctx.json(Map.of(
                "success", true,
                "dados", tarefas
            ));

            logger.debug("Listadas {} tarefas", tarefas.size());

        } catch (Exception e) {
            logger.error("Erro ao listar tarefas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar tarefas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/tarefas/paginado
     * Lista tarefas com paginação.
     * Query params: pagina (default 1), tamanho (default 10), ordenar (default prazo_final), direcao (default ASC)
     */
    public void listarPaginado(Context ctx) {
        try {
            // Obter parâmetros de paginação
            int pagina = ctx.queryParamAsClass("pagina", Integer.class).getOrDefault(1);
            int tamanho = ctx.queryParamAsClass("tamanho", Integer.class).getOrDefault(10);
            String ordenarPor = ctx.queryParamAsClass("ordenar", String.class).getOrDefault("prazo_final");
            String direcao = ctx.queryParamAsClass("direcao", String.class).getOrDefault("ASC");

            // Buscar tarefas paginadas
            PaginatedResponse<TarefaDTO> response = tarefaService.listarComPaginacao(
                pagina, tamanho, ordenarPor, direcao
            );

            ctx.json(Map.of(
                "success", true,
                "paginaAtual", response.getPaginaAtual(),
                "tamanhoPagina", response.getTamanhoPagina(),
                "totalRegistros", response.getTotalRegistros(),
                "totalPaginas", response.getTotalPaginas(),
                "temProxima", response.isTemProxima(),
                "temAnterior", response.isTemAnterior(),
                "dados", response.getDados()
            ));

            logger.debug("Listadas {} tarefas (página {}/{})",
                response.getDados().size(), pagina, response.getTotalPaginas());

        } catch (Exception e) {
            logger.error("Erro ao listar tarefas paginadas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar tarefas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/tarefas/{id}
     * Busca uma tarefa por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Optional<TarefaDTO> tarefaOpt = tarefaService.buscarPorId(id);

            if (tarefaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Tarefa não encontrada"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", tarefaOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao buscar tarefa", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar tarefa: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/tarefas/etiqueta/{etiquetaId}
     * Busca tarefas por etiqueta.
     */
    public void buscarPorEtiqueta(Context ctx) {
        try {
            Long etiquetaId = Long.parseLong(ctx.pathParam("etiquetaId"));
            List<TarefaDTO> tarefas = tarefaService.listarPorEtiqueta(etiquetaId);

            ctx.json(Map.of(
                "success", true,
                "dados", tarefas
            ));

            logger.debug("Encontradas {} tarefas da etiqueta {}", tarefas.size(), etiquetaId);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID da etiqueta inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao buscar tarefas por etiqueta", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar tarefas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/tarefas/buscar
     * Busca tarefas por texto no título ou conteúdo.
     * Query param: q (termo de busca)
     */
    public void buscarPorTexto(Context ctx) {
        try {
            String termo = ctx.queryParam("q");

            if (termo == null || termo.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro de busca 'q' é obrigatório"
                ));
                return;
            }

            List<TarefaDTO> tarefas = tarefaService.buscarPorTexto(termo);

            ctx.json(Map.of(
                "success", true,
                "dados", tarefas,
                "total", tarefas.size()
            ));

            logger.debug("Busca por '{}': encontradas {} tarefas", termo, tarefas.size());

        } catch (Exception e) {
            logger.error("Erro ao buscar tarefas por texto", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar tarefas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/tarefas/intervalo
     * Busca tarefas por intervalo de prazo final.
     * Query params: inicio (data início), fim (data fim)
     * Formatos aceitos: yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy
     */
    public void buscarPorIntervaloPrazo(Context ctx) {
        try {
            String dataInicio = ctx.queryParam("inicio");
            String dataFim = ctx.queryParam("fim");

            if (dataInicio == null || dataInicio.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro 'inicio' é obrigatório"
                ));
                return;
            }

            if (dataFim == null || dataFim.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro 'fim' é obrigatório"
                ));
                return;
            }

            List<TarefaDTO> tarefas = tarefaService.buscarPorIntervaloPrazo(dataInicio, dataFim);

            ctx.json(Map.of(
                "success", true,
                "dados", tarefas,
                "total", tarefas.size(),
                "intervalo", Map.of(
                    "inicio", dataInicio,
                    "fim", dataFim
                )
            ));

            logger.debug("Busca por intervalo {} - {}: encontradas {} tarefas",
                dataInicio, dataFim, tarefas.size());

        } catch (Exception e) {
            logger.error("Erro ao buscar tarefas por intervalo de prazo", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar tarefas: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/tarefas
     * Cria uma nova tarefa.
     */
    public void criar(Context ctx) {
        try {
            // Obter dados do JSON
            TarefaRequest request = ctx.bodyAsClass(TarefaRequest.class);

            // Obter sessão e usuário atual
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            // Criar tarefa
            TarefaDTO tarefa = tarefaService.criar(
                request.etiquetaId,
                request.statusId,
                request.titulo,
                request.conteudo,
                request.prazoFinal,
                sessaoId,
                usuarioId
            );

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Tarefa criada com sucesso",
                "dados", tarefa
            ));

            logger.info("Tarefa criada: {} por usuário {}", request.titulo, usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao criar tarefa", e);
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/tarefas/{id}
     * Atualiza uma tarefa existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Obter dados do JSON
            TarefaRequest request = ctx.bodyAsClass(TarefaRequest.class);

            // Atualizar tarefa
            TarefaDTO tarefa = tarefaService.atualizar(
                id,
                request.etiquetaId,
                request.statusId,
                request.titulo,
                request.conteudo,
                request.prazoFinal
            );

            ctx.json(Map.of(
                "success", true,
                "message", "Tarefa atualizada com sucesso",
                "dados", tarefa
            ));

            logger.info("Tarefa ID {} atualizada", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao atualizar tarefa", e);
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/tarefas/{id}
     * Deleta uma tarefa.
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Verificar se tarefa existe
            Optional<TarefaDTO> tarefaOpt = tarefaService.buscarPorId(id);
            if (tarefaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Tarefa não encontrada"
                ));
                return;
            }

            // Deletar tarefa
            tarefaService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Tarefa deletada com sucesso"
            ));

            logger.warn("Tarefa ID {} deletada", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao deletar tarefa", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar tarefa: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/tarefas/{id}/pdf
     * Gera PDF de uma tarefa específica.
     */
    public void gerarPDF(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Gerar PDF
            byte[] pdfBytes = pdfService.gerarPDFNota(id);

            // Buscar tarefa para obter o título e usar no nome do arquivo
            Optional<TarefaDTO> tarefaOpt = tarefaService.buscarPorId(id);
            String fileName = "tarefa_" + id;
            if (tarefaOpt.isPresent()) {
                String titulo = tarefaOpt.get().getTitulo();
                // Remover caracteres inválidos do nome do arquivo
                String tituloProcessado = titulo.replaceAll("[^a-zA-Z0-9\\s-]", "")
                                                .replaceAll("\\s+", "_");
                // Limitar tamanho baseado na string processada
                fileName = tituloProcessado.substring(0, Math.min(tituloProcessado.length(), 50));
            }

            // Adicionar timestamp ao nome do arquivo (formato brasileiro: DDMMYYYY_HHmmss)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
            fileName = fileName + "_" + timestamp + ".pdf";

            // Enviar PDF
            ctx.contentType("application/pdf");
            ctx.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            ctx.result(pdfBytes);

            logger.info("PDF gerado para tarefa ID {}", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao gerar PDF da tarefa", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao gerar PDF: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/tarefas/pdf/relatorio
     * Gera PDF com relatório de múltiplas tarefas.
     * Espera um JSON com array de IDs: {"ids": [1, 2, 3]}
     */
    public void gerarPDFRelatorio(Context ctx) {
        try {
            // Obter lista de IDs do body
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");

            if (ids == null || ids.isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Lista de IDs não pode estar vazia"
                ));
                return;
            }

            // Converter IDs para Long e buscar tarefas
            List<Long> tarefaIds = ids.stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());

            // Buscar as tarefas completas
            List<TarefaDTO> tarefasDTO = tarefaService.listarTodas();
            List<Tarefa> tarefas = tarefasDTO.stream()
                .filter(n -> tarefaIds.contains(n.getId()))
                .map(this::convertDTOToTarefa)
                .collect(Collectors.toList());

            if (tarefas.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nenhuma tarefa encontrada"
                ));
                return;
            }

            // Gerar PDF
            byte[] pdfBytes = pdfService.gerarPDFRelatorio(tarefas);

            // Nome do arquivo com timestamp (formato brasileiro: DDMMYYYY_HHmmss)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
            String fileName = "relatorio_tarefas_" + timestamp + ".pdf";

            // Enviar PDF
            ctx.contentType("application/pdf");
            ctx.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            ctx.result(pdfBytes);

            logger.info("PDF de relatório gerado com {} tarefas", tarefas.size());

        } catch (Exception e) {
            logger.error("Erro ao gerar PDF de relatório", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao gerar PDF: " + e.getMessage()
            ));
        }
    }

    /**
     * Converte TarefaDTO para Tarefa (helper method).
     */
    private Tarefa convertDTOToTarefa(TarefaDTO dto) {
        Tarefa tarefa = new Tarefa();
        tarefa.setId(dto.getId());
        tarefa.setEtiquetaId(dto.getEtiqueta() != null ? dto.getEtiqueta().getId() : null);
        tarefa.setStatusId(dto.getStatus() != null ? dto.getStatus().getId() : null);
        tarefa.setTitulo(dto.getTitulo());
        tarefa.setConteudo(dto.getConteudo());
        tarefa.setPrazoFinal(dto.getPrazoFinal());
        tarefa.setDataCriacao(dto.getDataCriacao());
        tarefa.setDataAtualizacao(dto.getDataAtualizacao());
        tarefa.setSessaoId(dto.getSessaoId());
        tarefa.setUsuarioId(dto.getUsuarioId());
        return tarefa;
    }
}
