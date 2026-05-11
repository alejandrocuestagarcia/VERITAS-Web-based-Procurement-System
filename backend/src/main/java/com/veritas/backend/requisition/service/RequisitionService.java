package com.veritas.backend.requisition.service;

import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface RequisitionService {
    RequisitionDto createRequest(RequisitionCreateDto createDto, User user);
    void saveAttachment(Long requestId, MultipartFile file);
}
