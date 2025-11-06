package com.notisblokk.audiencias.service;

import com.notisblokk.audiencias.dto.HorariosLivresRequest;
import com.notisblokk.audiencias.dto.TimeSlot;
import com.notisblokk.audiencias.model.Audiencia;
import com.notisblokk.audiencias.repository.AudienciaRepository;
import com.notisblokk.audiencias.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service para cálculo de horários livres para agendamento de audiências
 *
 * Implementa algoritmo de busca de slots disponíveis considerando:
 * - Janelas de trabalho por dia da semana
 * - Audiências existentes (intervalos ocupados)
 * - Buffers antes/depois de cada audiência
 * - Grade de arredondamento
 * - Gap mínimo entre slots
 */
public class HorariosLivresService {
    private static final Logger logger = LoggerFactory.getLogger(HorariosLivresService.class);

    private final AudienciaRepository audienciaRepository;

    // Janelas de trabalho padrão (podem ser configuradas)
    private static final LocalTime MANHA_INICIO = LocalTime.of(8, 0);
    private static final LocalTime MANHA_FIM = LocalTime.of(12, 0);
    private static final LocalTime TARDE_INICIO = LocalTime.of(13, 0);
    private static final LocalTime TARDE_FIM = LocalTime.of(18, 0);

    public HorariosLivresService() {
        this.audienciaRepository = new AudienciaRepository();
    }

    /**
     * Calcula horários livres dentro do intervalo especificado
     *
     * @param request Parâmetros de busca
     * @return Lista de slots disponíveis ordenados por data e horário
     * @throws Exception Em caso de erro
     */
    public List<TimeSlot> calcularHorariosLivres(HorariosLivresRequest request) throws Exception {
        logger.debug("DEBUG_AUDIENCIAS: Calculando horários livres para: {}", request);

        // Validações
        validarRequest(request);

        // Converter strings de data para LocalDate
        LocalDate dataInicio = DateUtil.parseDate(request.getDataInicio());
        LocalDate dataFim = DateUtil.parseDate(request.getDataFim());

        logger.debug("DEBUG_AUDIENCIAS: Datas convertidas: {} a {}", dataInicio, dataFim);

        // Construir janelas de trabalho para cada dia
        List<JanelaTrabalho> janelas = construirJanelasTrabalho(dataInicio, dataFim);
        logger.debug("DEBUG_AUDIENCIAS: Construídas {} janelas de trabalho", janelas.size());

        // Buscar audiências existentes no período
        List<IntervaloOcupado> ocupados = buscarIntervalosOcupados(
            dataInicio,
            dataFim,
            request.getVaraIdAsLong()
        );
        logger.debug("DEBUG_AUDIENCIAS: Encontrados {} intervalos ocupados", ocupados.size());

        // Aplicar buffers aos intervalos ocupados
        List<IntervaloOcupado> ocupadosComBuffer = aplicarBuffers(
            ocupados,
            request.getBufferAntesMinutos(),
            request.getBufferDepoisMinutos()
        );

        // Mesclar intervalos sobrepostos
        List<IntervaloOcupado> ocupadosMesclados = mesclarIntervalos(ocupadosComBuffer);
        logger.debug("DEBUG_AUDIENCIAS: Após mesclar: {} intervalos ocupados", ocupadosMesclados.size());

        // Subtrair ocupados das janelas de trabalho para obter intervalos livres
        List<IntervaloLivre> livres = subtrairOcupados(janelas, ocupadosMesclados);
        logger.debug("DEBUG_AUDIENCIAS: Encontrados {} intervalos livres", livres.size());

        // Fatiar intervalos livres em slots
        List<TimeSlot> slots = fatiarEmSlots(
            livres,
            request.getDuracaoMinutos(),
            request.getGradeMinutos(),
            request.getGapMinimoMinutos()
        );

        // Ordenar slots por data e horário
        slots.sort(Comparator
            .comparing(TimeSlot::getData)
            .thenComparing(TimeSlot::getHorarioInicio));

        logger.debug("DEBUG_AUDIENCIAS: Total de {} slots disponíveis calculados", slots.size());
        return slots;
    }

