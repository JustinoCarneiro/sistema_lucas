package com.sistema.lucas.model.dto;

import java.time.LocalTime;

public record SlotDTO(
    LocalTime startTime,
    LocalTime endTime
) {}
