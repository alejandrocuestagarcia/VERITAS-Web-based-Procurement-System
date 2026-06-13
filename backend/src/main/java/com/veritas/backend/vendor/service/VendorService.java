package com.veritas.backend.vendor.service;

import com.veritas.backend.user.entity.User;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorRatingDto;
import com.veritas.backend.vendor.dto.VendorStatsDto;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface VendorService {

    /**
     * Creates a new vendor.
     *
     * @param vendorCreateDto the vendor creation payload
     * @return the created {@link VendorDto}
     */
    VendorDto createVendor(VendorDto vendorCreateDto);

    /**
     * Updates non-null fields of an existing vendor. Tax ID uniqueness is enforced on change.
     *
     * @param id the ID of the vendor to edit
     * @param edits the edit payload; only non-blank fields are applied
     * @return the updated {@link VendorDto}
     * @throws EntityNotFoundException if no vendor exists with the given ID
     * @throws EntityExistsException if the updated tax ID is already in use by another vendor
     */
    VendorDto editVendor(Long id, VendorEditDto edits);

    /**
     * Returns a paginated list of vendors filtered by an optional search string and minimum rating.
     *
     * @param pageable pagination and sorting parameters
     * @param search optional search string
     * @param minimumRating optional minimum overall rating (0.0–10.0); defaults to 0.0 if null
     * @return a page of matching {@link VendorDto}
     * @throws IllegalArgumentException if the rating is outside the 0.0–10.0 range
     */
    Page<VendorDto> findVendorsByStringAndRating(Pageable pageable, String search, Double minimumRating);

    /**
     * Returns the total vendor count and average overall rating across all vendors.
     *
     * @return a {@link VendorStatsDto} with aggregate stats
     */
    VendorStatsDto getVendorStats();

    /**
     * Returns a single vendor by ID.
     *
     * @param id the vendor ID
     * @return the matching {@link VendorDto}
     * @throws IllegalArgumentException if no vendor exists with the given ID
     */
    VendorDto getVendorById(Long id);

    /**
     * Submits an evaluation for a vendor on a specific request.
     * Each vendor may only be evaluated once per request.
     *
     * @param vendorId the ID of the vendor to evaluate
     * @param requestId the ID of the request the evaluation relates to
     * @param ratingData the evaluation scores and notes
     * @param evaluator the user submitting the evaluation
     * @return the updated {@link VendorDto} reflecting the new evaluation
     * @throws EntityNotFoundException if the vendor or request is not found
     * @throws EntityExistsException if the vendor has already been evaluated for this request
     */
    VendorDto rateVendor(Long vendorId, Long requestId, VendorRatingDto ratingData, User evaluator);
}
