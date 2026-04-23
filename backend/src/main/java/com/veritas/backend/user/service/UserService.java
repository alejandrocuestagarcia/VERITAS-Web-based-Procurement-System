package com.veritas.backend.user.service;

import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.dto.UserStatsDto;
import com.veritas.backend.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
  UserDto createUser(UserCreationRequestDto userDto);

  UserDto editUser(Long id, UserEditDto edits);

  Page<UserDto> getAllUsers(Pageable pageable);

  UserStatsDto getUserStats();

  Page<UserDto> getAllUsersFiltered(Pageable pageable, String filter, UserRole userRole);
}
