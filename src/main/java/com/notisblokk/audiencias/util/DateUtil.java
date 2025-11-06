package com.notisblokk.audiencias.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Utilitário para conversão e formatação de datas e horários.
 *
 * <p><b>DECISÃO TÉCNICA CRÍTICA:</b></p>
 * Este projeto usa o formato brasileiro dd/MM/yyyy para todas as datas,
 * conforme definido no PROGRESS.md.
 *
 * <p><b>Formatos utilizados:</b></p>
 * <ul>
 *   <li>Data: dd/MM/yyyy (ex: 25/01/2025)</li>
 *   <li>Horário: HH:mm:ss (ex: 14:30:00)</li>
 *   <li>DateTime: dd/MM/yyyy HH:mm:ss (ex: 25/01/2025 14:30:00)</li>
 * </ul>
 *
 * <p><b>Timezone:</b> America/Sao_Paulo (horário de Brasília)</p>
 */
public class DateUtil {

    // Formatadores de data/hora (formato brasileiro)
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter TIME_FORMATTER_SHORT =
        DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Timezone do Brasil
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    // Locale brasileiro
    private static final Locale BRAZIL_LOCALE = new Locale("pt", "BR");

    // ========================================================================
    // CONVERSÃO: LocalDate → String
    // ========================================================================

    /**
     * Converte LocalDate para String no formato dd/MM/yyyy.
     *
     * @param date data a ser formatada
     * @return String no formato dd/MM/yyyy, ou null se data for null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            // DEBUG_AUDIENCIAS: Log de data nula
            System.out.println("DEBUG_AUDIENCIAS: DateUtil.formatDate() recebeu data nula");
            return null;
        }
        String formatted = date.format(DATE_FORMATTER);
        // DEBUG_AUDIENCIAS: Verificar formatação de data
        System.out.println("DEBUG_AUDIENCIAS: Data formatada: " + date + " → " + formatted);
        return formatted;
    }

    // ========================================================================
    // CONVERSÃO: String → LocalDate
    // ========================================================================

    /**
     * Converte String no formato dd/MM/yyyy para LocalDate.
     *
     * @param dateStr data em formato dd/MM/yyyy
     * @return LocalDate, ou null se string for null/vazia
     * @throws IllegalArgumentException se formato for inválido
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            // DEBUG_AUDIENCIAS: Log de string vazia
            System.out.println("DEBUG_AUDIENCIAS: DateUtil.parseDate() recebeu string vazia");
            return null;
        }

        try {
            LocalDate parsed = LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
            // DEBUG_AUDIENCIAS: Verificar parsing de data
            System.out.println("DEBUG_AUDIENCIAS: Data parseada: " + dateStr + " → " + parsed);
            return parsed;
        } catch (DateTimeParseException e) {
            // DEBUG_AUDIENCIAS: Log crítico de erro de parsing
            System.err.println("DEBUG_AUDIENCIAS: ERRO ao parsear data: " + dateStr);
            System.err.println("DEBUG_AUDIENCIAS: Formato esperado: dd/MM/yyyy (ex: 25/01/2025)");
            System.err.println("DEBUG_AUDIENCIAS: Erro: " + e.getMessage());
            throw new IllegalArgumentException(
                "Data inválida: '" + dateStr + "'. Use o formato dd/MM/yyyy (ex: 25/01/2025)", e);
        }
    }

    // ========================================================================
    // CONVERSÃO: LocalTime → String
    // ========================================================================

    /**
     * Converte LocalTime para String no formato HH:mm:ss.
     *
     * @param time horário a ser formatado
     * @return String no formato HH:mm:ss, ou null se time for null
     */
    public static String formatTime(LocalTime time) {
        if (time == null) {
            System.out.println("DEBUG_AUDIENCIAS: DateUtil.formatTime() recebeu horário nulo");
            return null;
        }
        return time.format(TIME_FORMATTER);
    }

    /**
     * Converte LocalTime para String no formato HH:mm (sem segundos).
     *
     * @param time horário a ser formatado
     * @return String no formato HH:mm, ou null se time for null
     */
    public static String formatTimeShort(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(TIME_FORMATTER_SHORT);
    }

    // ========================================================================
    // CONVERSÃO: String → LocalTime
    // ========================================================================

