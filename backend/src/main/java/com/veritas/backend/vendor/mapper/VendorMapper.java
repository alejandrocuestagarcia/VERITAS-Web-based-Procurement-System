package com.veritas.backend.vendor.mapper;

import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface VendorMapper {
    Vendor toVendor(VendorDto vendorDto);

    @Mapping(target = "communicationScore", expression = "java(round(vendor.getCommunicationScore()))")
    @Mapping(target = "qualityScore", expression = "java(round(vendor.getQualityScore()))")
    @Mapping(target = "deliveryScore", expression = "java(round(vendor.getDeliveryScore()))")
    @Mapping(target = "overallScore", expression = "java(round(vendor.getOverallScore()))")
    VendorDto toVendorDto(Vendor vendor);

    default Double round(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
