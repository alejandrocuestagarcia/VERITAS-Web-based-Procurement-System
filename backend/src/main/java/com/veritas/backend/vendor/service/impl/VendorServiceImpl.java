package com.veritas.backend.vendor.service.impl;

import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorStatsDto;
import com.veritas.backend.vendor.mapper.VendorMapper;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
