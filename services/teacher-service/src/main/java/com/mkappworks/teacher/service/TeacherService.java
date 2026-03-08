package com.mkappworks.teacher.service;

import com.mkappworks.teacher.dto.TeacherRequest;
import com.mkappworks.teacher.dto.TeacherResponse;
import com.mkappworks.common.exception.DuplicateResourceException;
import com.mkappworks.common.exception.ResourceNotFoundException;
import com.mkappworks.teacher.model.Teacher;
import com.mkappworks.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherResponse createTeacher(TeacherRequest request) {
        if (teacherRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Teacher with email already exists: " + request.getEmail());
        }

        Teacher teacher = Teacher.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .department(request.getDepartment())
                .qualification(request.getQualification())
                .specialization(request.getSpecialization())
                .yearsOfExperience(request.getYearsOfExperience())
                .employmentType(request.getEmploymentType())
                .status(Teacher.TeacherStatus.ACTIVE)
                .employeeId(generateEmployeeId())
                .build();

        Teacher saved = teacherRepository.save(teacher);
        log.info("Created teacher: {}", saved.getEmployeeId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacherById(Long id) {
        return mapToResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<TeacherResponse> getAllTeachers(Pageable pageable) {
        return teacherRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TeacherResponse> searchTeachers(String query, Pageable pageable) {
        return teacherRepository.search(query, pageable).map(this::mapToResponse);
    }

    public TeacherResponse updateTeacher(Long id, TeacherRequest request) {
        Teacher teacher = findById(id);
        if (!teacher.getEmail().equals(request.getEmail()) && teacherRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }
        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setEmail(request.getEmail());
        teacher.setPhone(request.getPhone());
        teacher.setDepartment(request.getDepartment());
        teacher.setQualification(request.getQualification());
        teacher.setSpecialization(request.getSpecialization());
        teacher.setYearsOfExperience(request.getYearsOfExperience());
        return mapToResponse(teacherRepository.save(teacher));
    }

    public void deleteTeacher(Long id) {
        if (!teacherRepository.existsById(id)) throw new ResourceNotFoundException("Teacher", "id", id);
        teacherRepository.deleteById(id);
    }

    private Teacher findById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));
    }

    private String generateEmployeeId() {
        return "EMP" + String.format("%06d", new Random().nextInt(999999));
    }

    private TeacherResponse mapToResponse(Teacher t) {
        return TeacherResponse.builder()
                .id(t.getId()).employeeId(t.getEmployeeId())
                .firstName(t.getFirstName()).lastName(t.getLastName())
                .email(t.getEmail()).phone(t.getPhone())
                .department(t.getDepartment()).qualification(t.getQualification())
                .specialization(t.getSpecialization()).yearsOfExperience(t.getYearsOfExperience())
                .status(t.getStatus()).employmentType(t.getEmploymentType())
                .assignedModuleIds(t.getAssignedModuleIds()).createdAt(t.getCreatedAt())
                .build();
    }
}
