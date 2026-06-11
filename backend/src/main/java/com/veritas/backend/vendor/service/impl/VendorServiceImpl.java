package com.veritas.backend.vendor.service.impl;

import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import java.math.BigDecimal;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorRatingDto;
import com.veritas.backend.vendor.dto.VendorStatsDto;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.entity.VendorEvaluation;
import com.veritas.backend.vendor.mapper.VendorMapper;
import com.veritas.backend.vendor.repository.VendorEvaluationRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.service.VendorService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;
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
    private final VendorEvaluationRepository vendorEvaluationRepository;
    private final RequestRepository requestRepository;
    private final EntityManager entityManager;

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

        if (averageRating != null) {
            averageRating = Math.round(averageRating * 100.0) / 100.0;
        } else {
            averageRating = 0.0;
        }

        return new VendorStatsDto(total, averageRating);
    }

    @Override
    public VendorDto getVendorById(Long id) {
        return vendorRepository.findById(id)
                .map(vendorMapper::toVendorDto)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + id));
    }

    @Override
    public VendorDto rateVendor(Long vendorId, Long requestId, VendorRatingDto ratingData, User evaluator) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + vendorId));

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));

        if (vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(vendorId, requestId)) {
            throw new EntityExistsException("Vendor has already been evaluated for this request");
        }

        VendorEvaluation evaluation = new VendorEvaluation();
        evaluation.setVendor(vendor);
        evaluation.setRequest(request);
        evaluation.setEvaluator(evaluator);
        evaluation.setCommunicationScore(ratingData.communicationScore());
        evaluation.setDeliveryScore(ratingData.deliveryScore());
        evaluation.setQualityScore(ratingData.qualityScore());
        evaluation.setGapScore(calculateGapScore(request));
        evaluation.setNotes(ratingData.notes());

        vendorEvaluationRepository.save(evaluation);
        entityManager.flush();
        entityManager.refresh(vendor);

        return vendorMapper.toVendorDto(vendor);
    }

    private Double calculateGapScore(Request request) {
        if (request == null) {
            return 10.0;
        }
        BigDecimal quoteTotal = request.getSelectedQuoteTotalAmount();
        Invoice invoice = request.getInvoice();
        if (quoteTotal == null || invoice == null || invoice.getTotalAmount() == null) {
            return 10.0;
        }

        double quoteVal = quoteTotal.doubleValue();
        double invoiceVal = invoice.getTotalAmount().doubleValue();

        if (quoteVal <= 0) {
            return 10.0;
        }

        double deviation = (invoiceVal - quoteVal) / quoteVal;
        if (deviation <= 0) {
            return 10.0;
        }

        double score = 10.0 - (deviation * 10.0);
        return Math.max(0.0, Math.min(10.0, score));
    }
}
