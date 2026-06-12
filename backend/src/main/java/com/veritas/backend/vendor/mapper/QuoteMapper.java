package com.veritas.backend.vendor.mapper;

import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.QuoteLineItemDto;
import com.veritas.backend.vendor.dto.VendorDto;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.QuoteLineItem;
import com.veritas.backend.vendor.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface QuoteMapper {

    @Mapping(target = "quoteId", source = "quote.quoteID")
    @Mapping(target = "vendorId", source = "quote.vendorID.id")
    @Mapping(target = "vendor", source = "quote.vendorID")
    @Mapping(target = "currency", source = "quote.currency")
    @Mapping(target = "baseAmount", source = "quote.baseAmount")
    @Mapping(target = "shippingCosts", source = "quote.shippingCosts")
    @Mapping(target = "totalAmount", source = "quote.totalAmount")
    @Mapping(target = "totalAmountEuro", source = "totalAmountEuro")
    @Mapping(target = "isSelected", source = "quote.selected")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "exchangeRateFetchedAt", source = "exchangeRateFetchedAt")
    @Mapping(target = "exchangeRateSource", source = "exchangeRateSource")
    QuoteDto toDto(Quote quote, List<QuoteLineItem> items, BigDecimal totalAmountEuro, LocalDateTime exchangeRateFetchedAt, ExchangeRateSource exchangeRateSource);

    @Mapping(target = "lineItemId", source = "lineItemId")
    @Mapping(target = "productDescription", source = "productDescription")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "subtotal", source = "subtotal")
    @Mapping(target = "requestItemId", source = "requestItem.id")
    QuoteLineItemDto toLineItemDto(QuoteLineItem item);

    VendorDto toVendorDto(Vendor vendor);
}
