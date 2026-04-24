package com.veritas.backend.vendor.mapper;

import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface VendorMapper {
    Vendor toVendor(VendorDto vendorDto);


    VendorDto toVendorDto(Vendor vendor);

}
