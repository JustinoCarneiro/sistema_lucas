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

        if (dto.endTime().isBefore(dto.startTime()) || dto.endTime().equals(dto.startTime())) {
            throw new RuntimeException("Horário de término deve ser posterior ao de início.");
        }

        // Mínimo de 1h de janela
        if (Duration.between(dto.startTime(), dto.endTime()).toMinutes() < 60) {
            throw new RuntimeException("A janela de atendimento deve ter no mínimo 1 hora.");
        }

        var existing = availabilityRepository.findByProfessionalEmailAndDayOfWeek(email, dto.dayOfWeek());

        ProfessionalAvailability availability;
        if (existing.isPresent()) {
            availability = existing.get();
        } else {
            availability = new ProfessionalAvailability();
            availability.setProfessional(profissional);
            availability.setDayOfWeek(dto.dayOfWeek());
        }

        availability.setStartTime(dto.startTime());
        availability.setEndTime(dto.endTime());
        availabilityRepository.save(availability);
    }

    @Transactional
    public void removerDia(String email, DayOfWeek dayOfWeek) {
        availabilityRepository.deleteByProfessionalEmailAndDayOfWeek(email, dayOfWeek);
    }

    // ─── Cálculo de Slots ────────────────────────────────────────────────────

    public List<SlotDTO> getSlotsDisponiveis(Long professionalId, LocalDate date) {
        var profissional = professionalRepository.findById(professionalId)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado."));

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // Busca a disponibilidade do profissional para esse dia da semana
        var availability = availabilityRepository
            .findByProfessionalEmailAndDayOfWeek(profissional.getEmail(), dayOfWeek);

        if (availability.isEmpty()) {
            return List.of(); // Profissional não atende nesse dia
        }

        var avail = availability.get();
        LocalTime start = avail.getStartTime();
        LocalTime end = avail.getEndTime();

        // Gera todos os slots de 1h dentro da janela
        List<SlotDTO> todosSlots = new ArrayList<>();
        LocalTime cursor = start;
        while (cursor.plusHours(1).compareTo(end) <= 0) {
            todosSlots.add(new SlotDTO(cursor, cursor.plusHours(1)));
            cursor = cursor.plusHours(1);
        }

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
        LocalDateTime agora = LocalDateTime.now();
        return todosSlots.stream()
            .filter(slot -> !horariosOcupados.contains(slot.startTime()))
            .filter(slot -> date.atTime(slot.startTime()).isAfter(agora))
            .toList();
    }
}
