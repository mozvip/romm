package com.github.mozvip.romm.model;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface RommArchiveRepository extends CrudRepository<RommArchive, Long> {

    @Query("delete FROM ARCHIVE WHERE DAT_PATH=:datPath")
    @Modifying
    Long deleteByDatPath(@Param("datPath") String datPath);

    RommArchive findByArchivePath(String archivePath);

    @Query("SELECT ARCHIVE_PATH FROM ARCHIVE")
    Set<String> findDistinctArchivePath();

    @Query("update ARCHIVE SET STATUS =:newStatus, LAST_MODIFIED_DATE = NOW() WHERE ARCHIVE_PATH=:archivePath")
    @Modifying
    void updateStatus(@Param("archivePath") String archivePath, @Param("newStatus") String status);

    List<RommArchive> findByContentSignatureSha1(String contentSignatureSha1);

    List<RommArchive> findByContentSignatureSha1AndArchivePathNotAndStatus(String contentSignatureSha1, String archivePath, String status);

    List<RommArchive> findByDatPath(String datPath);

    @Query("SELECT * FROM ARCHIVE WHERE STATUS<>'COMPLETE' AND CONTENT_SIGNATURE_SHA1 IN (SELECT CONTENT_SIGNATURE_SHA1 FROM ARCHIVE WHERE STATUS = 'COMPLETE')")
    List<RommArchive> findMissingFromCollectionBySignature();


    @Query("SELECT DISTINCT DAT_PATH FROM ARCHIVE")
    Set<String> findDistinctDatPath();
}
