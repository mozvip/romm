package com.github.mozvip.romm.service;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.model.ArchiveFile;
import com.github.mozvip.romm.model.ArchiveFileRepository;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Semaphore;

@Service
public class ZipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipService.class);

    private final Romm romm;
    private final FileSystemService fs;
    private final ArchiveFileRepository archiveFileRepository;

    public ZipService(Romm romm, FileSystemService fs, ArchiveFileRepository archiveFileRepository) {
        this.romm = romm;
        this.fs = fs;
        this.archiveFileRepository = archiveFileRepository;
    }

    public String computeContentSignatureSha1(List<FileHeader> fileHeaders) {
        List<String> fileIds = new ArrayList<>();
        for (FileHeader fileHeader : fileHeaders) {
            fileIds.add(String.format("%s;%d;%d", fileHeader.getFileName(), fileHeader.getUncompressedSize(), fileHeader.getCrc()));
        }
        fileIds.sort(String::compareTo);
        return DigestUtils.sha1Hex(StringUtils.collectionToDelimitedString(fileIds, "$"));
    }

    public void scanOutputZipFile(Path zipFile, Collection<? extends ArchiveFile> missingFiles) {
        Path relativePath = romm.getOutputFolderArg().relativize(zipFile);
        ZipFile file = new ZipFile(zipFile.toFile());
        final Semaphore semaphore = fs.lock(relativePath);
        try {
            semaphore.acquire();
            final List<FileHeader> fileHeaders = file.getFileHeaders();
            Set<Long> foundFileIds = new HashSet<>();
            for (FileHeader fileHeader : fileHeaders) {
                for (ArchiveFile missingFile : missingFiles) {
                    if (missingFile.getCrc32() != null && missingFile.getSize() != null) {
                        if (fileHeader.getCrc() == missingFile.getCrc32() && fileHeader.getUncompressedSize() == missingFile.getSize()) {
                            // we found one of our missing files
                            foundFileIds.add(missingFile.getArchiveFileId());
                        }
                    }
                }
            }
            if (!foundFileIds.isEmpty()) {
                LOGGER.info("{} known files found in {}", foundFileIds.size(), zipFile.toString());
                archiveFileRepository.setNotMissing(foundFileIds);
            }
        } catch (ZipException e) {
            try {
                Files.delete(zipFile);
            } catch (IOException eDelete) {
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            semaphore.release();
        }
    }
}
