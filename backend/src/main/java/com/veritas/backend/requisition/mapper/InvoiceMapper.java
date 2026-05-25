package com.veritas.backend.requisition.mapper;

import com.veritas.backend.requisition.dto.InvoiceDto;
import com.veritas.backend.requisition.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "requestId", source = "request.requestID")
    @Mapping(target = "vendorId", source = "vendor.id")
    @Mapping(target = "vendorName", source = "vendor.vendorName")
    InvoiceDto toDto(Invoice invoice);
}
