package com.mkappworks.moduleservice.repository;

import com.mkappworks.moduleservice.entity.Module;
import com.mkappworks.moduleservice.entity.ModuleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {
    Optional<Module> findByCode(String code);
    boolean existsByCode(String code);
    Page<Module> findByStatus(ModuleStatus status, Pageable pageable);
    List<Module> findByTeacherId(UUID teacherId);
    Page<Module> findBySemesterAndAcademicYear(String semester, String academicYear, Pageable pageable);
}
