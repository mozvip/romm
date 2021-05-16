package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.model.*;
import com.github.mozvip.romm.service.ArchiveService;
import com.github.mozvip.romm.service.SyncArchivesService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SyncArchivesTasklet implements Tasklet {

    private final ArchiveFileRepository archiveFileRepository;
    private final SyncArchivesService syncArchivesService;

    public SyncArchivesTasklet(ArchiveFileRepository archiveFileRepository, SyncArchivesService syncArchivesService) {
        this.archiveFileRepository = archiveFileRepository;
        this.syncArchivesService = syncArchivesService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        final Date startTime = chunkContext.getStepContext().getStepExecution().getJobExecution().getStartTime();
        final Collection<ArchiveFile> allWithLastModifiedDateAfter = archiveFileRepository.findByLastModifiedDateAfter(startTime);
        if (allWithLastModifiedDateAfter.size() == 0) {
            return RepeatStatus.FINISHED;
        }

        final List<String> allArchivePaths = allWithLastModifiedDateAfter.stream().map(ArchiveFile::getArchivePath).distinct().collect(Collectors.toList());
        syncArchivesService.fixCacheForArchiveNames(allArchivePaths);
        return RepeatStatus.FINISHED;
    }


}
