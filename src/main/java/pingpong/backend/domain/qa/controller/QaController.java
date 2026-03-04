package pingpong.backend.domain.qa.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import pingpong.backend.domain.qa.dto.QaCaseResponse;
import pingpong.backend.domain.qa.service.QaService;
import pingpong.backend.global.response.result.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/qa")
@Tag(name = "QA API", description = "Endpoint에 대한 QA 케이스를 조회하는 API입니다.")
public class QaController {

	private final QaService qaService;

	@GetMapping
	@Operation(
		summary = "Endpoint별 QA 목록 조회",
		description = "endpointId에 해당하는 QA 케이스 목록을 반환합니다."
	)
	public SuccessResponse<List<QaCaseResponse>> getQaCases(@RequestParam Long endpointId) {
		return SuccessResponse.ok(qaService.getQaCasesByEndpointId(endpointId));
	}
}
