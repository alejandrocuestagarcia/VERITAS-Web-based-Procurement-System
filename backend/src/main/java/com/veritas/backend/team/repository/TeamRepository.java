package com.veritas.backend.team.repository;

import com.veritas.backend.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    // Added to ensure a team name isn't already taken
	boolean existsByNameIgnoreCase(String name);
}