package com.platform.user.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "languages", schema = "user_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_languages_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_languages_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_languages_active", columnList = "active")
        })
@Getter
@Setter
public class Language extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "native_name", length = 100)
    private String nativeName;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean system = false;
}