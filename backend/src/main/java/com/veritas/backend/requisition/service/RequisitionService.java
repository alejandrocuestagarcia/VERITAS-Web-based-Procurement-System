package com.veritas.backend.requisition.service;

import com.veritas.backend.requisition.dto.InvoiceCreateDto;
import com.veritas.backend.requisition.dto.InvoiceDto;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.dto.RequisitionRejectDto;
import com.veritas.backend.requisition.dto.RequisitionUpdateDto;
import com.veritas.backend.user.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface RequisitionService {
    RequisitionDto createRequest(RequisitionCreateDto createDto, User user);
    void saveAttachment(Long requestId, MultipartFile file);
    Page<RequisitionDto> getRequests(String status, String search, Long projectId, User authUser, Pageable pageable);
    RequisitionDto getRequestById(Long id);
    ResponseEntity<Resource> downloadAttachment(Long attachmentId);
    RequisitionDto approveRequest(Long id, User actor);
    RequisitionDto rejectRequest(Long id, User actor, RequisitionRejectDto rejectionData);
    RequisitionDto submitRequest(Long id, User actor);
    RequisitionDto changeRequester(Long id, Long newRequesterId);
    RequisitionDto updateRequest(Long id, RequisitionUpdateDto updates, User actor);

    void processPayment(Long requestId);

    InvoiceDto createInvoice(Long requestId, InvoiceCreateDto createDto, MultipartFile file);

    InvoiceDto getInvoice(Long requestId);
}
