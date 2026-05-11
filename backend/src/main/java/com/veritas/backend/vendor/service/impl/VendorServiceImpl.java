package com.veritas.backend.vendor.service.impl;

import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorStatsDto;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.mapper.VendorMapper;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.service.VendorService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {
    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    @Override
    public VendorDto createVendor(VendorDto vendorDto) {
        return vendorMapper.toVendorDto(vendorRepository.save(vendorMapper.toVendor(vendorDto)));
    }

    @Override
    public VendorDto editVendor(Long id, VendorEditDto edits) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + id));

        if (StringUtils.hasText(edits.vendorName())) {
            vendor.setVendorName(edits.vendorName().trim());
        }

        if (StringUtils.hasText(edits.taxId())) {
            String updatedTaxId = edits.taxId().trim();
            if (!updatedTaxId.equals(vendor.getTaxId())) {
                vendorRepository.findByTaxId(updatedTaxId)
                        .filter(existing -> !existing.getId().equals(vendor.getId()))
                        .ifPresent(existing -> {
                            throw new EntityExistsException(
                                    "Vendor with tax ID '" + updatedTaxId + "' already exists");
                        });
            }
            vendor.setTaxId(updatedTaxId);
        }

        if (StringUtils.hasText(edits.description())) {
            vendor.setDescription(edits.description().trim());
        }

        if (StringUtils.hasText(edits.primaryContactName())) {
            vendor.setPrimaryContactName(edits.primaryContactName().trim());
        }

        if (StringUtils.hasText(edits.primaryContactEmail())) {
            vendor.setPrimaryContactEmail(edits.primaryContactEmail().trim());
        }

        Vendor saved = vendorRepository.save(vendor);
        return vendorMapper.toVendorDto(saved);
    }

    @Override
    public Page<VendorDto> findVendorsByStringAndRating(Pageable pageable, String search, Double minimumRating) {
        double rating = minimumRating != null ? minimumRating : 0.0;
        if (rating < 0.0 || rating > 10.0) {
            throw new IllegalArgumentException("Rating must be between 0.0 and 10.0");
        }
        return this.vendorRepository.findByRating(search, rating, pageable).map(vendorMapper::toVendorDto);
    }

    @Override
    public VendorStatsDto getVendorStats() {
        long total = vendorRepository.count();
        Double averageRating = vendorRepository.getAverageOverallScore();

        return new VendorStatsDto(total, averageRating != null ? averageRating : 0.0);
    }

    @Override
    public VendorDto getVendorById(Long id) {
        return vendorRepository.findById(id)
                .map(vendorMapper::toVendorDto)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + id));
    }
}
