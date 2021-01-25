package de.tum.in.www1.artemis.service.listeners;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.service.ScoreService;

@Component
public class ResultListener {

    private final Logger log = LoggerFactory.getLogger(ResultListener.class);

    private ScoreService scoreService;

    /**
     * While {@link javax.persistence.EntityManager} is being initialized it instantiates {@link javax.persistence.EntityListeners} including
     * {@link ResultListener}. Now {@link ResultListener} requires the {@link ScoreService} which requires {@link de.tum.in.www1.artemis.repository.StudentScoreRepository}
     * which requires {@link javax.persistence.EntityManager}. To break this circular dependency we use lazy injection of the service here.
     *
     * @param scoreService the student score service that will be lazily injected by Spring
     */
    public ResultListener(@Lazy ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    /**
     * Remove associated student scores before a result is removed
     *
     * @param resultToBeDeleted result about to be remove
     */
    @PreRemove
    public void removeAssociatedStudentScores(Result resultToBeDeleted) {
        scoreService.removeOrUpdateAssociatedParticipantScore(resultToBeDeleted);
    }

    @PostUpdate
    @PostPersist
    public void updateStudentScores(Result result) {
        scoreService.updateOrCreateParticipantScore(result);

    }
}