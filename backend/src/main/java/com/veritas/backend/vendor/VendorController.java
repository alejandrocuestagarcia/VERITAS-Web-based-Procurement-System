package com.veritas.backend.vendor;

import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorRatingDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendors")
@Tag(name = "Vendor Module", description = "Vendor management, ratings, and quote comparisons")
public class VendorController {

    @Operation(summary = "List vendors", description = "Retrieves all vendors including their reliability scores and basic info.")
    @GetMapping
    public List<VendorDto> getAllVendors() {
        return List.of();
    }

    @Operation(summary = "Get vendor", description = "Retrieves a vendor including their reliability score and basic info.")
    @GetMapping("/{id}")
    public VendorDto getVendor(@PathVariable Long id) {
        return new VendorDto();
    }

    @Operation(summary = "Edit vendor", description = "Edits a vendors basic info.")
    @PatchMapping("/{id}")
    public VendorDto editVendor(@PathVariable Long id, @RequestBody VendorEditDto edits) {
        return new VendorDto();
    }

    @Operation(summary = "Rate a vendor", description = "Saves communication, delivery, and quality scores for a specific vendor.")
    @PostMapping("/{id}/rate")
    public VendorDto rateVendor(@PathVariable Long id, @RequestBody VendorRatingDto ratingData) {
        return new VendorDto();
    }
}