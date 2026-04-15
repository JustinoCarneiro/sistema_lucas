package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record AvailabilityDTO(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull List<LocalTime> startTimes
) {}
