package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.dto.AudienciaRequest;
import com.notisblokk.audiencias.dto.ParticipanteRequest;
import com.notisblokk.audiencias.model.*;
import com.notisblokk.audiencias.model.enums.*;
import com.notisblokk.audiencias.repository.*;
import com.notisblokk.audiencias.util.DateUtil;
import com.notisblokk.audiencias.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço de lógica de negócio para Audiências Judiciais.
 *
 * <p>Responsável por validações e verificação de conflitos de horário.</p>
 */
public class AudienciaService {

    private static final Logger logger = LoggerFactory.getLogger(AudienciaService.class);
    private final AudienciaRepository audienciaRepository = new AudienciaRepository();
    private final VaraRepository varaRepository = new VaraRepository();
    private final JuizRepository juizRepository = new JuizRepository();
    private final PromotorRepository promotorRepository = new PromotorRepository();
    private final PessoaRepository pessoaRepository = new PessoaRepository();
    private final AdvogadoRepository advogadoRepository = new AdvogadoRepository();
    private final ParticipacaoAudienciaRepository participacaoRepository = new ParticipacaoAudienciaRepository();
    private final RepresentacaoAdvogadoRepository representacaoRepository = new RepresentacaoAdvogadoRepository();

    /**
     * Lista todas as audiências.
     */
    public List<Audiencia> listarTodas() throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: AudienciaService.listarTodas()");
        return audienciaRepository.buscarTodas();
    }

    /**
     * Busca audiência por ID.
     */
    public Optional<Audiencia> buscarPorId(Long id) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: AudienciaService.buscarPorId() - ID: " + id);
        return audienciaRepository.buscarPorId(id);
    }

    /**
     * Busca audiências por data.
     */
    public List<Audiencia> buscarPorData(LocalDate data) throws SQLException {
        return audienciaRepository.buscarPorData(data);
    }

    /**
     * Busca audiências por vara.
     */
    public List<Audiencia> buscarPorVara(Long varaId) throws SQLException {
        return audienciaRepository.buscarPorVara(varaId);
    }

    /**
     * Cria uma nova audiência com validações completas.
     */
    public Audiencia criar(Audiencia audiencia) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: AudienciaService.criar() - Processo: " +
            audiencia.getNumeroProcesso() + ", Data: " + DateUtil.formatDate(audiencia.getDataAudiencia()));

        // Calcular campos derivados
        audiencia.calcularHorarioFim();
        audiencia.calcularDiaSemana();

        // Validar dados
        List<String> erros = validar(audiencia);
        if (ValidationUtil.temErros(erros)) {
            String mensagem = ValidationUtil.formatarErros(erros);
            System.err.println("DEBUG_AUDIENCIAS: Validação falhou - " + mensagem);
            throw new IllegalArgumentException(mensagem);
        }

        // Verificar conflitos de horário
        verificarConflitos(audiencia, null);

        // Salvar
        Long id = audienciaRepository.salvar(audiencia);
        audiencia.setId(id);

        logger.info("Audiência criada - ID: {}, Processo: {}", id, audiencia.getNumeroProcesso());
        return audiencia;
    }

    /**
     * Atualiza uma audiência existente com validações.
     */
    public Audiencia atualizar(Long id, Audiencia audiencia) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: AudienciaService.atualizar() - ID: " + id);

        // Verificar se existe
        if (!audienciaRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Audiência não encontrada com ID: " + id);
        }

        // Calcular campos derivados
        audiencia.calcularHorarioFim();
        audiencia.calcularDiaSemana();

        // Validar dados
        List<String> erros = validar(audiencia);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        // Verificar conflitos (excluindo a própria audiência)
        verificarConflitos(audiencia, id);

        // Atualizar
        audiencia.setId(id);
        audienciaRepository.atualizar(audiencia);

        logger.info("Audiência atualizada - ID: {}", id);
        return audiencia;
    }

    /**
     * Deleta uma audiência.
     */
    public void deletar(Long id) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: AudienciaService.deletar() - ID: " + id);

        if (!audienciaRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Audiência não encontrada com ID: " + id);
        }

        audienciaRepository.deletar(id);
        logger.info("Audiência deletada - ID: {}", id);
    }

    /**
     * Verifica conflitos de horário e lança exceção se houver.
     */
    private void verificarConflitos(Audiencia audiencia, Long audienciaIdExcluir) throws SQLException {
        List<Map<String, Object>> conflitos = audienciaRepository.verificarConflitosHorario(
            audiencia.getDataAudiencia(),
            audiencia.getHorarioInicio(),
            audiencia.getDuracao(),
            audiencia.getVara().getId(),
            audienciaIdExcluir
        );

        if (!conflitos.isEmpty()) {
            StringBuilder mensagem = new StringBuilder("Conflito de horário detectado! Audiências conflitantes: ");
            for (Map<String, Object> conflito : conflitos) {
                mensagem.append("Processo ").append(conflito.get("numeroProcesso"))
                        .append(" (").append(conflito.get("horarioInicio"))
                        .append(" - ").append(conflito.get("horarioFim")).append("), ");
            }

            String erro = mensagem.toString();
            System.err.println("DEBUG_AUDIENCIAS: " + erro);
            throw new IllegalStateException(erro);
        }
    }

    /**
     * Valida os dados de uma audiência.
     */
    private List<String> validar(Audiencia audiencia) {
        List<String> erros = ValidationUtil.novaListaErros();

        // Validar número do processo
        ValidationUtil.validarObrigatorio(audiencia.getNumeroProcesso(), "Número do processo", erros);
        if (audiencia.getNumeroProcesso() != null && !audiencia.getNumeroProcesso().isEmpty()) {
            if (!ValidationUtil.validarNumeroProcesso(audiencia.getNumeroProcesso())) {
                erros.add("Número de processo inválido. Use o formato: NNNNNNN-NN.NNNN.N.NN.NNNN");
                System.err.println("DEBUG_AUDIENCIAS: Validação de processo falhou - " + audiencia.getNumeroProcesso());
            }
        }

        // Validar vara
        ValidationUtil.validarObrigatorio(audiencia.getVara(), "Vara", erros);

        // Validar data
        ValidationUtil.validarObrigatorio(audiencia.getDataAudiencia(), "Data da audiência", erros);

        // Validar horário
        ValidationUtil.validarObrigatorio(audiencia.getHorarioInicio(), "Horário de início", erros);

        // Validar duração
        ValidationUtil.validarObrigatorio(audiencia.getDuracao(), "Duração", erros);
        ValidationUtil.validarIntervalo(audiencia.getDuracao(), 15, 480, "Duração", erros);

        // Validar tipo, formato, competência e status
        ValidationUtil.validarObrigatorio(audiencia.getTipoAudiencia(), "Tipo de audiência", erros);
        ValidationUtil.validarObrigatorio(audiencia.getFormato(), "Formato da audiência", erros);
        ValidationUtil.validarObrigatorio(audiencia.getCompetencia(), "Competência", erros);
        ValidationUtil.validarObrigatorio(audiencia.getStatus(), "Status", erros);

        return erros;
    }

    /**
     * Verifica conflitos de horário sem salvar (para uso em formulários).
     */
    public List<Map<String, Object>> verificarConflitosHorario(
            LocalDate data, LocalTime horarioInicio, Integer duracao,
            Long varaId, Long audienciaIdExcluir) throws SQLException {

        System.out.println("DEBUG_AUDIENCIAS: AudienciaService.verificarConflitosHorario() - " +
            "Data: " + DateUtil.formatDate(data) +
            ", Horário: " + DateUtil.formatTime(horarioInicio) +
            ", Duração: " + duracao + " min");

        return audienciaRepository.verificarConflitosHorario(
            data, horarioInicio, duracao, varaId, audienciaIdExcluir);
    }

    /**
     * Cria audiência a partir do DTO request (converte IDs em objetos).
     */
    public Audiencia criarFromRequest(AudienciaRequest request) throws Exception {
        System.out.println("DEBUG_AUDIENCIAS: AudienciaService.criarFromRequest()");

        // Converter DTO em Audiencia
        Audiencia audiencia = converterRequestParaAudiencia(request);

        // Usar método de criação normal
        audiencia = criar(audiencia);

        // Salvar participantes se houver
        if (request.getParticipantes() != null && !request.getParticipantes().isEmpty()) {
            salvarParticipantes(audiencia, request.getParticipantes());
        }

        return audiencia;
    }

    /**
     * Atualiza audiência a partir do DTO request.
     */
    public Audiencia atualizarFromRequest(Long id, AudienciaRequest request) throws Exception {
        System.out.println("DEBUG_AUDIENCIAS: AudienciaService.atualizarFromRequest() - ID: " + id);

        // Converter DTO em Audiencia
        Audiencia audiencia = converterRequestParaAudiencia(request);

        // Usar método de atualização normal
        audiencia = atualizar(id, audiencia);

        // Remover participantes e representações antigas, depois salvar as novas
        System.out.println("DEBUG_AUDIENCIAS: Removendo participantes e representações antigas da audiência ID: " + id);

        try {
            // Deletar representações antigas primeiro (devido à foreign key)
            representacaoRepository.deletarPorAudiencia(id);
            System.out.println("DEBUG_AUDIENCIAS: Representações antigas removidas");

            // Deletar participações antigas
            participacaoRepository.deletarPorAudiencia(id);
            System.out.println("DEBUG_AUDIENCIAS: Participações antigas removidas");

        } catch (Exception e) {
            System.err.println("DEBUG_AUDIENCIAS: Erro ao remover participantes/representações antigas: " + e.getMessage());
            throw new Exception("Erro ao remover participantes antigos: " + e.getMessage(), e);
        }

        // Salvar novos participantes
        if (request.getParticipantes() != null && !request.getParticipantes().isEmpty()) {
            System.out.println("DEBUG_AUDIENCIAS: Salvando " + request.getParticipantes().size() + " novos participantes");
            salvarParticipantes(audiencia, request.getParticipantes());
        } else {
            System.out.println("DEBUG_AUDIENCIAS: Nenhum participante para salvar");
        }

        return audiencia;
    }

    /**
     * Converte AudienciaRequest (DTO) em Audiencia (entidade completa).
     * Busca objetos relacionados (Vara, Juiz, Promotor) pelos IDs.
     */
    private Audiencia converterRequestParaAudiencia(AudienciaRequest request) throws Exception {
        Audiencia audiencia = new Audiencia();

        // Dados básicos
        audiencia.setNumeroProcesso(request.getNumeroProcesso());

        // Converter datas usando DateUtil (já configurado no Jackson)
        audiencia.setDataAudiencia(DateUtil.parseDate(request.getDataAudiencia()));
        audiencia.setHorarioInicio(DateUtil.parseTime(request.getHorarioInicio()));
        audiencia.setHorarioFim(DateUtil.parseTime(request.getHorarioFim()));

        // Calcular duração
        if (audiencia.getHorarioInicio() != null && audiencia.getHorarioFim() != null) {
            int inicioMinutos = audiencia.getHorarioInicio().getHour() * 60 + audiencia.getHorarioInicio().getMinute();
            int fimMinutos = audiencia.getHorarioFim().getHour() * 60 + audiencia.getHorarioFim().getMinute();
            audiencia.setDuracao(fimMinutos - inicioMinutos);
        }

        // Buscar Vara pelo ID
        if (request.getVaraId() != null) {
            Vara vara = varaRepository.buscarPorId(request.getVaraId())
                .orElseThrow(() -> new IllegalArgumentException("Vara não encontrada com ID: " + request.getVaraId()));
            audiencia.setVara(vara);
        }

        // Buscar Juiz pelo ID (opcional)
        if (request.getJuizId() != null) {
            Juiz juiz = juizRepository.buscarPorId(request.getJuizId())
                .orElseThrow(() -> new IllegalArgumentException("Juiz não encontrado com ID: " + request.getJuizId()));
            audiencia.setJuiz(juiz);
        }

        // Buscar Promotor pelo ID (opcional)
        if (request.getPromotorId() != null) {
            Promotor promotor = promotorRepository.buscarPorId(request.getPromotorId())
                .orElseThrow(() -> new IllegalArgumentException("Promotor não encontrado com ID: " + request.getPromotorId()));
            audiencia.setPromotor(promotor);
        }

        // Converter enums
        if (request.getTipoAudiencia() != null) {
            audiencia.setTipoAudiencia(TipoAudiencia.valueOf(request.getTipoAudiencia()));
        }
        if (request.getFormato() != null) {
            audiencia.setFormato(FormatoAudiencia.valueOf(request.getFormato()));
        }
        if (request.getStatus() != null) {
            audiencia.setStatus(StatusAudiencia.valueOf(request.getStatus()));
        }

        // Competencia padrão (enquanto não implementado no frontend)
        audiencia.setCompetencia(Competencia.CRIMINAL);

        // Informações adicionais
        audiencia.setArtigo(request.getArtigo());
        audiencia.setReuPreso(request.getReuPreso() != null ? request.getReuPreso() : false);
        audiencia.setAgendamentoTeams(request.getAgendamentoTeams() != null ? request.getAgendamentoTeams() : false);
        audiencia.setReconhecimento(request.getReconhecimento() != null ? request.getReconhecimento() : false);
        audiencia.setDepoimentoEspecial(request.getDepoimentoEspecial() != null ? request.getDepoimentoEspecial() : false);
        audiencia.setObservacoes(request.getObservacoes());
        // Nota: linkVideoconferencia e ataAudiencia não existem no modelo Audiencia (foram removidos do schema)

        System.out.println("DEBUG_AUDIENCIAS: Audiencia convertida do request - Processo: " +
            audiencia.getNumeroProcesso() + ", Vara: " + audiencia.getVara().getNome());

        return audiencia;
    }

    /**
     * Salva participantes da audiência.
     * Para cada participante, cria ParticipacaoAudiencia e, se houver advogado, cria RepresentacaoAdvogado.
     */
    private void salvarParticipantes(Audiencia audiencia, List<ParticipanteRequest> participantesRequest) throws Exception {
        System.out.println("DEBUG_AUDIENCIAS: Salvando " + participantesRequest.size() + " participantes para audiência ID: " + audiencia.getId());

        for (int i = 0; i < participantesRequest.size(); i++) {
            ParticipanteRequest partRequest = participantesRequest.get(i);
            try {
                System.out.println("DEBUG_AUDIENCIAS: Processando participante " + (i+1) + "/" + participantesRequest.size() +
                    " - PessoaID: " + partRequest.getPessoaId() + ", Tipo: " + partRequest.getTipoParticipacao());

                // Buscar pessoa
                Pessoa pessoa = pessoaRepository.buscarPorId(partRequest.getPessoaId())
                    .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com ID: " + partRequest.getPessoaId()));

                // Criar participação
                ParticipacaoAudiencia participacao = new ParticipacaoAudiencia();
                participacao.setAudiencia(audiencia);
                participacao.setPessoa(pessoa);
                participacao.setTipo(TipoParticipacao.valueOf(partRequest.getTipoParticipacao()));
                participacao.setIntimado(partRequest.getIntimado() != null ? partRequest.getIntimado() : false);
                participacao.setObservacoes(partRequest.getObservacoes());

                // Salvar participação
                Long participacaoId = participacaoRepository.salvar(participacao);
                System.out.println("DEBUG_AUDIENCIAS: Participação salva - ID: " + participacaoId + ", Pessoa: " + pessoa.getNome());

                // Se tiver advogado, criar representação
                if (partRequest.getAdvogadoId() != null && partRequest.getAdvogadoId() > 0) {
                    System.out.println("DEBUG_AUDIENCIAS: Salvando representação - AdvogadoID: " + partRequest.getAdvogadoId() + ", Tipo: " + partRequest.getTipoRepresentacao());

                    Advogado advogado = advogadoRepository.buscarPorId(partRequest.getAdvogadoId())
                        .orElseThrow(() -> new IllegalArgumentException("Advogado não encontrado com ID: " + partRequest.getAdvogadoId()));

                    RepresentacaoAdvogado representacao = new RepresentacaoAdvogado();
                    representacao.setAudiencia(audiencia);
                    representacao.setAdvogado(advogado);
                    representacao.setCliente(pessoa);

                    if (partRequest.getTipoRepresentacao() != null && !partRequest.getTipoRepresentacao().isEmpty()) {
                        representacao.setTipo(TipoRepresentacao.valueOf(partRequest.getTipoRepresentacao()));
                    } else {
                        throw new IllegalArgumentException("Tipo de representação não informado para advogado ID: " + partRequest.getAdvogadoId());
                    }

                    Long representacaoId = representacaoRepository.salvar(representacao);
                    System.out.println("DEBUG_AUDIENCIAS: Representação salva - ID: " + representacaoId + ", Advogado: " + advogado.getNome());
                }
            } catch (Exception e) {
                System.err.println("DEBUG_AUDIENCIAS: ERRO ao salvar participante " + (i+1) + ": " + e.getMessage());
                e.printStackTrace();
                throw new Exception("Erro ao salvar participante " + (i+1) + ": " + e.getMessage(), e);
            }
        }

        System.out.println("DEBUG_AUDIENCIAS: Todos os participantes foram salvos com sucesso");
    }

    /**
     * Calcula o nível de criticidade de uma audiência com base nos dias restantes.
     *
     * @param audiencia Audiência a ser analisada
     * @return Nível de criticidade: CRITICO (0-3 dias), ALTO (4-7 dias), MEDIO (8-15 dias), BAIXO (>15 dias)
     */
    public String calcularCriticidade(Audiencia audiencia) {
        if (audiencia == null || audiencia.getDataAudiencia() == null) {
            return "BAIXO";
        }

        LocalDate hoje = LocalDate.now();
        long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoje, audiencia.getDataAudiencia());

        if (diasRestantes < 0) {
            // Audiência já passou
            return "BAIXO";
        } else if (diasRestantes <= 3) {
            return "CRITICO";
        } else if (diasRestantes <= 7) {
            return "ALTO";
        } else if (diasRestantes <= 15) {
            return "MEDIO";
        } else {
            return "BAIXO";
        }
    }

    /**
     * Lista informações ausentes/pendentes em uma audiência.
     *
     * @param audiencia Audiência a ser verificada
     * @return Lista de strings descrevendo o que falta (ex: "Juiz", "Promotor", "Tipo")
     * @throws SQLException Se houver erro ao buscar participantes
     */
    public List<String> listarInformacoesAusentes(Audiencia audiencia) throws SQLException {
        List<String> ausentes = new java.util.ArrayList<>();

        if (audiencia == null) {
            return ausentes;
        }

        // Verificar campos obrigatórios/importantes
        if (audiencia.getJuiz() == null) {
            ausentes.add("Juiz");
        }

        if (audiencia.getPromotor() == null) {
            ausentes.add("Promotor");
        }

        if (audiencia.getTipoAudiencia() == null) {
            ausentes.add("Tipo de Audiência");
        }

        if (audiencia.getFormato() == null) {
            ausentes.add("Formato");
        }

        // Verificar participantes
        List<ParticipacaoAudiencia> participantes = participacaoRepository.buscarPorAudiencia(audiencia.getId());
        if (participantes == null || participantes.isEmpty()) {
            ausentes.add("Nenhum participante cadastrado");
        } else {
            // Contar participantes não intimados
            long naoIntimados = participantes.stream()
                .filter(p -> p.getIntimado() == null || !p.getIntimado())
                .count();

            if (naoIntimados > 0) {
                ausentes.add(naoIntimados + " participante(s) não intimado(s)");
            }
        }

        return ausentes;
    }

    /**
     * Calcula quantos dias faltam para uma audiência.
     *
     * @param audiencia Audiência
     * @return Número de dias restantes (negativo se já passou)
     */
    public long calcularDiasRestantes(Audiencia audiencia) {
        if (audiencia == null || audiencia.getDataAudiencia() == null) {
            return -1;
        }

        LocalDate hoje = LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(hoje, audiencia.getDataAudiencia());
    }

    /**
     * Busca audiências com alertas nos próximos N dias.
     * Retorna apenas audiências que têm alguma informação ausente.
     *
     * @param diasProximos Quantidade de dias para buscar (ex: 3, 7, 15)
     * @return Lista de audiências com alertas
     * @throws SQLException Se houver erro no banco
     */
    public List<Audiencia> buscarAudienciasComAlertas(int diasProximos) throws SQLException {
        LocalDate hoje = LocalDate.now();
        LocalDate dataLimite = hoje.plusDays(diasProximos);

        // Buscar todas as audiências do período
        List<Audiencia> todasAudiencias = audienciaRepository.buscarTodas();
        List<Audiencia> comAlertas = new java.util.ArrayList<>();

        for (Audiencia aud : todasAudiencias) {
            if (aud.getDataAudiencia() == null) continue;

            // Filtrar por data
            if (aud.getDataAudiencia().isAfter(hoje.minusDays(1)) &&
                aud.getDataAudiencia().isBefore(dataLimite.plusDays(1))) {

                // Verificar se tem informações ausentes
                List<String> ausentes = listarInformacoesAusentes(aud);
                if (!ausentes.isEmpty()) {
                    comAlertas.add(aud);
                }
            }
        }

        return comAlertas;
    }

    /**
     * Busca audiências por múltiplos critérios (busca avançada).
     * Pesquisa em: número do processo, vara, competência, juiz, promotor, tipo, status.
     *
     * @param termo Termo de busca
     * @return Lista de audiências que correspondem ao termo
     * @throws SQLException Se houver erro no banco
     */
    public List<Audiencia> buscarAvancada(String termo) throws SQLException {
        if (termo == null || termo.trim().isEmpty()) {
            return listarTodas();
        }

        String termoLower = termo.toLowerCase();
        List<Audiencia> todas = listarTodas();
        List<Audiencia> resultados = new java.util.ArrayList<>();

        for (Audiencia aud : todas) {
            boolean match = false;

            // Buscar em número do processo
            if (aud.getNumeroProcesso() != null &&
                aud.getNumeroProcesso().toLowerCase().contains(termoLower)) {
                match = true;
            }

            // Buscar em vara
            if (aud.getVara() != null && aud.getVara().getNome() != null &&
                aud.getVara().getNome().toLowerCase().contains(termoLower)) {
                match = true;
            }

            // Buscar em competência
            if (aud.getCompetencia() != null &&
                aud.getCompetencia().getDescricao().toLowerCase().contains(termoLower)) {
                match = true;
            }

            // Buscar em juiz
            if (aud.getJuiz() != null && aud.getJuiz().getNome() != null &&
                aud.getJuiz().getNome().toLowerCase().contains(termoLower)) {
                match = true;
            }

            // Buscar em promotor
            if (aud.getPromotor() != null && aud.getPromotor().getNome() != null &&
                aud.getPromotor().getNome().toLowerCase().contains(termoLower)) {
                match = true;
            }

            // Buscar em tipo
            if (aud.getTipoAudiencia() != null &&
                aud.getTipoAudiencia().getDescricao().toLowerCase().contains(termoLower)) {
                match = true;
            }

            // Buscar em status
            if (aud.getStatus() != null &&
                aud.getStatus().name().toLowerCase().contains(termoLower)) {
                match = true;
            }

            if (match) {
                resultados.add(aud);
            }
        }

        return resultados;
    }
}
