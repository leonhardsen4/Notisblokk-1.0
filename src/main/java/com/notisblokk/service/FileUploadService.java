package com.notisblokk.service;

import com.notisblokk.config.AppConfig;
import com.notisblokk.config.DatabaseConfig;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de upload e gerenciamento de arquivos anexados às notas.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Validar tipos de arquivo permitidos</li>
 *   <li>Validar tamanho máximo de arquivos</li>
 *   <li>Salvar arquivos no sistema de arquivos</li>
 *   <li>Registrar anexos no banco de dados</li>
 *   <li>Listar e remover anexos</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final Tika tika = new Tika();

    /**
     * Faz upload de foto de perfil (não registra na tabela anexos).
     *
     * @param nomeArquivo nome original do arquivo
     * @param inputStream stream de dados do arquivo
     * @return caminho do arquivo salvo
     * @throws Exception se houver erro no upload
     */
    public String uploadFotoPerfil(String nomeArquivo, InputStream inputStream) throws Exception {
        logger.info("Iniciando upload de foto de perfil: {}", nomeArquivo);

        // Validar se é imagem
        String extensao = getExtensao(nomeArquivo);
        List<String> extensoesImagem = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        if (!extensoesImagem.contains(extensao.toLowerCase())) {
            throw new Exception("Apenas imagens são permitidas (jpg, jpeg, png, gif, webp)");
        }

        // Criar pasta de uploads se não existir
        String uploadsFolder = AppConfig.getUploadsFolder();
        Path uploadsPath = Paths.get(uploadsFolder, "perfil");

        if (!Files.exists(uploadsPath)) {
            Files.createDirectories(uploadsPath);
            logger.info("Pasta de fotos de perfil criada: {}", uploadsPath);
        }

        // Gerar nome único para o arquivo
        String timestamp = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);
        String nomeUnico = String.format("perfil_%s_%s.%s",
            timestamp,
            UUID.randomUUID().toString().substring(0, 8),
            extensao);

        // Caminho para salvar no banco (uploads/perfil/arquivo.jpg)
        // O Javalin serve arquivos de "uploads/" em "/uploads/"
        // Arquivo físico: uploads/perfil/foto.png -> URL: /uploads/perfil/foto.png
        String caminhoRelativo = "uploads" + java.io.File.separator + "perfil" + java.io.File.separator + nomeUnico;

        // Caminho absoluto para salvar o arquivo no disco
        Path targetPath = uploadsPath.resolve(nomeUnico);

        try {
            // Salvar arquivo no disco
            long tamanhoBytes = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Validar tamanho (máximo 5 MB para fotos de perfil)
            long maxSizeBytes = 5 * 1024L * 1024L;
            if (tamanhoBytes > maxSizeBytes) {
                Files.delete(targetPath);
                throw new Exception("Foto muito grande. Máximo: 5 MB");
            }

            logger.info("Foto de perfil salva com sucesso: {} ({} bytes)", caminhoRelativo, tamanhoBytes);
            return caminhoRelativo;

        } catch (IOException e) {
            logger.error("Erro ao fazer upload da foto de perfil: {}", e.getMessage(), e);
            throw new Exception("Falha ao fazer upload: " + e.getMessage());
        }
    }

    /**
     * Faz upload de um arquivo e o associa a uma nota.
     *
     * @param notaId ID da nota
     * @param nomeArquivo nome original do arquivo
     * @param inputStream stream de dados do arquivo
     * @param userId ID do usuário que fez upload
     * @return informações do anexo criado
     * @throws Exception se houver erro no upload
     */
    public AnexoInfo uploadArquivo(Long notaId, String nomeArquivo, InputStream inputStream, Long userId) throws Exception {
        logger.info("Iniciando upload de arquivo: {} para nota {}", nomeArquivo, notaId);

        // Validar extensão
        String extensao = getExtensao(nomeArquivo);
        if (!isExtensaoPermitida(extensao)) {
            throw new Exception("Tipo de arquivo não permitido: " + extensao);
        }

        // Criar pasta de uploads se não existir
        String uploadsFolder = AppConfig.getUploadsFolder();
        Path uploadsPath = Paths.get(uploadsFolder);
        if (!Files.exists(uploadsPath)) {
            Files.createDirectories(uploadsPath);
            logger.info("Pasta de uploads criada: {}", uploadsFolder);
        }

        // Gerar nome único para o arquivo
        String timestamp = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);
        String nomeUnico = String.format("%s_%s_%s", timestamp, UUID.randomUUID().toString().substring(0, 8), nomeArquivo);
        String caminhoArquivo = Paths.get(uploadsFolder, nomeUnico).toString();

        try {
            // Salvar arquivo no disco
            Path targetPath = Paths.get(caminhoArquivo);
            long tamanhoBytes = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Validar tamanho
            long maxSizeBytes = AppConfig.getUploadsMaxSizeMb() * 1024L * 1024L;
            if (tamanhoBytes > maxSizeBytes) {
                // Remover arquivo se exceder tamanho
                Files.delete(targetPath);
                throw new Exception(String.format("Arquivo muito grande. Máximo: %d MB", AppConfig.getUploadsMaxSizeMb()));
            }

            // Detectar tipo MIME
            String tipoMime = tika.detect(targetPath);

            logger.info("Arquivo salvo: {} ({} bytes, tipo: {})", caminhoArquivo, tamanhoBytes, tipoMime);

            // Registrar no banco
            Long anexoId = registrarAnexo(notaId, nomeArquivo, caminhoArquivo, tipoMime, tamanhoBytes, userId);

            // Retornar informações
            AnexoInfo info = new AnexoInfo();
            info.id = anexoId;
            info.notaId = notaId;
            info.nomeArquivo = nomeArquivo;
            info.caminhoArquivo = caminhoArquivo;
            info.tipoMime = tipoMime;
            info.tamanhoBytes = tamanhoBytes;
            info.usuarioId = userId;

            return info;

        } catch (IOException e) {
            logger.error("Erro ao fazer upload do arquivo", e);
            throw new Exception("Falha ao fazer upload: " + e.getMessage());
        }
    }

    /**
     * Registra um anexo no banco de dados.
     */
    private Long registrarAnexo(Long notaId, String nomeArquivo, String caminhoArquivo,
                                String tipoMime, long tamanhoBytes, Long userId) throws SQLException {
        String sql = """
            INSERT INTO anexos (nota_id, nome_arquivo, caminho_arquivo, tipo_mime, tamanho_bytes, usuario_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, notaId);
            pstmt.setString(2, nomeArquivo);
            pstmt.setString(3, caminhoArquivo);
            pstmt.setString(4, tipoMime);
            pstmt.setLong(5, tamanhoBytes);
            if (userId != null) {
                pstmt.setLong(6, userId);
            } else {
                pstmt.setNull(6, java.sql.Types.INTEGER);
            }

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    logger.info("Anexo registrado no banco: ID={}, arquivo={}", id, nomeArquivo);
                    return id;
                }
            }

            throw new SQLException("Falha ao obter ID do anexo criado");
        }
    }

    /**
     * Lista todos os anexos de uma nota.
     *
     * @param notaId ID da nota
     * @return lista de anexos
     */
    public List<AnexoInfo> listarAnexos(Long notaId) throws SQLException {
        List<AnexoInfo> anexos = new ArrayList<>();

        String sql = """
            SELECT a.id, a.nota_id, a.nome_arquivo, a.caminho_arquivo, a.tipo_mime,
                   a.tamanho_bytes, a.data_upload, a.usuario_id, u.full_name as usuario_nome
            FROM anexos a
            LEFT JOIN users u ON a.usuario_id = u.id
            WHERE a.nota_id = ?
            ORDER BY a.data_upload DESC
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, notaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AnexoInfo info = new AnexoInfo();
                    info.id = rs.getLong("id");
                    info.notaId = rs.getLong("nota_id");
                    info.nomeArquivo = rs.getString("nome_arquivo");
                    info.caminhoArquivo = rs.getString("caminho_arquivo");
                    info.tipoMime = rs.getString("tipo_mime");
                    info.tamanhoBytes = rs.getLong("tamanho_bytes");
                    info.dataUpload = rs.getString("data_upload");
                    info.usuarioId = rs.getLong("usuario_id");
                    info.usuarioNome = rs.getString("usuario_nome");

                    // Verificar se arquivo existe
                    info.existe = new File(info.caminhoArquivo).exists();

                    anexos.add(info);
                }
            }
        }

        return anexos;
    }

    /**
     * Busca um anexo por ID.
     *
     * @param anexoId ID do anexo
     * @return informações do anexo
     */
    public AnexoInfo buscarAnexo(Long anexoId) throws SQLException {
        String sql = """
            SELECT a.id, a.nota_id, a.nome_arquivo, a.caminho_arquivo, a.tipo_mime,
                   a.tamanho_bytes, a.data_upload, a.usuario_id
            FROM anexos a
            WHERE a.id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, anexoId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AnexoInfo info = new AnexoInfo();
                    info.id = rs.getLong("id");
                    info.notaId = rs.getLong("nota_id");
                    info.nomeArquivo = rs.getString("nome_arquivo");
                    info.caminhoArquivo = rs.getString("caminho_arquivo");
                    info.tipoMime = rs.getString("tipo_mime");
                    info.tamanhoBytes = rs.getLong("tamanho_bytes");
                    info.dataUpload = rs.getString("data_upload");
                    info.usuarioId = rs.getLong("usuario_id");
                    info.existe = new File(info.caminhoArquivo).exists();

                    return info;
                }
            }
        }

        return null;
    }

    /**
     * Remove um anexo.
     *
     * @param anexoId ID do anexo
     * @throws Exception se houver erro ao remover
     */
    public void removerAnexo(Long anexoId) throws Exception {
        // Buscar anexo
        AnexoInfo info = buscarAnexo(anexoId);
        if (info == null) {
            throw new Exception("Anexo não encontrado: " + anexoId);
        }

        // Remover arquivo do disco
        try {
            File file = new File(info.caminhoArquivo);
            if (file.exists()) {
                file.delete();
                logger.info("Arquivo removido: {}", info.caminhoArquivo);
            }
        } catch (Exception e) {
            logger.warn("Não foi possível remover arquivo físico: {}", info.caminhoArquivo, e);
        }

        // Remover registro do banco
        String sql = "DELETE FROM anexos WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, anexoId);
            pstmt.executeUpdate();

            logger.info("Anexo removido do banco: ID={}", anexoId);
        }
    }

    /**
     * Verifica se a extensão do arquivo é permitida.
     */
    private boolean isExtensaoPermitida(String extensao) {
        String allowedExtensions = AppConfig.getUploadsAllowedExtensions();
        List<String> extensoesPermitidas = Arrays.asList(allowedExtensions.toLowerCase().split(","));
        return extensoesPermitidas.contains(extensao.toLowerCase());
    }

    /**
     * Extrai a extensão de um nome de arquivo.
     */
    private String getExtensao(String nomeArquivo) {
        int lastDot = nomeArquivo.lastIndexOf('.');
        if (lastDot > 0 && lastDot < nomeArquivo.length() - 1) {
            return nomeArquivo.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Classe para armazenar informações de anexo.
     */
    public static class AnexoInfo {
        public Long id;
        public Long notaId;
        public String nomeArquivo;
        public String caminhoArquivo;
        public String tipoMime;
        public Long tamanhoBytes;
        public String dataUpload;
        public Long usuarioId;
        public String usuarioNome;
        public boolean existe;

        public String getTamanhoFormatado() {
            if (tamanhoBytes == null) return "0 B";

            long bytes = tamanhoBytes;
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }

        public String getIcone() {
            if (tipoMime == null) return "📄";

            if (tipoMime.startsWith("image/")) return "🖼️";
            if (tipoMime.contains("pdf")) return "📕";
            if (tipoMime.contains("word")) return "📘";
            if (tipoMime.contains("excel") || tipoMime.contains("spreadsheet")) return "📗";
            if (tipoMime.contains("text")) return "📄";
            if (tipoMime.contains("zip") || tipoMime.contains("rar")) return "📦";

            return "📎";
        }
    }
}
