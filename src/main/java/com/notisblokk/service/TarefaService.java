package com.notisblokk.service;

import com.notisblokk.model.Etiqueta;
import com.notisblokk.model.Tarefa;
import com.notisblokk.model.TarefaDTO;
import com.notisblokk.model.PaginatedResponse;
import com.notisblokk.model.StatusTarefa;
import com.notisblokk.repository.EtiquetaRepository;
import com.notisblokk.repository.TarefaRepository;
import com.notisblokk.repository.StatusTarefaRepository;
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
 * Serviço responsável pela lógica de negócio relacionada a tarefas.
 *
 * <p>Coordena operações entre controllers e repositories, implementando
 * regras de negócio para gerenciamento de tarefas.</p>
 *
 * <p>Responsável por criar DTOs completos que combinam dados de
 * Tarefa, Etiqueta e StatusTarefa.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class TarefaService {

    private static final Logger logger = LoggerFactory.getLogger(TarefaService.class);
    private static final DateTimeFormatter DATE_FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_BR_HIFEN = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final TarefaRepository tarefaRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final StatusTarefaRepository statusTarefaRepository;

    /**
     * Construtor padrão.
     */
    public TarefaService() {
        this.tarefaRepository = new TarefaRepository();
        this.etiquetaRepository = new EtiquetaRepository();
        this.statusTarefaRepository = new StatusTarefaRepository();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param tarefaRepository repositório de tarefas
     * @param etiquetaRepository repositório de etiquetas
     * @param statusTarefaRepository repositório de status
     */
    public TarefaService(TarefaRepository tarefaRepository, EtiquetaRepository etiquetaRepository,
                      StatusTarefaRepository statusTarefaRepository) {
        this.tarefaRepository = tarefaRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.statusTarefaRepository = statusTarefaRepository;
    }

    /**
     * Lista todas as tarefas do sistema como DTOs completos.
     *
     * <p><b>OTIMIZADO:</b> Utiliza query com JOIN para evitar N+1 queries.
     * Busca tarefas, etiquetas e status em uma única consulta ao banco.</p>
     *
     * @return List<TarefaDTO> lista de tarefas com etiquetas e status embutidos
     * @throws Exception se houver erro ao listar
     */
    public List<TarefaDTO> listarTodas() throws Exception {
        try {
            // Usa método otimizado que faz JOIN e retorna DTOs diretamente
            return tarefaRepository.buscarTodasComRelacionamentos();

        } catch (SQLException e) {
            logger.error("Erro ao listar tarefas", e);
            throw new Exception("Erro ao listar tarefas: " + e.getMessage(), e);
        }
    }

    /**
     * Lista tarefas com paginação.
     *
     * @param pagina número da página (começa em 1)
     * @param tamanhoPagina quantidade de registros por página
     * @param ordenarPor campo para ordenação (prazo_final, data_criacao, titulo)
     * @param direcao direção da ordenação (ASC ou DESC)
     * @return PaginatedResponse<TarefaDTO> resposta paginada com DTOs completos
     * @throws Exception se houver erro ao listar
     */
    public PaginatedResponse<TarefaDTO> listarComPaginacao(int pagina, int tamanhoPagina,
                                                          String ordenarPor, String direcao)
            throws Exception {
        try {
            // Validar parâmetros
            if (pagina < 1) pagina = 1;
            if (tamanhoPagina < 1) tamanhoPagina = 10;
            if (tamanhoPagina > 100) tamanhoPagina = 100; // Limite máximo

            // Buscar total de registros
            long totalRegistros = tarefaRepository.contarTotal();

            // Buscar tarefas paginadas
            List<Tarefa> tarefas = tarefaRepository.buscarComPaginacao(pagina, tamanhoPagina, ordenarPor, direcao);
            List<TarefaDTO> dtos = converterParaDTOs(tarefas);

            // Criar resposta paginada
            return new PaginatedResponse<>(dtos, pagina, tamanhoPagina, totalRegistros);

        } catch (SQLException e) {
            logger.error("Erro ao listar tarefas paginadas", e);
            throw new Exception("Erro ao listar tarefas: " + e.getMessage(), e);
        }
    }

    /**
     * Lista tarefas por etiqueta como DTOs completos.
     *
     * @param etiquetaId ID da etiqueta
     * @return List<TarefaDTO> lista de tarefas da etiqueta
     * @throws Exception se houver erro ao listar
     */
    public List<TarefaDTO> listarPorEtiqueta(Long etiquetaId) throws Exception {
        try {
            List<Tarefa> tarefas = tarefaRepository.buscarPorEtiqueta(etiquetaId);
            return converterParaDTOs(tarefas);

        } catch (SQLException e) {
            logger.error("Erro ao listar tarefas por etiqueta {}", etiquetaId, e);
            throw new Exception("Erro ao listar tarefas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca tarefas por texto no título ou conteúdo.
     *
     * <p>Busca case-insensitive que procura o termo fornecido tanto no título
     * quanto no conteúdo das tarefas. Retorna DTOs completos com etiquetas e status.</p>
     *
     * @param termo termo de busca (pode conter espaços)
     * @return List<TarefaDTO> lista de tarefas que contêm o termo
     * @throws Exception se houver erro ao buscar
     */
    public List<TarefaDTO> buscarPorTexto(String termo) throws Exception {
        try {
            if (termo == null || termo.trim().isEmpty()) {
                logger.debug("Termo de busca vazio, retornando lista vazia");
                return new ArrayList<>();
            }

            return tarefaRepository.buscarPorTexto(termo.trim());

        } catch (SQLException e) {
            logger.error("Erro ao buscar tarefas por texto '{}'", termo, e);
            throw new Exception("Erro ao buscar tarefas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca tarefas por intervalo de prazo final.
     *
     * <p>Retorna tarefas cujo prazo final está entre as datas especificadas.
     * Ambas as datas são inclusivas.</p>
     *
     * @param dataInicio data inicial do intervalo (formato: yyyy-MM-dd, dd/MM/yyyy ou dd-MM-yyyy)
     * @param dataFim data final do intervalo (formato: yyyy-MM-dd, dd/MM/yyyy ou dd-MM-yyyy)
     * @return List<TarefaDTO> lista de tarefas no intervalo especificado
     * @throws Exception se houver erro ao buscar ou formato de data inválido
     */
    public List<TarefaDTO> buscarPorIntervaloPrazo(String dataInicio, String dataFim) throws Exception {
        try {
            if (dataInicio == null || dataInicio.trim().isEmpty()) {
                throw new Exception("Data de início é obrigatória");
            }

            if (dataFim == null || dataFim.trim().isEmpty()) {
                throw new Exception("Data de fim é obrigatória");
            }

            // Parse datas
            LocalDate inicio = parsePrazoFinal(dataInicio);
            LocalDate fim = parsePrazoFinal(dataFim);

            // Validar que data de início não é posterior à data de fim
            if (inicio.isAfter(fim)) {
                throw new Exception("Data de início não pode ser posterior à data de fim");
            }

            return tarefaRepository.buscarPorIntervaloPrazo(inicio, fim);

        } catch (SQLException e) {
            logger.error("Erro ao buscar tarefas por intervalo de prazo: {} - {}", dataInicio, dataFim, e);
            throw new Exception("Erro ao buscar tarefas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca uma tarefa por ID e retorna como DTO completo.
     *
     * @param id ID da tarefa
     * @return Optional<TarefaDTO> tarefa encontrada ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<TarefaDTO> buscarPorId(Long id) throws Exception {
        try {
            Optional<Tarefa> tarefaOpt = tarefaRepository.buscarPorId(id);
            if (tarefaOpt.isEmpty()) {
                return Optional.empty();
            }

            Tarefa tarefa = tarefaOpt.get();
            Etiqueta etiqueta = etiquetaRepository.buscarPorId(tarefa.getEtiquetaId())
                .orElseThrow(() -> new Exception("Etiqueta não encontrada"));
            StatusTarefa status = statusTarefaRepository.buscarPorId(tarefa.getStatusId())
                .orElseThrow(() -> new Exception("Status não encontrado"));

            return Optional.of(TarefaDTO.from(tarefa, etiqueta, status));

        } catch (SQLException e) {
            logger.error("Erro ao buscar tarefa ID {}", id, e);
            throw new Exception("Erro ao buscar tarefa: " + e.getMessage(), e);
        }
    }

    /**
     * Cria uma nova tarefa.
     *
     * @param etiquetaId ID da etiqueta
     * @param statusId ID do status
     * @param titulo título da tarefa
     * @param conteudo conteúdo da tarefa
     * @param prazoFinalISO prazo final em formato ISO (yyyy-MM-dd)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return TarefaDTO tarefa criada
     * @throws Exception se houver erro de validação ou ao criar
     */
    public TarefaDTO criar(Long etiquetaId, Long statusId, String titulo, String conteudo,
                        String prazoFinalISO, Long sessaoId, Long usuarioId) throws Exception {

        logger.info("Criando nova tarefa: {}", titulo);

        // Validações
        validarDadosTarefa(etiquetaId, statusId, titulo, prazoFinalISO);

        try {
            // Verificar se etiqueta existe
            Etiqueta etiqueta = etiquetaRepository.buscarPorId(etiquetaId)
                .orElseThrow(() -> new Exception("Etiqueta não encontrada"));

            // Verificar se status existe
            StatusTarefa status = statusTarefaRepository.buscarPorId(statusId)
                .orElseThrow(() -> new Exception("Status não encontrado"));

            // Parse prazo final (aceita múltiplos formatos)
            LocalDate prazoFinal = parsePrazoFinal(prazoFinalISO);

            // Criar tarefa
            Tarefa tarefa = new Tarefa();
            tarefa.setEtiquetaId(etiquetaId);
            tarefa.setStatusId(statusId);
            tarefa.setTitulo(titulo.trim());
            tarefa.setConteudo(conteudo != null ? conteudo.trim() : "");
            tarefa.setPrazoFinal(prazoFinal);

            tarefa = tarefaRepository.salvar(tarefa, sessaoId, usuarioId);

            logger.info("Tarefa criada com sucesso: {} (ID: {})", tarefa.getTitulo(), tarefa.getId());
            return TarefaDTO.from(tarefa, etiqueta, status);

        } catch (SQLException e) {
            logger.error("Erro ao criar tarefa: {}", titulo, e);
            throw new Exception("Erro ao criar tarefa: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza uma tarefa existente.
     *
     * @param id ID da tarefa
     * @param etiquetaId ID da etiqueta
     * @param statusId ID do status
     * @param titulo título da tarefa
     * @param conteudo conteúdo da tarefa
     * @param prazoFinalISO prazo final em formato ISO (yyyy-MM-dd)
     * @return TarefaDTO tarefa atualizada
     * @throws Exception se houver erro de validação ou ao atualizar
     */
    public TarefaDTO atualizar(Long id, Long etiquetaId, Long statusId, String titulo,
                            String conteudo, String prazoFinalISO) throws Exception {

        logger.info("Atualizando tarefa ID {}", id);

        // Validações
        validarDadosTarefa(etiquetaId, statusId, titulo, prazoFinalISO);

        try {
            // Buscar tarefa existente
            Tarefa tarefa = tarefaRepository.buscarPorId(id)
                .orElseThrow(() -> new Exception("Tarefa não encontrada"));

            // Verificar se etiqueta existe
            Etiqueta etiqueta = etiquetaRepository.buscarPorId(etiquetaId)
                .orElseThrow(() -> new Exception("Etiqueta não encontrada"));

            // Verificar se status existe
            StatusTarefa status = statusTarefaRepository.buscarPorId(statusId)
                .orElseThrow(() -> new Exception("Status não encontrado"));

            // Parse prazo final (aceita múltiplos formatos)
            LocalDate prazoFinal = parsePrazoFinal(prazoFinalISO);

            // Atualizar dados
            tarefa.setEtiquetaId(etiquetaId);
            tarefa.setStatusId(statusId);
            tarefa.setTitulo(titulo.trim());
            tarefa.setConteudo(conteudo != null ? conteudo.trim() : "");
            tarefa.setPrazoFinal(prazoFinal);

            tarefaRepository.atualizar(tarefa);

            logger.info("Tarefa atualizada com sucesso: {} (ID: {})", tarefa.getTitulo(), tarefa.getId());
            return TarefaDTO.from(tarefa, etiqueta, status);

        } catch (SQLException e) {
            logger.error("Erro ao atualizar tarefa ID {}", id, e);
            throw new Exception("Erro ao atualizar tarefa: " + e.getMessage(), e);
        }
    }

    /**
     * Deleta uma tarefa.
     *
     * @param id ID da tarefa a ser deletada
     * @throws Exception se houver erro ao deletar
     */
    public void deletar(Long id) throws Exception {
        logger.warn("Deletando tarefa ID {}", id);

        try {
            tarefaRepository.deletar(id);
            logger.info("Tarefa ID {} deletada com sucesso", id);

        } catch (SQLException e) {
            logger.error("Erro ao deletar tarefa ID {}", id, e);
            throw new Exception("Erro ao deletar tarefa: " + e.getMessage(), e);
        }
    }

    /**
     * Valida dados de uma tarefa.
     *
     * @param etiquetaId ID da etiqueta
     * @param statusId ID do status
     * @param titulo título da tarefa
     * @param prazoFinalISO prazo final
     * @throws Exception se alguma validação falhar
     */
    private void validarDadosTarefa(Long etiquetaId, Long statusId, String titulo, String prazoFinalISO)
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
     * Converte uma lista de Tarefas em TarefaDTOs.
     *
     * @param tarefas lista de tarefas
     * @return List<TarefaDTO> lista de DTOs
     * @throws SQLException se houver erro ao buscar etiquetas/status
     */
    private List<TarefaDTO> converterParaDTOs(List<Tarefa> tarefas) throws SQLException {
        List<TarefaDTO> dtos = new ArrayList<>();

        for (Tarefa tarefa : tarefas) {
            Etiqueta etiqueta = etiquetaRepository.buscarPorId(tarefa.getEtiquetaId())
                .orElse(null);
            StatusTarefa status = statusTarefaRepository.buscarPorId(tarefa.getStatusId())
                .orElse(null);

            if (etiqueta != null && status != null) {
                dtos.add(TarefaDTO.from(tarefa, etiqueta, status));
            } else {
                logger.warn("Tarefa ID {} tem etiqueta ou status inválido", tarefa.getId());
            }
        }

        return dtos;
    }
}
