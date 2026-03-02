package com.sms.teacher.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "teachers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String profilePicture;

    @Column(nullable = false)
    private String department;

    private String qualification;
    private String specialization;
    private Integer yearsOfExperience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherStatus status;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @ElementCollection
    @CollectionTable(name = "teacher_modules", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "module_id")
    @Builder.Default
    private Set<Long> assignedModuleIds = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum TeacherStatus { ACTIVE, INACTIVE, ON_LEAVE, RETIRED }
    public enum EmploymentType { FULL_TIME, PART_TIME, CONTRACT, VISITING }
}
