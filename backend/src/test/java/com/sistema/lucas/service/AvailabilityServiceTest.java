package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.ProfessionalAvailability;
import com.sistema.lucas.model.dto.AvailabilityDTO;
import com.sistema.lucas.model.dto.SlotDTO;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.ProfessionalAvailabilityRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @InjectMocks
    private AvailabilityService availabilityService;

    @Mock
    private ProfessionalAvailabilityRepository availabilityRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    // ──────────────────────── CRUD ────────────────────────

    @Nested
    @DisplayName("CRUD de Disponibilidade")
    class CrudTests {

        @Test
        @DisplayName("Deve salvar slots para um dia com sucesso")
        void salvarDiaComSucesso() {
            String email = "ana@clinica.com";
            var dto = new AvailabilityDTO(DayOfWeek.MONDAY, List.of(LocalTime.of(8, 0), LocalTime.of(9, 0)));

            var profissional = new Professional();
            profissional.setEmail(email);

            when(professionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            assertDoesNotThrow(() -> availabilityService.salvarDia(email, dto));
            
            // Deve deletar os antigos e salvar os novos
            verify(availabilityRepository, times(1)).deleteByProfessionalEmailAndDayOfWeek(email, DayOfWeek.MONDAY);
            verify(availabilityRepository, times(2)).save(any(ProfessionalAvailability.class));
        }

        @Test
        @DisplayName("Deve lançar erro se profissional não encontrado")
        void erroProfissionalInexistente() {
            String email = "inexistente@clinica.com";
            var dto = new AvailabilityDTO(DayOfWeek.MONDAY, List.of(LocalTime.of(8, 0)));

            when(professionalRepository.findByEmail(email)).thenReturn(Optional.empty());

            var exception = assertThrows(RuntimeException.class,
                () -> availabilityService.salvarDia(email, dto));
            assertTrue(exception.getMessage().contains("Profissional não encontrado"));
        }

        @Test
        @DisplayName("Deve remover disponibilidade de um dia")
        void removerDia() {
            String email = "ana@clinica.com";
            assertDoesNotThrow(() -> availabilityService.removerDia(email, DayOfWeek.MONDAY));
            verify(availabilityRepository, times(1))
                .deleteByProfessionalEmailAndDayOfWeek(email, DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("Deve buscar disponibilidade do profissional logado")
        void buscarMinhaDisponibilidade() {
            String email = "ana@clinica.com";
            var avail = new ProfessionalAvailability();
            when(availabilityRepository.findByProfessionalEmail(email)).thenReturn(List.of(avail));

            var resultado = availabilityService.getMinhaDisponibilidade(email);
            assertEquals(1, resultado.size());
        }
    }

    // ──────────────────────── Cálculo de Slots ────────────────────────

    @Nested
    @DisplayName("Cálculo de Slots Disponíveis")
    class SlotTests {

        @Test
        @DisplayName("Deve retornar slots salvos no banco")
        void gerarSlotsCorretos() {
            Long profId = 1L;
            LocalDate data = LocalDate.now().plusDays(7);
            DayOfWeek dow = data.getDayOfWeek();

            var profissional = new Professional();
            profissional.setId(profId);
            profissional.setEmail("ana@clinica.com");

            var slot1 = new ProfessionalAvailability();
            slot1.setStartTime(LocalTime.of(8, 0));
            slot1.setEndTime(LocalTime.of(9, 0));

            var slot2 = new ProfessionalAvailability();
            slot2.setStartTime(LocalTime.of(10, 0));
            slot2.setEndTime(LocalTime.of(11, 0));

            when(professionalRepository.findById(profId)).thenReturn(Optional.of(profissional));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", dow))
                .thenReturn(List.of(slot1, slot2));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(profId), any(), any(), any()
            )).thenReturn(List.of());

            List<SlotDTO> slots = availabilityService.getSlotsDisponiveis(profId, data);

            assertEquals(2, slots.size());
            assertEquals(LocalTime.of(8, 0), slots.get(0).startTime());
            assertEquals(LocalTime.of(10, 0), slots.get(1).startTime());
        }

        @Test
        @DisplayName("Deve excluir slots já ocupados por consultas existentes")
        void excluirSlotsOcupados() {
            Long profId = 1L;
            LocalDate data = LocalDate.now().plusDays(7);
            DayOfWeek dow = data.getDayOfWeek();

            var profissional = new Professional();
            profissional.setId(profId);
            profissional.setEmail("ana@clinica.com");

            var slot1 = new ProfessionalAvailability();
            slot1.setStartTime(LocalTime.of(8, 0));
            slot1.setEndTime(LocalTime.of(9, 0));

            var slot2 = new ProfessionalAvailability();
            slot2.setStartTime(LocalTime.of(9, 0));
            slot2.setEndTime(LocalTime.of(10, 0));

            // Simula consulta existente às 09:00
            var consultaExistente = new Appointment();
            consultaExistente.setDateTime(data.atTime(9, 0));

            when(professionalRepository.findById(profId)).thenReturn(Optional.of(profissional));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", dow))
                .thenReturn(List.of(slot1, slot2));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(profId), any(), any(), any()
            )).thenReturn(List.of(consultaExistente));

            List<SlotDTO> slots = availabilityService.getSlotsDisponiveis(profId, data);

            // 2 slots - 1 ocupado = 1 slot restasnte (08:00)
            assertEquals(1, slots.size());
            assertEquals(LocalTime.of(8, 0), slots.get(0).startTime());
        }

        @Test
        @DisplayName("Deve excluir slots que já passaram no horário de Brasília")
        void excluirSlotsPassadosSP() {
            Long profId = 1L;
            // Hoje: Quarta, 15/04/2026
            LocalDate data = LocalDate.of(2026, 4, 15);
            DayOfWeek dow = data.getDayOfWeek();

            var profissional = new Professional();
            profissional.setId(profId);
            profissional.setEmail("ana@clinica.com");

            var slotPassado = new ProfessionalAvailability();
            slotPassado.setStartTime(LocalTime.of(8, 0)); // Passado (08:00 < 12:00)

            var slotFuturo = new ProfessionalAvailability();
            slotFuturo.setStartTime(LocalTime.of(14, 0)); // Futuro (14:00 > 12:00)

            when(professionalRepository.findById(profId)).thenReturn(Optional.of(profissional));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", dow))
                .thenReturn(List.of(slotPassado, slotFuturo));
            
            // Simula que "agora" em SP é 12:00 do dia 15/04
            try (var mockedLocalDateTime = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
                LocalDateTime agoraFake = LocalDateTime.of(2026, 4, 15, 12, 0);
                mockedLocalDateTime.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                    .thenReturn(agoraFake);

                List<SlotDTO> slots = availabilityService.getSlotsDisponiveis(profId, data);

                assertEquals(1, slots.size(), "Deve retornar apenas o slot futuro");
                assertEquals(LocalTime.of(14, 0), slots.get(0).startTime());
            }
        }

        @Test
        @DisplayName("Deve chamar flush após deletar para evitar erros de integridade")
        void verificarFlushAoSalvar() {
            String email = "ana@clinica.com";
            var dto = new AvailabilityDTO(DayOfWeek.MONDAY, List.of(LocalTime.of(8, 0)));
            when(professionalRepository.findByEmail(email)).thenReturn(Optional.of(new Professional()));

            availabilityService.salvarDia(email, dto);

            verify(availabilityRepository).deleteByProfessionalEmailAndDayOfWeek(email, DayOfWeek.MONDAY);
            verify(availabilityRepository, atLeastOnce()).flush();
        }
    }
}
