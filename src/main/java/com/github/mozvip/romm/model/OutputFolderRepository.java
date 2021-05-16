package com.github.mozvip.romm.model;

import com.github.mozvip.romm.model.OutputFolder;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OutputFolderRepository extends CrudRepository<OutputFolder, Long> {

    Optional<OutputFolder> findByRelativePath(String relativePath);

    @Modifying
    @Query("update OUTPUT_FOLDER set SCANNED_DATE = :scannedDate where RELATIVE_PATH = :relativePath")
    void updateScannedDate(@Param(value = "relativePath") String relativePath, @Param(value = "scannedDate") LocalDateTime scannedDate);

}
