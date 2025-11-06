package com.notisblokk.audiencias.dto;

import com.notisblokk.audiencias.util.DateUtil;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO representando um slot de horário disponível
 * Serializa datas e horários no formato brasileiro para compatibilidade com frontend
 */
public class TimeSlot {
    private String data;  // Formato: dd/MM/yyyy
    private String horarioInicio;  // Formato: HH:mm:ss
    private String horarioFim;  // Formato: HH:mm:ss
    private int duracaoMinutos;

    public TimeSlot() {
    }

    public TimeSlot(LocalDate data, LocalTime horarioInicio, LocalTime horarioFim) {
        this.data = DateUtil.formatDate(data);
        this.horarioInicio = DateUtil.formatTime(horarioInicio);
        this.horarioFim = DateUtil.formatTime(horarioFim);
        this.duracaoMinutos = calcularDuracao(horarioInicio, horarioFim);
    }

    private int calcularDuracao(LocalTime inicio, LocalTime fim) {
        return (int) java.time.Duration.between(inicio, fim).toMinutes();
    }

    // Getters e Setters
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(String horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public String getHorarioFim() {
        return horarioFim;
    }

    public void setHorarioFim(String horarioFim) {
        this.horarioFim = horarioFim;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(int duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    @Override
    public String toString() {
        return "TimeSlot{" +
                "data=" + data +
                ", horarioInicio=" + horarioInicio +
                ", horarioFim=" + horarioFim +
                ", duracaoMinutos=" + duracaoMinutos +
                '}';
    }
}
