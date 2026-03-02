package com.sms.student.service;

import com.sms.student.dto.StudentRequest;
import com.sms.student.dto.StudentResponse;
import com.sms.student.exception.DuplicateResourceException;
import com.sms.student.exception.ResourceNotFoundException;
import com.sms.student.model.Student;
import com.sms.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Unit Tests")
class StudentServiceTest {

    @Mock private StudentRepository studentRepository;
    @InjectMocks private StudentService studentService;

    private StudentRequest request;
    private Student student;

    @BeforeEach
    void setUp() {
        request = StudentRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .phone("1234567890")
                .dateOfBirth(LocalDate.of(2000, 1, 15))
                .programme("Computer Science")
                .yearOfStudy(1)
                .gender(Student.Gender.MALE)
                .build();

        student = Student.builder()
                .id(1L)
                .studentNumber("STU2401234")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .phone("1234567890")
                .programme("Computer Science")
                .yearOfStudy(1)
                .status(Student.StudentStatus.ACTIVE)
                .gpa(0.0)
                .build();
    }

    @Test
    @DisplayName("createStudent — succeeds for new email")
    void createStudent_NewEmail_ReturnsResponse() {
        when(studentRepository.existsByEmail("john.doe@test.com")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        StudentResponse response = studentService.createStudent(request);

        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getEmail()).isEqualTo("john.doe@test.com");
        assertThat(response.getStatus()).isEqualTo(Student.StudentStatus.ACTIVE);
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("createStudent — throws DuplicateResourceException for existing email")
    void createStudent_DuplicateEmail_Throws() {
        when(studentRepository.existsByEmail("john.doe@test.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john.doe@test.com");
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("getStudentById — returns response for existing ID")
    void getStudentById_Exists_ReturnsResponse() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        StudentResponse response = studentService.getStudentById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStudentNumber()).isEqualTo("STU2401234");
    }

    @Test
    @DisplayName("getStudentById — throws ResourceNotFoundException for unknown ID")
    void getStudentById_NotFound_Throws() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAllStudents — returns paginated result")
    void getAllStudents_ReturnsPaginatedPage() {
        var pageable = PageRequest.of(0, 10);
        Page<Student> page = new PageImpl<>(List.of(student), pageable, 1);
        when(studentRepository.findAll(pageable)).thenReturn(page);

        Page<StudentResponse> result = studentService.getAllStudents(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john.doe@test.com");
    }

    @Test
    @DisplayName("updateStudent — saves updated fields")
    void updateStudent_ValidRequest_SavesAndReturns() {
        // student.email == request.email → existsByEmail is short-circuited (not called)
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        StudentResponse response = studentService.updateStudent(1L, request);

        assertThat(response).isNotNull();
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("updateStudent — throws DuplicateResourceException when changing to existing email")
    void updateStudent_EmailTakenByOther_Throws() {
        student.setEmail("old@test.com"); // different from request email
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.existsByEmail("john.doe@test.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.updateStudent(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteStudent — calls deleteById for existing ID")
    void deleteStudent_Exists_DeletesSuccessfully() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(studentRepository).deleteById(1L);

        assertThatCode(() -> studentService.deleteStudent(1L)).doesNotThrowAnyException();
        verify(studentRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteStudent — throws ResourceNotFoundException for unknown ID")
    void deleteStudent_NotFound_Throws() {
        when(studentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> studentService.deleteStudent(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateStudentStatus — persists new status")
    void updateStudentStatus_ValidStatus_Updates() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        student.setStatus(Student.StudentStatus.SUSPENDED);
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        StudentResponse response = studentService.updateStudentStatus(1L, Student.StudentStatus.SUSPENDED);

        assertThat(response.getStatus()).isEqualTo(Student.StudentStatus.SUSPENDED);
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("searchStudents — delegates to repository search")
    void searchStudents_ReturnsMatchingPage() {
        var pageable = PageRequest.of(0, 10);
        Page<Student> page = new PageImpl<>(List.of(student), pageable, 1);
        when(studentRepository.searchStudents("John", pageable)).thenReturn(page);

        Page<StudentResponse> result = studentService.searchStudents("John", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
    }
}
