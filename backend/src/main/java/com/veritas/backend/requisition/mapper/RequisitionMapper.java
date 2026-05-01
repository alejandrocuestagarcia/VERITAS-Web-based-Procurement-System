package com.veritas.backend.requisition.mapper;

import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.entity.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequisitionMapper {

    @Mapping(target = "id", source = "requestID")
    @Mapping(target = "projectName", source = "projectID.name")
    @Mapping(target = "teamName", source = "teamID.name")
    @Mapping(target = "requesterName", source = "userID.name")
    @Mapping(target = "status", expression = "java(request.getCurrentStepID() != null ? request.getCurrentStepID().getStepName() : \"DRAFT\")")
    @Mapping(target = "requestName", source = "requestName")
    @Mapping(target = "priority", source = "priority")
    RequisitionDto toDto(Request request);
}
