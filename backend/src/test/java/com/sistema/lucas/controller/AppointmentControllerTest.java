package com.sistema.lucas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.lucas.model.dto.AppointmentCancelDTO;
import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.service.AppointmentService;
import com.sistema.lucas.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@SuppressWarnings("null")
class AppointmentControllerTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AppointmentService appointmentService;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AuditLogService auditLogService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(Objects.requireNonNull(context))
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("POST /consultas — PATIENT cria consulta e recebe 201")
    void agendar_patient_retorna201() throws Exception {
        var dto = new AppointmentCreateDTO(1L, LocalDateTime.now().plusDays(30), "Rotina");

        mockMvc.perform(post("/consultas")
                .with(user("pac@test.com").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PATCH /consultas/{id}/aprovar — PROFESSIONAL aprova e recebe 200")
    void aprovar_professional_retorna200() throws Exception {
        mockMvc.perform(patch("/consultas/1/aprovar")
                .with(user("prof@test.com").roles("PROFESSIONAL")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /consultas/{id}/confirmar-paciente — PATIENT confirma e recebe 200")
    void confirmarPaciente_patient_retorna200() throws Exception {
        mockMvc.perform(patch("/consultas/1/confirmar-paciente")
                .with(user("pac@test.com").roles("PATIENT")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /consultas/profissional/atrasadas — PROFESSIONAL recebe lista 200")
    void getAtrasadas_professional_retorna200() throws Exception {
        when(appointmentService.findAtrasadasPorProfissional(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/consultas/profissional/atrasadas")
                .with(user("prof@test.com").roles("PROFESSIONAL")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /consultas/profissional/atrasadas — sem autenticação retorna 4xx")
    void getAtrasadas_semAuth_retorna4xx() throws Exception {
        mockMvc.perform(get("/consultas/profissional/atrasadas"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /consultas/{id}/cancelar — com justificativa retorna 204")
    void cancelar_comJustificativa_retorna204() throws Exception {
        var dto = new AppointmentCancelDTO("Paciente não compareceu");

        mockMvc.perform(post("/consultas/1/cancelar")
                .with(user("prof@test.com").roles("PROFESSIONAL"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
    }
}