    /**
     * Converte String no formato HH:mm:ss ou HH:mm para LocalTime.
     *
     * @param timeStr horário em formato HH:mm:ss ou HH:mm
     * @return LocalTime, ou null se string for null/vazia
     * @throws IllegalArgumentException se formato for inválido
     */
    public static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            System.out.println("DEBUG_AUDIENCIAS: DateUtil.parseTime() recebeu string vazia");
            return null;
        }

        try {
            String trimmed = timeStr.trim();

            // Aceitar tanto HH:mm quanto HH:mm:ss
            if (trimmed.length() == 5) {
                // Formato HH:mm - adicionar :00
                trimmed = trimmed + ":00";
                System.out.println("DEBUG_AUDIENCIAS: Horário curto detectado, adicionando segundos: " +
                    timeStr + " → " + trimmed);
            }

            LocalTime parsed = LocalTime.parse(trimmed, TIME_FORMATTER);
            System.out.println("DEBUG_AUDIENCIAS: Horário parseado: " + timeStr + " → " + parsed);
            return parsed;
        } catch (DateTimeParseException e) {
            // DEBUG_AUDIENCIAS: Log crítico de erro
            System.err.println("DEBUG_AUDIENCIAS: ERRO ao parsear horário: " + timeStr);
            System.err.println("DEBUG_AUDIENCIAS: Formato esperado: HH:mm ou HH:mm:ss (ex: 14:30 ou 14:30:00)");
            System.err.println("DEBUG_AUDIENCIAS: Erro: " + e.getMessage());
            throw new IllegalArgumentException(
                "Horário inválido: '" + timeStr + "'. Use o formato HH:mm ou HH:mm:ss (ex: 14:30)", e);
        }
    }

    // ========================================================================
    // CONVERSÃO: LocalDateTime → String
    // ========================================================================

    /**
     * Converte LocalDateTime para String no formato dd/MM/yyyy HH:mm:ss.
     *
     * @param dateTime data/hora a ser formatada
     * @return String no formato dd/MM/yyyy HH:mm:ss, ou null se dateTime for null
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            System.out.println("DEBUG_AUDIENCIAS: DateUtil.formatDateTime() recebeu datetime nulo");
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    // ========================================================================
    // CONVERSÃO: String → LocalDateTime
    // ========================================================================

    /**
     * Converte String no formato dd/MM/yyyy HH:mm:ss para LocalDateTime.
     *
     * @param dateTimeStr datetime em formato dd/MM/yyyy HH:mm:ss
     * @return LocalDateTime, ou null se string for null/vazia
     * @throws IllegalArgumentException se formato for inválido
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            System.out.println("DEBUG_AUDIENCIAS: DateUtil.parseDateTime() recebeu string vazia");
            return null;
        }

        try {
            LocalDateTime parsed = LocalDateTime.parse(dateTimeStr.trim(), DATETIME_FORMATTER);
            System.out.println("DEBUG_AUDIENCIAS: DateTime parseado: " + dateTimeStr + " → " + parsed);
            return parsed;
        } catch (DateTimeParseException e) {
            // DEBUG_AUDIENCIAS: Log crítico de erro
            System.err.println("DEBUG_AUDIENCIAS: ERRO ao parsear datetime: " + dateTimeStr);
            System.err.println("DEBUG_AUDIENCIAS: Formato esperado: dd/MM/yyyy HH:mm:ss (ex: 25/01/2025 14:30:00)");
            System.err.println("DEBUG_AUDIENCIAS: Erro: " + e.getMessage());
            throw new IllegalArgumentException(
                "Data/hora inválida: '" + dateTimeStr +
                "'. Use o formato dd/MM/yyyy HH:mm:ss (ex: 25/01/2025 14:30:00)", e);
        }
    }

    // ========================================================================
    // UTILITÁRIOS
    // ========================================================================

    /**
     * Retorna a data atual no timezone do Brasil.
     *
     * @return LocalDate de hoje
     */
    public static LocalDate hoje() {
        LocalDate hoje = LocalDate.now(BRAZIL_ZONE);
        System.out.println("DEBUG_AUDIENCIAS: Data atual (Brazil timezone): " + formatDate(hoje));
        return hoje;
    }

    /**
     * Retorna o horário atual no timezone do Brasil.
     *
     * @return LocalTime de agora
     */
    public static LocalTime agora() {
        return LocalTime.now(BRAZIL_ZONE);
    }

    /**
     * Retorna a data/hora atual no timezone do Brasil.
     *
     * @return LocalDateTime de agora
     */
    public static LocalDateTime agoraCompleto() {
        return LocalDateTime.now(BRAZIL_ZONE);
    }

    /**
     * Retorna o timezone do Brasil (America/Sao_Paulo).
     *
     * @return ZoneId do Brasil
     */
    public static ZoneId getBrazilZone() {
        return BRAZIL_ZONE;
    }

    /**
     * Retorna o locale brasileiro (pt_BR).
     *
     * @return Locale do Brasil
     */
    public static Locale getBrazilLocale() {
        return BRAZIL_LOCALE;
    }

    /**
     * Retorna o formatador de data (dd/MM/yyyy).
     *
     * @return DateTimeFormatter para datas
     */
    public static DateTimeFormatter getDateFormatter() {
        return DATE_FORMATTER;
    }

    /**
     * Retorna o formatador de horário (HH:mm:ss).
     *
     * @return DateTimeFormatter para horários
     */
    public static DateTimeFormatter getTimeFormatter() {
        return TIME_FORMATTER;
    }

    /**
     * Retorna o formatador de data/hora (dd/MM/yyyy HH:mm:ss).
     *
     * @return DateTimeFormatter para data/hora
     */
    public static DateTimeFormatter getDateTimeFormatter() {
        return DATETIME_FORMATTER;
    }

    /**
     * Valida se uma string está no formato de data dd/MM/yyyy.
     *
     * @param dateStr string a validar
     * @return true se formato válido, false caso contrário
     */
    public static boolean isValidDateFormat(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Valida se uma string está no formato de horário HH:mm ou HH:mm:ss.
     *
     * @param timeStr string a validar
     * @return true se formato válido, false caso contrário
     */
    public static boolean isValidTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return false;
        }
        try {
            parseTime(timeStr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
