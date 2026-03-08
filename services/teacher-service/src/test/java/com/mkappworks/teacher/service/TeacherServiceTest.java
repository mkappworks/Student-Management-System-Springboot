package com.mkappworks.teacher.service;

import com.mkappworks.teacher.dto.TeacherRequest;
import com.mkappworks.teacher.dto.TeacherResponse;
import com.mkappworks.common.exception.DuplicateResourceException;
import com.mkappworks.common.exception.ResourceNotFoundException;
import com.mkappworks.teacher.model.Teacher;
import com.mkappworks.teacher.repository.TeacherRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherService Unit Tests")
class TeacherServiceTest {

    @Mock private TeacherRepository teacherRepository;
    @InjectMocks private TeacherService teacherService;

    private TeacherRequest request;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        request = TeacherRequest.builder()
                .firstName("Jane").lastName("Doe")
                .email("jane.doe@school.com")
                .department("Computer Science")
                .qualification("PhD")
                .specialization("Machine Learning")
                .yearsOfExperience(5)
                .employmentType(Teacher.EmploymentType.FULL_TIME)
                .build();

        teacher = Teacher.builder()
                .id(1L)
                .employeeId("EMP001234")
                .firstName("Jane").lastName("Doe")
                .email("jane.doe@school.com")
                .department("Computer Science")
                .qualification("PhD")
                .specialization("Machine Learning")
                .yearsOfExperience(5)
                .status(Teacher.TeacherStatus.ACTIVE)
                .employmentType(Teacher.EmploymentType.FULL_TIME)
                .assignedModuleIds(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("createTeacher — succeeds for unique email")
    void createTeacher_UniqueEmail_ReturnsResponse() {
        when(teacherRepository.existsByEmail("jane.doe@school.com")).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenReturn(teacher);

        TeacherResponse response = teacherService.createTeacher(request);

        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getDepartment()).isEqualTo("Computer Science");
        assertThat(response.getStatus()).isEqualTo(Teacher.TeacherStatus.ACTIVE);
        assertThat(response.getEmployeeId()).isNotNull();
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    @DisplayName("createTeacher — throws DuplicateResourceException for existing email")
    void createTeacher_DuplicateEmail_Throws() {
        when(teacherRepository.existsByEmail("jane.doe@school.com")).thenReturn(true);

        assertThatThrownBy(() -> teacherService.createTeacher(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("jane.doe@school.com");
        verify(teacherRepository, never()).save(any());
    }

    @Test
    @DisplayName("getTeacherById — returns response for existing ID")
    void getTeacherById_Exists_ReturnsResponse() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        TeacherResponse response = teacherService.getTeacherById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmployeeId()).isEqualTo("EMP001234");
        assertThat(response.getEmail()).isEqualTo("jane.doe@school.com");
    }

    @Test
    @DisplayName("getTeacherById — throws ResourceNotFoundException for unknown ID")
    void getTeacherById_NotFound_Throws() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getTeacherById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAllTeachers — returns paginated result")
    void getAllTeachers_ReturnsPaginatedPage() {
        var pageable = PageRequest.of(0, 10);
        Page<Teacher> page = new PageImpl<>(List.of(teacher), pageable, 1);
        when(teacherRepository.findAll(pageable)).thenReturn(page);

        Page<TeacherResponse> result = teacherService.getAllTeachers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("searchTeachers — delegates to repository search query")
    void searchTeachers_MatchingQuery_ReturnsList() {
        var pageable = PageRequest.of(0, 10);
        Page<Teacher> page = new PageImpl<>(List.of(teacher), pageable, 1);
        when(teacherRepository.search("Jane", pageable)).thenReturn(page);

        Page<TeacherResponse> result = teacherService.searchTeachers("Jane", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("updateTeacher — saves updated fields")
    void updateTeacher_ValidRequest_SavesAndReturns() {
        teacher.setEmail("jane.doe@school.com"); // same as request
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(teacher);

        TeacherResponse response = teacherService.updateTeacher(1L, request);

        assertThat(response).isNotNull();
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    @DisplayName("updateTeacher — throws when changing to already-used email")
    void updateTeacher_EmailUsedByOther_Throws() {
        teacher.setEmail("old@school.com"); // different from request
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.existsByEmail("jane.doe@school.com")).thenReturn(true);

        assertThatThrownBy(() -> teacherService.updateTeacher(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(teacherRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteTeacher — calls deleteById for existing ID")
    void deleteTeacher_Exists_DeletesSuccessfully() {
        when(teacherRepository.existsById(1L)).thenReturn(true);
        doNothing().when(teacherRepository).deleteById(1L);

        assertThatCode(() -> teacherService.deleteTeacher(1L)).doesNotThrowAnyException();
        verify(teacherRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTeacher — throws ResourceNotFoundException for unknown ID")
    void deleteTeacher_NotFound_Throws() {
        when(teacherRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> teacherService.deleteTeacher(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
