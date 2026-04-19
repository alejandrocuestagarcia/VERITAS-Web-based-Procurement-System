package com.veritas.backend.project.service;

import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.user.entity.User;

import java.util.List;

public interface ProjectService {
    List<ProjectDto> getProjectsForUser(User user);
}
