package com.veritas.backend.vendor.service;

import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorStatsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface VendorService {
    VendorDto createVendor(VendorDto vendorCreateDto);

    VendorDto editVendor(Long id, VendorEditDto edits);

    Page<VendorDto> findVendorsByStringAndRating(Pageable pageable, String search, Double minimumRating);

    VendorStatsDto getVendorStats();

    VendorDto getVendorById(Long id);
}
