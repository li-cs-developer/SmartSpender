package com.smartspender.category.dto;

import com.smartspender.category.Category;

public record CategoryResponse(
    Long id,
    String name,
    String icon,
    String color,
    boolean isDefault
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
            c.getId(), c.getName(), c.getIcon(), c.getColor(), c.isDefault()
        );
    }
}