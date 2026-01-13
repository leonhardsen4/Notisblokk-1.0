package com.notisblokk.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.notisblokk.config.AppConfig;
import com.notisblokk.model.Etiqueta;
import com.notisblokk.model.Tarefa;
import com.notisblokk.model.StatusTarefa;
import com.notisblokk.repository.TarefaRepository;
import com.notisblokk.repository.EtiquetaRepository;
import com.notisblokk.repository.StatusTarefaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviço de geração de PDF para tarefas.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Gerar PDF de uma tarefa individual</li>
 *   <li>Gerar PDF com lista de tarefas</li>
 *   <li>Formatar conteúdo HTML das tarefas</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class PDFService {

    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TarefaRepository tarefaRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final StatusTarefaRepository statusRepository;

    public PDFService() {
        this.tarefaRepository = new TarefaRepository();
        this.etiquetaRepository = new EtiquetaRepository();
        this.statusRepository = new StatusTarefaRepository();
    }

    /**
     * Gera PDF de uma tarefa específica.
     *
     * @param tarefaId ID da tarefa
     * @return byte array do PDF gerado
     * @throws Exception se houver erro ao gerar PDF
     */
    public byte[] gerarPDFNota(Long tarefaId) throws Exception {
        logger.info("Gerando PDF para tarefa ID: {}", tarefaId);

        var tarefaOpt = tarefaRepository.buscarPorId(tarefaId);
        if (tarefaOpt.isEmpty()) {
            throw new Exception("Tarefa não encontrada: " + tarefaId);
        }

        Tarefa tarefa = tarefaOpt.get();

        // Buscar etiqueta e status
        Etiqueta etiqueta = tarefa.getEtiquetaId() != null
            ? etiquetaRepository.buscarPorId(tarefa.getEtiquetaId()).orElse(null)
            : null;
        StatusTarefa status = tarefa.getStatusId() != null
            ? statusRepository.buscarPorId(tarefa.getStatusId()).orElse(null)
            : null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Título da tarefa como cabeçalho principal
            document.add(new Paragraph(tarefa.getTitulo())
                .setFontSize(18)
                .setBold()
                .setFontColor(new DeviceRgb(51, 65, 85))
                .setMarginBottom(12));

            // Quadro de informações compacto (5 colunas)
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20, 20}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

            // Headers compactos
            infoTable.addCell(createCompactHeaderCell("Etiqueta"));
            infoTable.addCell(createCompactHeaderCell("Status"));
            infoTable.addCell(createCompactHeaderCell("Criado em"));
            infoTable.addCell(createCompactHeaderCell("Atualizado em"));
            infoTable.addCell(createCompactHeaderCell("Prazo Final"));

            // Valores
            infoTable.addCell(createCompactValueCell(etiqueta != null ? etiqueta.getNome() : "N/A"));

            Cell statusCell = createCompactValueCell(status != null ? status.getNome() : "N/A");
            if (status != null && status.getCorHex() != null) {
                DeviceRgb color = hexToRgb(status.getCorHex());
                statusCell.setBackgroundColor(color);
                statusCell.setFontColor(getContrastColor(color));
                statusCell.setBold();
            }
            infoTable.addCell(statusCell);

            String dataCriacao = tarefa.getDataCriacao() != null
                ? tarefa.getDataCriacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";
            infoTable.addCell(createCompactValueCell(dataCriacao));

            String dataAtualizacao = tarefa.getDataAtualizacao() != null
                ? tarefa.getDataAtualizacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";
            infoTable.addCell(createCompactValueCell(dataAtualizacao));

            String prazoFinal = tarefa.getPrazoFinal() != null
                ? tarefa.getPrazoFinal().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";
            infoTable.addCell(createCompactValueCell(prazoFinal));

            document.add(infoTable);

            // Linha divisória sutil
            document.add(new Paragraph("\u00A0")
                .setMarginTop(0)
                .setMarginBottom(10)
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(226, 232, 240), 1)));

            // Conteúdo direto (sem título)
            String conteudo = tarefa.getConteudo() != null && !tarefa.getConteudo().isEmpty()
                ? stripHtml(tarefa.getConteudo())
                : "Sem conteúdo";

            document.add(new Paragraph(conteudo)
                .setFontSize(10)
                .setMarginBottom(25)
                .setTextAlignment(TextAlignment.JUSTIFIED));

            // Rodapé discreto
            document.add(new Paragraph("\u00A0")
                .setMarginTop(15)
                .setMarginBottom(5)
                .setBorderTop(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(226, 232, 240), 0.5f)));

            String rodape = String.format("Gerado em %s | %s",
                LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER),
                AppConfig.getAppName());
            document.add(new Paragraph(rodape)
                .setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(148, 163, 184)));

            document.close();
            logger.info("PDF gerado com sucesso para tarefa ID: {}", tarefaId);

            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Erro ao gerar PDF para tarefa ID: {}", tarefaId, e);
            throw new Exception("Erro ao gerar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Gera PDF com lista de tarefas (relatório).
     *
     * @param tarefas lista de tarefas
     * @return byte array do PDF gerado
     * @throws Exception se houver erro ao gerar PDF
     */
    public byte[] gerarPDFRelatorio(List<Tarefa> tarefas) throws Exception {
        logger.info("Gerando PDF com relatório de {} tarefas", tarefas.size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Cabeçalho com fundo colorido
            Table headerTable = new Table(UnitValue.createPercentArray(1))
                .useAllAvailableWidth()
                .setBackgroundColor(new DeviceRgb(74, 144, 226))
                .setMarginBottom(20);

            Cell headerCell = new Cell()
                .add(new Paragraph(AppConfig.getAppName())
                    .setFontSize(24)
                    .setBold()
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(3))
                .add(new Paragraph("Relatório de Tarefas")
                    .setFontSize(14)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER))
                .setPadding(15)
                .setBorder(null);

            headerTable.addCell(headerCell);
            document.add(headerTable);

            // Informações do relatório em caixa destacada
            Table infoBox = new Table(UnitValue.createPercentArray(2))
                .useAllAvailableWidth()
                .setMarginBottom(20)
                .setBackgroundColor(new DeviceRgb(241, 245, 249));

            infoBox.addCell(new Cell()
                .add(new Paragraph("Total de tarefas:")
                    .setBold()
                    .setFontSize(10))
                .setBorder(null)
                .setPadding(8));

            infoBox.addCell(new Cell()
                .add(new Paragraph(String.valueOf(tarefas.size()))
                    .setFontSize(10))
                .setBorder(null)
                .setPadding(8));

            infoBox.addCell(new Cell()
                .add(new Paragraph("Data de geração:")
                    .setBold()
                    .setFontSize(10))
                .setBorder(null)
                .setPadding(8));

            infoBox.addCell(new Cell()
                .add(new Paragraph(LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER))
                    .setFontSize(10))
                .setBorder(null)
                .setPadding(8));

            document.add(infoBox);

            // Tabela de tarefas com bordas
            Table table = new Table(UnitValue.createPercentArray(new float[]{35, 20, 20, 25}))
                .useAllAvailableWidth()
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(200, 200, 200), 1));

            // Cabeçalho da tabela
            table.addHeaderCell(createHeaderCell("Título"));
            table.addHeaderCell(createHeaderCell("Etiqueta"));
            table.addHeaderCell(createHeaderCell("Status"));
            table.addHeaderCell(createHeaderCell("Prazo Final"));

            // Linhas
            for (Tarefa tarefa : tarefas) {
                // Buscar etiqueta e status
                Etiqueta etiqueta = tarefa.getEtiquetaId() != null
                    ? etiquetaRepository.buscarPorId(tarefa.getEtiquetaId()).orElse(null)
                    : null;
                StatusTarefa status = tarefa.getStatusId() != null
                    ? statusRepository.buscarPorId(tarefa.getStatusId()).orElse(null)
                    : null;

                table.addCell(createTableCell(tarefa.getTitulo()));
                table.addCell(createTableCell(etiqueta != null ? etiqueta.getNome() : "N/A"));

                Cell statusCell = createTableCell(status != null ? status.getNome() : "N/A");
                if (status != null && status.getCorHex() != null) {
                    DeviceRgb color = hexToRgb(status.getCorHex());
                    statusCell.setBackgroundColor(color);
                    statusCell.setFontColor(getContrastColor(color));
                }
                table.addCell(statusCell);

                String prazo = tarefa.getPrazoFinal() != null
                    ? tarefa.getPrazoFinal().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "N/A";
                table.addCell(createTableCell(prazo));
            }

            document.add(table);

            document.close();
            logger.info("PDF de relatório gerado com sucesso");

            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Erro ao gerar PDF de relatório", e);
            throw new Exception("Erro ao gerar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Cria célula de label (negrito).
     */
    private Cell createLabelCell(String text) {
        return new Cell()
            .add(new Paragraph(text).setBold())
            .setFontSize(10)
            .setPadding(8)
            .setBackgroundColor(new DeviceRgb(241, 245, 249))
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(226, 232, 240), 0.5f));
    }

    /**
     * Cria célula de valor.
     */
    private Cell createValueCell(String text) {
        return new Cell()
            .add(new Paragraph(text))
            .setFontSize(10)
            .setPadding(8)
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(226, 232, 240), 0.5f));
    }

    /**
     * Cria célula de cabeçalho de tabela.
     */
    private Cell createHeaderCell(String text) {
        return new Cell()
            .add(new Paragraph(text).setBold())
            .setBackgroundColor(new DeviceRgb(51, 65, 85))
            .setFontColor(ColorConstants.WHITE)
            .setFontSize(10)
            .setPadding(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(30, 41, 59), 1));
    }

    /**
     * Cria célula de cabeçalho compacta para quadro de informações.
     */
    private Cell createCompactHeaderCell(String text) {
        return new Cell()
            .add(new Paragraph(text).setBold())
            .setBackgroundColor(new DeviceRgb(241, 245, 249))
            .setFontColor(new DeviceRgb(71, 85, 105))
            .setFontSize(8)
            .setPadding(5)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(226, 232, 240), 0.5f));
    }

    /**
     * Cria célula de valor compacta para quadro de informações.
     */
    private Cell createCompactValueCell(String text) {
        return new Cell()
            .add(new Paragraph(text))
            .setFontSize(9)
            .setPadding(5)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(226, 232, 240), 0.5f));
    }

    /**
     * Cria célula normal de tabela.
     */
    private Cell createTableCell(String text) {
        return new Cell()
            .add(new Paragraph(text))
            .setFontSize(9)
            .setPadding(8)
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(226, 232, 240), 0.5f));
    }

    /**
     * Remove tags HTML de uma string.
     */
    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .trim();
    }

    /**
     * Converte cor hexadecimal para DeviceRgb.
     */
    private DeviceRgb hexToRgb(String hex) {
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new DeviceRgb(r, g, b);
    }

    /**
     * Retorna cor de texto contrastante (preto ou branco).
     */
    private DeviceRgb getContrastColor(DeviceRgb color) {
        float r = color.getColorValue()[0];
        float g = color.getColorValue()[1];
        float b = color.getColorValue()[2];

        double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
        return (DeviceRgb) (luminance > 128 ? ColorConstants.BLACK : ColorConstants.WHITE);
    }
}
