package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.model.ArchiveFile;
import com.github.mozvip.romm.model.ArchiveFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class GenerateEmptyFilesTasklet implements Tasklet {

    private final static Logger log = LoggerFactory.getLogger(GenerateEmptyFilesTasklet.class);

    private final ArchiveFileRepository archiveFileRepository;
    private final Romm romm;

    public GenerateEmptyFilesTasklet(ArchiveFileRepository archiveFileRepository, Romm romm) {
        this.archiveFileRepository = archiveFileRepository;
        this.romm = romm;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        final Collection<ArchiveFile> missingEmptyFiles = archiveFileRepository.findBySizeAndCrc32AndMissingIsTrue(0, 0);
        for (ArchiveFile missingEmptyFile : missingEmptyFiles) {
            try {
                romm.generateEmptyFile(missingEmptyFile);
                archiveFileRepository.setNotMissing(missingEmptyFile.getArchiveFileId());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return RepeatStatus.FINISHED;
    }
}
