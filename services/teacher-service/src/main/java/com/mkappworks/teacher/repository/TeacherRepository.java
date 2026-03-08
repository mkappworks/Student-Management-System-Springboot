package com.mkappworks.teacher.repository;

import com.mkappworks.teacher.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmail(String email);
    Optional<Teacher> findByEmployeeId(String employeeId);
    boolean existsByEmail(String email);
    Page<Teacher> findByDepartment(String department, Pageable pageable);

    @Query("SELECT t FROM Teacher t WHERE LOWER(t.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Teacher> search(@Param("q") String query, Pageable pageable);
}
