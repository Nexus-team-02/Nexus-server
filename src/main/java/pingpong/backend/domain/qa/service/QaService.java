package pingpong.backend.domain.qa.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import pingpong.backend.domain.qa.QaErrorCode;
import pingpong.backend.domain.qa.dto.QaCaseResponse;
import pingpong.backend.domain.qa.repository.QaCaseRepository;
import pingpong.backend.global.exception.CustomException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QaService {

	private final QaCaseRepository qaCaseRepository;
	private final ObjectMapper objectMapper;

	public List<QaCaseResponse> getQaCasesByEndpointId(Long endpointId) {
		return qaCaseRepository.findAllByEndpointId(endpointId).stream()
			.map(qa -> new QaCaseResponse(
				qa.getId(),
				qa.getEndpoint().getId(),
				qa.getIsSuccess(),
				qa.getDescription(),
				parseStringMap(qa.getPathVariables()),
				parseStringMap(qa.getQueryParams()),
				parseStringMap(qa.getHeaders()),
				parseBody(qa.getBody()),
				qa.getCreatedAt()
			))
			.toList();
	}

	private Map<String, String> parseStringMap(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(json, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			throw new CustomException(QaErrorCode.QA_JSON_PROCESSING_ERROR);
		}
	}

	private Object parseBody(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readTree(json);
		} catch (JsonProcessingException e) {
			return json;
		}
	}
}
