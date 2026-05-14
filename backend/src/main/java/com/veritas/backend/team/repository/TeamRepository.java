package com.veritas.backend.team.repository;

import com.veritas.backend.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    // Added to ensure a team name isn't already taken
	boolean existsByNameIgnoreCase(String name);
    boolean existsByLeaderId(Long leaderId);
    boolean existsByLeaderIdAndTeamIdNot(Long leaderId, Long teamId);
    boolean existsByDepartmentDepartmentId(Long departmentId);
    Optional<Team> findByName(String name);
}