package com.notisblokk.service;

import com.notisblokk.config.AppConfig;
import com.notisblokk.config.DatabaseConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.List;

/**
 * Serviço de backup do banco de dados.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Criar backups automáticos do banco SQLite</li>
 *   <li>Exportar dados em formato CSV</li>
 *   <li>Manter histórico de backups</li>
 *   <li>Limpar backups antigos</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    // Formato brasileiro para backups: DDMMYYYY_HHmmss
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss");

    /**
     * Cria um backup completo do banco de dados SQLite.
     *
     * @param userId ID do usuário que solicitou o backup (null se automático)
     * @param tipo tipo do backup: AUTO, MANUAL, CSV
     * @return caminho do arquivo de backup criado
     * @throws Exception se houver erro ao criar backup
     */
    public String criarBackup(Long userId, String tipo) throws Exception {
        logger.info("Iniciando backup do tipo: {}", tipo);

        // Criar pasta de backups se não existir
        String backupFolder = AppConfig.getBackupFolder();
        Path backupPath = Paths.get(backupFolder);
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
            logger.info("Pasta de backups criada: {}", backupFolder);
        }

        // Nome do arquivo de backup
        String timestamp = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);
        String filename = String.format("notisblokk_backup_%s.db", timestamp);
        String backupFilePath = Paths.get(backupFolder, filename).toString();

        try {
            // Copiar arquivo do banco de dados
            String dbPath = AppConfig.getDatabasePath();
            Path source = Paths.get(dbPath);
            Path target = Paths.get(backupFilePath);

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            long fileSize = Files.size(target);
            logger.info("Backup criado com sucesso: {} ({} bytes)", backupFilePath, fileSize);

            // Registrar backup no banco
            registrarBackup(backupFilePath, tipo, fileSize, userId);

            // Limpar backups antigos (manter últimos 10)
            limparBackupsAntigos(10);

            return backupFilePath;

        } catch (IOException e) {
            logger.error("Erro ao criar backup", e);
            throw new Exception("Falha ao criar backup: " + e.getMessage());
        }
    }

    /**
     * Exporta todas as tarefas para arquivo CSV.
     *
     * @param userId ID do usuário que solicitou o export
     * @return caminho do arquivo CSV criado
     * @throws Exception se houver erro ao exportar
     */
    public String exportarTarefasCSV(Long userId) throws Exception {
        logger.info("Iniciando exportação de tarefas para CSV");

        // Criar pasta de backups se não existir
        String backupFolder = AppConfig.getBackupFolder();
        Path backupPath = Paths.get(backupFolder);
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
        }

        // Nome do arquivo CSV
        String timestamp = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);
        String filename = String.format("notisblokk_tarefas_%s.csv", timestamp);
        String csvFilePath = Paths.get(backupFolder, filename).toString();

        try (FileWriter writer = new FileWriter(csvFilePath);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("ID", "Título", "Etiqueta", "Status", "Prazo Final",
                                "Data Criação", "Data Atualização", "Conteúdo"))) {

            // Buscar todas as tarefas do banco
            String sql = """
                SELECT
                    n.id, n.titulo, n.prazo_final, n.data_criacao, n.data_atualizacao, n.conteudo,
                    e.nome as etiqueta_nome,
                    s.nome as status_nome
                FROM tarefas n
                LEFT JOIN etiquetas e ON n.etiqueta_id = e.id
                LEFT JOIN status_tarefa s ON n.status_id = s.id
                ORDER BY n.prazo_final ASC
            """;

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                int count = 0;
                while (rs.next()) {
                    csvPrinter.printRecord(
                        rs.getLong("id"),
                        rs.getString("titulo"),
                        rs.getString("etiqueta_nome"),
                        rs.getString("status_nome"),
                        rs.getString("prazo_final"),
                        rs.getString("data_criacao"),
                        rs.getString("data_atualizacao"),
                        rs.getString("conteudo")
                    );
                    count++;
                }

                logger.info("{} tarefas exportadas para CSV: {}", count, csvFilePath);
            }

            long fileSize = new File(csvFilePath).length();

            // Registrar export no banco
            registrarBackup(csvFilePath, "CSV", fileSize, userId);

            return csvFilePath;

        } catch (IOException | SQLException e) {
            logger.error("Erro ao exportar CSV", e);
            throw new Exception("Falha ao exportar CSV: " + e.getMessage());
        }
    }

    /**
     * Registra um backup no banco de dados.
     *
     * @param caminhoArquivo caminho do arquivo de backup
     * @param tipo tipo do backup
     * @param tamanhoBytes tamanho do arquivo em bytes
     * @param userId ID do usuário (null se automático)
     */
    private void registrarBackup(String caminhoArquivo, String tipo, long tamanhoBytes, Long userId) {
        String sql = """
            INSERT INTO backups (caminho_arquivo, tipo, tamanho_bytes, usuario_id)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, caminhoArquivo);
            pstmt.setString(2, tipo);
            pstmt.setLong(3, tamanhoBytes);
            if (userId != null) {
                pstmt.setLong(4, userId);
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }

            pstmt.executeUpdate();
            logger.info("Backup registrado no banco: {} ({})", caminhoArquivo, tipo);

        } catch (SQLException e) {
            logger.error("Erro ao registrar backup no banco", e);
        }
    }

    /**
     * Lista todos os backups registrados.
     *
     * @return lista de informações de backups
     */
    public List<BackupInfo> listarBackups() throws SQLException {
        List<BackupInfo> backups = new ArrayList<>();

        String sql = """
            SELECT b.id, b.caminho_arquivo, b.tipo, b.tamanho_bytes, b.data_backup,
                   u.full_name as usuario_nome
            FROM backups b
            LEFT JOIN users u ON b.usuario_id = u.id
            ORDER BY b.data_backup DESC
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                BackupInfo info = new BackupInfo();
                info.id = rs.getLong("id");
                info.caminhoArquivo = rs.getString("caminho_arquivo");
                info.tipo = rs.getString("tipo");
                info.tamanhoBytes = rs.getLong("tamanho_bytes");
                info.dataBackup = rs.getString("data_backup");
                info.usuarioNome = rs.getString("usuario_nome");

                // Verificar se arquivo ainda existe
                info.existe = new File(info.caminhoArquivo).exists();

                backups.add(info);
            }
        }

        return backups;
    }

    /**
     * Remove backups antigos, mantendo apenas os N mais recentes.
     *
     * @param manterQuantidade quantidade de backups a manter
     */
    private void limparBackupsAntigos(int manterQuantidade) {
        String sql = """
            SELECT id, caminho_arquivo
            FROM backups
            ORDER BY data_backup DESC
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            List<Long> idsParaRemover = new ArrayList<>();
            List<String> arquivosParaRemover = new ArrayList<>();
            int count = 0;

            while (rs.next()) {
                count++;
                if (count > manterQuantidade) {
                    idsParaRemover.add(rs.getLong("id"));
                    arquivosParaRemover.add(rs.getString("caminho_arquivo"));
                }
            }

            // Remover registros do banco
            if (!idsParaRemover.isEmpty()) {
                String deleteSql = "DELETE FROM backups WHERE id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    for (Long id : idsParaRemover) {
                        deleteStmt.setLong(1, id);
                        deleteStmt.executeUpdate();
                    }
                }

                // Remover arquivos físicos
                for (String arquivo : arquivosParaRemover) {
                    try {
                        File file = new File(arquivo);
                        if (file.exists()) {
                            file.delete();
                            logger.info("Backup antigo removido: {}", arquivo);
                        }
                    } catch (Exception e) {
                        logger.warn("Não foi possível remover arquivo: {}", arquivo, e);
                    }
                }

                logger.info("{} backups antigos removidos", idsParaRemover.size());
            }

        } catch (SQLException e) {
            logger.error("Erro ao limpar backups antigos", e);
        }
    }

    /**
     * Classe para armazenar informações de backup.
     */
    public static class BackupInfo {
        public Long id;
        public String caminhoArquivo;
        public String tipo;
        public Long tamanhoBytes;
        public String dataBackup;
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
    }
}
