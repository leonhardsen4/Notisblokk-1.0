package com.notisblokk.controller;

import com.notisblokk.model.Nota;
import com.notisblokk.model.NotaDTO;
import com.notisblokk.model.PaginatedResponse;
import com.notisblokk.service.NotaService;
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
 * Controller responsável pelo gerenciamento de notas.
 *
 * <p>Gerencia os endpoints REST para operações CRUD de notas:</p>
 * <ul>
 *   <li>GET /api/notas - Listar todas as notas</li>
 *   <li>GET /api/notas/{id} - Buscar nota por ID</li>
 *   <li>GET /api/notas/etiqueta/{etiquetaId} - Buscar notas por etiqueta</li>
 *   <li>POST /api/notas - Criar nova nota</li>
 *   <li>PUT /api/notas/{id} - Atualizar nota</li>
 *   <li>DELETE /api/notas/{id} - Deletar nota</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class NotaController {

    private static final Logger logger = LoggerFactory.getLogger(NotaController.class);
    private final NotaService notaService;
    private final PDFService pdfService;

    /**
     * Construtor padrão.
     */
    public NotaController() {
        this.notaService = new NotaService();
        this.pdfService = new PDFService();
    }

    /**
     * Classe interna para receber dados da requisição de criar/atualizar nota.
     */
    private static class NotaRequest {
        public Long etiquetaId;
        public Long statusId;
        public String titulo;
        public String conteudo;
        public String prazoFinal; // Formato: yyyy-MM-dd
    }

    /**
     * GET /api/notas
     * Lista todas as notas como DTOs completos.
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

            // Caso contrário, retornar todas as notas
            List<NotaDTO> notas = notaService.listarTodas();

            ctx.json(Map.of(
                "success", true,
                "dados", notas
            ));

            logger.debug("Listadas {} notas", notas.size());

        } catch (Exception e) {
            logger.error("Erro ao listar notas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar notas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/notas/paginado
     * Lista notas com paginação.
     * Query params: pagina (default 1), tamanho (default 10), ordenar (default prazo_final), direcao (default ASC)
     */
    public void listarPaginado(Context ctx) {
        try {
            // Obter parâmetros de paginação
            int pagina = ctx.queryParamAsClass("pagina", Integer.class).getOrDefault(1);
            int tamanho = ctx.queryParamAsClass("tamanho", Integer.class).getOrDefault(10);
            String ordenarPor = ctx.queryParamAsClass("ordenar", String.class).getOrDefault("prazo_final");
            String direcao = ctx.queryParamAsClass("direcao", String.class).getOrDefault("ASC");

            // Buscar notas paginadas
            PaginatedResponse<NotaDTO> response = notaService.listarComPaginacao(
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

            logger.debug("Listadas {} notas (página {}/{})",
                response.getDados().size(), pagina, response.getTotalPaginas());

        } catch (Exception e) {
            logger.error("Erro ao listar notas paginadas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar notas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/notas/{id}
     * Busca uma nota por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Optional<NotaDTO> notaOpt = notaService.buscarPorId(id);

            if (notaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nota não encontrada"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", notaOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao buscar nota", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar nota: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/notas/etiqueta/{etiquetaId}
     * Busca notas por etiqueta.
     */
    public void buscarPorEtiqueta(Context ctx) {
        try {
            Long etiquetaId = Long.parseLong(ctx.pathParam("etiquetaId"));
            List<NotaDTO> notas = notaService.listarPorEtiqueta(etiquetaId);

            ctx.json(Map.of(
                "success", true,
                "dados", notas
            ));

            logger.debug("Encontradas {} notas da etiqueta {}", notas.size(), etiquetaId);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID da etiqueta inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao buscar notas por etiqueta", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar notas: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/notas
     * Cria uma nova nota.
     */
    public void criar(Context ctx) {
        try {
            // Obter dados do JSON
            NotaRequest request = ctx.bodyAsClass(NotaRequest.class);

            // Obter sessão e usuário atual
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            // Criar nota
            NotaDTO nota = notaService.criar(
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
                "message", "Nota criada com sucesso",
                "dados", nota
            ));

            logger.info("Nota criada: {} por usuário {}", request.titulo, usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao criar nota", e);
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/notas/{id}
     * Atualiza uma nota existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Obter dados do JSON
            NotaRequest request = ctx.bodyAsClass(NotaRequest.class);

            // Atualizar nota
            NotaDTO nota = notaService.atualizar(
                id,
                request.etiquetaId,
                request.statusId,
                request.titulo,
                request.conteudo,
                request.prazoFinal
            );

            ctx.json(Map.of(
                "success", true,
                "message", "Nota atualizada com sucesso",
                "dados", nota
            ));

            logger.info("Nota ID {} atualizada", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao atualizar nota", e);
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/notas/{id}
     * Deleta uma nota.
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Verificar se nota existe
            Optional<NotaDTO> notaOpt = notaService.buscarPorId(id);
            if (notaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nota não encontrada"
                ));
                return;
            }

            // Deletar nota
            notaService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Nota deletada com sucesso"
            ));

            logger.warn("Nota ID {} deletada", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao deletar nota", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar nota: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/notas/{id}/pdf
     * Gera PDF de uma nota específica.
     */
    public void gerarPDF(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Gerar PDF
            byte[] pdfBytes = pdfService.gerarPDFNota(id);

            // Buscar nota para obter o título e usar no nome do arquivo
            Optional<NotaDTO> notaOpt = notaService.buscarPorId(id);
            String fileName = "nota_" + id;
            if (notaOpt.isPresent()) {
                String titulo = notaOpt.get().getTitulo();
                // Remover caracteres inválidos do nome do arquivo
                fileName = titulo.replaceAll("[^a-zA-Z0-9\\s-]", "")
                                 .replaceAll("\\s+", "_")
                                 .substring(0, Math.min(titulo.length(), 50));
            }

            // Adicionar timestamp ao nome do arquivo (formato brasileiro: DDMMYYYY_HHmmss)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
            fileName = fileName + "_" + timestamp + ".pdf";

            // Enviar PDF
            ctx.contentType("application/pdf");
            ctx.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            ctx.result(pdfBytes);

            logger.info("PDF gerado para nota ID {}", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao gerar PDF da nota", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao gerar PDF: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/notas/pdf/relatorio
     * Gera PDF com relatório de múltiplas notas.
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

            // Converter IDs para Long e buscar notas
            List<Long> notaIds = ids.stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());

            // Buscar as notas completas
            List<NotaDTO> notasDTO = notaService.listarTodas();
            List<Nota> notas = notasDTO.stream()
                .filter(n -> notaIds.contains(n.getId()))
                .map(this::convertDTOToNota)
                .collect(Collectors.toList());

            if (notas.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nenhuma nota encontrada"
                ));
                return;
            }

            // Gerar PDF
            byte[] pdfBytes = pdfService.gerarPDFRelatorio(notas);

            // Nome do arquivo com timestamp (formato brasileiro: DDMMYYYY_HHmmss)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
            String fileName = "relatorio_notas_" + timestamp + ".pdf";

            // Enviar PDF
            ctx.contentType("application/pdf");
            ctx.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            ctx.result(pdfBytes);

            logger.info("PDF de relatório gerado com {} notas", notas.size());

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
     * Converte NotaDTO para Nota (helper method).
     */
    private Nota convertDTOToNota(NotaDTO dto) {
        Nota nota = new Nota();
        nota.setId(dto.getId());
        nota.setEtiquetaId(dto.getEtiqueta() != null ? dto.getEtiqueta().getId() : null);
        nota.setStatusId(dto.getStatus() != null ? dto.getStatus().getId() : null);
        nota.setTitulo(dto.getTitulo());
        nota.setConteudo(dto.getConteudo());
        nota.setPrazoFinal(dto.getPrazoFinal());
        nota.setDataCriacao(dto.getDataCriacao());
        nota.setDataAtualizacao(dto.getDataAtualizacao());
        nota.setSessaoId(dto.getSessaoId());
        nota.setUsuarioId(dto.getUsuarioId());
        return nota;
    }
}
