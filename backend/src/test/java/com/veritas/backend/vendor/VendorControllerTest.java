package com.veritas.backend.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VendorControllerTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VendorService vendorService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void createVendor_AsProcurementOfficer_ShouldReturnCreated() throws Exception {
        VendorDto inputDto = new VendorDto(
                "Controller Test Vendor",
                "TAX-CTRL-789",
                null, null, null, null,
                "Controller test description",
                "Alice",
                "alice@example.com"
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
    void createVendorAsRequesterShouldReturnForbidden() throws Exception {
        VendorDto inputDto = new VendorDto(
                "Forbidden Vendor",
                "TAX-FORBIDDEN",
                null, null, null, null,
                "Should fail",
                "Bob",
                "bob@example.com"
        );

        mockMvc.perform(post("/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void createVendorWithInvalidDataShouldReturnBadRequest() throws Exception {
        VendorDto invalidDto = new VendorDto(
                "",
                "",
                null, null, null, null,
                "",
                "Invalid",
                "not-an-email"
        );

        mockMvc.perform(post("/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
}
