package com.mkappworks.enrollment.repository;

import com.mkappworks.enrollment.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByModuleId(Long moduleId);
    List<Enrollment> findByStudentIdAndStatus(Long studentId, Enrollment.EnrollmentStatus status);
    Optional<Enrollment> findByStudentIdAndModuleIdAndAcademicYearAndSemester(
            Long studentId, Long moduleId, String academicYear, Integer semester);
    boolean existsByStudentIdAndModuleIdAndAcademicYearAndSemester(
            Long studentId, Long moduleId, String academicYear, Integer semester);
}
