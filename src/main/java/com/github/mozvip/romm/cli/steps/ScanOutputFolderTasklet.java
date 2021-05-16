package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.model.ArchiveFile;
import com.github.mozvip.romm.model.ArchiveFileRepository;
import com.github.mozvip.romm.model.OutputFolder;
import com.github.mozvip.romm.model.OutputFolderRepository;
import com.github.mozvip.romm.service.FileSystemService;
import com.github.mozvip.romm.service.ZipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ScanOutputFolderTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ScanOutputFolderTasklet.class);
    private final Romm romm;
    private final FileSystemService fs;
    private final ZipService zipService;
    private final ArchiveFileRepository archiveFileRepository;
    private final OutputFolderRepository outputFolderRepository;

    public ScanOutputFolderTasklet(Romm romm, FileSystemService fs, ZipService zipService, ArchiveFileRepository archiveFileRepository, OutputFolderRepository outputFolderRepository) {
        this.romm = romm;
        this.fs = fs;
        this.zipService = zipService;
        this.archiveFileRepository = archiveFileRepository;
        this.outputFolderRepository = outputFolderRepository;
    }

    private void scanOutput(Path outputFolder, Instant lastScannedDate) throws IOException {
        assert (Files.isDirectory(outputFolder) && Files.isReadable(outputFolder));
        log.debug("Scanning output folder {}", outputFolder);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(outputFolder, Files::isRegularFile)) {
            for (Path path : ds) {
                if (lastScannedDate != null && Files.getLastModifiedTime(path).toInstant().isBefore(lastScannedDate)) {
                    continue;
                }
                if (!Files.isReadable(path)) {
                    log.warn("Output path {} is not readable", path);
                    continue;
                }
                final String fileName = path.getFileName().toString();
                if (fileName.endsWith(".zip") || fileName.endsWith(".7z")) {
                    Path relativePath = romm.getOutputFolderArg().relativize(path);
                    final String archivePath = relativePath.toString();
                    String pathWithoutExtension = archivePath.substring(0, archivePath.lastIndexOf('.'));
                    final Collection<? extends ArchiveFile> expectedFiles = archiveFileRepository.findByArchivePath(pathWithoutExtension);
                    if (expectedFiles == null || expectedFiles.isEmpty()) {
                        log.warn("File {} is not expected at this location", path.toString());
                    } else {
                        final List<? extends ArchiveFile> missingFiles = expectedFiles.stream().filter((ArchiveFile::isMissing)).collect(Collectors.toList());
                        if (!missingFiles.isEmpty()) {
                            zipService.scanOutputZipFile(path, missingFiles);
                        }
                    }
                }
            }
        }
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        log.info("Parsing output folders");
        if (Files.isDirectory(romm.getOutputFolderArg())) {
            fs.forEachFolder(romm.getOutputFolderArg(), Romm.MAX_THREADS, folder -> {
                try {
                    String relativePath = romm.getOutputFolderArg().relativize(folder).toString();
                    final Optional<OutputFolder> optionalOutputFolder = outputFolderRepository.findByRelativePath(relativePath);
                    OutputFolder outputFolder;
                    Instant lastScannedDate = null;
                    if (optionalOutputFolder.isPresent()) {
                        outputFolder = optionalOutputFolder.get();
                        lastScannedDate = optionalOutputFolder.get().getScannedDate().toInstant();
                        final FileTime lastModifiedTime = Files.getLastModifiedTime(folder);
                        if (lastScannedDate.isAfter(lastModifiedTime.toInstant())) {
                            return;
                        } else {
                            log.info("Refreshing contents of output folder {} modified {}", folder, lastModifiedTime);
                        }
                    } else {
                        outputFolder = new OutputFolder(relativePath);
                    }
                    scanOutput(folder, lastScannedDate);
                    outputFolder.setScannedDate(java.sql.Timestamp.from(Instant.now()));
                    outputFolderRepository.save(outputFolder);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        } else {
            Files.createDirectories(romm.getOutputFolderArg());
        }
        return RepeatStatus.FINISHED;
    }
}
