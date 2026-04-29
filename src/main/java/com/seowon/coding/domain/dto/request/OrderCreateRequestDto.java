package com.seowon.coding.domain.dto.request;

import java.util.List;

public record OrderCreateRequestDto(
	String customerName,
	String customerEmail,
	List<ProductRequestDto> products
) {
	public record ProductRequestDto(
		Long productId,
		int quantity
	){
	}
}
