package com.smartspender.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

    @NotBlank(message = "name is required")
    @Size(max = 80, message = "name must be 80 characters or fewer")
    String name,

    @NotBlank(message = "icon is required")
    @Size(max = 50, message = "icon must be 50 characters or fewer")
    String icon,

    @NotBlank(message = "color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a hex string like #22c55e")
    String color
) {}