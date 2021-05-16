package com.github.mozvip.romm.core;

import com.github.mozvip.romm.model.*;
import com.github.mozvip.romm.service.FileSystemService;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

@Service
public class Romm {

    public static final Logger log = LoggerFactory.getLogger(Romm.class);
    public static final String ROMM_VERSION = "0.0.1";
    public static final int MAX_THREADS = 4;

    private static final byte[] emptyFile = new byte[0];

    private final RommProperties rommProperties;
    private final FileSystemService fs;
    private final ArchiveFileRepository archiveFileRepository;
    private final RommArchiveRepository rommArchiveRepository;

    private Set<String> removedDatFiles = new HashSet<>();
    private Set<String> createdOutputFolders = new HashSet<>();

    public Romm(RommProperties rommProperties, FileSystemService fs,
                ArchiveFileRepository archiveFileRepository,
                RommArchiveRepository rommArchiveRepository) {
        this.rommProperties = rommProperties;
        this.fs = fs;
        this.archiveFileRepository = archiveFileRepository;
        this.rommArchiveRepository = rommArchiveRepository;
    }

    public Path getDatFolderArg() {
        return rommProperties.getDatFolder();
    }

    public Path getOutputFolderArg() {
        return rommProperties.getOutputFolder();
    }

    public Path getInputFolderArg() {
        return rommProperties.getInputFolder();
    }

    public Path getUnknownFolderArg() {
        return rommProperties.getUnknownFolder();
    }

    public Set<String> getRemovedDatFiles() {
        return removedDatFiles;
    }

    public void setRemovedDatFiles(Set<String> removedDatFiles) {
        this.removedDatFiles = removedDatFiles;
    }

    public Set<String> getCreatedOutputFolders() {
        return createdOutputFolders;
    }

    public void setCreatedOutputFolders(Set<String> createdOutputFolders) {
        this.createdOutputFolders = createdOutputFolders;
    }

    public void addToArchive(Path pathToArchiveFile, String relativePathInZipFile, InputStreamBuilder inputBuilder) throws IOException {
        Path relativePath = getOutputFolderArg().relativize(pathToArchiveFile);
        createOutputFolderIfMissing(pathToArchiveFile.getParent());
        final Semaphore semaphore = fs.lock(relativePath);
        try {
            ZipParameters parameters = new ZipParameters();
            parameters.setFileNameInZip(relativePathInZipFile);
            try (InputStream input = inputBuilder.build()) {
                final ZipFile zipFile = new ZipFile(pathToArchiveFile.toFile());
                semaphore.acquire();
                zipFile.addStream(input, parameters);
            }
        } catch (ZipException e) {
            log.error("Error writing to {}", pathToArchiveFile.toString(), e);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            semaphore.release();
        }
    }

    public void addEmptyFileToArchive(Path pathToZip, String relativePathInZipFile) throws IOException {
        addToArchive(pathToZip, relativePathInZipFile, () -> new ByteArrayInputStream(emptyFile));
    }

    public Path getDestinationArchivePath(ArchiveFile file) {
        return file.isFolder() ?
                getOutputFolderArg().resolve(file.getArchivePath()) :
                getOutputFolderArg().resolve(file.getArchivePath() + ".zip");
    }

    public Path getDestinationArchivePath(String archivePath) {
        return getOutputFolderArg().resolve(archivePath + ".zip");
    }

    private void createOutputFolderIfMissing(Path outputFolder) throws IOException {
        if (!Files.isDirectory(outputFolder)) {
            Path relativePath = getOutputFolderArg().relativize(outputFolder);
            createdOutputFolders.add(relativePath.toString());
            Files.createDirectories(outputFolder);
        }
    }

    @Transactional
    public void copyInputFileToOutput(Path inputFile, RommArchive matchingRommArchive) throws IOException {
        log.info("Copying file {} to {}", inputFile, matchingRommArchive.getArchivePath());
        final Path destination = getDestinationArchivePath(matchingRommArchive.getArchivePath());
        Files.createDirectories(destination.getParent());
        final Semaphore lock = fs.lock(destination);
        try {
            lock.acquire();
            Files.copy(inputFile, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.release();
        }
        archiveFileRepository.setNotMissing(matchingRommArchive.getArchivePath());
        rommArchiveRepository.updateStatus(matchingRommArchive.getArchivePath(), ArchiveStatus.COMPLETE.name());
    }

    public void generateOutputFile(ArchiveFile file, long uncompressedSize, InputStreamBuilder inputStreamBuilder) throws IOException, RommMissingFileException {
        Path destinationGameFile = getDestinationArchivePath(file);
        if (file.isFolder()) {
            Path destinationFile = destinationGameFile.resolve(file.getFilePath());
            if (Files.exists(destinationFile) && Files.size(destinationFile) == uncompressedSize) {
                return;
            }
            createOutputFolderIfMissing(destinationGameFile);
            if (inputStreamBuilder != null) {
                Files.copy(inputStreamBuilder.build(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.createFile(destinationFile);
            }
            archiveFileRepository.setNotMissing(file.getArchiveFileId());
        } else {
            // FIXME: Zip output harcoded for now
            if (inputStreamBuilder != null) {
                addToArchive(destinationGameFile, file.getFilePath(), inputStreamBuilder);
            } else {
                addEmptyFileToArchive(destinationGameFile, file.getFilePath());
            }
            archiveFileRepository.setNotMissing(file.getArchiveFileId());
        }
    }

    public void generateEmptyFile(ArchiveFile file) throws IOException {
        generateOutputFile(file, 0, () -> new ByteArrayInputStream(emptyFile));
    }
}
