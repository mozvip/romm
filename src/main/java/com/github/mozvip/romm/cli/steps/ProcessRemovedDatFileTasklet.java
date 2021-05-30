package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.model.ArchiveFile;
import com.github.mozvip.romm.model.ArchiveFileRepository;
import com.github.mozvip.romm.model.RommArchiveRepository;
import com.github.mozvip.romm.service.RommImporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProcessRemovedDatFileTasklet implements Tasklet {

    public static final Logger log = LoggerFactory.getLogger(ProcessRemovedDatFileTasklet.class);

    private final ArchiveFileRepository archiveFileRepository;
    private final RommArchiveRepository archiveRepository;
    private final Romm romm;
    private final RommImporterService importerService;

    public ProcessRemovedDatFileTasklet(ArchiveFileRepository archiveFileRepository, RommArchiveRepository archiveRepository, Romm romm, RommImporterService importerService) {
        this.archiveFileRepository = archiveFileRepository;
        this.archiveRepository = archiveRepository;
        this.romm = romm;
        this.importerService = importerService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        final Set<String> removedDatFiles = romm.getRemovedDatFiles();
        for (String removedDatFile : removedDatFiles) {
            log.info("Processing removed dat file {}", removedDatFile);
            final Collection<ArchiveFile> files = archiveFileRepository.findByDatPathAndMissingIsFalse(removedDatFile);
            log.info("{} had {} files", removedDatFile, files.size());
            final List<String> distinctArchivePath = archiveFileRepository.findDistinctArchivePath(removedDatFile);
            final Set<Path> foldersToImport = distinctArchivePath.stream().map(s -> romm.getOutputFolderArg().resolve(s)).map(path -> Files.isDirectory(path) ? path : path.getParent()).collect(Collectors.toSet());
            for (Path folder: foldersToImport) {
                importerService.importFolder(folder);
            }
            archiveFileRepository.deleteByDatPath(removedDatFile);
            archiveRepository.deleteByDatPath(removedDatFile);
        }

        return RepeatStatus.FINISHED;
    }
}
