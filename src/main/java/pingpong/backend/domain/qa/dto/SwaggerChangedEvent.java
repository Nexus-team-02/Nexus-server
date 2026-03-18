package pingpong.backend.domain.qa.dto;

import pingpong.backend.domain.member.Member;

public record SwaggerChangedEvent(Long teamId, Member member) {
}
