package com.mkappworks.student.service;

import com.mkappworks.student.dto.StudentRequest;
import com.mkappworks.student.dto.StudentResponse;
import com.mkappworks.common.exception.DuplicateResourceException;
import com.mkappworks.common.exception.ResourceNotFoundException;
import com.mkappworks.student.model.Student;
import com.mkappworks.student.repository.StudentRepository;
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
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Student with email already exists: " + request.getEmail());
        }

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .programme(request.getProgramme())
                .yearOfStudy(request.getYearOfStudy())
                .gender(request.getGender())
                .status(Student.StudentStatus.ACTIVE)
                .studentNumber(generateStudentNumber())
                .gpa(0.0)
                .build();

        Student saved = studentRepository.save(student);
        log.info("Created student: {}", saved.getStudentNumber());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        return mapToResponse(findStudentById(id));
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByStudentNumber(String studentNumber) {
        return studentRepository.findByStudentNumber(studentNumber)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "studentNumber", studentNumber));
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> searchStudents(String query, Pageable pageable) {
        return studentRepository.searchStudents(query, pageable).map(this::mapToResponse);
    }

    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student student = findStudentById(id);

        if (!student.getEmail().equals(request.getEmail()) &&
                studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setAddress(request.getAddress());
        student.setProgramme(request.getProgramme());
        student.setYearOfStudy(request.getYearOfStudy());
        student.setGender(request.getGender());

        return mapToResponse(studentRepository.save(student));
    }

    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student", "id", id);
        }
        studentRepository.deleteById(id);
        log.info("Deleted student with id: {}", id);
    }

    public StudentResponse updateStudentStatus(Long id, Student.StudentStatus status) {
        Student student = findStudentById(id);
        student.setStatus(status);
        return mapToResponse(studentRepository.save(student));
    }

    private Student findStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }

    private String generateStudentNumber() {
        String year = String.valueOf(java.time.Year.now().getValue()).substring(2);
        String random = String.format("%06d", new Random().nextInt(999999));
        return "STU" + year + random;
    }

    private StudentResponse mapToResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .studentNumber(student.getStudentNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .dateOfBirth(student.getDateOfBirth())
                .address(student.getAddress())
                .programme(student.getProgramme())
                .yearOfStudy(student.getYearOfStudy())
                .gpa(student.getGpa())
                .status(student.getStatus())
                .gender(student.getGender())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}
