package com.github.mozvip.romm.cli.steps;

import com.github.mozvip.romm.model.OutputFolder;
import com.github.mozvip.romm.model.OutputFolderRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SyncOutputFoldersTasklet implements Tasklet {

    private final OutputFolderRepository outputFolderRepository;

    public SyncOutputFoldersTasklet(OutputFolderRepository outputFolderRepository) {
        this.outputFolderRepository = outputFolderRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        final Iterable<OutputFolder> all = outputFolderRepository.findAll();
        for (OutputFolder outputFolder : all) {
            outputFolderRepository.updateScannedDate(outputFolder.getRelativePath(), LocalDateTime.now());
        }
        return RepeatStatus.FINISHED;
    }
}
