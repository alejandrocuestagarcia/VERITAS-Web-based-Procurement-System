package com.veritas.backend.project.repository;

import com.veritas.backend.department.entity.Department;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByTeam(Team team);
    List<Project> findByTeamDepartment(Department department);
    boolean existsByNameOrProjectKey(String name, String projectKey);

    @Query("SELECT p FROM Project p WHERE p.team.department = :department")
    List<Project> findByTeamDepartment(@Param("department") Department department);
}