package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.model.Vara;
import com.notisblokk.audiencias.repository.VaraRepository;
import com.notisblokk.audiencias.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de lógica de negócio para Varas Judiciais.
 */
public class VaraService {

    private static final Logger logger = LoggerFactory.getLogger(VaraService.class);
    private final VaraRepository varaRepository = new VaraRepository();

    /**
     * Lista todas as varas.
     */
    public List<Vara> listarTodas() throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: VaraService.listarTodas()");
        return varaRepository.buscarTodas();
    }

    /**
     * Busca vara por ID.
     */
    public Optional<Vara> buscarPorId(Long id) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: VaraService.buscarPorId() - ID: " + id);
        return varaRepository.buscarPorId(id);
    }

    /**
     * Busca varas por nome.
     */
    public List<Vara> buscarPorNome(String nome) throws SQLException {
        return varaRepository.buscarPorNome(nome);
    }

    /**
     * Cria uma nova vara com validações.
     */
    public Vara criar(Vara vara) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: VaraService.criar() - " + vara.getNome());

        // Validar dados
        List<String> erros = validar(vara);
        if (ValidationUtil.temErros(erros)) {
            String mensagem = ValidationUtil.formatarErros(erros);
            System.err.println("DEBUG_AUDIENCIAS: Validação falhou - " + mensagem);
            throw new IllegalArgumentException(mensagem);
        }

        // Salvar
        Long id = varaRepository.salvar(vara);
        vara.setId(id);

        logger.info("Vara criada com sucesso - ID: {}, Nome: {}", id, vara.getNome());
        return vara;
    }

    /**
     * Atualiza uma vara existente com validações.
     */
    public Vara atualizar(Long id, Vara vara) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: VaraService.atualizar() - ID: " + id);

        // Verificar se existe
        if (!varaRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Vara não encontrada com ID: " + id);
        }

        // Validar dados
        List<String> erros = validar(vara);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        vara.setId(id);
        varaRepository.atualizar(vara);

        logger.info("Vara atualizada - ID: {}", id);
        return vara;
    }

    /**
     * Deleta uma vara.
     */
    public void deletar(Long id) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: VaraService.deletar() - ID: " + id);

        // Verificar se existe
        if (!varaRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Vara não encontrada com ID: " + id);
        }

        varaRepository.deletar(id);
        logger.info("Vara deletada - ID: {}", id);
    }

    /**
     * Valida os dados de uma vara.
     */
    private List<String> validar(Vara vara) {
        List<String> erros = ValidationUtil.novaListaErros();

        ValidationUtil.validarObrigatorio(vara.getNome(), "Nome da vara", erros);
        ValidationUtil.validarTamanhoMinimo(vara.getNome(), 3, "Nome da vara", erros);
        ValidationUtil.validarTamanhoMaximo(vara.getNome(), 200, "Nome da vara", erros);

        if (vara.getEmail() != null && !vara.getEmail().isEmpty()) {
            if (!ValidationUtil.validarEmail(vara.getEmail())) {
                erros.add("Email inválido");
            }
        }

        if (vara.getTelefone() != null && !vara.getTelefone().isEmpty()) {
            if (!ValidationUtil.validarTelefone(vara.getTelefone())) {
                erros.add("Telefone inválido");
            }
        }

        return erros;
    }
}
