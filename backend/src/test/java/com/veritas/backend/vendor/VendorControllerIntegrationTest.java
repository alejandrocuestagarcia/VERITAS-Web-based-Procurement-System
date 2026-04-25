package com.veritas.backend.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VendorControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VendorService vendorService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void VendorCreation_AsProcurementOfficer_ReturnsCreated() throws Exception {
        VendorDto inputDto = new VendorDto(
                1L,
                "Controller Test Vendor",
                "TAX-CTRL-789",
                null, null, null, null,
                "Controller test description",
                "Alice",
                "alice@example.com",
                null, null, null
        );

        when(vendorService.createVendor(any(VendorDto.class))).thenReturn(inputDto);

        mockMvc.perform(post("/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorName").value(inputDto.vendorName()))
                .andExpect(jsonPath("$.taxId").value(inputDto.taxId()));
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void VendorCreation_AsRequester_ReturnsForbidden() throws Exception {
        VendorDto inputDto = new VendorDto(
                1L,
                "Forbidden Vendor",
                "TAX-FORBIDDEN",
                null, null, null, null,
                "Should fail",
                "Bob",
                "bob@example.com",
                null, null, null
        );

        mockMvc.perform(post("/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void VendorCreation_InvalidData_ReturnsBadRequest() throws Exception {
        VendorDto invalidDto = new VendorDto(
                1L,
                "",
                "",
                null, null, null, null,
                "",
                "Invalid",
                "not-an-email",
                null, null, null
        );

        mockMvc.perform(post("/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void VendorCreation_AsProcurementOfficer_ShouldReturnCreated() throws Exception {
        VendorDto inputDto = new VendorDto(
                1L,
                "Controller Test Vendor",
                "TAX-CTRL-789",
                null, null, null, null,
                "Controller test description",
                "Alice",
                "alice@example.com",
                null, null, null
        );

        when(vendorService.createVendor(any(VendorDto.class))).thenReturn(inputDto);

        mockMvc.perform(post("/vendors")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorName").value(inputDto.vendorName()))
                .andExpect(jsonPath("$.taxId").value(inputDto.taxId()));
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void FindVendors_ShouldReturn_AllVendors() throws Exception {
        VendorDto vendor1 = new VendorDto(
                1L,
                "Vendor 1",
                "TAX-1",
                null, null, null, null,
                "Description 1",
                "Contact 1",
                "contact1@example.com",
                null, null, null
        );
        VendorDto vendor2 = new VendorDto(
                1L,
                "Vendor 2",
                "TAX-2",
                null, null, null, null,
                "Description 2",
                "Contact 2",
                "contact2@example.com",
                null, null, null
        );

        Page<VendorDto> page = new PageImpl<>(java.util.List.of(vendor1, vendor2));

        when(vendorService.findVendorsByStringAndRating(any(Pageable.class), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/vendors")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vendorName").value("Vendor 1"))
                .andExpect(jsonPath("$.content[1].vendorName").value("Vendor 2"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void FindVendors_WithSearchAndRating_ShouldReturnFilteredVendors() throws Exception {
        Page<VendorDto> emptyPage = new PageImpl<>(java.util.List.of());

        when(vendorService.findVendorsByStringAndRating(any(Pageable.class), any(String.class), any(Double.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/vendors")
                .param("search", "test")
                .param("minimumRating", "4.5"))
                .andExpect(status().isOk());

        verify(vendorService).findVendorsByStringAndRating(any(Pageable.class), org.mockito.ArgumentMatchers.eq("test"),
                org.mockito.ArgumentMatchers.eq(4.5));
    }
}
