package de.tum.in.www1.artemis.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.Team;
import de.tum.in.www1.artemis.domain.scores.TeamScore;
import de.tum.in.www1.artemis.web.rest.dto.ParticipantScoreAverageDTO;

@Repository
public interface TeamScoreRepository extends JpaRepository<TeamScore, Long> {

    void deleteAllByTeam(Team team);

    @EntityGraph(type = LOAD, attributePaths = { "team", "exercise", "lastResult", "lastRatedResult" })
    Optional<TeamScore> findTeamScoreByExerciseAndTeam(Exercise exercise, Team team);

    @EntityGraph(type = LOAD, attributePaths = { "team", "exercise", "lastResult", "lastRatedResult" })
    List<TeamScore> findAllByExerciseIn(Set<Exercise> exercises, Pageable pageable);

    @Query("""
                    SELECT new de.tum.in.www1.artemis.web.rest.dto.ParticipantScoreAverageDTO(t.team, AVG(t.lastScore), AVG(t.lastRatedScore))
                    FROM TeamScore t
                    WHERE t.exercise IN :exercises
                    GROUP BY t.team

            """)
    List<ParticipantScoreAverageDTO> getAvgScoreOfTeamInExercises(@Param("exercises") Set<Exercise> exercises);
}