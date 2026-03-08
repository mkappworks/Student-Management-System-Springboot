package com.mkappworks.moduleservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private Integer credits;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "current_enrollment")
    private Integer currentEnrollment = 0;

    @Enumerated(EnumType.STRING)
    private ModuleStatus status;

    private String semester;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private String location;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ModuleStatus.ACTIVE;
        if (currentEnrollment == null) currentEnrollment = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasCapacity() {
        return maxStudents == null || currentEnrollment < maxStudents;
    }
}
