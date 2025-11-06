package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.model.Advogado;
import com.notisblokk.audiencias.repository.AdvogadoRepository;
import com.notisblokk.audiencias.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de lógica de negócio para Advogados.
 */
public class AdvogadoService {

    private static final Logger logger = LoggerFactory.getLogger(AdvogadoService.class);
    private final AdvogadoRepository advogadoRepository = new AdvogadoRepository();

    public List<Advogado> listarTodos() throws SQLException {
        return advogadoRepository.buscarTodos();
    }

    public Optional<Advogado> buscarPorId(Long id) throws SQLException {
        return advogadoRepository.buscarPorId(id);
    }

    public List<Advogado> buscarPorNome(String nome) throws SQLException {
        return advogadoRepository.buscarPorNome(nome);
    }

    public List<Advogado> buscarPorOAB(String oab) throws SQLException {
        return advogadoRepository.buscarPorOAB(oab);
    }

    public Advogado criar(Advogado advogado) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: AdvogadoService.criar() - " + advogado.getNome() + " (OAB: " + advogado.getOab() + ")");

        List<String> erros = validar(advogado);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        Long id = advogadoRepository.salvar(advogado);
        advogado.setId(id);
        return advogado;
    }

    public Advogado atualizar(Long id, Advogado advogado) throws SQLException {
        if (!advogadoRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Advogado não encontrado com ID: " + id);
        }

        List<String> erros = validar(advogado);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        advogado.setId(id);
        advogadoRepository.atualizar(advogado);
        return advogado;
    }

    public void deletar(Long id) throws SQLException {
        if (!advogadoRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Advogado não encontrado com ID: " + id);
        }
        advogadoRepository.deletar(id);
    }

    private List<String> validar(Advogado advogado) {
        List<String> erros = ValidationUtil.novaListaErros();

        ValidationUtil.validarObrigatorio(advogado.getNome(), "Nome do advogado", erros);
        ValidationUtil.validarTamanhoMinimo(advogado.getNome(), 3, "Nome do advogado", erros);

        ValidationUtil.validarObrigatorio(advogado.getOab(), "Número da OAB", erros);

        // Validação específica de OAB
        if (advogado.getOab() != null && !advogado.getOab().isEmpty()) {
            if (!ValidationUtil.validarOAB(advogado.getOab())) {
                erros.add("Número de OAB inválido. Use formato: 123456 ou 123456/SP");
                System.err.println("DEBUG_AUDIENCIAS: Validação OAB falhou - " + advogado.getOab());
            }
        }

        if (advogado.getEmail() != null && !advogado.getEmail().isEmpty()) {
            if (!ValidationUtil.validarEmail(advogado.getEmail())) {
                erros.add("Email inválido");
            }
        }

        return erros;
    }
}
