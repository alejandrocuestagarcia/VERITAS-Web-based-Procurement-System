package com.veritas.backend.vendor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.dto.VendorEditDto;
import com.veritas.backend.vendor.dto.VendorRatingDto;
import com.veritas.backend.vendor.dto.VendorStatsDto;
import com.veritas.backend.vendor.service.VendorService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class VendorControllerUnitTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private VendorService vendorService;

    @InjectMocks
    private VendorController vendorController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vendorController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void getAllVendors_ValidRequest_ReturnsPage() throws Exception {
        VendorDto vendorDto = new VendorDto(
                1L, "Vendor 1", "TAX-123", 4.5, 4.2, 4.8, 0.1, 4.5,
                "Description 1", "Contact 1", "contact1@veritas.com",
                LocalDateTime.now(), LocalDateTime.now(), null
        );
        Page<VendorDto> page = new PageImpl<>(List.of(vendorDto), PageRequest.of(0, 10), 1);

        when(vendorService.findVendorsByStringAndRating(any(Pageable.class), eq("Vendor"), eq(4.0)))
                .thenReturn(page);

        mockMvc.perform(get("/vendors")
                .param("page", "0")
                .param("size", "10")
                .param("search", "Vendor")
                .param("minimumRating", "4.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].vendorName").value("Vendor 1"))
                .andExpect(jsonPath("$.content[0].taxId").value("TAX-123"))
                .andExpect(jsonPath("$.content[0].overallScore").value(4.5));

        verify(vendorService).findVendorsByStringAndRating(any(Pageable.class), eq("Vendor"), eq(4.0));
    }

    @Test
    void getVendorStats_ValidRequest_ReturnsStats() throws Exception {
        VendorStatsDto stats = new VendorStatsDto(15L, 4.2);
        when(vendorService.getVendorStats()).thenReturn(stats);

        mockMvc.perform(get("/vendors/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.averageRating").value(4.2));

        verify(vendorService).getVendorStats();
    }

    @Test
    void getVendor_ValidRequest_ReturnsDto() throws Exception {
        VendorDto vendorDto = new VendorDto(
                1L, "Vendor 1", "TAX-123", 4.5, 4.2, 4.8, 0.1, 4.5,
                "Description 1", "Contact 1", "contact1@veritas.com",
                LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(vendorService.getVendorById(1L)).thenReturn(vendorDto);

        mockMvc.perform(get("/vendors/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vendorName").value("Vendor 1"));

        verify(vendorService).getVendorById(1L);
    }

    @Test
    void createVendor_ValidRequest_ReturnsDto() throws Exception {
        VendorDto createDto = new VendorDto(
                null, "Vendor New", "TAX-999", null, null, null, null, null,
                "Description New", "Contact New", "new@veritas.com",
                null, null, null
        );
        VendorDto savedDto = new VendorDto(
                2L, "Vendor New", "TAX-999", 0.0, 0.0, 0.0, 0.0, 0.0,
                "Description New", "Contact New", "new@veritas.com",
                LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(vendorService.createVendor(any(VendorDto.class))).thenReturn(savedDto);

        mockMvc.perform(post("/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.vendorName").value("Vendor New"));

        verify(vendorService).createVendor(any(VendorDto.class));
    }

    @Test
    void createVendor_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Blank vendorName, taxId, description to trigger validation error
        VendorDto invalidDto = new VendorDto(
                null, "", "", null, null, null, null, null,
                "", "Contact New", "invalid-email-format",
                null, null, null
        );

        mockMvc.perform(post("/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(vendorService, never()).createVendor(any());
    }

    @Test
    void editVendor_ValidRequest_ReturnsDto() throws Exception {
        VendorEditDto edits = new VendorEditDto(
                "Vendor Edited", "TAX-EDITED", "Description Edited", "Contact Edited", "edited@veritas.com"
        );
        VendorDto updatedDto = new VendorDto(
                1L, "Vendor Edited", "TAX-EDITED", 4.5, 4.2, 4.8, 0.1, 4.5,
                "Description Edited", "Contact Edited", "edited@veritas.com",
                LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(vendorService.editVendor(eq(1L), any(VendorEditDto.class))).thenReturn(updatedDto);

        mockMvc.perform(patch("/vendors/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(edits)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorName").value("Vendor Edited"))
                .andExpect(jsonPath("$.taxId").value("TAX-EDITED"));

        verify(vendorService).editVendor(eq(1L), any(VendorEditDto.class));
    }

    @Test
    void editVendor_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Email is invalid
        VendorEditDto edits = new VendorEditDto(
                "Vendor Edited", "TAX-EDITED", "Description Edited", "Contact Edited", "not-an-email"
        );

        mockMvc.perform(patch("/vendors/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(edits)))
                .andExpect(status().isBadRequest());

        verify(vendorService, never()).editVendor(anyLong(), any());
    }

    @Test
    void rateVendor_ValidRequest_ReturnsDto() throws Exception {
        VendorRatingDto ratingData = new VendorRatingDto(8, 9, 7, "Good performance");
        VendorDto ratedDto = new VendorDto(
                1L, "Vendor 1", "TAX-123", 8.0, 9.0, 7.0, 0.1, 8.0,
                "Description 1", "Contact 1", "contact1@veritas.com",
                LocalDateTime.now(), LocalDateTime.now(), null
        );

        // Standalone setup passes null for @AuthenticationPrincipal User by default unless resolved
        when(vendorService.rateVendor(eq(1L), eq(100L), any(VendorRatingDto.class), any())).thenReturn(ratedDto);

        mockMvc.perform(post("/vendors/{id}/rate", 1L)
                .param("requestId", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ratingData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(8.0));

        verify(vendorService).rateVendor(eq(1L), eq(100L), any(VendorRatingDto.class), any());
    }

    @Test
    void rateVendor_InvalidRatingValue_ReturnsBadRequest() throws Exception {
        // Communication score is 11 (max is 10)
        VendorRatingDto ratingData = new VendorRatingDto(11, 9, 7, "Invalid score");

        mockMvc.perform(post("/vendors/{id}/rate", 1L)
                .param("requestId", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ratingData)))
                .andExpect(status().isBadRequest());

        verify(vendorService, never()).rateVendor(anyLong(), anyLong(), any(), any());
    }
}
