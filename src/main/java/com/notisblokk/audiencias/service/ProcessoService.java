package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.dto.ProcessoDTO;
import com.notisblokk.audiencias.dto.ProcessoParticipanteDTO;
import com.notisblokk.audiencias.dto.ProcessoRequestDTO;
import com.notisblokk.audiencias.model.Pessoa;
import com.notisblokk.audiencias.model.Processo;
import com.notisblokk.audiencias.model.ProcessoParticipante;
import com.notisblokk.audiencias.model.Vara;
import com.notisblokk.audiencias.model.enums.Competencia;
import com.notisblokk.audiencias.model.enums.StatusProcesso;
import com.notisblokk.audiencias.model.enums.TipoParticipacao;
import com.notisblokk.audiencias.repository.ProcessoParticipanteRepository;
import com.notisblokk.audiencias.repository.ProcessoRepository;
import com.notisblokk.audiencias.repository.VaraRepository;
import com.notisblokk.audiencias.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serviço de lógica de negócio para Processos.
 *
 * Responsável por validações, conversões de DTO e coordenação de operações
 * envolvendo processos e seus participantes.
 */
public class ProcessoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessoService.class);

    private final ProcessoRepository processoRepository = new ProcessoRepository();
    private final ProcessoParticipanteRepository participanteRepository = new ProcessoParticipanteRepository();
    private final VaraRepository varaRepository = new VaraRepository();

    /**
     * Lista todos os processos com dados completos.
     *
     * @return List de ProcessoDTO
     * @throws SQLException se houver erro no banco de dados
     */
    public List<ProcessoDTO> listar() throws SQLException {
        List<Processo> processos = processoRepository.buscarTodos();
        return processos.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca um processo por ID com participantes carregados.
     *
     * @param id ID do processo
     * @return ProcessoDTO com participantes
     * @throws SQLException se houver erro no banco de dados
     */
    public Optional<ProcessoDTO> buscarPorId(Long id) throws SQLException {
        Optional<Processo> processo = processoRepository.buscarPorId(id);
        if (processo.isEmpty()) {
            return Optional.empty();
        }

        // Carregar participantes
        List<ProcessoParticipante> participantes = participanteRepository.buscarPorProcesso(id);
        ProcessoDTO dto = toDTO(processo.get());
        dto.setParticipantes(participantes.stream()
                .map(this::toParticipanteDTO)
                .collect(Collectors.toList()));

        return Optional.of(dto);
    }

    /**
     * Busca um processo pelo número.
     *
     * @param numeroProcesso Número do processo
     * @return ProcessoDTO se encontrado
     * @throws SQLException se houver erro no banco de dados
     */
    public Optional<ProcessoDTO> buscarPorNumero(String numeroProcesso) throws SQLException {
        Optional<Processo> processo = processoRepository.buscarPorNumero(numeroProcesso);
        return processo.map(this::toDTO);
    }

    /**
     * Cria um novo processo.
     *
     * @param request Dados do processo
     * @return ProcessoDTO criado
     * @throws SQLException se houver erro no banco de dados
     */
    public ProcessoDTO criar(ProcessoRequestDTO request) throws SQLException {
        logger.debug("Criando processo: {}", request.getNumeroProcesso());

        // Validar
        List<String> erros = validar(request);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        // Verificar se já existe processo com mesmo número
        if (processoRepository.buscarPorNumero(request.getNumeroProcesso()).isPresent()) {
            throw new IllegalArgumentException("Já existe um processo com o número: " + request.getNumeroProcesso());
        }

        // Buscar vara
        Optional<Vara> vara = varaRepository.buscarPorId(request.getVaraId());
        if (vara.isEmpty()) {
            throw new IllegalArgumentException("Vara não encontrada com ID: " + request.getVaraId());
        }

        // Criar processo
        Processo processo = new Processo();
        processo.setNumeroProcesso(request.getNumeroProcesso());
        processo.setCompetencia(Competencia.valueOf(request.getCompetencia()));
        processo.setArtigo(request.getArtigo());
        processo.setVara(vara.get());
        processo.setStatus(request.getStatus() != null ?
                StatusProcesso.valueOf(request.getStatus()) :
                StatusProcesso.EM_ANDAMENTO);
        processo.setObservacoes(request.getObservacoes());

        Long id = processoRepository.salvar(processo);
        processo.setId(id);

        logger.info("Processo criado com sucesso: ID {}, Número {}", id, processo.getNumeroProcesso());
        return toDTO(processo);
    }

    /**
     * Atualiza um processo existente.
     *
     * @param id ID do processo
     * @param request Dados atualizados
     * @return ProcessoDTO atualizado
     * @throws SQLException se houver erro no banco de dados
     */
    public ProcessoDTO atualizar(Long id, ProcessoRequestDTO request) throws SQLException {
        logger.debug("Atualizando processo ID: {}", id);

        // Verificar existência
        Optional<Processo> processoExistente = processoRepository.buscarPorId(id);
        if (processoExistente.isEmpty()) {
            throw new IllegalArgumentException("Processo não encontrado com ID: " + id);
        }

        // Validar
        List<String> erros = validar(request);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        // Verificar conflito de número (se mudou)
        Processo atual = processoExistente.get();
        if (!atual.getNumeroProcesso().equals(request.getNumeroProcesso())) {
            Optional<Processo> conflito = processoRepository.buscarPorNumero(request.getNumeroProcesso());
            if (conflito.isPresent() && !conflito.get().getId().equals(id)) {
                throw new IllegalArgumentException("Já existe outro processo com o número: " + request.getNumeroProcesso());
            }
        }

        // Buscar vara
        Optional<Vara> vara = varaRepository.buscarPorId(request.getVaraId());
        if (vara.isEmpty()) {
            throw new IllegalArgumentException("Vara não encontrada com ID: " + request.getVaraId());
        }

        // Atualizar
        Processo processo = new Processo();
        processo.setId(id);
        processo.setNumeroProcesso(request.getNumeroProcesso());
        processo.setCompetencia(Competencia.valueOf(request.getCompetencia()));
        processo.setArtigo(request.getArtigo());
        processo.setVara(vara.get());
        processo.setStatus(StatusProcesso.valueOf(request.getStatus()));
        processo.setObservacoes(request.getObservacoes());

        processoRepository.atualizar(processo);
        logger.info("Processo atualizado: ID {}", id);

        return toDTO(processo);
    }

    /**
     * Deleta um processo.
     * Deleta em CASCADE todas as audiências e participantes vinculados.
     *
     * @param id ID do processo
     * @throws SQLException se houver erro no banco de dados
     */
    public void deletar(Long id) throws SQLException {
        logger.debug("Deletando processo ID: {}", id);

        if (processoRepository.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("Processo não encontrado com ID: " + id);
        }

        processoRepository.deletar(id);
        logger.info("Processo deletado: ID {}", id);
    }

    /**
     * Adiciona um participante ao processo.
     *
     * @param processoId ID do processo
     * @param pessoaId ID da pessoa
     * @param tipo Tipo de participação
     * @return ProcessoParticipanteDTO criado
     * @throws SQLException se houver erro no banco de dados
     */
    public ProcessoParticipanteDTO adicionarParticipante(Long processoId, Long pessoaId, TipoParticipacao tipo) throws SQLException {
        logger.debug("Adicionando participante: Processo {}, Pessoa {}, Tipo {}", processoId, pessoaId, tipo);

        // Verificar se processo existe
        if (processoRepository.buscarPorId(processoId).isEmpty()) {
            throw new IllegalArgumentException("Processo não encontrado com ID: " + processoId);
        }

        // Verificar se já existe
        if (participanteRepository.existeParticipacao(processoId, pessoaId, tipo)) {
            throw new IllegalArgumentException("Esta pessoa já participa do processo com este tipo de participação");
        }

        // Criar participante
        ProcessoParticipante participante = new ProcessoParticipante();
        participante.setProcessoId(processoId);
        participante.setPessoaId(pessoaId);
        participante.setTipoParticipacao(tipo);

        Long id = participanteRepository.salvar(participante);
        participante.setId(id);

        logger.info("Participante adicionado ao processo: ID {}", id);

        // Buscar novamente para ter dados completos
        return participanteRepository.buscarPorId(id)
                .map(this::toParticipanteDTO)
                .orElseThrow(() -> new SQLException("Erro ao buscar participante criado"));
    }

    /**
     * Remove um participante do processo.
     *
     * @param processoId ID do processo
     * @param participanteId ID do participante
     * @throws SQLException se houver erro no banco de dados
     */
    public void removerParticipante(Long processoId, Long participanteId) throws SQLException {
        logger.debug("Removendo participante ID {} do processo ID {}", participanteId, processoId);

        Optional<ProcessoParticipante> participante = participanteRepository.buscarPorId(participanteId);
        if (participante.isEmpty()) {
            throw new IllegalArgumentException("Participante não encontrado com ID: " + participanteId);
        }

        if (!participante.get().getProcessoId().equals(processoId)) {
            throw new IllegalArgumentException("Participante não pertence ao processo especificado");
        }

        participanteRepository.deletar(participanteId);
        logger.info("Participante removido do processo: ID {}", participanteId);
    }

    /**
     * Lista todos os participantes de um processo.
     *
     * @param processoId ID do processo
     * @return List de ProcessoParticipanteDTO
     * @throws SQLException se houver erro no banco de dados
     */
    public List<ProcessoParticipanteDTO> listarParticipantes(Long processoId) throws SQLException {
        List<ProcessoParticipante> participantes = participanteRepository.buscarPorProcesso(processoId);
        return participantes.stream()
                .map(this::toParticipanteDTO)
                .collect(Collectors.toList());
    }

    /**
     * Valida dados do processo.
     *
     * @param request Dados a validar
     * @return Lista de erros (vazia se válido)
     */
    private List<String> validar(ProcessoRequestDTO request) {
        List<String> erros = ValidationUtil.novaListaErros();

        ValidationUtil.validarObrigatorio(request.getNumeroProcesso(), "Número do processo", erros);
        ValidationUtil.validarTamanhoMinimo(request.getNumeroProcesso(), 3, "Número do processo", erros);

        if (request.getCompetencia() == null || request.getCompetencia().isEmpty()) {
            erros.add("Competência é obrigatória");
        } else {
            try {
                Competencia.valueOf(request.getCompetencia());
            } catch (IllegalArgumentException e) {
                erros.add("Competência inválida: " + request.getCompetencia());
            }
        }

        if (request.getVaraId() == null) {
            erros.add("Vara é obrigatória");
        }

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                StatusProcesso.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                erros.add("Status inválido: " + request.getStatus());
            }
        }

        return erros;
    }

    /**
     * Converte Processo para ProcessoDTO.
     *
     * @param processo Processo
     * @return ProcessoDTO
     */
    private ProcessoDTO toDTO(Processo processo) {
        ProcessoDTO dto = new ProcessoDTO();
        dto.setId(processo.getId());
        dto.setNumeroProcesso(processo.getNumeroProcesso());
        dto.setCompetencia(processo.getCompetencia());
        dto.setArtigo(processo.getArtigo());
        dto.setVara(processo.getVara());
        dto.setStatus(processo.getStatus());
        dto.setObservacoes(processo.getObservacoes());
        dto.setCriadoEm(processo.getCriadoEm());
        dto.setAtualizadoEm(processo.getAtualizadoEm());
        return dto;
    }

    /**
     * Converte ProcessoParticipante para ProcessoParticipanteDTO.
     *
     * @param participante ProcessoParticipante
     * @return ProcessoParticipanteDTO
     */
    private ProcessoParticipanteDTO toParticipanteDTO(ProcessoParticipante participante) {
        ProcessoParticipanteDTO dto = new ProcessoParticipanteDTO();
        dto.setId(participante.getId());
        dto.setProcessoId(participante.getProcessoId());
        dto.setPessoa(participante.getPessoa());
        dto.setTipoParticipacao(participante.getTipoParticipacao());
        dto.setObservacoes(participante.getObservacoes());
        return dto;
    }
}
