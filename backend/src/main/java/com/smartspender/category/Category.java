package com.smartspender.category;

import com.smartspender.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A spending category owned by exactly one user.
 *
 * Encapsulation note: fields are private, mutation happens through intent-revealing
 * methods. The no-args constructor is required by JPA but is package-private
 * to discourage direct instantiation outside this package.
 */
@Entity
@Table(
    name = "categories",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_categories_user_name",
        columnNames = {"user_id", "name"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 50)
    private String icon;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ---- Factory constructor: the ONLY way to create a new Category ----

    public Category(User user, String name, String icon, String color) {
        this.user = requireNonNull(user, "user");
        this.name = normalizeAndValidateName(name);
        this.icon = requireNonBlank(icon, "icon");
        this.color = validateColor(color);
        this.isDefault = false;
        this.createdAt = Instant.now();
    }

    // ---- Intent-revealing mutation methods ----

    public void renameTo(String newName) {
        this.name = normalizeAndValidateName(newName);
    }

    public void restyle(String newIcon, String newColor) {
        this.icon = requireNonBlank(newIcon, "icon");
        this.color = validateColor(newColor);
    }

    // ---- Domain validation lives HERE, not in the service ----

    private static String normalizeAndValidateName(String raw) {
        String trimmed = requireNonBlank(raw, "name").trim();
        if (trimmed.length() > 80) {
            throw new IllegalArgumentException("Category name must not exceed 80 characters");
        }
        return trimmed;
    }

    private static String validateColor(String color) {
        String value = requireNonBlank(color, "color");
        if (!value.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Color must be a hex string like #6366f1");
        }
        return value;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}