package com.veritas.backend.team.repository;

import com.veritas.backend.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
    // Added to ensure a team name isn't already taken
    boolean existsByNameIgnoreCase(String name);
    boolean existsByLeaderId(Long leaderId);
    boolean existsByLeaderIdAndTeamIdNot(Long leaderId, Long teamId);
    boolean existsByDepartmentDepartmentId(Long departmentId);
    boolean existsByDepartmentDepartmentIdAndIsActiveTrue(Long departmentId);
    List<Team> findByIsActiveTrue();
}