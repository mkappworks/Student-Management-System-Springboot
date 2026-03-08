package com.mkappworks.student.repository;

import com.mkappworks.student.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByStudentNumber(String studentNumber);
    boolean existsByEmail(String email);
    boolean existsByStudentNumber(String studentNumber);
    Page<Student> findByProgramme(String programme, Pageable pageable);
    Page<Student> findByStatus(Student.StudentStatus status, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Student> searchStudents(@Param("query") String query, Pageable pageable);
}
