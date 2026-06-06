package com.veritas.backend.requisition.mapper;

import com.veritas.backend.requisition.dto.InvoiceDto;
import com.veritas.backend.requisition.entity.Invoice;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "requestId", source = "invoice.request.requestID")
    @Mapping(target = "vendorId", source = "invoice.vendor.id")
    @Mapping(target = "vendorName", source = "invoice.vendor.vendorName")
    @Mapping(target = "totalAmountEuro", source = "totalAmountEuro")
    InvoiceDto toDto(Invoice invoice, BigDecimal totalAmountEuro);
}
