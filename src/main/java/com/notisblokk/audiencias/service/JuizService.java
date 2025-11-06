package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.model.Juiz;
import com.notisblokk.audiencias.repository.JuizRepository;
import com.notisblokk.audiencias.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de lógica de negócio para Juízes.
 */
public class JuizService {

    private static final Logger logger = LoggerFactory.getLogger(JuizService.class);
    private final JuizRepository juizRepository = new JuizRepository();

    public List<Juiz> listarTodos() throws SQLException {
        return juizRepository.buscarTodos();
    }

    public Optional<Juiz> buscarPorId(Long id) throws SQLException {
        return juizRepository.buscarPorId(id);
    }

    public List<Juiz> buscarPorNome(String nome) throws SQLException {
        return juizRepository.buscarPorNome(nome);
    }

    public Juiz criar(Juiz juiz) throws SQLException {
        List<String> erros = validar(juiz);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        Long id = juizRepository.salvar(juiz);
        juiz.setId(id);
        return juiz;
    }

    public Juiz atualizar(Long id, Juiz juiz) throws SQLException {
        if (!juizRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Juiz não encontrado com ID: " + id);
        }

        List<String> erros = validar(juiz);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        juiz.setId(id);
        juizRepository.atualizar(juiz);
        return juiz;
    }

    public void deletar(Long id) throws SQLException {
        if (!juizRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Juiz não encontrado com ID: " + id);
        }
        juizRepository.deletar(id);
    }

    private List<String> validar(Juiz juiz) {
        List<String> erros = ValidationUtil.novaListaErros();
        ValidationUtil.validarObrigatorio(juiz.getNome(), "Nome do juiz", erros);
        ValidationUtil.validarTamanhoMinimo(juiz.getNome(), 3, "Nome do juiz", erros);

        if (juiz.getEmail() != null && !juiz.getEmail().isEmpty()) {
            if (!ValidationUtil.validarEmail(juiz.getEmail())) {
                erros.add("Email inválido");
            }
        }

        return erros;
    }
}
