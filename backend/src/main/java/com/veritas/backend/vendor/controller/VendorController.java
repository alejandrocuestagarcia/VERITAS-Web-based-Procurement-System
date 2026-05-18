package com.veritas.backend.vendor.controller;

import com.veritas.backend.config.annotations.IsProcurementOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorRatingDto;
import com.veritas.backend.vendor.dto.VendorStatsDto;
import com.veritas.backend.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendor Module", description = "Vendor management, ratings, and quote comparisons")
public class VendorController {
    private final VendorService vendorService;

    @Operation(summary = "List vendors", description = "Retrieves all vendors including their reliability scores and basic info.")
    @IsRequester
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<VendorDto>> getAllVendors(Pageable pageable,
                                                         @RequestParam(required = false) String search,
                                                         @RequestParam(defaultValue = "0.0") Double minimumRating) {
        return ResponseEntity.ok(this.vendorService.findVendorsByStringAndRating(pageable, search, minimumRating));
    }

    @Operation(summary = "Get vendor stats", description = "Retrieves stats about the vendors of Veritas")
    @GetMapping(path = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VendorStatsDto> getVendorStats() {
        return ResponseEntity.ok(vendorService.getVendorStats());
    }

    @Operation(summary = "Get vendor", description = "Retrieves a vendor including their reliability score and basic info.")
    @IsRequester
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VendorDto> getVendor(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    @Operation(summary = "Create vendor", description = "Creates a new vendor.")
    @IsProcurementOfficer
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VendorDto> createVendor(@Valid @RequestBody VendorDto create) {
        return ResponseEntity.ok(vendorService.createVendor(create));
    }

    @Operation(summary = "Edit vendor", description = "Edits a vendors basic info.")
    @IsProcurementOfficer
    @PatchMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VendorDto> editVendor(@PathVariable Long id, @Valid @RequestBody VendorEditDto edits) {
        return ResponseEntity.ok(vendorService.editVendor(id, edits));
    }

    @Operation(summary = "Rate a vendor", description = "Saves communication, delivery, and quality scores for a specific vendor, scoped to a specific request. One evaluation per request is allowed.")
    @IsProcurementOfficer
    @PostMapping(path = "/{id}/rate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VendorDto> rateVendor(
            @PathVariable Long id,
            @RequestParam Long requestId,
            @Valid @RequestBody VendorRatingDto ratingData,
            @AuthenticationPrincipal User evaluator) {
        return ResponseEntity.ok(vendorService.rateVendor(id, requestId, ratingData, evaluator));
    }
}