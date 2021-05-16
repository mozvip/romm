package com.github.mozvip.romm.service;

import com.github.mozvip.romm.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class SyncArchivesService {

    private final static Logger log = LoggerFactory.getLogger(SyncArchivesService.class);

    private final RommArchiveRepository rommArchiveRepository;
    private final ArchiveFileRepository archiveFileRepository;
    private final ArchiveService archiveService;

    public SyncArchivesService(RommArchiveRepository rommArchiveRepository, ArchiveFileRepository archiveFileRepository, ArchiveService archiveService) {
        this.rommArchiveRepository = rommArchiveRepository;
        this.archiveFileRepository = archiveFileRepository;
        this.archiveService = archiveService;
    }

    public void fixCacheForArchiveNames(Collection<String> archivePaths) {
        final Set<String> createdArchivePaths = rommArchiveRepository.findDistinctArchivePath();

        if (archivePaths == null) {
            // we refresh all of them
            archivePaths = rommArchiveRepository.findDistinctArchivePath();
        }

        for (String archivePath : archivePaths) {
            final List<ArchiveFile> archiveFiles = archiveFileRepository.findByArchivePath(archivePath);
            ArchiveStatus status = archiveService.computeStatusFromFiles(archiveFiles);
            if (createdArchivePaths.contains(archivePath)) {
                final RommArchive rommArchive = rommArchiveRepository.findByArchivePath(archivePath);
                if (rommArchive.getStatus() != status) {
                    rommArchiveRepository.updateStatus(archivePath, status.name());
                    if (status == ArchiveStatus.COMPLETE) {
                        log.info("{} is complete", archivePath);
                    }
                }
            } else {
                RommArchive rommArchive = new RommArchive();
                rommArchive.setArchivePath(archivePath);
                rommArchive.setDatPath(archiveFiles.get(0).getDatPath());
                rommArchive.setContentSignatureSha1(archiveService.computeContentSignatureSha1(archiveFiles));
                rommArchive.setStatus(status);
                if (status == ArchiveStatus.COMPLETE) {
                    log.info("{} is complete", archivePath);
                }
                rommArchiveRepository.save(rommArchive);
            }
        }
    }
}
