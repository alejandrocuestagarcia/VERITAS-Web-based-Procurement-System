package com.veritas.backend.vendor.service;

import com.veritas.backend.vendor.dto.VendorDto;

public interface VendorService {
    VendorDto createVendor(VendorDto vendorCreateDto);
}
