package com.sistema.lucas.controller;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.service.AuditLogService;
import com.sistema.lucas.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class PatientControllerTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext context;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private PatientService patientService;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AuditLogService auditLogService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(Objects.requireNonNull(context))
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /patients — ADMIN recebe lista 200")
    void getAll_admin_retorna200() throws Exception {
        when(patientService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/patients")
                .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /patients/me — PATIENT recebe próprio perfil 200")
    void getMyProfile_patient_retorna200() throws Exception {
        when(patientService.getMyProfile("pac@test.com")).thenReturn(new Patient());

        mockMvc.perform(get("/patients/me")
                .with(user("pac@test.com").roles("PATIENT")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /patients/{id}/desbloquear — ADMIN desbloqueia e recebe 200")
    void desbloquear_admin_retorna200() throws Exception {
        mockMvc.perform(patch("/patients/1/desbloquear")
                .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /patients/{id}/desbloquear — PATIENT recebe 403 (sem permissão)")
    void desbloquear_patient_retorna403() throws Exception {
        mockMvc.perform(patch("/patients/1/desbloquear")
                .with(user("pac@test.com").roles("PATIENT")))
                .andExpect(status().isForbidden());
    }
}
