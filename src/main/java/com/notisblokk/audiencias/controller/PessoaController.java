package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.model.Pessoa;
import com.notisblokk.audiencias.service.PessoaService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de Pessoas.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>GET /api/audiencias/pessoas - Listar todas as pessoas</li>
 *   <li>GET /api/audiencias/pessoas/{id} - Buscar pessoa por ID</li>
 *   <li>GET /api/audiencias/pessoas/buscar?nome=... - Buscar pessoas por nome</li>
 *   <li>GET /api/audiencias/pessoas/buscar-cpf?cpf=... - Buscar pessoas por CPF</li>
 *   <li>POST /api/audiencias/pessoas - Criar nova pessoa</li>
 *   <li>PUT /api/audiencias/pessoas/{id} - Atualizar pessoa</li>
 *   <li>DELETE /api/audiencias/pessoas/{id} - Deletar pessoa</li>
 * </ul>
 */
public class PessoaController {

    private static final Logger logger = LoggerFactory.getLogger(PessoaController.class);
    private final PessoaService pessoaService;

    public PessoaController() {
        this.pessoaService = new PessoaService();
    }

    /**
     * GET /api/audiencias/pessoas
     * Lista todas as pessoas cadastradas.
     */
    public void listar(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: PessoaController.listar()");
            List<Pessoa> pessoas = pessoaService.listarTodas();

            ctx.json(Map.of(
                "success", true,
                "dados", pessoas
            ));

            logger.debug("DEBUG_AUDIENCIAS: Listadas {} pessoas", pessoas.size());

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao listar pessoas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar pessoas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/pessoas/{id}
     * Busca uma pessoa por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: PessoaController.buscarPorId() - ID: " + id);

            Optional<Pessoa> pessoaOpt = pessoaService.buscarPorId(id);

            if (pessoaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Pessoa não encontrada"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", pessoaOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar pessoa", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar pessoa: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/pessoas/buscar?nome=...
     * Busca pessoas por nome (busca parcial).
     */
    public void buscarPorNome(Context ctx) {
        try {
            String nome = ctx.queryParam("nome");
            System.out.println("DEBUG_AUDIENCIAS: PessoaController.buscarPorNome() - Nome: " + nome);

            if (nome == null || nome.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro 'nome' é obrigatório"
                ));
                return;
            }

            List<Pessoa> pessoas = pessoaService.buscarPorNome(nome);

            ctx.json(Map.of(
                "success", true,
                "dados", pessoas
            ));

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar pessoas por nome", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar pessoas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/pessoas/buscar-cpf?cpf=...
     * Busca pessoas por CPF (busca exata).
     */
    public void buscarPorCPF(Context ctx) {
        try {
            String cpf = ctx.queryParam("cpf");
            System.out.println("DEBUG_AUDIENCIAS: PessoaController.buscarPorCPF() - CPF: " + cpf);

            if (cpf == null || cpf.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro 'cpf' é obrigatório"
                ));
                return;
            }

            List<Pessoa> pessoas = pessoaService.buscarPorCPF(cpf);

            ctx.json(Map.of(
                "success", true,
                "dados", pessoas
            ));

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar pessoas por CPF", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar pessoas: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/audiencias/pessoas
     * Cria uma nova pessoa.
     */
    public void criar(Context ctx) {
        try {
            Pessoa pessoa = ctx.bodyAsClass(Pessoa.class);
            System.out.println("DEBUG_AUDIENCIAS: PessoaController.criar() - Nome: " + pessoa.getNome());

            pessoa = pessoaService.criar(pessoa);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Pessoa criada com sucesso",
                "dados", pessoa
            ));

            logger.info("DEBUG_AUDIENCIAS: Pessoa criada - ID: {}, Nome: {}", pessoa.getId(), pessoa.getNome());

        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao criar pessoa", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar pessoa: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/audiencias/pessoas/{id}
     * Atualiza uma pessoa existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Pessoa pessoa = ctx.bodyAsClass(Pessoa.class);
            System.out.println("DEBUG_AUDIENCIAS: PessoaController.atualizar() - ID: " + id);

            pessoa = pessoaService.atualizar(id, pessoa);

            ctx.json(Map.of(
                "success", true,
                "message", "Pessoa atualizada com sucesso",
                "dados", pessoa
            ));

            logger.info("DEBUG_AUDIENCIAS: Pessoa atualizada - ID: {}", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao atualizar pessoa", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar pessoa: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/audiencias/pessoas/{id}
     * Deleta uma pessoa.
     * ATENÇÃO: Participações associadas serão deletadas em cascata.
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: PessoaController.deletar() - ID: " + id);

            pessoaService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Pessoa deletada com sucesso"
            ));

            logger.warn("DEBUG_AUDIENCIAS: Pessoa deletada - ID: {}", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(404);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao deletar pessoa", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar pessoa: " + e.getMessage()
            ));
        }
    }
}
