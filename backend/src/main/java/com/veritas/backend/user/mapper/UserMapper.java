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

  @Mapping(source = "isActive", target = "active")
  @Mapping(source = "team.name", target = "teamName")
  @Mapping(target = "departmentName", expression = "java(user.getDepartment() != null ? user.getDepartment().getName() : (user.getTeam() != null && user.getTeam().getDepartment() != null ? user.getTeam().getDepartment().getName() : null))")
  UserDto toUserDto(User user);

  @Mapping(source = "user.team.teamId", target = "teamId")
  @Mapping(source = "user.department.departmentId", target = "departmentId")
  @Mapping(source = "isTeamLeader", target = "isTeamLeader")
  UserEditDto toUserEditDto(User user, boolean isTeamLeader);
}
