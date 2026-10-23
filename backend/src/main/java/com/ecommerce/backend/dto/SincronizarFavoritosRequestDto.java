package com.ecommerce.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SincronizarFavoritosRequestDto(
        @NotNull @Size(max = 500) List<@NotNull UUID> agregar,
        @NotNull @Size(max = 500) List<@NotNull UUID> eliminar) {}
