package com.veritas.backend.user.mapper;

import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserMapper {

  User toUser(UserCreationRequestDto userCreationRequestDto);

  @Mapping(source = "team.name", target = "teamName")
  UserDto toUserDto(User user);

  @Mapping(source = "user.team.teamId", target = "teamId")
  @Mapping(source = "isTeamLeader", target = "isTeamLeader")
  UserEditDto toUserEditDto(User user, boolean isTeamLeader);
}
