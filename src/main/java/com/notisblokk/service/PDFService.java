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
import com.notisblokk.model.Nota;
import com.notisblokk.model.StatusNota;
import com.notisblokk.repository.NotaRepository;
import com.notisblokk.repository.EtiquetaRepository;
import com.notisblokk.repository.StatusNotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviço de geração de PDF para notas.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Gerar PDF de uma nota individual</li>
 *   <li>Gerar PDF com lista de notas</li>
 *   <li>Formatar conteúdo HTML das notas</li>
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

    private final NotaRepository notaRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final StatusNotaRepository statusRepository;

    public PDFService() {
        this.notaRepository = new NotaRepository();
        this.etiquetaRepository = new EtiquetaRepository();
        this.statusRepository = new StatusNotaRepository();
    }

    /**
     * Gera PDF de uma nota específica.
     *
     * @param notaId ID da nota
     * @return byte array do PDF gerado
     * @throws Exception se houver erro ao gerar PDF
     */
    public byte[] gerarPDFNota(Long notaId) throws Exception {
        logger.info("Gerando PDF para nota ID: {}", notaId);

        var notaOpt = notaRepository.buscarPorId(notaId);
        if (notaOpt.isEmpty()) {
            throw new Exception("Nota não encontrada: " + notaId);
        }

        Nota nota = notaOpt.get();

        // Buscar etiqueta e status
        Etiqueta etiqueta = nota.getEtiquetaId() != null
            ? etiquetaRepository.buscarPorId(nota.getEtiquetaId()).orElse(null)
            : null;
        StatusNota status = nota.getStatusId() != null
            ? statusRepository.buscarPorId(nota.getStatusId()).orElse(null)
            : null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Título do documento
            Paragraph header = new Paragraph(AppConfig.getAppName())
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
            document.add(header);

            Paragraph subtitle = new Paragraph("Nota Detalhada")
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(subtitle);

            // Informações da nota
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

            // Título
            infoTable.addCell(createLabelCell("Título:"));
            infoTable.addCell(createValueCell(nota.getTitulo()));

            // Etiqueta
            infoTable.addCell(createLabelCell("Etiqueta:"));
            infoTable.addCell(createValueCell(etiqueta != null ? etiqueta.getNome() : "N/A"));

            // Status
            infoTable.addCell(createLabelCell("Status:"));
            Cell statusCell = createValueCell(status != null ? status.getNome() : "N/A");
            if (status != null && status.getCorHex() != null) {
                DeviceRgb color = hexToRgb(status.getCorHex());
                statusCell.setBackgroundColor(color);
                statusCell.setFontColor(getContrastColor(color));
            }
            infoTable.addCell(statusCell);

            // Prazo Final
            infoTable.addCell(createLabelCell("Prazo Final:"));
            String prazoFinal = nota.getPrazoFinal() != null
                ? nota.getPrazoFinal().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";
            infoTable.addCell(createValueCell(prazoFinal));

            // Data de Criação
            infoTable.addCell(createLabelCell("Data de Criação:"));
            String dataCriacao = nota.getDataCriacao() != null
                ? nota.getDataCriacao().format(FORMATTER)
                : "N/A";
            infoTable.addCell(createValueCell(dataCriacao));

            // Data de Atualização
            infoTable.addCell(createLabelCell("Última Atualização:"));
            String dataAtualizacao = nota.getDataAtualizacao() != null
                ? nota.getDataAtualizacao().format(FORMATTER)
                : "N/A";
            infoTable.addCell(createValueCell(dataAtualizacao));

            document.add(infoTable);

            // Conteúdo
            document.add(new Paragraph("Conteúdo:")
                .setBold()
                .setFontSize(12)
                .setMarginTop(10)
                .setMarginBottom(5));

            String conteudo = nota.getConteudo() != null && !nota.getConteudo().isEmpty()
                ? stripHtml(nota.getConteudo())
                : "Sem conteúdo";

            document.add(new Paragraph(conteudo)
                .setFontSize(10)
                .setMarginBottom(20));

            // Rodapé
            String rodape = String.format("Gerado em %s",
                LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER));
            document.add(new Paragraph(rodape)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setMarginTop(30));

            document.close();
            logger.info("PDF gerado com sucesso para nota ID: {}", notaId);

            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Erro ao gerar PDF para nota ID: {}", notaId, e);
            throw new Exception("Erro ao gerar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Gera PDF com lista de notas (relatório).
     *
     * @param notas lista de notas
     * @return byte array do PDF gerado
     * @throws Exception se houver erro ao gerar PDF
     */
    public byte[] gerarPDFRelatorio(List<Nota> notas) throws Exception {
        logger.info("Gerando PDF com relatório de {} notas", notas.size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Título
            Paragraph header = new Paragraph(AppConfig.getAppName())
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
            document.add(header);

            Paragraph subtitle = new Paragraph("Relatório de Notas")
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(subtitle);

            // Informações do relatório
            document.add(new Paragraph(String.format("Total de notas: %d", notas.size()))
                .setFontSize(10)
                .setMarginBottom(3));

            document.add(new Paragraph(String.format("Gerado em: %s",
                LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER)))
                .setFontSize(10)
                .setMarginBottom(15));

            // Tabela de notas
            Table table = new Table(UnitValue.createPercentArray(new float[]{35, 20, 20, 25}))
                .useAllAvailableWidth();

            // Cabeçalho da tabela
            table.addHeaderCell(createHeaderCell("Título"));
            table.addHeaderCell(createHeaderCell("Etiqueta"));
            table.addHeaderCell(createHeaderCell("Status"));
            table.addHeaderCell(createHeaderCell("Prazo Final"));

            // Linhas
            for (Nota nota : notas) {
                // Buscar etiqueta e status
                Etiqueta etiqueta = nota.getEtiquetaId() != null
                    ? etiquetaRepository.buscarPorId(nota.getEtiquetaId()).orElse(null)
                    : null;
                StatusNota status = nota.getStatusId() != null
                    ? statusRepository.buscarPorId(nota.getStatusId()).orElse(null)
                    : null;

                table.addCell(createTableCell(nota.getTitulo()));
                table.addCell(createTableCell(etiqueta != null ? etiqueta.getNome() : "N/A"));

                Cell statusCell = createTableCell(status != null ? status.getNome() : "N/A");
                if (status != null && status.getCorHex() != null) {
                    DeviceRgb color = hexToRgb(status.getCorHex());
                    statusCell.setBackgroundColor(color);
                    statusCell.setFontColor(getContrastColor(color));
                }
                table.addCell(statusCell);

                String prazo = nota.getPrazoFinal() != null
                    ? nota.getPrazoFinal().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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
            .setPadding(5);
    }

    /**
     * Cria célula de valor.
     */
    private Cell createValueCell(String text) {
        return new Cell()
            .add(new Paragraph(text))
            .setFontSize(10)
            .setPadding(5);
    }

    /**
     * Cria célula de cabeçalho de tabela.
     */
    private Cell createHeaderCell(String text) {
        return new Cell()
            .add(new Paragraph(text).setBold())
            .setBackgroundColor(new DeviceRgb(74, 144, 226))
            .setFontColor(ColorConstants.WHITE)
            .setFontSize(10)
            .setPadding(5)
            .setTextAlignment(TextAlignment.CENTER);
    }

    /**
     * Cria célula normal de tabela.
     */
    private Cell createTableCell(String text) {
        return new Cell()
            .add(new Paragraph(text))
            .setFontSize(9)
            .setPadding(5);
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
