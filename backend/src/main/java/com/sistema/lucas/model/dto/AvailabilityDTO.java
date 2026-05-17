package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailabilityDTO(
    @NotNull LocalDate date,
    @NotNull List<LocalTime> startTimes
) {}
