package com.veritas.backend.user.service;

import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
  UserDto createUser(UserCreationRequestDto userDto);


  Page<UserDto> getAllUsers(Pageable pageable);

}
