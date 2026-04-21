package com.veritas.backend.user.mapper;

import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserMapper {

  User toUser(UserCreationRequestDto userCreationRequestDto);


  @Mapping(source = "team.name", target = "teamName")
  UserDto toUserDto(User user);

}
