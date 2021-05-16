package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.core.RommMissingFileException;
import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.model.*;
import com.github.mozvip.romm.service.FileSystemService;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Semaphore;

@Component
public class FindRomsInCollectionTasklet implements Tasklet {

    public static final Logger log = LoggerFactory.getLogger(FindRomsInCollectionTasklet.class);

    private final Romm romm;
    private final ArchiveFileRepository archiveFileRepository;
    private final RommArchiveRepository rommArchiveRepository;
    private final FileSystemService fs;

    public FindRomsInCollectionTasklet(Romm romm, ArchiveFileRepository archiveFileRepository, RommArchiveRepository rommArchiveRepository, FileSystemService fs) {
        this.romm = romm;
        this.archiveFileRepository = archiveFileRepository;
        this.rommArchiveRepository = rommArchiveRepository;
        this.fs = fs;
    }

    public void processFoundFile(ArchiveFile missingFile, ArchiveFile existingFile) throws IOException {
        final Path existingArchivePath = romm.getDestinationArchivePath(existingFile);
        try {
            romm.generateOutputFile(missingFile, missingFile.getSize(), () -> {
                final String fileName = existingArchivePath.getFileName().toString();
                if (fileName.endsWith(".zip")) {
                    final Semaphore semaphore = fs.lock(Paths.get(existingFile.getArchivePath()));
                    try {
                        final ZipFile zipFile = new ZipFile(existingArchivePath.toFile());
                        semaphore.acquire();
                        final FileHeader fileHeader = zipFile.getFileHeader(existingFile.getFilePath());
                        if (fileHeader == null) {
                            throw new RommMissingFileException(String.format("Archive %s do not contain expected file %s", existingArchivePath, existingFile.getFilePath()));
                        }
                        return zipFile.getInputStream(fileHeader);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        semaphore.release();
                    }
                } else if (fileName.endsWith(".7z")) {
                    // TODO
                } else {
                    // FIXME
                    return Files.newInputStream(existingArchivePath);
                }

                return null;
            });
        } catch (RommMissingFileException e) {
            log.error(e.getMessage());
            archiveFileRepository.setMissing(existingFile.getArchiveFileId());
        }
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        final long haveFileCount = archiveFileRepository.countByMissingIsFalse();
        if (haveFileCount == 0) {
            // only execute this step if we actually have some non-missing files
            return RepeatStatus.FINISHED;
        }

        // first pass, try to locate whole archives with contentSignatureSha1
        final List<RommArchive> missingRommArchives = rommArchiveRepository.findMissingFromCollectionBySignature();
        for (RommArchive missingRommArchive : missingRommArchives) {
            final List<RommArchive> otherRommArchives = rommArchiveRepository.findByContentSignatureSha1AndArchivePathNotAndStatus(missingRommArchive.getContentSignatureSha1(), missingRommArchive.getArchivePath(), ArchiveStatus.COMPLETE.name());
            final Path sourceFile = romm.getDestinationArchivePath(otherRommArchives.get(0).getArchivePath());
            romm.copyInputFileToOutput(sourceFile, missingRommArchive);
        }

        // second pass, try to locate archive files by crc32 and size
        final List<ArchiveFile> missingFromCollectionByCrc32AndSize = archiveFileRepository.findMissingFromCollectionByCrc32AndSize();
        for (ArchiveFile missingFile : missingFromCollectionByCrc32AndSize) {
            List<ArchiveFile> existingFiles = archiveFileRepository.findBySizeAndCrc32AndMissingIsFalse(missingFile.getSize(), missingFile.getCrc32());
            if (existingFiles.isEmpty()) {
                log.error("Could not find expected file for size={} & crc32={}", missingFile.getSize(), missingFile.getCrc32());
            } else {
                ArchiveFile existingFile = existingFiles.get(0);
                try {
                    processFoundFile(missingFile, existingFile);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

        // third pass, try to locate archive files by SHA1
        final List<ArchiveFile> missingFromCollectionBySha1 = archiveFileRepository.findMissingFromCollectionBySha1();
        for (ArchiveFile missingFile : missingFromCollectionBySha1) {
            List<ArchiveFile> existingFiles = archiveFileRepository.findBySha1AndMissingIsFalse(missingFile.getSha1());
            if (existingFiles.isEmpty()) {
                log.error("Could not find expected file for sha1={}", missingFile.getSha1());
            } else {
                ArchiveFile existingFile = existingFiles.get(0);
                try {
                    processFoundFile(missingFile, existingFile);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

        return RepeatStatus.FINISHED;
    }
}
