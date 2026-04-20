package com.veritas.backend.vendor.controller;

import com.veritas.backend.config.annotations.IsProcurementOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorRatingDto;
import com.veritas.backend.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendor Module", description = "Vendor management, ratings, and quote comparisons")
public class VendorController {
    private final VendorService vendorService;

    @Operation(summary = "List vendors", description = "Retrieves all vendors including their reliability scores and basic info.")
    @IsRequester
    @GetMapping
    public List<VendorDto> getAllVendors() {
        return List.of();
    }

    @Operation(summary = "Get vendor", description = "Retrieves a vendor including their reliability score and basic info.")
    @IsRequester
    @GetMapping("/{id}")
    public VendorDto getVendor(@PathVariable Long id) {
        return new VendorDto(null, null, null, null, null, null, null, null, null, null, null);
    }

    @Operation(summary = "Create vendor", description = "Creates a new vendor.")
    @IsProcurementOfficer
    @PostMapping()
    public VendorDto createVendor(@RequestBody VendorDto create) {
        return vendorService.createVendor(create);
    }

    @Operation(summary = "Edit vendor", description = "Edits a vendors basic info.")
    @IsProcurementOfficer
    @PatchMapping("/{id}")
    public VendorDto editVendor(@PathVariable Long id, @RequestBody VendorDto edits) {
        return new VendorDto(null, null, null, null, null, null, null, null, null, null, null);
    }

    @Operation(summary = "Rate a vendor", description = "Saves communication, delivery, and quality scores for a specific vendor.")
    @IsProcurementOfficer
    @PostMapping("/{id}/rate")
    public VendorDto rateVendor(@PathVariable Long id, @RequestBody VendorRatingDto ratingData) {
        return new VendorDto(null, null, null, null, null, null, null, null, null, null, null);
    }
}