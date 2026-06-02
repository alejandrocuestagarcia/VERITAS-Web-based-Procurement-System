package com.veritas.backend.user.repository;

import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("SELECT u from User u where u.isActive = true and " +
      "(:role is null or u.role = :role) and " +
      "(:search IS NULL OR (LOWER(u.name) LIKE :search OR LOWER(u.email) LIKE :search))")
  Page<User> findAllFiltered(@Param("search") String search, @Param("role") UserRole role,
      Pageable pageable);

  long countByIsActiveFalse();

  long countByIsActiveTrue();

  List<User> findAllByTeamTeamId(Long teamId);

  boolean existsByTeamTeamId(Long teamId);

  Page<User> findAllByRoleAndIsActiveTrue(UserRole role, Pageable pageable);

  List<User> findAllByRoleAndIsActiveTrue(UserRole role);

  boolean existsByDepartmentDepartmentId(Long departmentId);
}