package com.mkappworks.grade.repository;

import com.mkappworks.grade.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudentId(Long studentId);
    List<Grade> findByModuleId(Long moduleId);
    List<Grade> findByStudentIdAndModuleId(Long studentId, Long moduleId);
    Optional<Grade> findByStudentIdAndModuleIdAndAssessmentType(Long studentId, Long moduleId, Grade.AssessmentType type);

    @Query("SELECT AVG(g.score / g.maxScore * 100) FROM Grade g WHERE g.studentId = :studentId")
    Optional<Double> calculateStudentGpa(@Param("studentId") Long studentId);
}
