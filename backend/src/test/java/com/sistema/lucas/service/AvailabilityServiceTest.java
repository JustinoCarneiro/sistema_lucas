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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        @DisplayName("Deve salvar disponibilidade para um dia com sucesso")
        void salvarDiaComSucesso() {
            String email = "ana@clinica.com";
            var dto = new AvailabilityDTO(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

            var profissional = new Professional();
            profissional.setEmail(email);

            when(professionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek(email, DayOfWeek.MONDAY))
                .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> availabilityService.salvarDia(email, dto));
            verify(availabilityRepository, times(1)).save(any(ProfessionalAvailability.class));
        }

        @Test
        @DisplayName("Deve atualizar disponibilidade existente para o mesmo dia")
        void atualizarDiaExistente() {
            String email = "ana@clinica.com";
            var dto = new AvailabilityDTO(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));

            var profissional = new Professional();
            profissional.setEmail(email);

            var existing = new ProfessionalAvailability();
            existing.setProfessional(profissional);
            existing.setDayOfWeek(DayOfWeek.MONDAY);
            existing.setStartTime(LocalTime.of(8, 0));
            existing.setEndTime(LocalTime.of(12, 0));

            when(professionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek(email, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(existing));

            assertDoesNotThrow(() -> availabilityService.salvarDia(email, dto));
            verify(availabilityRepository, times(1)).save(existing);
            assertEquals(LocalTime.of(9, 0), existing.getStartTime());
            assertEquals(LocalTime.of(17, 0), existing.getEndTime());
        }

        @Test
        @DisplayName("Deve lançar erro se horário de fim é antes do início")
        void erroFimAntesDeInicio() {
            String email = "ana@clinica.com";
            var dto = new AvailabilityDTO(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(8, 0));

            var profissional = new Professional();
            profissional.setEmail(email);

            when(professionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            var exception = assertThrows(RuntimeException.class,
                () -> availabilityService.salvarDia(email, dto));
            assertTrue(exception.getMessage().contains("posterior ao de início"));
            verify(availabilityRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro se janela menor que 1 hora")
        void erroJanelaMenorQue1Hora() {
            String email = "ana@clinica.com";
            var dto = new AvailabilityDTO(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(8, 30));

            var profissional = new Professional();
            profissional.setEmail(email);

            when(professionalRepository.findByEmail(email)).thenReturn(Optional.of(profissional));

            var exception = assertThrows(RuntimeException.class,
                () -> availabilityService.salvarDia(email, dto));
            assertTrue(exception.getMessage().contains("no mínimo 1 hora"));
            verify(availabilityRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro se profissional não encontrado")
        void erroProfissionalInexistente() {
            String email = "inexistente@clinica.com";
            var dto = new AvailabilityDTO(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

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
        @DisplayName("Deve gerar slots de 1h dentro da janela configurada")
        void gerarSlotsCorretos() {
            Long profId = 1L;
            // Usamos uma data futura para evitar filtro de passado
            LocalDate data = LocalDate.now().plusDays(7);
            DayOfWeek dow = data.getDayOfWeek();

            var profissional = new Professional();
            profissional.setId(profId);
            profissional.setEmail("ana@clinica.com");

            var availability = new ProfessionalAvailability();
            availability.setStartTime(LocalTime.of(8, 0));
            availability.setEndTime(LocalTime.of(12, 0));

            when(professionalRepository.findById(profId)).thenReturn(Optional.of(profissional));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", dow))
                .thenReturn(Optional.of(availability));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(profId), any(), any(), any()
            )).thenReturn(List.of());

            List<SlotDTO> slots = availabilityService.getSlotsDisponiveis(profId, data);

            // 08-09, 09-10, 10-11, 11-12 = 4 slots
            assertEquals(4, slots.size());
            assertEquals(LocalTime.of(8, 0), slots.get(0).startTime());
            assertEquals(LocalTime.of(9, 0), slots.get(0).endTime());
            assertEquals(LocalTime.of(11, 0), slots.get(3).startTime());
            assertEquals(LocalTime.of(12, 0), slots.get(3).endTime());
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

            var availability = new ProfessionalAvailability();
            availability.setStartTime(LocalTime.of(8, 0));
            availability.setEndTime(LocalTime.of(12, 0));

            // Simula consulta existente às 09:00
            var consultaExistente = new Appointment();
            consultaExistente.setDateTime(data.atTime(9, 0));

            when(professionalRepository.findById(profId)).thenReturn(Optional.of(profissional));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", dow))
                .thenReturn(Optional.of(availability));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(profId), any(), any(), any()
            )).thenReturn(List.of(consultaExistente));

            List<SlotDTO> slots = availabilityService.getSlotsDisponiveis(profId, data);

            // 4 slots - 1 ocupado = 3 slots
            assertEquals(3, slots.size());
            // Verifica que o slot das 09:00 não está presente
            assertTrue(slots.stream().noneMatch(s -> s.startTime().equals(LocalTime.of(9, 0))));
        }

        @Test
        @DisplayName("Deve retornar lista vazia se profissional não atende no dia")
        void retornarVazioSeProfissionalNaoAtende() {
            Long profId = 1L;
            LocalDate data = LocalDate.now().plusDays(7);
            DayOfWeek dow = data.getDayOfWeek();

            var profissional = new Professional();
            profissional.setId(profId);
            profissional.setEmail("ana@clinica.com");

            when(professionalRepository.findById(profId)).thenReturn(Optional.of(profissional));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", dow))
                .thenReturn(Optional.empty());

            List<SlotDTO> slots = availabilityService.getSlotsDisponiveis(profId, data);
            assertTrue(slots.isEmpty());
        }

        @Test
        @DisplayName("Deve lançar erro se profissional não existe")
        void erroProfissionalInexistente() {
            Long profId = 99L;
            when(professionalRepository.findById(profId)).thenReturn(Optional.empty());

            var exception = assertThrows(RuntimeException.class,
                () -> availabilityService.getSlotsDisponiveis(profId, LocalDate.now()));
            assertTrue(exception.getMessage().contains("Profissional não encontrado"));
        }
    }

    // ──────────────────────── Listagem de Profissionais ────────────────

    @Nested
    @DisplayName("Profissionais com Disponibilidade")
    class ProfissionaisComDisponibilidadeTests {

        @Test
        @DisplayName("Deve retornar apenas profissionais que possuem disponibilidade")
        void retornarProfissionaisComDisponibilidade() {
            var ana = new Professional();
            ana.setId(1L);
            ana.setName("Dra. Ana");

            when(availabilityRepository.findProfessionalIdsComDisponibilidade()).thenReturn(List.of(1L));
            when(professionalRepository.findAllById(List.of(1L))).thenReturn(List.of(ana));

            var resultado = availabilityService.getProfissionaisComDisponibilidade();
            assertEquals(1, resultado.size());
            assertEquals("Dra. Ana", resultado.get(0).getName());
        }

        @Test
        @DisplayName("Deve retornar lista vazia se nenhum profissional tem disponibilidade")
        void retornarVazioSemDisponibilidade() {
            when(availabilityRepository.findProfessionalIdsComDisponibilidade()).thenReturn(List.of());
            when(professionalRepository.findAllById(List.of())).thenReturn(List.of());

            var resultado = availabilityService.getProfissionaisComDisponibilidade();
            assertTrue(resultado.isEmpty());
        }
    }
}
