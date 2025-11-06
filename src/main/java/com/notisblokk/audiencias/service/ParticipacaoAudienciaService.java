package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.dto.ParticipanteDetalhesDTO;
import com.notisblokk.audiencias.model.*;
import com.notisblokk.audiencias.repository.*;
import com.notisblokk.audiencias.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de lógica de negócio para Participação em Audiências.
 */
public class ParticipacaoAudienciaService {

    private static final Logger logger = LoggerFactory.getLogger(ParticipacaoAudienciaService.class);
    private final ParticipacaoAudienciaRepository participacaoRepository = new ParticipacaoAudienciaRepository();
    private final PessoaRepository pessoaRepository = new PessoaRepository();
    private final AdvogadoRepository advogadoRepository = new AdvogadoRepository();
    private final RepresentacaoAdvogadoRepository representacaoRepository = new RepresentacaoAdvogadoRepository();

    public List<ParticipacaoAudiencia> buscarPorAudiencia(Long audienciaId) throws SQLException {
        return participacaoRepository.buscarPorAudiencia(audienciaId);
    }

    /**
     * Busca participantes de uma audiência com dados completos (pessoa + advogado).
     * Retorna DTOs enriquecidos para exibição no frontend.
     */
    public List<ParticipanteDetalhesDTO> buscarDetalhesParticipantes(Long audienciaId) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaService.buscarDetalhesParticipantes() - AudienciaID: " + audienciaId);

        List<ParticipacaoAudiencia> participacoes = participacaoRepository.buscarPorAudiencia(audienciaId);
        List<ParticipanteDetalhesDTO> detalhes = new ArrayList<>();

        for (ParticipacaoAudiencia participacao : participacoes) {
            ParticipanteDetalhesDTO dto = new ParticipanteDetalhesDTO();

            // Carregar dados da pessoa
            Long pessoaId = participacao.getPessoa().getId();
            Optional<Pessoa> pessoaOpt = pessoaRepository.buscarPorId(pessoaId);

            if (pessoaOpt.isPresent()) {
                Pessoa pessoa = pessoaOpt.get();
                dto.setPessoaId(pessoa.getId());
                dto.setPessoaNome(pessoa.getNome());
                dto.setPessoaCpf(pessoa.getCpf());
                dto.setPessoaTelefone(pessoa.getTelefone());
                dto.setPessoaEmail(pessoa.getEmail());
            }

            // Dados da participação
            dto.setTipoParticipacao(participacao.getTipo().name());
            dto.setIntimado(participacao.getIntimado());
            dto.setObservacoes(participacao.getObservacoes());

            // Buscar representação de advogado para esta pessoa nesta audiência
            List<RepresentacaoAdvogado> representacoes = representacaoRepository.buscarPorAudiencia(audienciaId);
            for (RepresentacaoAdvogado repr : representacoes) {
                if (repr.getCliente().getId().equals(pessoaId)) {
                    // Carregar dados completos do advogado
                    Long advogadoId = repr.getAdvogado().getId();
                    Optional<Advogado> advogadoOpt = advogadoRepository.buscarPorId(advogadoId);

                    if (advogadoOpt.isPresent()) {
                        Advogado advogado = advogadoOpt.get();
                        dto.setAdvogadoId(advogado.getId());
                        dto.setAdvogadoNome(advogado.getNome());
                        dto.setAdvogadoOab(advogado.getOab());
                        dto.setAdvogadoTelefone(advogado.getTelefone());
                        dto.setAdvogadoEmail(advogado.getEmail());
                        dto.setTipoRepresentacao(repr.getTipo().name());
                    }
                    break; // Apenas uma representação por pessoa por audiência
                }
            }

            detalhes.add(dto);
            System.out.println("DEBUG_AUDIENCIAS: Participante carregado - " + dto.getPessoaNome() +
                " (" + dto.getTipoParticipacao() + ")" +
                (dto.getAdvogadoNome() != null ? " - Advogado: " + dto.getAdvogadoNome() : ""));
        }

        System.out.println("DEBUG_AUDIENCIAS: Total de participantes carregados: " + detalhes.size());
        return detalhes;
    }

    public List<ParticipacaoAudiencia> buscarPorPessoa(Long pessoaId) throws SQLException {
        return participacaoRepository.buscarPorPessoa(pessoaId);
    }

    public Optional<ParticipacaoAudiencia> buscarPorId(Long id) throws SQLException {
        return participacaoRepository.buscarPorId(id);
    }

    public ParticipacaoAudiencia criar(ParticipacaoAudiencia participacao) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaService.criar() - " +
            "Audiência: " + participacao.getAudiencia().getId() +
            ", Pessoa: " + participacao.getPessoa().getId() +
            ", Tipo: " + participacao.getTipo());

        List<String> erros = validar(participacao);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        Long id = participacaoRepository.salvar(participacao);
        participacao.setId(id);
        return participacao;
    }

    public ParticipacaoAudiencia atualizar(Long id, ParticipacaoAudiencia participacao) throws SQLException {
        if (!participacaoRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Participação não encontrada com ID: " + id);
        }

        List<String> erros = validar(participacao);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        participacao.setId(id);
        participacaoRepository.atualizar(participacao);
        return participacao;
    }

    public void deletar(Long id) throws SQLException {
        if (!participacaoRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Participação não encontrada com ID: " + id);
        }
        participacaoRepository.deletar(id);
    }

    public void deletarPorAudiencia(Long audienciaId) throws SQLException {
        participacaoRepository.deletarPorAudiencia(audienciaId);
    }

    private List<String> validar(ParticipacaoAudiencia participacao) {
        List<String> erros = ValidationUtil.novaListaErros();

        ValidationUtil.validarObrigatorio(participacao.getAudiencia(), "Audiência", erros);
        ValidationUtil.validarObrigatorio(participacao.getPessoa(), "Pessoa", erros);
        ValidationUtil.validarObrigatorio(participacao.getTipo(), "Tipo de participação", erros);

        return erros;
    }
}
