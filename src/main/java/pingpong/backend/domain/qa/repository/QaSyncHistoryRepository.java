package pingpong.backend.domain.qa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pingpong.backend.domain.qa.QaSyncHistory;

public interface QaSyncHistoryRepository extends JpaRepository<QaSyncHistory, Long> {
}
