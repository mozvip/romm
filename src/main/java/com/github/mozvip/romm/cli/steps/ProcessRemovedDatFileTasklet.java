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

import java.util.Collection;
import java.util.Set;

@Component
public class ProcessRemovedDatFileTasklet implements Tasklet {

    public static final Logger log = LoggerFactory.getLogger(ProcessRemovedDatFileTasklet.class);

    private final ArchiveFileRepository archiveFileRepository;
    private final Romm romm;

    public ProcessRemovedDatFileTasklet(ArchiveFileRepository archiveFileRepository, Romm romm) {
        this.archiveFileRepository = archiveFileRepository;
        this.romm = romm;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        final Set<String> removedDatFiles = romm.getRemovedDatFiles();
        for (String removedDatFile : removedDatFiles) {
            log.info("TODO : Processing removed dat file {}", removedDatFile);
            archiveFileRepository.deleteByDatPathAndMissingIsTrue(removedDatFile);
            final Collection<ArchiveFile> files = archiveFileRepository.findByDatPathAndMissingIsFalse(removedDatFile);
            log.info("{} has {} files", removedDatFile, files.size());
            if (files.size() == 0) {
                archiveFileRepository.deleteByDatPath(removedDatFile);
            } else {
                for (ArchiveFile file : files) {
                    if (file.getSha1() != null) {
                        final Collection<ArchiveFile> missing = archiveFileRepository.findBySha1AndMissingIsTrue(file.getSha1());
                        if (missing.size() > 0) {

                        }
                    } else {

                    }
                }
            }
        }

        return RepeatStatus.FINISHED;
    }
}
