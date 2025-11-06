package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.model.Pessoa;
import com.notisblokk.audiencias.repository.PessoaRepository;
import com.notisblokk.audiencias.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de lógica de negócio para Pessoas.
 */
public class PessoaService {

    private static final Logger logger = LoggerFactory.getLogger(PessoaService.class);
    private final PessoaRepository pessoaRepository = new PessoaRepository();

    public List<Pessoa> listarTodas() throws SQLException {
        return pessoaRepository.buscarTodas();
    }

    public Optional<Pessoa> buscarPorId(Long id) throws SQLException {
        return pessoaRepository.buscarPorId(id);
    }

    public List<Pessoa> buscarPorNome(String nome) throws SQLException {
        return pessoaRepository.buscarPorNome(nome);
    }

    public List<Pessoa> buscarPorCPF(String cpf) throws SQLException {
        return pessoaRepository.buscarPorCPF(cpf);
    }

    public Pessoa criar(Pessoa pessoa) throws SQLException {
        System.out.println("DEBUG_AUDIENCIAS: PessoaService.criar() - " + pessoa.getNome());

        List<String> erros = validar(pessoa);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        Long id = pessoaRepository.salvar(pessoa);
        pessoa.setId(id);
        return pessoa;
    }

    public Pessoa atualizar(Long id, Pessoa pessoa) throws SQLException {
        if (!pessoaRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Pessoa não encontrada com ID: " + id);
        }

        List<String> erros = validar(pessoa);
        if (ValidationUtil.temErros(erros)) {
            throw new IllegalArgumentException(ValidationUtil.formatarErros(erros));
        }

        pessoa.setId(id);
        pessoaRepository.atualizar(pessoa);
        return pessoa;
    }

    public void deletar(Long id) throws SQLException {
        if (!pessoaRepository.buscarPorId(id).isPresent()) {
            throw new IllegalArgumentException("Pessoa não encontrada com ID: " + id);
        }
        pessoaRepository.deletar(id);
    }

    private List<String> validar(Pessoa pessoa) {
        List<String> erros = ValidationUtil.novaListaErros();

        ValidationUtil.validarObrigatorio(pessoa.getNome(), "Nome da pessoa", erros);
        ValidationUtil.validarTamanhoMinimo(pessoa.getNome(), 3, "Nome da pessoa", erros);

        // Validação específica de CPF (opcional, mas se preenchido deve ser válido)
        if (pessoa.getCpf() != null && !pessoa.getCpf().isEmpty()) {
            if (!ValidationUtil.validarCPF(pessoa.getCpf())) {
                erros.add("CPF inválido");
                System.err.println("DEBUG_AUDIENCIAS: Validação CPF falhou - " + pessoa.getCpf());
            }
        }

        if (pessoa.getEmail() != null && !pessoa.getEmail().isEmpty()) {
            if (!ValidationUtil.validarEmail(pessoa.getEmail())) {
                erros.add("Email inválido");
            }
        }

        return erros;
    }
}
