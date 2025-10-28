package com.notisblokk.service;

import com.notisblokk.model.Etiqueta;
import com.notisblokk.model.Nota;
import com.notisblokk.model.NotaDTO;
import com.notisblokk.model.PaginatedResponse;
import com.notisblokk.model.StatusNota;
import com.notisblokk.repository.EtiquetaRepository;
import com.notisblokk.repository.NotaRepository;
import com.notisblokk.repository.StatusNotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável pela lógica de negócio relacionada a notas.
 *
 * <p>Coordena operações entre controllers e repositories, implementando
 * regras de negócio para gerenciamento de notas.</p>
 *
 * <p>Responsável por criar DTOs completos que combinam dados de
 * Nota, Etiqueta e StatusNota.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class NotaService {

    private static final Logger logger = LoggerFactory.getLogger(NotaService.class);
    private static final DateTimeFormatter DATE_FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_BR_HIFEN = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final NotaRepository notaRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final StatusNotaRepository statusNotaRepository;

    /**
     * Construtor padrão.
     */
    public NotaService() {
        this.notaRepository = new NotaRepository();
        this.etiquetaRepository = new EtiquetaRepository();
        this.statusNotaRepository = new StatusNotaRepository();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param notaRepository repositório de notas
     * @param etiquetaRepository repositório de etiquetas
     * @param statusNotaRepository repositório de status
     */
    public NotaService(NotaRepository notaRepository, EtiquetaRepository etiquetaRepository,
                      StatusNotaRepository statusNotaRepository) {
        this.notaRepository = notaRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.statusNotaRepository = statusNotaRepository;
    }

    /**
     * Lista todas as notas do sistema como DTOs completos.
     *
     * @return List<NotaDTO> lista de notas com etiquetas e status embutidos
     * @throws Exception se houver erro ao listar
     */
    public List<NotaDTO> listarTodas() throws Exception {
        try {
            List<Nota> notas = notaRepository.buscarTodos();
            return converterParaDTOs(notas);

        } catch (SQLException e) {
            logger.error("Erro ao listar notas", e);
            throw new Exception("Erro ao listar notas: " + e.getMessage(), e);
        }
    }

    /**
     * Lista notas com paginação.
     *
     * @param pagina número da página (começa em 1)
     * @param tamanhoPagina quantidade de registros por página
     * @param ordenarPor campo para ordenação (prazo_final, data_criacao, titulo)
     * @param direcao direção da ordenação (ASC ou DESC)
     * @return PaginatedResponse<NotaDTO> resposta paginada com DTOs completos
     * @throws Exception se houver erro ao listar
     */
    public PaginatedResponse<NotaDTO> listarComPaginacao(int pagina, int tamanhoPagina,
                                                          String ordenarPor, String direcao)
            throws Exception {
        try {
            // Validar parâmetros
            if (pagina < 1) pagina = 1;
            if (tamanhoPagina < 1) tamanhoPagina = 10;
            if (tamanhoPagina > 100) tamanhoPagina = 100; // Limite máximo

            // Buscar total de registros
            long totalRegistros = notaRepository.contarTotal();

            // Buscar notas paginadas
            List<Nota> notas = notaRepository.buscarComPaginacao(pagina, tamanhoPagina, ordenarPor, direcao);
            List<NotaDTO> dtos = converterParaDTOs(notas);

            // Criar resposta paginada
            return new PaginatedResponse<>(dtos, pagina, tamanhoPagina, totalRegistros);

        } catch (SQLException e) {
            logger.error("Erro ao listar notas paginadas", e);
            throw new Exception("Erro ao listar notas: " + e.getMessage(), e);
        }
    }

    /**
     * Lista notas por etiqueta como DTOs completos.
     *
     * @param etiquetaId ID da etiqueta
     * @return List<NotaDTO> lista de notas da etiqueta
     * @throws Exception se houver erro ao listar
     */
    public List<NotaDTO> listarPorEtiqueta(Long etiquetaId) throws Exception {
        try {
            List<Nota> notas = notaRepository.buscarPorEtiqueta(etiquetaId);
            return converterParaDTOs(notas);

        } catch (SQLException e) {
            logger.error("Erro ao listar notas por etiqueta {}", etiquetaId, e);
            throw new Exception("Erro ao listar notas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca uma nota por ID e retorna como DTO completo.
     *
     * @param id ID da nota
     * @return Optional<NotaDTO> nota encontrada ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<NotaDTO> buscarPorId(Long id) throws Exception {
        try {
            Optional<Nota> notaOpt = notaRepository.buscarPorId(id);
            if (notaOpt.isEmpty()) {
                return Optional.empty();
            }

            Nota nota = notaOpt.get();
            Etiqueta etiqueta = etiquetaRepository.buscarPorId(nota.getEtiquetaId())
                .orElseThrow(() -> new Exception("Etiqueta não encontrada"));
            StatusNota status = statusNotaRepository.buscarPorId(nota.getStatusId())
                .orElseThrow(() -> new Exception("Status não encontrado"));

            return Optional.of(NotaDTO.from(nota, etiqueta, status));

        } catch (SQLException e) {
            logger.error("Erro ao buscar nota ID {}", id, e);
            throw new Exception("Erro ao buscar nota: " + e.getMessage(), e);
        }
    }

    /**
     * Cria uma nova nota.
     *
     * @param etiquetaId ID da etiqueta
     * @param statusId ID do status
     * @param titulo título da nota
     * @param conteudo conteúdo da nota
     * @param prazoFinalISO prazo final em formato ISO (yyyy-MM-dd)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return NotaDTO nota criada
     * @throws Exception se houver erro de validação ou ao criar
     */
    public NotaDTO criar(Long etiquetaId, Long statusId, String titulo, String conteudo,
                        String prazoFinalISO, Long sessaoId, Long usuarioId) throws Exception {

        logger.info("Criando nova nota: {}", titulo);

        // Validações
        validarDadosNota(etiquetaId, statusId, titulo, prazoFinalISO);

        try {
            // Verificar se etiqueta existe
            Etiqueta etiqueta = etiquetaRepository.buscarPorId(etiquetaId)
                .orElseThrow(() -> new Exception("Etiqueta não encontrada"));

            // Verificar se status existe
            StatusNota status = statusNotaRepository.buscarPorId(statusId)
                .orElseThrow(() -> new Exception("Status não encontrado"));

            // Parse prazo final (aceita múltiplos formatos)
            LocalDate prazoFinal = parsePrazoFinal(prazoFinalISO);

            // Criar nota
            Nota nota = new Nota();
            nota.setEtiquetaId(etiquetaId);
            nota.setStatusId(statusId);
            nota.setTitulo(titulo.trim());
            nota.setConteudo(conteudo != null ? conteudo.trim() : "");
            nota.setPrazoFinal(prazoFinal);

            nota = notaRepository.salvar(nota, sessaoId, usuarioId);

            logger.info("Nota criada com sucesso: {} (ID: {})", nota.getTitulo(), nota.getId());
            return NotaDTO.from(nota, etiqueta, status);

        } catch (SQLException e) {
            logger.error("Erro ao criar nota: {}", titulo, e);
            throw new Exception("Erro ao criar nota: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza uma nota existente.
     *
     * @param id ID da nota
     * @param etiquetaId ID da etiqueta
     * @param statusId ID do status
     * @param titulo título da nota
     * @param conteudo conteúdo da nota
     * @param prazoFinalISO prazo final em formato ISO (yyyy-MM-dd)
     * @return NotaDTO nota atualizada
     * @throws Exception se houver erro de validação ou ao atualizar
     */
    public NotaDTO atualizar(Long id, Long etiquetaId, Long statusId, String titulo,
                            String conteudo, String prazoFinalISO) throws Exception {

        logger.info("Atualizando nota ID {}", id);

        // Validações
        validarDadosNota(etiquetaId, statusId, titulo, prazoFinalISO);

        try {
            // Buscar nota existente
            Nota nota = notaRepository.buscarPorId(id)
                .orElseThrow(() -> new Exception("Nota não encontrada"));

            // Verificar se etiqueta existe
            Etiqueta etiqueta = etiquetaRepository.buscarPorId(etiquetaId)
                .orElseThrow(() -> new Exception("Etiqueta não encontrada"));

            // Verificar se status existe
            StatusNota status = statusNotaRepository.buscarPorId(statusId)
                .orElseThrow(() -> new Exception("Status não encontrado"));

            // Parse prazo final (aceita múltiplos formatos)
            LocalDate prazoFinal = parsePrazoFinal(prazoFinalISO);

            // Atualizar dados
            nota.setEtiquetaId(etiquetaId);
            nota.setStatusId(statusId);
            nota.setTitulo(titulo.trim());
            nota.setConteudo(conteudo != null ? conteudo.trim() : "");
            nota.setPrazoFinal(prazoFinal);

            notaRepository.atualizar(nota);

            logger.info("Nota atualizada com sucesso: {} (ID: {})", nota.getTitulo(), nota.getId());
            return NotaDTO.from(nota, etiqueta, status);

        } catch (SQLException e) {
            logger.error("Erro ao atualizar nota ID {}", id, e);
            throw new Exception("Erro ao atualizar nota: " + e.getMessage(), e);
        }
    }

    /**
     * Deleta uma nota.
     *
     * @param id ID da nota a ser deletada
     * @throws Exception se houver erro ao deletar
     */
    public void deletar(Long id) throws Exception {
        logger.warn("Deletando nota ID {}", id);

        try {
            notaRepository.deletar(id);
            logger.info("Nota ID {} deletada com sucesso", id);

        } catch (SQLException e) {
            logger.error("Erro ao deletar nota ID {}", id, e);
            throw new Exception("Erro ao deletar nota: " + e.getMessage(), e);
        }
    }

    /**
     * Valida dados de uma nota.
     *
     * @param etiquetaId ID da etiqueta
     * @param statusId ID do status
     * @param titulo título da nota
     * @param prazoFinalISO prazo final
     * @throws Exception se alguma validação falhar
     */
    private void validarDadosNota(Long etiquetaId, Long statusId, String titulo, String prazoFinalISO)
            throws Exception {

        if (etiquetaId == null || etiquetaId <= 0) {
            throw new Exception("Etiqueta é obrigatória");
        }

        if (statusId == null || statusId <= 0) {
            throw new Exception("Status é obrigatório");
        }

        if (titulo == null || titulo.trim().isEmpty()) {
            throw new Exception("Título é obrigatório");
        }

        if (titulo.trim().length() > 200) {
            throw new Exception("Título deve ter no máximo 200 caracteres");
        }

        if (prazoFinalISO == null || prazoFinalISO.trim().isEmpty()) {
            throw new Exception("Prazo final é obrigatório");
        }
    }

    /**
     * Faz parse de uma data em múltiplos formatos.
     * Aceita: dd/MM/yyyy, dd-MM-yyyy, yyyy-MM-dd
     *
     * @param dataStr string da data
     * @return LocalDate data parseada
     * @throws Exception se o formato for inválido
     */
    private LocalDate parsePrazoFinal(String dataStr) throws Exception {
        if (dataStr == null || dataStr.trim().isEmpty()) {
            throw new Exception("Prazo final é obrigatório");
        }

        // Tentar formato brasileiro com barras (dd/MM/yyyy)
        if (dataStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
            try {
                return LocalDate.parse(dataStr, DATE_FORMATTER_BR);
            } catch (DateTimeParseException e) {
                logger.warn("Erro ao parsear data com formato dd/MM/yyyy: {}", dataStr);
            }
        }

        // Tentar formato brasileiro com hífens (dd-MM-yyyy)
        if (dataStr.matches("\\d{2}-\\d{2}-\\d{4}")) {
            try {
                return LocalDate.parse(dataStr, DATE_FORMATTER_BR_HIFEN);
            } catch (DateTimeParseException e) {
                logger.warn("Erro ao parsear data com formato dd-MM-yyyy: {}", dataStr);
            }
        }

        // Tentar formato ISO (yyyy-MM-dd)
        if (dataStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                return LocalDate.parse(dataStr);
            } catch (DateTimeParseException e) {
                logger.warn("Erro ao parsear data com formato yyyy-MM-dd: {}", dataStr);
            }
        }

        throw new Exception("Formato de data inválido. Use dd/MM/yyyy, dd-MM-yyyy ou yyyy-MM-dd");
    }

    /**
     * Converte uma lista de Notas em NotaDTOs.
     *
     * @param notas lista de notas
     * @return List<NotaDTO> lista de DTOs
     * @throws SQLException se houver erro ao buscar etiquetas/status
     */
    private List<NotaDTO> converterParaDTOs(List<Nota> notas) throws SQLException {
        List<NotaDTO> dtos = new ArrayList<>();

        for (Nota nota : notas) {
            Etiqueta etiqueta = etiquetaRepository.buscarPorId(nota.getEtiquetaId())
                .orElse(null);
            StatusNota status = statusNotaRepository.buscarPorId(nota.getStatusId())
                .orElse(null);

            if (etiqueta != null && status != null) {
                dtos.add(NotaDTO.from(nota, etiqueta, status));
            } else {
                logger.warn("Nota ID {} tem etiqueta ou status inválido", nota.getId());
            }
        }

        return dtos;
    }
}
