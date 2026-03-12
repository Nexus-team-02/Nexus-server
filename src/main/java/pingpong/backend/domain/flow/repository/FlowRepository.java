package pingpong.backend.domain.flow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pingpong.backend.domain.flow.Flow;

public interface FlowRepository extends JpaRepository<Flow, Long> {

	List<Flow> findByTeamId(Long teamId);

	@Query("""
		select f
		from Flow f
		where f.team.id in :teamIds
		  and f.id = (
		    select min(f2.id)
		    from Flow f2
		    where f2.team.id = f.team.id
		  )
		""")
	List<Flow> findFirstFlowPerTeam(@Param("teamIds") List<Long> teamIds);
}
