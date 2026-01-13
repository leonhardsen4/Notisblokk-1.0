package com.notisblokk.service;

import com.notisblokk.model.BlocoNota;
import com.notisblokk.repository.BlocoNotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Serviço responsável pela lógica de negócio do bloco de notas.
 *
 * <p>Gerencia o documento único de bloco de notas de cada usuário,
 * incluindo validações, salvamento e exportação.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class BlocoNotaService {

    private static final Logger logger = LoggerFactory.getLogger(BlocoNotaService.class);
    private static final int CONTEUDO_MAX_LENGTH = 1_000_000; // 1 MB de texto
    private final BlocoNotaRepository blocoNotaRepository;

    /**
     * Construtor padrão.
     */
    public BlocoNotaService() {
        this.blocoNotaRepository = new BlocoNotaRepository();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param blocoNotaRepository repositório de bloco de notas
     */
    public BlocoNotaService(BlocoNotaRepository blocoNotaRepository) {
        this.blocoNotaRepository = blocoNotaRepository;
    }

    /**
     * Obtém o bloco de notas do usuário (cria se não existir).
     *
     * @param usuarioId ID do usuário
     * @param sessaoId ID da sessão atual
     * @return BlocoNota documento do usuário
     * @throws Exception se houver erro ao obter documento
     */
    public BlocoNota obter(Long usuarioId, Long sessaoId) throws Exception {
        try {
            BlocoNota nota = blocoNotaRepository.obterOuCriar(usuarioId, sessaoId);
            logger.debug("Bloco de notas obtido para usuário ID {}", usuarioId);
            return nota;

        } catch (SQLException e) {
            logger.error("Erro ao obter bloco de notas do usuário ID {}", usuarioId, e);
            throw new Exception("Erro ao obter bloco de notas: " + e.getMessage(), e);
        }
    }

    /**
     * Salva o conteúdo do bloco de notas.
     *
     * @param conteudoMarkdown conteúdo em formato Markdown
     * @param usuarioId ID do usuário
     * @param sessaoId ID da sessão atual
     * @return BlocoNota documento atualizado
     * @throws Exception se houver erro ao salvar
     */
    public BlocoNota salvar(String conteudoMarkdown, Long usuarioId, Long sessaoId) throws Exception {
        // Validações
        validarConteudo(conteudoMarkdown);

        try {
            // Obter ou criar documento
            BlocoNota nota = blocoNotaRepository.obterOuCriar(usuarioId, sessaoId);

            // Atualizar conteúdo
            nota.setConteudoMarkdown(conteudoMarkdown != null ? conteudoMarkdown : "");
            nota.setConteudoHtml(""); // Frontend renderiza Markdown
            nota.setSessaoId(sessaoId);

            // Salvar
            blocoNotaRepository.atualizar(nota);

            logger.info("Bloco de notas salvo para usuário ID {} ({} caracteres)",
                    usuarioId, conteudoMarkdown != null ? conteudoMarkdown.length() : 0);

            return nota;

        } catch (SQLException e) {
            logger.error("Erro ao salvar bloco de notas do usuário ID {}", usuarioId, e);
            throw new Exception("Erro ao salvar bloco de notas: " + e.getMessage(), e);
        }
    }

    /**
     * Exporta o conteúdo do bloco de notas como arquivo .txt.
     *
     * @param usuarioId ID do usuário
     * @return byte[] conteúdo do arquivo TXT
     * @throws Exception se houver erro ao exportar
     */
    public byte[] exportarTxt(Long usuarioId) throws Exception {
        try {
            BlocoNota nota = blocoNotaRepository.obterOuCriar(usuarioId, null);

            String conteudo = nota.getConteudoMarkdown() != null ? nota.getConteudoMarkdown() : "";

            logger.info("Bloco de notas exportado como TXT para usuário ID {}", usuarioId);

            return conteudo.getBytes(StandardCharsets.UTF_8);

        } catch (SQLException e) {
            logger.error("Erro ao exportar TXT do usuário ID {}", usuarioId, e);
            throw new Exception("Erro ao exportar TXT: " + e.getMessage(), e);
        }
    }

    /**
     * Exporta o conteúdo do bloco de notas como arquivo .md.
     *
     * @param usuarioId ID do usuário
     * @return byte[] conteúdo do arquivo Markdown
     * @throws Exception se houver erro ao exportar
     */
    public byte[] exportarMarkdown(Long usuarioId) throws Exception {
        try {
            BlocoNota nota = blocoNotaRepository.obterOuCriar(usuarioId, null);

            String conteudo = nota.getConteudoMarkdown() != null ? nota.getConteudoMarkdown() : "";

            // Adicionar header Markdown com metadados
            StringBuilder markdown = new StringBuilder();
            markdown.append("# Bloco de Notas - Notisblokk\n\n");
            markdown.append("---\n\n");
            markdown.append(conteudo);

            logger.info("Bloco de notas exportado como MD para usuário ID {}", usuarioId);

            return markdown.toString().getBytes(StandardCharsets.UTF_8);

        } catch (SQLException e) {
            logger.error("Erro ao exportar MD do usuário ID {}", usuarioId, e);
            throw new Exception("Erro ao exportar Markdown: " + e.getMessage(), e);
        }
    }

    /**
     * Valida o conteúdo do bloco de notas.
     *
     * @param conteudo conteúdo a validar
     * @throws Exception se o conteúdo for inválido
     */
    private void validarConteudo(String conteudo) throws Exception {
        if (conteudo != null && conteudo.length() > CONTEUDO_MAX_LENGTH) {
            throw new Exception("Conteúdo muito grande (máximo " + (CONTEUDO_MAX_LENGTH / 1000) + " KB)");
        }
    }
}
