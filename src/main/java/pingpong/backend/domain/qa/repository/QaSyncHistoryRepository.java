package pingpong.backend.domain.qa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pingpong.backend.domain.qa.QaSyncHistory;

public interface QaSyncHistoryRepository extends JpaRepository<QaSyncHistory, Long> {
	/**
	 * 특정 팀의 가장 최근 동기화 이력 1건 조회
	 * OrderByIdDesc: ID 내림차순 (최신순)
	 * findTop: 그 중 첫 번째 레코드만 반환
	 */
	Optional<QaSyncHistory> findTopByTeamIdOrderByIdDesc(Long teamId);

	/**
	 * 전체 팀의 가장 최근 동기화 이력을 팀별 1건씩 조회
	 */
	@Query("SELECT h FROM QaSyncHistory h WHERE h.id IN " +
		"(SELECT MAX(h2.id) FROM QaSyncHistory h2 GROUP BY h2.team)")
	List<QaSyncHistory> findLatestPerTeam();
}
