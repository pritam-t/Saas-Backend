package com.pritam.saasbackend.permission.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
        name = "permissions",
        schema = "public"
)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    protected Permission() {
    }

    public Permission(
            String code,
            String description
    ) {
        this.code = code;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}