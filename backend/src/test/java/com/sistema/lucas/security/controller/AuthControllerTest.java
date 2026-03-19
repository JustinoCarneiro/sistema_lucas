package com.sistema.lucas.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.lucas.security.dto.RegisterDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.sistema.lucas.repository.UserRepository userRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.sistema.lucas.repository.PatientRepository patientRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.sistema.lucas.security.service.EmailVerificationService emailVerificationService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void registrarSucesso() throws Exception {
        var dto = new RegisterDTO("Novo Usuario", "novo@email.com", "senha123", "111.111.111-11", "11999998888");
        
        org.mockito.Mockito.when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("Paciente registrado com sucesso! Verifique seu e-mail para confirmar a conta."));
        
        org.mockito.Mockito.verify(emailVerificationService).createAndSendVerificationEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void verificarSucesso() throws Exception {
        org.mockito.Mockito.when(emailVerificationService.verifyToken("valid-token")).thenReturn("E-mail verificado com sucesso!");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/auth/verify")
                .param("token", "valid-token"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("E-mail verificado com sucesso!"));
    }

    @Test
    void verificarFalhaTokenInvalido() throws Exception {
        org.mockito.Mockito.when(emailVerificationService.verifyToken("invalid-token")).thenReturn("Token inválido");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/auth/verify")
                .param("token", "invalid-token"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("Token inválido"));
    }

    @Test
    void registrarEmailDuplicado() throws Exception {
        var dto = new RegisterDTO("Usuario Duplicado", "jaexiste@email.com", "senha123", "222.222.222-22", "11999997777");
        
        org.mockito.Mockito.when(userRepository.findByEmail("jaexiste@email.com")).thenReturn(new com.sistema.lucas.model.User());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("Email já cadastrado"));
    }
}
