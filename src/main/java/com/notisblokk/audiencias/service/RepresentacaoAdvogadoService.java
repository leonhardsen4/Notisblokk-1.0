package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.model.RepresentacaoAdvogado;
import com.notisblokk.audiencias.repository.RepresentacaoAdvogadoRepository;
import com.notisblokk.audiencias.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de lógica de negócio para Representação de Advogados.
 */
public class RepresentacaoAdvogadoService {

    private static final Logger logger = LoggerFactory.getLogger(RepresentacaoAdvogadoService.class);
    private final RepresentacaoAdvogadoRepository representacaoRepository = new RepresentacaoAdvogadoRepository();

    public List<RepresentacaoAdvogado> buscarPorAudiencia(Long audienciaId) throws SQLException {
        return representacaoRepository.buscarPorAudiencia(audienciaId);
    }

    public List<RepresentacaoAdvogado> buscarPorAdvogado(Long advogadoId) throws SQLException {
        return representacaoRepository.buscarPorAdvogado(advogadoId);
    }

    public Optional<RepresentacaoAdvogado> buscarPorId(Long id) throws SQLException {
        return representacaoRepository.buscarPorId(id);
    }

    public RepresentacaoAdvogado criar(RepresentacaoAdvogado representacao) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: RepresentacaoAdvogadoService.criar() - " +
            "Audiência: " + representacao.getAudiencia().getId() +
            ", Advogado: " + representacao.getAdvogado().getId() +
            ", Cliente: " + representacao.getCliente().getId());

        List<String> erros = validar(representacao);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        Long id = representacaoRepository.salvar(representacao);
        representacao.setId(id);
        return representacao;
    }

    public RepresentacaoAdvogado atualizar(Long id, RepresentacaoAdvogado representacao) throws SQLException {
        if (!representacaoRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Representação não encontrada com ID: " + id);
        }

        List<String> erros = validar(representacao);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        representacao.setId(id);
        representacaoRepository.atualizar(representacao);
        return representacao;
    }

    public void deletar(Long id) throws SQLException {
        if (!representacaoRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Representação não encontrada com ID: " + id);
        }
        representacaoRepository.deletar(id);
    }

    public void deletarPorAudiencia(Long audienciaId) throws SQLException {
        representacaoRepository.deletarPorAudiencia(audienciaId);
    }

    private List<String> validar(RepresentacaoAdvogado representacao) {
        List<String> erros = ValidationUtil.novaListaErros();

        ValidationUtil.validarObrigatorio(representacao.getAudiencia(), "Audiência", erros);
        ValidationUtil.validarObrigatorio(representacao.getAdvogado(), "Advogado", erros);
        ValidationUtil.validarObrigatorio(representacao.getCliente(), "Cliente", erros);
        ValidationUtil.validarObrigatorio(representacao.getTipo(), "Tipo de representação", erros);

        return erros;
    }
}
