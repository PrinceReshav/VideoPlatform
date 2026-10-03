package com.platform.user.entity;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "genres", schema = "user_schema",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_genres_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_genres_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_genres_parent", columnList = "parent_id"),
                @Index(name = "idx_genres_active", columnList = "active"),
                @Index(name = "idx_genres_sort_order", columnList = "sort_order")
        })
@Getter
@Setter
public class Genre extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Genre parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Genre> children = new HashSet<>();

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean system = false;
}