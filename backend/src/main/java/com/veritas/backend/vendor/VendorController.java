package com.veritas.backend.vendor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vendors")
@Tag(name = "Vendor Module", description = "Vendor management, ratings, and quote comparisons")
public class VendorController {

    @Operation(summary = "List vendors", description = "Retrieves all vendors including their reliability scores and basic info.")
    @GetMapping
    public List<Object> getAllVendors() {
        return List.of();
    }

    @Operation(summary = "Get vendor", description = "Retrieves a vendor including their reliability score and basic info.")
    @GetMapping("/{id}")
    public List<Object> getVendor(@PathVariable Long id) {
        return List.of();
    }

    @Operation(summary = "Edit vendor", description = "Edits a vendors basic info.")
    @PatchMapping("/{id}")
    public String editVendor(@PathVariable Long id) {
        return "Vendor " + id + " edited";
    }

    @Operation(summary = "Rate a vendor", description = "Saves communication, delivery, and quality scores for a specific vendor.")
    @PostMapping("/{id}/rate")
    public String rateVendor(@PathVariable Long id, @RequestBody Map<String, Object> ratingData) {
        return "Rating saved for vendor " + id;
    }
}