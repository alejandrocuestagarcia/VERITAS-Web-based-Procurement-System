package com.veritas.backend.util;

import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class InvoiceFactory {

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private VendorRepository vendorRepository;

    public Vendor createVendor(String name) {
        Vendor vendor = new Vendor();
        vendor.setVendorName(name);
        vendor.setPrimaryContactEmail("contact@" + name.toLowerCase().replace(" ", "") + ".com");
        vendor.setTaxId("TAX-" + System.currentTimeMillis() + "-" + Math.abs(name.hashCode()));
        vendor.setDescription("Description for " + name);
        return vendorRepository.save(vendor);
    }

    public Vendor getOrCreateDefaultVendor() {
        return vendorRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> createVendor("Default Vendor"));
    }

    public Invoice createInvoice(Request request, Vendor vendor, BigDecimal amount) {
        if (vendor == null) {
            vendor = getOrCreateDefaultVendor();
        }
        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setVendor(vendor);
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setTotalAmount(amount);
        invoice.setCurrency(Currency.EUR);
        invoice.setIsPaid(false);
        return invoiceRepository.save(invoice);
    }
}
