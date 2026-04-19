package com.veritas.backend.user.service;

import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;

public interface UserService {
  UserDto createUser(UserCreationRequestDto userDto);
}
