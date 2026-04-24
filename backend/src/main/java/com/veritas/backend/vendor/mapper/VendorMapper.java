package com.veritas.backend.vendor.mapper;

import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface VendorMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(source = "userRole", target = "role")
    Vendor toVendor(VendorDto vendorDto);


    @Mapping(source = "team.name", target = "teamName")
    VendorDto toVendorDto(Vendor vendor);

}
