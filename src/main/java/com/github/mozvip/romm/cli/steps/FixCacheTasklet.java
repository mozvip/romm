package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.service.SyncArchivesService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class FixCacheTasklet implements Tasklet {

    private final SyncArchivesService syncArchivesService;

    public FixCacheTasklet(SyncArchivesService syncArchivesService) {
        this.syncArchivesService = syncArchivesService;
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        syncArchivesService.fixCacheForArchiveNames(null);

        return RepeatStatus.FINISHED;
    }
}
