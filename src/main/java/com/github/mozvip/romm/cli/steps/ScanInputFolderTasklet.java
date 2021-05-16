package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.model.*;
import com.github.mozvip.romm.service.FileSystemService;
import com.github.mozvip.romm.service.RommImporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ScanInputFolderTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ScanInputFolderTasklet.class);

    private final Romm romm;
    private final FileSystemService fs;
    private final RommImporterService importer;

    public ScanInputFolderTasklet(Romm romm, FileSystemService fs, RommImporterService importer) {
        this.romm = romm;
        this.importer = importer;
        this.fs = fs;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Parsing input folders");
        if (Files.isDirectory(romm.getInputFolderArg())) {
            fs.forEachFolder(romm.getInputFolderArg(), 4, folder -> {
                try {
                    importer.importFolder(folder);
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
