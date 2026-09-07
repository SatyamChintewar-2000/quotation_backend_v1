package com.satyam.quotation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.satyam.quotation.dto.QuotationItemDTO;
import com.satyam.quotation.dto.QuotationItemRequestDTO;
import com.satyam.quotation.model.QuotationItem;

@Mapper(componentModel = "spring")
public interface QuotationItemMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.productName", target = "productName")
    @Mapping(source = "product.imagePath", target = "imagePath")
    @Mapping(source = "imagePathSnapshot", target = "imagePathSnapshot")
    @Mapping(source = "productNameSnapshot", target = "productNameSnapshot")
    @Mapping(source = "productDescriptionSnapshot", target = "productDescriptionSnapshot")
    @Mapping(source = "productDescription", target = "productDescription")
    @Mapping(source = "unitPrice", target = "unitPrice")
    @Mapping(source = "discountPercentage", target = "discountPercentage")
    @Mapping(source = "discountAmount", target = "discountAmount")
    @Mapping(source = "taxPercentage", target = "taxPercentage")
    @Mapping(source = "taxAmount", target = "taxAmount")
    @Mapping(source = "itemTotal", target = "itemTotal")
    @Mapping(source = "netWeightSnapshot", target = "netWeightSnapshot")
    @Mapping(source = "stackWeightSnapshot", target = "stackWeightSnapshot")
    @Mapping(source = "cbmSnapshot", target = "cbmSnapshot")
    @Mapping(source = "product.purchasePriceUsd", target = "purchasePriceUsd")
    @Mapping(source = "product.hsnSacCode", target = "hsnSacCode")
    QuotationItemDTO toDto(QuotationItem item);

    @Mapping(source = "productId", target = "product.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quotation", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "productDescription", ignore = true)
    @Mapping(target = "productNameSnapshot", ignore = true)
    @Mapping(target = "productDescriptionSnapshot", ignore = true)
    @Mapping(target = "unitSnapshot", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "itemTotal", ignore = true)
    @Mapping(target = "imagePathSnapshot", ignore = true)
    @Mapping(target = "itemDiscountPercentage", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "netWeightSnapshot", ignore = true)
    @Mapping(target = "stackWeightSnapshot", ignore = true)
    @Mapping(target = "cbmSnapshot", ignore = true)
    QuotationItem toEntity(QuotationItemRequestDTO requestDTO);
}
