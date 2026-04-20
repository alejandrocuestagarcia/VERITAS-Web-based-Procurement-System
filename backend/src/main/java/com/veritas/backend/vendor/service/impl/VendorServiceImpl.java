package com.veritas.backend.vendor.service.impl;

import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {
    private final VendorRepository vendorRepository;

    @Override
    public VendorDto createVendor(VendorDto vendorDto) {

        Vendor vendor = new Vendor();
        vendor.setTaxId(vendorDto.taxId());
        vendor.setVendorName(vendorDto.vendorName());
        vendor.setDescription(vendorDto.description());
        vendor.setPrimaryContactEmail(vendorDto.primaryContactEmail());
        vendor.setPrimaryContactName(vendorDto.primaryContactName());

        return convertVendorToVendorDto(vendorRepository.save(vendor));
    }

    private VendorDto convertVendorToVendorDto(Vendor vendor) {
        return new VendorDto(vendor.getVendorName(), vendor.getTaxId(), null, null, null,
                null, vendor.getDescription(), vendor.getPrimaryContactName(), vendor.getPrimaryContactEmail());
    }
}
