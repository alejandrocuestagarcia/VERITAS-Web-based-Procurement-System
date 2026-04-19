package com.veritas.backend.user.service.impl;

import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.mapper.UserMapper;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.user.service.UserService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  private final TeamRepository teamRepository;
  private final UserMapper userMapper;


  @Override
  public UserDto createUser(UserCreationRequestDto userDto) {

    if (userRepository.existsByEmail(userDto.email())) {
      throw new EntityExistsException("Email already registered");
    }

    Team team = teamRepository.findById(userDto.teamId()).orElseThrow(
        () -> new EntityNotFoundException("Team with id " + userDto.teamId() + " not found"));

    User user = userMapper.toUser(userDto);
    user.setPasswordHash(passwordEncoder.encode(userDto.password()));
    user.setTeam(team);
    user.setIsActive(true);

    User savedUser = userRepository.save(user);
    return userMapper.toUserDto(savedUser);


  }
}
