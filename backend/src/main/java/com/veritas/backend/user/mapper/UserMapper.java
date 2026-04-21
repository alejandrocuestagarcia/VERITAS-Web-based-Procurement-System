package com.veritas.backend.user.mapper;

import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "passwordHash", ignore = true)
  @Mapping(target = "team", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "isActive", ignore = true)
  @Mapping(source = "userRole", target = "role")
  User toUser(UserCreationRequestDto userCreationRequestDto);


  @Mapping(source = "team.name", target = "teamName")
  UserDto toUserDto(User user);

}
