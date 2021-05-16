package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.service.FileSystemService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DeleteEmptyInputFoldersTasklet implements Tasklet {

    private final FileSystemService fs;
    private final Romm romm;

    public DeleteEmptyInputFoldersTasklet(FileSystemService fs, Romm romm) {
        this.fs = fs;
        this.romm = romm;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(romm.getInputFolderArg())) {
            for (Path path : paths) {
                if (Files.isDirectory(path)) {
                }
            }
        }

        return RepeatStatus.FINISHED;
    }
}
