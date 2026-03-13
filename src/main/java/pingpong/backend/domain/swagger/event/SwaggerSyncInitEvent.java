package pingpong.backend.domain.swagger.event;

import pingpong.backend.domain.member.Member;

public record SwaggerSyncInitEvent(Long teamId, Member member) {
}