    /**
     * Valida parâmetros da requisição
     */
    private void validarRequest(HorariosLivresRequest request) {
        if (request.getDataInicio() == null || request.getDataInicio().isEmpty()) {
            throw new IllegalArgumentException("Data de início é obrigatória");
        }
        if (request.getDataFim() == null || request.getDataFim().isEmpty()) {
            throw new IllegalArgumentException("Data de fim é obrigatória");
        }

        // Validar formato e comparar datas
        try {
            LocalDate inicio = DateUtil.parseDate(request.getDataInicio());
            LocalDate fim = DateUtil.parseDate(request.getDataFim());

            if (inicio.isAfter(fim)) {
                throw new IllegalArgumentException("Data de início deve ser anterior ou igual à data de fim");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de data inválido. Use dd/MM/yyyy");
        }

        if (request.getDuracaoMinutos() < 15) {
            throw new IllegalArgumentException("Duração mínima é de 15 minutos");
        }
        if (request.getDuracaoMinutos() > 480) {
            throw new IllegalArgumentException("Duração máxima é de 480 minutos (8 horas)");
        }
        if (request.getGradeMinutos() < 0 || request.getGradeMinutos() > 60) {
            throw new IllegalArgumentException("Grade deve estar entre 0 e 60 minutos");
        }
    }

    /**
     * Constrói janelas de trabalho para cada dia útil no intervalo
     */
    private List<JanelaTrabalho> construirJanelasTrabalho(LocalDate dataInicio, LocalDate dataFim) {
        List<JanelaTrabalho> janelas = new ArrayList<>();
        LocalDate data = dataInicio;

        while (!data.isAfter(dataFim)) {
            DayOfWeek diaSemana = data.getDayOfWeek();

            // Pular finais de semana (sábado e domingo)
            if (diaSemana != DayOfWeek.SATURDAY && diaSemana != DayOfWeek.SUNDAY) {
                // Adicionar janela da manhã
                janelas.add(new JanelaTrabalho(data, MANHA_INICIO, MANHA_FIM));

                // Adicionar janela da tarde
                janelas.add(new JanelaTrabalho(data, TARDE_INICIO, TARDE_FIM));
            }

            data = data.plusDays(1);
        }

        return janelas;
    }

    /**
     * Busca intervalos ocupados por audiências existentes
     */
    private List<IntervaloOcupado> buscarIntervalosOcupados(LocalDate dataInicio, LocalDate dataFim, Long varaId)
            throws Exception {
        List<Audiencia> audiencias = audienciaRepository.buscarTodasPorPeriodo(dataInicio, dataFim);

        return audiencias.stream()
            .filter(a -> varaId == null || (a.getVara() != null && Objects.equals(a.getVara().getId(), varaId)))
            .map(a -> new IntervaloOcupado(
                a.getDataAudiencia(),
                a.getHorarioInicio(),
                a.getHorarioFim()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Aplica buffers antes e depois de cada intervalo ocupado
     */
    private List<IntervaloOcupado> aplicarBuffers(List<IntervaloOcupado> ocupados,
                                                   int bufferAntes, int bufferDepois) {
        return ocupados.stream()
            .map(o -> new IntervaloOcupado(
                o.data,
                o.inicio.minusMinutes(bufferAntes),
                o.fim.plusMinutes(bufferDepois)
            ))
            .collect(Collectors.toList());
    }

    /**
     * Mescla intervalos sobrepostos ou contíguos
     */
    private List<IntervaloOcupado> mesclarIntervalos(List<IntervaloOcupado> intervalos) {
        if (intervalos.isEmpty()) {
            return new ArrayList<>();
        }

        // Ordenar por data e hora de início
        List<IntervaloOcupado> ordenados = new ArrayList<>(intervalos);
        ordenados.sort(Comparator
            .comparing((IntervaloOcupado i) -> i.data)
            .thenComparing(i -> i.inicio));

        List<IntervaloOcupado> mesclados = new ArrayList<>();
        IntervaloOcupado atual = ordenados.get(0);

        for (int i = 1; i < ordenados.size(); i++) {
            IntervaloOcupado proximo = ordenados.get(i);

            // Se mesmo dia e há sobreposição ou são contíguos
            if (atual.data.equals(proximo.data) &&
                !atual.fim.isBefore(proximo.inicio)) {
                // Mesclar: estender fim se necessário
                if (proximo.fim.isAfter(atual.fim)) {
                    atual = new IntervaloOcupado(atual.data, atual.inicio, proximo.fim);
                }
            } else {
                // Não há sobreposição, adicionar atual e avançar
                mesclados.add(atual);
                atual = proximo;
            }
        }

        // Adicionar último intervalo
        mesclados.add(atual);
        return mesclados;
    }

    /**
     * Subtrai intervalos ocupados das janelas de trabalho
     */
    private List<IntervaloLivre> subtrairOcupados(List<JanelaTrabalho> janelas,
                                                   List<IntervaloOcupado> ocupados) {
        List<IntervaloLivre> livres = new ArrayList<>();

        for (JanelaTrabalho janela : janelas) {
            List<IntervaloOcupado> ocupadosDoDia = ocupados.stream()
                .filter(o -> o.data.equals(janela.data))
                .collect(Collectors.toList());

            // Se não há ocupados neste dia, toda a janela está livre
            if (ocupadosDoDia.isEmpty()) {
                livres.add(new IntervaloLivre(janela.data, janela.inicio, janela.fim));
                continue;
            }

            // Processar subtrações
            LocalTime pontoAtual = janela.inicio;

            for (IntervaloOcupado ocupado : ocupadosDoDia) {
                // Se ocupado começa depois do ponto atual, há espaço livre
                if (ocupado.inicio.isAfter(pontoAtual)) {
                    livres.add(new IntervaloLivre(janela.data, pontoAtual, ocupado.inicio));
                }

                // Avançar ponto atual para depois do ocupado
                if (ocupado.fim.isAfter(pontoAtual)) {
                    pontoAtual = ocupado.fim;
                }
            }

            // Se ainda há tempo livre até o fim da janela
            if (pontoAtual.isBefore(janela.fim)) {
                livres.add(new IntervaloLivre(janela.data, pontoAtual, janela.fim));
            }
        }

        return livres;
    }

    /**
     * Fatia intervalos livres em slots de duração específica
     */
    private List<TimeSlot> fatiarEmSlots(List<IntervaloLivre> livres,
                                          int duracaoMinutos,
                                          int gradeMinutos,
                                          int gapMinimoMinutos) {
        List<TimeSlot> slots = new ArrayList<>();

        for (IntervaloLivre livre : livres) {
            LocalTime horarioAtual = arredondarParaGrade(livre.inicio, gradeMinutos, true);

            while (true) {
                LocalTime horarioFim = horarioAtual.plusMinutes(duracaoMinutos);

                // Verificar se slot cabe no intervalo livre
                if (horarioFim.isAfter(livre.fim)) {
                    break;
                }

                // Adicionar slot
                slots.add(new TimeSlot(livre.data, horarioAtual, horarioFim));

                // Avançar para próximo slot (duração + gap mínimo)
                horarioAtual = horarioAtual.plusMinutes(duracaoMinutos + gapMinimoMinutos);
                horarioAtual = arredondarParaGrade(horarioAtual, gradeMinutos, true);
            }
        }

        return slots;
    }

    /**
     * Arredonda horário para a grade especificada
     * @param horario Horário a arredondar
     * @param gradeMinutos Grade em minutos (0 = sem arredondamento)
     * @param arredondarParaCima Se true, arredonda para cima; se false, para baixo
     */
    private LocalTime arredondarParaGrade(LocalTime horario, int gradeMinutos, boolean arredondarParaCima) {
        // Se grade é 0, não arredondar
        if (gradeMinutos == 0) {
            return horario;
        }

        int minutos = horario.getHour() * 60 + horario.getMinute();
        int resto = minutos % gradeMinutos;

        if (resto == 0) {
            return horario;
        }

        int minutosArredondados;
        if (arredondarParaCima) {
            minutosArredondados = minutos + (gradeMinutos - resto);
        } else {
            minutosArredondados = minutos - resto;
        }

        return LocalTime.of(minutosArredondados / 60, minutosArredondados % 60);
    }

    // ========================================================================
    // CLASSES INTERNAS AUXILIARES
    // ========================================================================

    /**
     * Representa uma janela de trabalho (período disponível para agendamento)
     */
    private static class JanelaTrabalho {
        final LocalDate data;
        final LocalTime inicio;
        final LocalTime fim;

        JanelaTrabalho(LocalDate data, LocalTime inicio, LocalTime fim) {
            this.data = data;
            this.inicio = inicio;
            this.fim = fim;
        }
    }

    /**
     * Representa um intervalo ocupado por uma audiência
     */
    private static class IntervaloOcupado {
        final LocalDate data;
        final LocalTime inicio;
        final LocalTime fim;

        IntervaloOcupado(LocalDate data, LocalTime inicio, LocalTime fim) {
            this.data = data;
            this.inicio = inicio;
            this.fim = fim;
        }
    }

    /**
     * Representa um intervalo livre (disponível para agendamento)
     */
    private static class IntervaloLivre {
        final LocalDate data;
        final LocalTime inicio;
        final LocalTime fim;

        IntervaloLivre(LocalDate data, LocalTime inicio, LocalTime fim) {
            this.data = data;
            this.inicio = inicio;
            this.fim = fim;
        }
    }
}
