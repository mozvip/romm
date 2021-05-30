package com.github.mozvip.romm.cli;

import com.github.mozvip.romm.core.Romm;
import com.github.mozvip.romm.cli.steps.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RommBatchConfig {

    public static final Logger log = LoggerFactory.getLogger(RommBatchConfig.class);

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ApplicationArguments applicationArguments;

    private final DatFolderScannerTasklet datFolderScannerTasklet;
    private final ScanOutputFolderTasklet scanOutputTasklet;
    private final ScanInputFolderTasklet scanInputTasklet;
    private final ProcessRemovedDatFileTasklet processRemovedDatFileTasklet;

    private final Romm romm;

    public RommBatchConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, ApplicationArguments applicationArguments, DatFolderScannerTasklet datFolderScannerTasklet, ScanOutputFolderTasklet scanOutputTasklet, ScanInputFolderTasklet scanInputTasklet, ProcessRemovedDatFileTasklet processRemovedDatFileTasklet, Romm romm) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.applicationArguments = applicationArguments;
        this.datFolderScannerTasklet = datFolderScannerTasklet;
        this.scanOutputTasklet = scanOutputTasklet;
        this.scanInputTasklet = scanInputTasklet;
        this.processRemovedDatFileTasklet = processRemovedDatFileTasklet;
        this.romm = romm;
    }

    @Bean
    private Step stepReadDat() {
        return stepBuilderFactory.get("stepReadDat")
                .tasklet(datFolderScannerTasklet)
                .build();
    }

    @Bean
    private Step stepReadOutput() {
        return stepBuilderFactory.get("stepReadOutput")
                .tasklet(scanOutputTasklet)
                .build();
    }

    @Bean
    private Step stepReadInput() {
        return stepBuilderFactory.get("stepReadInput")
                .tasklet(scanInputTasklet)
                .build();
    }

    @Bean
    private Step stepProcessRemovedDatFiles() {
        return stepBuilderFactory.get("stepProcessRemovedDatFiles")
                .tasklet(processRemovedDatFileTasklet)
                .build();
    }

    @Bean
    private Step stepGenerateEmptyFiles(GenerateEmptyFilesTasklet generateEmptyFilesTasklet) {
        return stepBuilderFactory.get("stepGenerateEmptyFiles")
                .tasklet(generateEmptyFilesTasklet)
                .build();
    }

    @Bean
    private Step stepSyncOutputFolders(SyncOutputFoldersTasklet syncOutputFoldersTasklet) {
        return stepBuilderFactory.get("stepSyncOutputFolders")
                .tasklet(syncOutputFoldersTasklet)
                .build();
    }

    @Bean
    private Step stepFindRomsInCollection(FindRomsInCollectionTasklet findRomsInCollectionTasklet) {
        return stepBuilderFactory.get("stepFindRomsInCollection")
                .tasklet(findRomsInCollectionTasklet)
                .build();
    }

    @Bean
    private Step stepSyncArchives(SyncArchivesTasklet syncArchivesTasklet) {
        return stepBuilderFactory.get("stepSyncArchives")
                .tasklet(syncArchivesTasklet)
                .build();
    }

    /*
        @Bean
        private Step stepCreateEmptyFiles() {
            return stepBuilderFactory.get("stepCreateEmptyFiles")
                    .tasklet(createEmptyFilesTasklet)
                    .build();
        }


        @Bean
        private Step stepValidateArgs() {
            return stepBuilderFactory.get("stepValidateArgs")
                    .tasklet((stepContribution, chunkContext) -> {
                        final List<String> outputFolders = applicationArguments.getOptionValues("output");
                        if (outputFolders.size() == 1) {
                            romm.setOutputFolderArg(Paths.get(outputFolders.get(0)));
                        }
                        if (romm.getOutputFolderArg() == null || !Files.isWritable(romm.getOutputFolderArg())) {
                            LOGGER.error("output parameter is required and must point to a readable directory");
                        }
                        final List<String> datFolders = applicationArguments.getOptionValues("dat");
                        if (datFolders.size() == 1) {
                            romm.setDatFolderArg(Paths.get(datFolders.get(0)));
                        }
                        if (romm.getDatFolderArg() == null || !Files.isReadable(romm.getDatFolderArg())) {
                            LOGGER.error("dat parameter is required and must point to a readable directory");
                        }
                        final List<String> inputFolders = applicationArguments.getOptionValues("input");
                        if (inputFolders.size() == 1) {
                            romm.setInputFolderArg(Paths.get(inputFolders.get(0)));
                        }
                        if (romm.getInputFolderArg() == null || !Files.isReadable(romm.getInputFolderArg())) {
                            LOGGER.error("input parameter is required and must point to a readable directory");
                        }
                        final List<String> unknownFolders = applicationArguments.getOptionValues("unknown");
                        if (unknownFolders.size() == 1) {
                            romm.setUnknownFolderArg(Paths.get(unknownFolders.get(0)));
                        }
                        if (romm.getUnknownFolderArg() == null || !Files.isWritable(romm.getUnknownFolderArg())) {
                            LOGGER.error("unknown parameter is required and must point to a writable directory");
                        }
                        return RepeatStatus.FINISHED;
                    })
                    .build();
        }

        @Bean(name = "jobBatch")
        public Job job(Step stepValidateArgs,
                       Step stepReadDat,
                       Step stepReadOutput,
                       Step stepCreateEmptyFiles,
                       Step stepReadInput) {

            return jobBuilderFactory.get("jobBatch")
                    .start(stepValidateArgs)
                    .next(stepReadDat)
                    .next(stepReadOutput)
                    .next(stepCreateEmptyFiles)
                    .next(stepReadInput)
                    .build();
        }

     */

    @Bean
    public Job job(Step stepReadDat, Step stepReadOutput, Step stepSyncArchives, Step stepSyncOutputFolders, Step stepGenerateEmptyFiles, Step stepFindRomsInCollection, Step stepProcessRemovedDatFiles, Step stepReadInput) {
        return jobBuilderFactory.get("jobBatch")
                .preventRestart()
                .incrementer(new RunIdIncrementer())
                .start(stepReadDat)
                .next(stepFindRomsInCollection)
                .next(stepProcessRemovedDatFiles)
                .next(stepReadOutput)
                .next(stepGenerateEmptyFiles)
                .next(stepReadInput)
                .next(stepSyncArchives)
                .next(stepSyncOutputFolders)
                .build();
    }

}
