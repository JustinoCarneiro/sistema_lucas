package com.sistema.lucas.service;

import com.sistema.lucas.model.AppointmentCreateDTO;
import com.sistema.lucas.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @InjectMocks
    private AppointmentService service;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Test
    @DisplayName("Deve lançar erro quando o profissional não existe no agendamento")
    void scenario01() {
        // Arrange
        var dto = new AppointmentCreateDTO(99L, 1L, LocalDateTime.now().plusDays(1), "Consulta");
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> service.schedule(dto));
    }
}