// backend/src/test/java/com/sistema/lucas/service/ProfessionalServiceTest.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.dto.ProfessionalCreateDTO;
import com.sistema.lucas.model.enums.TipoRegistro;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {

    @InjectMocks
    private ProfessionalService professionalService;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Não deve cadastrar profissional com registro duplicado")
    void cadastrarRegistroDuplicado() {
        // ✅ DTO atualizado: agora tem tipoRegistro e registroConselho
        var dto = new ProfessionalCreateDTO(
            "Dr. House", "house@med.com", "senha123",
            TipoRegistro.CRM, "12345-SP", "Infectologia"
        );

        // ✅ era existsByCrm, agora é existsByRegistroConselho
        when(professionalRepository.existsByRegistroConselho("12345-SP")).thenReturn(true);

        var exception = assertThrows(RuntimeException.class, () -> professionalService.create(dto));
        assertTrue(exception.getMessage().contains("registro já está cadastrado"));

        verify(professionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar um profissional com sucesso quando os dados são válidos")
    void cadastrarComSucesso() {
        // ✅ DTO atualizado
        var dto = new ProfessionalCreateDTO(
            "Dr. House", "house@med.com", "senha123",
            TipoRegistro.CRM, "12345-SP", "Infectologia"
        );

        // ✅ era existsByCrm, agora é existsByRegistroConselho
        when(professionalRepository.existsByRegistroConselho(dto.registroConselho())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("senhaCriptografada");

        assertDoesNotThrow(() -> professionalService.create(dto));

        verify(professionalRepository, times(1)).save(any(Professional.class));
    }
}