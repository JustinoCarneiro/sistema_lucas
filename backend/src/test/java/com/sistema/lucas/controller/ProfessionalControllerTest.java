package com.sistema.lucas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.dto.ProfessionalCreateDTO;
import com.sistema.lucas.model.enums.TipoRegistro;
import com.sistema.lucas.service.AuditLogService;
import com.sistema.lucas.service.ProfessionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@SuppressWarnings("null")
class ProfessionalControllerTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private ProfessionalService professionalService;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AuditLogService auditLogService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(Objects.requireNonNull(context))
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /professionals — autenticado recebe lista 200")
    void getAll_autenticado_retorna200() throws Exception {
        when(professionalService.findAll()).thenReturn(List.of(new Professional()));

        mockMvc.perform(get("/professionals")
                .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /professionals — ADMIN cria profissional e recebe 201")
    void create_admin_retorna201() throws Exception {
        var dto = new ProfessionalCreateDTO("Dr. Novo", "novo@med.com", "senha123",
                TipoRegistro.CRM, "99999-SP", "Psicologia");

        mockMvc.perform(post("/professionals")
                .with(user("admin@test.com").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /professionals/{id} — ADMIN exclui e recebe 204")
    void delete_admin_retorna204() throws Exception {
        mockMvc.perform(delete("/professionals/1")
                .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /professionals/{id} — PROFESSIONAL recebe 403 (sem permissão)")
    void delete_professional_retorna403() throws Exception {
        mockMvc.perform(delete("/professionals/1")
                .with(user("prof@test.com").roles("PROFESSIONAL")))
                .andExpect(status().isForbidden());
    }
}
