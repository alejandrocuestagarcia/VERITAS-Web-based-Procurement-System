package com.veritas.backend.project.repository;

import com.veritas.backend.project.entity.Project;
import com.veritas.backend.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByTeam(Team team);
}