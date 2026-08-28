package com.sistema.lucas.controller;

import com.sistema.lucas.repository.SystemLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@SuppressWarnings("null")
class SystemLogControllerTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext context;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private SystemLogRepository systemLogRepository;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(Objects.requireNonNull(context))
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /system-logs — ADMIN recebe 200")
    void listar_admin_retorna200() throws Exception {
        Page<com.sistema.lucas.model.SystemLog> pagina = new PageImpl<>(List.of());
        when(systemLogRepository.findAllByOrderByCriadoEmDesc(any())).thenReturn(pagina);

        mockMvc.perform(get("/system-logs")
                .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /system-logs — TECNICO recebe 200 (mesmo nível de acesso do ADMIN)")
    void listar_tecnico_retorna200() throws Exception {
        Page<com.sistema.lucas.model.SystemLog> pagina = new PageImpl<>(List.of());
        when(systemLogRepository.findAllByOrderByCriadoEmDesc(any())).thenReturn(pagina);

        mockMvc.perform(get("/system-logs")
                .with(user("tecnico@test.com").roles("TECNICO")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /system-logs — PROFESSIONAL recebe 403")
    void listar_professional_retorna403() throws Exception {
        mockMvc.perform(get("/system-logs")
                .with(user("prof@test.com").roles("PROFESSIONAL")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /system-logs — PATIENT recebe 403")
    void listar_patient_retorna403() throws Exception {
        mockMvc.perform(get("/system-logs")
                .with(user("pac@test.com").roles("PATIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /system-logs?level=ERROR — filtra por nível")
    void listar_comFiltroDeLevel_chamaRepositoryCorreto() throws Exception {
        Page<com.sistema.lucas.model.SystemLog> pagina = new PageImpl<>(List.of());
        when(systemLogRepository.findByLevelOrderByCriadoEmDesc(org.mockito.ArgumentMatchers.eq("ERROR"), any())).thenReturn(pagina);

        mockMvc.perform(get("/system-logs?level=error")
                .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
