package com.notisblokk.audiencias.dto;

/**
 * DTO para requisição de busca de horários livres
 * Usa Strings para datas para permitir formato brasileiro dd/MM/yyyy
 */
public class HorariosLivresRequest {
    private String dataInicio;  // Formato: dd/MM/yyyy
    private String dataFim;      // Formato: dd/MM/yyyy
    private String varaId;       // Pode vir como string do JSON
    private int duracaoMinutos;
    private int bufferAntesMinutos = 10;  // Buffer padrão: 10 minutos antes
    private int bufferDepoisMinutos = 10; // Buffer padrão: 10 minutos depois
    private int gradeMinutos = 15;        // Grade padrão: 15 minutos
    private int gapMinimoMinutos = 5;     // Gap mínimo padrão: 5 minutos

    public HorariosLivresRequest() {
    }

    // Getters e Setters
    public String getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(String dataInicio) {
        this.dataInicio = dataInicio;
    }

    public String getDataFim() {
        return dataFim;
    }

    public void setDataFim(String dataFim) {
        this.dataFim = dataFim;
    }

    public String getVaraId() {
        return varaId;
    }

    public void setVaraId(String varaId) {
        this.varaId = varaId;
    }

    /**
     * Obtém varaId como Long, retorna null se vazio ou inválido
     */
    public Long getVaraIdAsLong() {
        if (varaId == null || varaId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(varaId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(int duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    public int getBufferAntesMinutos() {
        return bufferAntesMinutos;
    }

    public void setBufferAntesMinutos(int bufferAntesMinutos) {
        this.bufferAntesMinutos = bufferAntesMinutos;
    }

    public int getBufferDepoisMinutos() {
        return bufferDepoisMinutos;
    }

    public void setBufferDepoisMinutos(int bufferDepoisMinutos) {
        this.bufferDepoisMinutos = bufferDepoisMinutos;
    }

    public int getGradeMinutos() {
        return gradeMinutos;
    }

    public void setGradeMinutos(int gradeMinutos) {
        this.gradeMinutos = gradeMinutos;
    }

    public int getGapMinimoMinutos() {
        return gapMinimoMinutos;
    }

    public void setGapMinimoMinutos(int gapMinimoMinutos) {
        this.gapMinimoMinutos = gapMinimoMinutos;
    }

    @Override
    public String toString() {
        return "HorariosLivresRequest{" +
                "dataInicio=" + dataInicio +
                ", dataFim=" + dataFim +
                ", varaId=" + varaId +
                ", duracaoMinutos=" + duracaoMinutos +
                ", bufferAntesMinutos=" + bufferAntesMinutos +
                ", bufferDepoisMinutos=" + bufferDepoisMinutos +
                ", gradeMinutos=" + gradeMinutos +
                ", gapMinimoMinutos=" + gapMinimoMinutos +
                '}';
    }
}
