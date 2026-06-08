package com.veritas.backend.notification.mapper;

import com.veritas.backend.notification.dto.NotificationDto;
import com.veritas.backend.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "id", source = "notificationId")
    @Mapping(target = "type", expression = "java(notification.getType().name())")
    @Mapping(target = "requestId", source = "request.requestID")
    @Mapping(target = "requestKey", source = "request.requestKey")
    @Mapping(target = "requestName", source = "request.requestName")
    NotificationDto toDto(Notification notification);
}
