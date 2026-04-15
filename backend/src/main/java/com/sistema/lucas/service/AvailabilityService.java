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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    @Autowired private ProfessionalAvailabilityRepository availabilityRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    // ─── Leitura ─────────────────────────────────────────────────────────────

    public List<ProfessionalAvailability> getMinhaDisponibilidade(String email) {
        return availabilityRepository.findByProfessionalEmail(email);
    }

    public List<Professional> getProfissionaisComDisponibilidade() {
        List<Long> ids = availabilityRepository.findProfessionalIdsComDisponibilidade();
        return professionalRepository.findAllById(ids);
    }

    public List<DayOfWeek> getWorkingDays(Long professionalId) {
        return availabilityRepository.findByProfessionalId(professionalId)
            .stream()
            .map(ProfessionalAvailability::getDayOfWeek)
            .distinct()
            .sorted()
            .toList();
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    @Transactional
    public void salvarDia(String email, AvailabilityDTO dto) {
        var profissional = professionalRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado."));

        // Remove a disponibilidade antiga do dia para esse profissional
        availabilityRepository.deleteByProfessionalEmailAndDayOfWeek(email, dto.dayOfWeek());
        availabilityRepository.flush();

        // Cria os novos slots
        if (dto.startTimes() != null) {
            for (LocalTime startTime : dto.startTimes()) {
                ProfessionalAvailability availability = new ProfessionalAvailability();
                availability.setProfessional(profissional);
                availability.setDayOfWeek(dto.dayOfWeek());
                availability.setStartTime(startTime);
                availability.setEndTime(startTime.plusHours(1)); // Always 1 hour
                availabilityRepository.save(availability);
            }
        }
    }

    @Transactional
    public void removerDia(String email, DayOfWeek dayOfWeek) {
        availabilityRepository.deleteByProfessionalEmailAndDayOfWeek(email, dayOfWeek);
        availabilityRepository.flush();
    }

    // ─── Cálculo de Slots ────────────────────────────────────────────────────

    public List<SlotDTO> getSlotsDisponiveis(Long professionalId, LocalDate date) {
        var profissional = professionalRepository.findById(professionalId)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado."));

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // Busca todos os slots salvos para esse dia da semana
        var availabilities = availabilityRepository
            .findByProfessionalEmailAndDayOfWeek(profissional.getEmail(), dayOfWeek);

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
