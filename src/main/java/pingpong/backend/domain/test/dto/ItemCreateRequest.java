package pingpong.backend.domain.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 등록 요청")
public record ItemCreateRequest(
	@Schema(description = "상품명", example = "무선 키보드") String name,
	@Schema(description = "상품 설명", example = "저소음 무선 기계식 키보드") String description,
	@Schema(description = "가격(원)", example = "89000") Long price,
	@Schema(description = "카테고리", example = "전자기기") String category
) {}
