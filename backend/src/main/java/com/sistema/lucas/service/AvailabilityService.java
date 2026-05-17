// backend/src/main/java/com/sistema/lucas/service/AvailabilityService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.ProfessionalAvailability;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.dto.AvailabilityDTO;
import com.sistema.lucas.model.dto.SlotDTO;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.ProfessionalAvailabilityRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    @Autowired private ProfessionalAvailabilityRepository availabilityRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    // ─── Leitura ─────────────────────────────────────────────────────────────

    public List<ProfessionalAvailability> getMinhaDisponibilidade(String email, YearMonth month) {
        return availabilityRepository.findByProfessionalEmail(email).stream()
            .filter(a -> YearMonth.from(a.getDate()).equals(month))
            .toList();
    }

    public List<Professional> getProfissionaisComDisponibilidade() {
        List<Long> ids = availabilityRepository.findProfessionalIdsComDisponibilidade();
        return professionalRepository.findAllById(java.util.Objects.requireNonNull(ids));
    }

    public List<Professional> getProfissionaisSemAgendaNoMes(YearMonth mes) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();
        
        return professionalRepository.findAll().stream()
            .filter(p -> !availabilityRepository.existsByProfessionalIdAndDateBetween(p.getId(), inicio, fim))
            .collect(Collectors.toList());
    }

    public List<DayOfWeek> getWorkingDays(Long professionalId) {
        return availabilityRepository.findByProfessionalId(professionalId)
            .stream()
            .map(a -> a.getDate().getDayOfWeek())
            .distinct()
            .sorted()
            .toList();
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    @Transactional
    public void salvarMes(String email, List<AvailabilityDTO> dtos, YearMonth mesAlvo) {
        verificarPrazoSubmissao(mesAlvo);
        
        var profissional = professionalRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado."));

        // ✅ Validação: Não pode remover horários onde já existam pacientes agendados
        validarRemocaoHorariosOcupados(profissional.getId(), mesAlvo, dtos);

        // Remove a disponibilidade antiga do mês alvo para esse profissional
        availabilityRepository.deleteByProfessionalEmailAndDateBetween(
            email, mesAlvo.atDay(1), mesAlvo.atEndOfMonth());
        availabilityRepository.flush();

        // Cria os novos slots
        for (AvailabilityDTO dto : dtos) {
            if (dto.startTimes() != null) {
                for (LocalTime startTime : dto.startTimes()) {
                    ProfessionalAvailability availability = new ProfessionalAvailability();
                    availability.setProfessional(profissional);
                    availability.setDate(dto.date());
                    availability.setStartTime(startTime);
                    availability.setEndTime(startTime.plusHours(1)); // Always 1 hour
                    availabilityRepository.save(availability);
                }
            }
        }
    }

    private void validarRemocaoHorariosOcupados(Long professionalId, YearMonth mesAlvo, List<AvailabilityDTO> novosDtos) {
        LocalDateTime inicio = mesAlvo.atDay(1).atStartOfDay();
        LocalDateTime fim = mesAlvo.atEndOfMonth().atTime(LocalTime.MAX);

        var consultasOcupadas = appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
            professionalId, inicio, fim, com.sistema.lucas.model.enums.StatusConsulta.CANCELADA
        );

        // ✅ Apenas valida consultas que estão agendadas ou confirmadas (ativas)
        var consultasAtivas = consultasOcupadas.stream()
            .filter(c -> c.getStatus() == com.sistema.lucas.model.enums.StatusConsulta.AGENDADA
                      || c.getStatus() == com.sistema.lucas.model.enums.StatusConsulta.CONFIRMADA
                      || c.getStatus() == com.sistema.lucas.model.enums.StatusConsulta.CONFIRMADA_PROFISSIONAL)
            .toList();

        for (var consulta : consultasAtivas) {
            LocalDate dataConsulta = consulta.getDateTime().toLocalDate();
            LocalTime horaConsulta = consulta.getDateTime().toLocalTime().withMinute(0).withSecond(0).withNano(0);

            // Verifica se este horário de consulta existe na nova lista
            boolean mantido = novosDtos.stream()
                .filter(dto -> dto.date().equals(dataConsulta))
                .anyMatch(dto -> dto.startTimes() != null && dto.startTimes().contains(horaConsulta));

            if (!mantido) {
                String msg = String.format("Paciente marcado para o dia %s às %s. Para desmarcar, justifique e cancele a consulta individualmente.",
                    dataConsulta.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")),
                    horaConsulta.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                throw new RuntimeException(msg);
            }
        }
    }

    private void verificarPrazoSubmissao(YearMonth mesAlvo) {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        YearMonth mesAtual = YearMonth.from(hoje);
        YearMonth proximoMes = mesAtual.plusMonths(1);
        
        if (mesAlvo.equals(proximoMes) || mesAlvo.equals(mesAtual)) {
            // Permitido alterar mês atual e próximo.
            // Para o mês atual, a regra de não remover horários ocupados é validada em validarRemocaoHorariosOcupados.
        } else if (mesAlvo.isBefore(mesAtual)) {
            throw new RuntimeException("Não é permitido alterar a disponibilidade de meses passados.");
        }
    }

    // ─── Cálculo de Slots ────────────────────────────────────────────────────

    public List<SlotDTO> getSlotsDisponiveis(@org.springframework.lang.NonNull Long professionalId, LocalDate date) {
        var profissional = professionalRepository.findById(professionalId)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado."));

        // Busca todos os slots salvos para esse dia da semana (agora por data específica)
        var availabilities = availabilityRepository
            .findByProfessionalEmailAndDate(profissional.getEmail(), date);

        if (availabilities.isEmpty()) {
            return List.of(); 
        }

        // Gera a lista de DTOs a partir da disponibilidade salva
        List<SlotDTO> todosSlots = availabilities.stream()
            .map(a -> new SlotDTO(a.getStartTime(), a.getEndTime()))
            .sorted((a, b) -> a.startTime().compareTo(b.startTime()))
            .collect(Collectors.toList());

        // Busca consultas já agendadas nesse dia (status != CANCELADA)
        LocalDateTime inicioDia = date.atStartOfDay();
        LocalDateTime fimDia = date.plusDays(1).atStartOfDay();
        var consultasExistentes = appointmentRepository
            .findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                professionalId, inicioDia, fimDia,
                com.sistema.lucas.model.enums.StatusConsulta.CANCELADA
            );

        // Extrai os horários já ocupados
        Set<LocalTime> horariosOcupados = consultasExistentes.stream()
            .map(c -> c.getDateTime().toLocalTime())
            .collect(Collectors.toSet());

        // Filtra slots livres + não permite agendar no passado
        // Usamos ZoneId de SP para alinhar com o horário do servidor/usuário
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        return todosSlots.stream()
            .filter(slot -> !horariosOcupados.contains(slot.startTime()))
            .filter(slot -> date.atTime(slot.startTime()).isAfter(agora))
            .toList();
    }
}
