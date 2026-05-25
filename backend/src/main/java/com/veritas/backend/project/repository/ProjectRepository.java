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
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByTeam(Team team);
    Optional<Project> findByIdAndTeam(Long id, Team team);
    @Query("SELECT p FROM Project p WHERE p.id = :id AND  p.team.department = :department")
    Optional<Project> findByIdAndTeamDepartment(@Param("id") Long id, @Param("department") Department department);
    boolean existsByNameOrProjectKey(String name, String projectKey);

    Optional<Project> findByProjectKey(String projectKey);
    Optional<Project> findByName(String name);

    @Query("SELECT p FROM Project p WHERE p.team.department = :department")
    List<Project> findByTeamDepartment(@Param("department") Department department);


}