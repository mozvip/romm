package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.filetypes.Dat;
import com.github.mozvip.romm.filetypes.DatFactory;
import com.github.mozvip.romm.filetypes.DatFile;
import com.github.mozvip.romm.filetypes.DatGame;
import com.github.mozvip.romm.model.*;
import com.github.mozvip.romm.service.ArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class DatFolderScannerTasklet implements Tasklet {

    public static final Logger log = LoggerFactory.getLogger(DatFolderScannerTasklet.class);

    private final ArchiveFileRepository archiveFileRepository;
    private final RommArchiveRepository rommArchiveRepository;
    private final ExecutorService executorService;
    private final Romm romm;
    private final ArchiveService archiveService;

    private final Set<String> missingDats = new HashSet<>();

    public DatFolderScannerTasklet(ArchiveFileRepository archiveFileRepository, RommArchiveRepository rommArchiveRepository, Romm romm, ArchiveService archiveService) {
        this.archiveFileRepository = archiveFileRepository;
        this.rommArchiveRepository = rommArchiveRepository;
        this.romm = romm;
        this.archiveService = archiveService;

        this.executorService = Executors.newFixedThreadPool(Romm.MAX_THREADS);
    }

    private void scanDatFolder(Path folder) throws IOException {
        Path root = romm.getDatFolderArg();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            for (Path path : ds) {
                if (Files.isDirectory(path)) {
                    scanDatFolder(path);
                } else {
                    final String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".dat") || fileName.endsWith(".xml")){
                        executorService.submit(() -> {
                            try {
                                final String datPath = root.relativize(path).toString();
                                if (missingDats.contains(datPath)) {
                                    missingDats.remove(datPath);
                                }
                                final LocalDateTime maxAddedByDatPath = archiveFileRepository.findMaxCreatedDateByDatPath(datPath);
                                if (maxAddedByDatPath != null && maxAddedByDatPath.isAfter(LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault()))) {
                                    // no update since last time we read this file
                                    log.debug("Dat file {} already in cache", datPath);
                                } else {
                                    loadDatFileFromPath(path);
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        });
                    }
                }
            }
        }
    }

    @Transactional
    void loadDatFileFromPath(Path path) throws IOException {
        final Dat dat = DatFactory.readDat(path);
        if (dat == null) {
            return;
        }
        final Path root = romm.getDatFolderArg();

        Path relativeFolder = root.relativize(path.getParent()).resolve(dat.getName());
        final String datPath = root.relativize(path).toString();

        archiveFileRepository.deleteByDatPath(datPath);
        rommArchiveRepository.deleteByDatPath(datPath);

        final List<DatGame> games = dat.getGames();
        List<ArchiveFile> datArchives = new ArrayList<>();
        for (DatGame game : games) {
            if (game.getArchiveFiles().isEmpty()) {
                log.warn("Archive {} in dat {} has no files, ignoring", game.getName(), dat.getName());
                continue;
            }
            final String archivePath = relativeFolder.resolve(game.getPath()).toString();
            List<ArchiveFile> archiveFiles = new ArrayList<>();
            for (DatFile file : game.getArchiveFiles().values()) {
                ArchiveFile archiveFile = new ArchiveFile();
                archiveFile.setDatPath(datPath);
                archiveFile.setArchivePath(archivePath);
                archiveFile.setFilePath(file.getName());
                archiveFile.setFolder(file.isDisk());
                archiveFile.setCrc32(file.getCrc());
                archiveFile.setMd5(file.getMd5());
                archiveFile.setSha1(file.getSha1());
                archiveFile.setSize(file.getSize());
                archiveFile.setMissing(true);
                archiveFiles.add(archiveFile);
            }
            final String contentSignatureSha1 = archiveService.computeContentSignatureSha1(archiveFiles);
            RommArchive rommArchive = new RommArchive();
            rommArchive.setArchivePath(archivePath);
            rommArchive.setDatPath(datPath);
            rommArchive.setContentSignatureSha1(contentSignatureSha1);
            rommArchive.setStatus(ArchiveStatus.MISSING);
            rommArchiveRepository.save(rommArchive);
            datArchives.addAll(archiveFiles);
        }
        log.info("Saving {} files in DB", datArchives.size());
        archiveFileRepository.saveAll(datArchives);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void scanDatFolder() throws IOException, InterruptedException {
        Set<String> distinctDatPath = archiveFileRepository.findDistinctDatPath();
        missingDats.addAll(distinctDatPath);
        scanDatFolder(romm.getDatFolderArg());
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);
        romm.setRemovedDatFiles(missingDats);
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        scanDatFolder();
        return RepeatStatus.FINISHED;
    }
}
