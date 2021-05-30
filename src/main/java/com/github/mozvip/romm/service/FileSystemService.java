package com.github.mozvip.romm.service;

import com.github.mozvip.romm.core.FolderAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

@Service
public class FileSystemService {

    private static final Logger log = LoggerFactory.getLogger(FileSystemService.class);

    ConcurrentHashMap<Path, Semaphore> fileLocks = new ConcurrentHashMap<>();

    private void buildFileTree(Path folder, DirectoryStream.Filter<Path> filter, ExecutorService service, FolderAction folderAction) {
        if (folderAction != null) {
            service.submit(() -> {
                try {
                    folderAction.run(folder);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder, filter)) {
            for (Path d : ds) {
                buildFileTree(d, filter, service, folderAction);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void forEachFolder(Path startFolder, int maxThreads, FolderAction folderAction) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
        buildFileTree(startFolder, Files::isDirectory, executorService, folderAction);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);
    }

    public Semaphore lock(Path relativePath) {
        return fileLocks.computeIfAbsent(relativePath, s -> new Semaphore(1));
    }

    public void deleteFolderIfEmpty(Path folder, Path untilRoot) throws IOException {
        if (folder.equals(untilRoot)) {
            return;
        }
        boolean empty = true;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            for (Path d : ds) {
                empty = false;
                break;
            }
        }
        if (empty) {
            log.info("Deleting empty folder {}", folder);
            deleteSinglePath(folder);
            deleteFolderIfEmpty(folder.getParent(), untilRoot);
        }
    }

    public void deleteSinglePath(Path pathToDelete) throws IOException {
        pathToDelete.toFile().setWritable(true);
        Files.delete(pathToDelete);
    }
}
