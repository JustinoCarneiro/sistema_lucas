package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record WaitlistEntradaDTO(
    @NotNull Long professionalId,
    @NotNull @Future LocalDateTime dateTime
) {}
