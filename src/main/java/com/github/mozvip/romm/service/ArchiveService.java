package com.github.mozvip.romm.service;

import com.github.mozvip.romm.model.ArchiveFile;
import com.github.mozvip.romm.model.ArchiveStatus;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class ArchiveService {

    public String computeContentSignatureSha1(Collection<ArchiveFile> archiveFiles) {
        List<String> fileIds = new ArrayList<>();
        for (ArchiveFile archiveFile : archiveFiles) {
            if (archiveFile.getSize() == null || archiveFile.getCrc32() == null) {
                // cannot generate an archive id in this case
                return null;
            }
            fileIds.add(String.format("%s;%d;%d", archiveFile.getFilePath(), archiveFile.getSize(), archiveFile.getCrc32()));
        }
        fileIds.sort(String::compareTo);
        return DigestUtils.sha1Hex(StringUtils.collectionToDelimitedString(fileIds, "$"));
    }

    public ArchiveStatus computeStatusFromFiles(Collection<ArchiveFile> archiveFiles) {
        ArchiveStatus status = ArchiveStatus.MISSING;
        for (ArchiveFile archiveFile : archiveFiles) {
            if (archiveFile.isMissing()) {
                if (status == ArchiveStatus.COMPLETE) {
                    status = ArchiveStatus.PARTIAL;
                }
            } else {
                if (status == ArchiveStatus.MISSING) {
                    status = ArchiveStatus.COMPLETE;
                }
            }
        }
        return status;
    }
}
