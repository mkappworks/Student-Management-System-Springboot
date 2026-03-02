package com.sms.grade.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "modules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String moduleCode;

    @Column(nullable = false)
    private String moduleName;

    private String description;
    private Integer creditHours;
    private String department;
    private Long teacherId;

    @Enumerated(EnumType.STRING)
    private ModuleStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ModuleStatus { ACTIVE, INACTIVE, ARCHIVED }
}
