package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.model.Promotor;
import com.notisblokk.audiencias.repository.PromotorRepository;
import com.notisblokk.audiencias.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de lógica de negócio para Promotores.
 */
public class PromotorService {

    private static final Logger logger = LoggerFactory.getLogger(PromotorService.class);
    private final PromotorRepository promotorRepository = new PromotorRepository();

    public List<Promotor> listarTodos() throws SQLException {
        return promotorRepository.buscarTodos();
    }

    public Optional<Promotor> buscarPorId(Long id) throws SQLException {
        return promotorRepository.buscarPorId(id);
    }

    public List<Promotor> buscarPorNome(String nome) throws SQLException {
        return promotorRepository.buscarPorNome(nome);
    }

    public Promotor criar(Promotor promotor) throws SQLException {
        List<String> erros = validar(promotor);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        Long id = promotorRepository.salvar(promotor);
        promotor.setId(id);
        return promotor;
    }

    public Promotor atualizar(Long id, Promotor promotor) throws SQLException {
        if (!promotorRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Promotor não encontrado com ID: " + id);
        }

        List<String> erros = validar(promotor);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        promotor.setId(id);
        promotorRepository.atualizar(promotor);
        return promotor;
    }

    public void deletar(Long id) throws SQLException {
        if (!promotorRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Promotor não encontrado com ID: " + id);
        }
        promotorRepository.deletar(id);
    }

    private List<String> validar(Promotor promotor) {
        List<String> erros = ValidationUtil.novaListaErros();
        ValidationUtil.validarObrigatorio(promotor.getNome(), "Nome do promotor", erros);
        ValidationUtil.validarTamanhoMinimo(promotor.getNome(), 3, "Nome do promotor", erros);

        if (promotor.getEmail() != null && !promotor.getEmail().isEmpty()) {
            if (!ValidationUtil.validarEmail(promotor.getEmail())) {
                erros.add("Email inválido");
            }
        }

        return erros;
    }
}
