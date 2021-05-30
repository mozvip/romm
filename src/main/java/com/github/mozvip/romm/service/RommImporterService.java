package com.github.mozvip.romm.service;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.core.RommProperties;
import com.github.mozvip.romm.core.util.CRCUtil;
import com.github.mozvip.romm.model.*;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import net.sf.sevenzipjbinding.util.ByteArrayStream;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RommImporterService {

    public static final Logger log = LoggerFactory.getLogger(RommImporterService.class);

    private final Romm romm;
    private final RommProperties rommProperties;
    private final ZipService zipService;
    private final RommArchiveRepository rommArchiveRepository;
    private final ArchiveFileRepository archiveFileRepository;
    private final FileSystemService fs;

    public RommImporterService(Romm romm, RommProperties rommProperties, ZipService zipService, RommArchiveRepository rommArchiveRepository, ArchiveFileRepository archiveFileRepository, FileSystemService fs) {
        this.romm = romm;
        this.rommProperties = rommProperties;
        this.zipService = zipService;
        this.rommArchiveRepository = rommArchiveRepository;
        this.archiveFileRepository = archiveFileRepository;
        this.fs = fs;
    }

    private void moveInputFileToUnknown(Path file) throws IOException {
        moveInputFileToFolder(file, romm.getUnknownFolderArg());
    }

    private void moveInputFileToBad(Path file) throws IOException {
        moveInputFileToFolder(file, romm.getBadFolderArg());
    }

    private void foundArchiveFiles(byte[] data, Collection<ArchiveFile> files) throws IOException {
        for (ArchiveFile file : files) {
            foundArchiveFile(data, file);
        }
    }

    private void foundArchiveFiles(Path inputFile, Collection<ArchiveFile> files) throws IOException {
        final List<ArchiveFile> missingFiles = files.stream().filter(ArchiveFile::isMissing).collect(Collectors.toList());
        for (int i=0; i<missingFiles.size(); i++) {
            final ArchiveFile archiveFile = missingFiles.get(i);
            Path destination = romm.getDestinationArchivePath(archiveFile);
            if (archiveFile.isFolder()) {
                if (!Files.isDirectory(destination)) {
                    Files.createDirectories(destination);
                }
                destination = destination.resolve(archiveFile.getFilePath());
            } else {
                Files.createDirectories(destination.getParent());
            }

            if (Files.exists(destination)) {
                log.warn("{} already exists and was not expected, size is {}, alternate file size is {}", destination, Files.size(inputFile), Files.size(destination));
            } else {
                if (i == missingFiles.size() - 1) {
                    Files.move(inputFile, destination);
                } else {
                    Files.copy(inputFile, destination);
                }
            }
            archiveFileRepository.setNotMissing(archiveFile.getArchiveFileId());
        }
    }

    private void foundArchiveFile(byte[] data, ArchiveFile file) throws IOException {
        final Path destination = romm.getDestinationArchivePath(file);
        if (file.isFolder()) {
            if (!Files.isDirectory(destination)) {
                Files.createDirectories(destination);
            }
            Files.copy(new ByteArrayInputStream(data), destination.resolve(file.getFilePath()));
        } else {
            Files.createDirectories(destination.getParent());
            romm.addToArchive(destination, file.getFilePath(), () -> new ByteArrayInputStream(data));
        }
        archiveFileRepository.setNotMissing(file.getArchiveFileId());
    }

    public boolean handleInputCHD(Path chdPath) throws IOException {

        String sha1 = null;

        try (InputStream input = Files.newInputStream(chdPath)) {
            ByteBuffer buf = ByteBuffer.allocate(124);
            if (input.read(buf.array(), 0, buf.capacity()) != buf.capacity()) {
                throw new EOFException();
            }

            byte[] sigBytes = new byte[8];
            buf.get(sigBytes);
            String signature = new String(sigBytes);

            if (!signature.equals("MComprHD")) {
                return false;
            }

            int length = buf.getInt();
            int version = buf.getInt();

            if (version == 5) {

                buf.position(84);
                byte[] sha1Bytes = new byte[20];
                buf.get(sha1Bytes);
                sha1 = Hex.encodeHexString(sha1Bytes, true);

            } else if (version == 4) {

                buf.position(48);
                byte[] sha1Bytes = new byte[20];
                buf.get(sha1Bytes);
                sha1 = Hex.encodeHexString(sha1Bytes, true);

            } else if (version == 3) {

                buf.position(80);
                byte[] sha1Bytes = new byte[20];
                buf.get(sha1Bytes);
                sha1 = Hex.encodeHexString(sha1Bytes, true);

            } else {
                // TODO
                log.warn("Unsupported CHD version {}", version);
            }
        }

        boolean unknown = true;
        if (sha1 != null) {
            final Collection<ArchiveFile> archivesFiles = archiveFileRepository.findBySha1(sha1);
            if (archivesFiles != null && archivesFiles.size() > 0) {
                unknown = false;
                foundArchiveFiles(chdPath, archivesFiles);
                Files.deleteIfExists(chdPath);
            }
        }

        if (unknown) {
            moveInputFileToUnknown(chdPath);
        }

        return true;
    }

    public void handleInputOther(Path path, String extension) throws IOException {
        final long size = Files.size(path);
        long crc = CRCUtil.computeFileCrc(path.toFile());
        String sha1 = "";

        Collection<ArchiveFile> matchingFiles = archiveFileRepository.findBySizeAndCrc32(size, crc);
        if (matchingFiles.isEmpty()) {
            // try by sha1
            try (InputStream input = Files.newInputStream(path)) {
                sha1 = DigestUtils.sha1Hex(input);
            }
            matchingFiles = archiveFileRepository.findBySha1(sha1);
        }

        if (!matchingFiles.isEmpty()) {
            final List<ArchiveFile> missingFiles = matchingFiles.stream().filter(ArchiveFile::isMissing).collect(Collectors.toList());
            if (!missingFiles.isEmpty()) {
                for (ArchiveFile file : missingFiles) {
                    romm.generateOutputFile(file, size, () -> Files.newInputStream(path));
                }
                final Set<String> archivePaths = missingFiles.stream().map(ArchiveFile::getArchivePath).collect(Collectors.toSet());
                log.info("{} file occurrences were found from {} for {}", missingFiles.size(), path, StringUtils.collectionToCommaDelimitedString(archivePaths));
            }
            fs.deleteSinglePath(path);
        } else {
            log.debug("{} : no match for sha1 {} crc {} and size {}", path.getFileName().toString(), sha1, crc, size);
            if (!rommProperties.getDeleteExtensions().contains(extension)) {
                moveInputFileToUnknown(path);
            } else {
                fs.deleteSinglePath(path);
            }
        }
    }

    public boolean handleInputZip(Path inputZipFile) throws IOException {
        ZipFile zipFile = new ZipFile(inputZipFile.toFile());
        Set<String> matchedFilesFromArchive = new HashSet<>();
        List<FileHeader> fileHeaders;
        try {
            fileHeaders = zipFile.getFileHeaders();
        } catch (ZipException e) {
            log.error("{} does not seem to be a Zip file", inputZipFile);
            return handleInput7Zip(inputZipFile, "zip");
        }

        final String contentSignatureSha1 = zipService.computeContentSignatureSha1(fileHeaders);
        final List<RommArchive> matchingRommArchives = rommArchiveRepository.findByContentSignatureSha1(contentSignatureSha1);
        for (RommArchive matchingRommArchive : matchingRommArchives) {
            matchedFilesFromArchive.addAll(fileHeaders.stream().map(FileHeader::getFileName).collect(Collectors.toList()));
            if (matchingRommArchive.getStatus().equals(ArchiveStatus.COMPLETE)) {
                continue;
            }
            romm.copyInputFileToOutput(inputZipFile, matchingRommArchive);
        }

        for (FileHeader fileHeader : fileHeaders) {
            if (fileHeader.getUncompressedSize() > 0) { // empty files processed elsewhere
                final Collection<ArchiveFile> matchingFiles = archiveFileRepository.findBySizeAndCrc32AndMissingIsTrue(fileHeader.getUncompressedSize(), fileHeader.getCrc());
                if (!matchingFiles.isEmpty()) {
                    matchedFilesFromArchive.add(fileHeader.getFileName());
                    for (ArchiveFile file : matchingFiles) {
                        romm.generateOutputFile(file, fileHeader.getUncompressedSize(), () -> zipFile.getInputStream(fileHeader));
                    }
                    final Set<String> archivePaths = matchingFiles.stream().map(ArchiveFile::getArchivePath).collect(Collectors.toSet());
                    log.info("{} file occurrences were found from {} for {}", matchingFiles.size(), inputZipFile, StringUtils.collectionToCommaDelimitedString(archivePaths));
                }
            }
        }
        if (matchedFilesFromArchive.size() == fileHeaders.size()) {
            // we are removing all files from the archive
            try {
                Files.delete(inputZipFile);
            } catch (IOException e) {
                log.error("Error deleting file {}", inputZipFile);
            }
        } else {
            if (!matchedFilesFromArchive.isEmpty()) {
                try {
                    zipFile.setRunInThread(false);
                    zipFile.removeFiles(new ArrayList<>(matchedFilesFromArchive));
                } catch (Exception e) {
                    log.error("Error removing files from {}", inputZipFile);
                }
            }
            try {
                moveInputFileToUnknown(inputZipFile);
            } catch (IOException e) {
                log.error("Error moving file {} to {}", inputZipFile, romm.getUnknownFolderArg());
            }
        }
        return true;
    }

    public boolean handleInput7Zip(Path inputFile, String fileExtension) throws IOException {

        boolean allMatched = true;

        ArchiveFormat archiveFormat;
        switch (fileExtension) {
            case "7z":
                archiveFormat = ArchiveFormat.SEVEN_ZIP;
                break;
            case "arj":
                archiveFormat = ArchiveFormat.ARJ;
                break;
            case "lzh":
                archiveFormat = ArchiveFormat.LZH;
                break;
            case "iso":
                archiveFormat = ArchiveFormat.ISO;
                break;
            default:
                archiveFormat = ArchiveFormat.ZIP;
                break;
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(inputFile.toFile(), "r")) {
            try (IInArchive archive = SevenZip.openInArchive(archiveFormat, new RandomAccessFileInStream(randomAccessFile))) {
                final ISimpleInArchiveItem[] archiveItems = archive.getSimpleInterface().getArchiveItems();
                for (ISimpleInArchiveItem archiveItem : archiveItems) {
                    final Long size = archiveItem.getSize();
                    if (size == null || size == 0) {
                        continue;
                    }
                    byte[] bytes = null;
                    long crc = 0;
                    if (archiveItem.getCRC() == null) {
                        ByteArrayStream byteArrayStream = new ByteArrayStream(archiveItem.getSize().intValue());
                        archiveItem.extractSlow(byteArrayStream);
                        bytes = byteArrayStream.getBytes();
                        crc = CRCUtil.computeFileCrc(bytes);
                    }
                    final List<ArchiveFile> allFiles = archiveFileRepository.findBySizeAndCrc32(size, crc);
                    if (allFiles.size() > 0) {
                        for (ArchiveFile archiveFile : allFiles) {
                            if (archiveFile.isMissing()) {
                                if (bytes == null) {
                                    ByteArrayStream byteArrayStream = new ByteArrayStream(archiveItem.getSize().intValue());
                                    archiveItem.extractSlow(byteArrayStream);
                                    bytes = byteArrayStream.getBytes();
                                }
                                byte[] finalBytes = bytes;
                                romm.generateOutputFile(archiveFile, size, () -> new ByteArrayInputStream(finalBytes));
                            }
                        }
                    } else {
                        allMatched = false;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        if (allMatched) {
            Files.delete(inputFile);
        } else {
            moveInputFileToUnknown(inputFile);
        }

        return true;
    }

    public void importFolder(Path inputFolder) throws IOException, InterruptedException {
        assert (Files.isDirectory(inputFolder) && Files.isReadable(inputFolder));
        log.debug("Scanning input folder {}", inputFolder);

        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(inputFolder, Files::isRegularFile)) {
            for (Path path : ds) {
                executorService.submit(() -> {
                    try {
                        if (Files.size(path) == 0) {
                            fs.deleteSinglePath(path);
                        } else {
                            final String fileName = path.getFileName().toString();
                            String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                            try {
                                if (extension.equals("zip")) {
                                    if (!handleInputZip(path)) {
                                        log.warn("File {} is not a valid ZIP file", path);
                                        moveInputFileToBad(path);
                                    }
                                } else if (extension.equals("7z") || extension.equals("iso") || extension.equals("arj") || extension.equals("lzh")) {
                                    if (!handleInput7Zip(path, extension)) {
                                        log.warn("File {} is not a valid archive", path);
                                        moveInputFileToBad(path);
                                    }
                                } else if (extension.equals("chd")) {
                                    if (!handleInputCHD(path)) {
                                        log.warn("File {} is not a valid CHD file", path);
                                        moveInputFileToBad(path);
                                    }
                                } else {
                                    handleInputOther(path, extension);
                                }
                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                                moveInputFileToBad(path);
                            }
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        fs.deleteFolderIfEmpty(inputFolder, romm.getInputFolderArg());
    }

    private void moveInputFileToFolder(Path file, Path output) throws IOException {
        final Path relativePath = romm.getInputFolderArg().relativize(file);
        final Path destinationFile = output.resolve(relativePath);
        Files.createDirectories(destinationFile.getParent());
        try {
            if (!Files.exists(destinationFile)) {
                destinationFile.toFile().setWritable(true);
                Files.move(file, destinationFile);
            } else {
                if (Files.size(destinationFile) == Files.size(file)) {
                    fs.deleteSinglePath(file);
                } else {
                    log.warn("Can't move file {} to {} : file already exists", file, destinationFile);
                }
            }
        } catch (IOException e) {
            log.error("Error moving file {} to {}", file, destinationFile);
        }
    }

}
