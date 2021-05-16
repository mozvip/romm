package com.github.mozvip.romm.model;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface ArchiveFileRepository extends CrudRepository<ArchiveFile, Long> {

    @Query("SELECT MAX(CREATED_DATE) FROM ARCHIVE_FILE WHERE DAT_PATH=:datPath")
    LocalDateTime findMaxCreatedDateByDatPath(@Param("datPath") String datPath);

    @Query("SELECT DISTINCT DAT_PATH FROM ARCHIVE_FILE")
    Set<String> findDistinctDatPath();

    List<ArchiveFile> findByArchivePath(String pathWithoutExtension);

    Collection<ArchiveFile> findByDatPathAndMissingIsFalse(String datPath);

    @Query("UPDATE ARCHIVE_FILE SET MISSING=false, LAST_MODIFIED_DATE = NOW() WHERE ARCHIVE_FILE_ID IN (:ids)")
    @Modifying
    void setNotMissing(@Param("ids") Set<Long> ids);

    @Query("UPDATE ARCHIVE_FILE SET MISSING=false, LAST_MODIFIED_DATE = NOW() WHERE ARCHIVE_FILE_ID=:id")
    @Modifying
    void setNotMissing(@Param("id") Long id);

    List<ArchiveFile> findBySizeAndCrc32(long uncompressedSize, long crc);

    Collection<ArchiveFile> findBySha1(String sha1);

    @Query("delete FROM ARCHIVE_FILE WHERE DAT_PATH=:datPath")
    @Modifying
    Long deleteByDatPath(@Param("datPath") String datPath);

    Collection<ArchiveFile> findBySha1AndMissingIsTrue(String sha1);
    List<ArchiveFile> findBySha1AndMissingIsFalse(String sha1);

    void deleteByDatPathAndMissingIsTrue(String removedDatFile);

    Collection<ArchiveFile> findBySizeAndCrc32AndMissingIsTrue(long uncompressedSize, long crc);
    List<ArchiveFile> findBySizeAndCrc32AndMissingIsFalse(long uncompressedSize, long crc);

    @Query("SELECT * FROM ARCHIVE_FILE WHERE MISSING=TRUE AND (CRC32, SIZE) IN (SELECT CRC32, SIZE FROM ARCHIVE_FILE WHERE MISSING = FALSE)")
    List<ArchiveFile> findMissingFromCollectionByCrc32AndSize();

    @Query("SELECT * FROM ARCHIVE_FILE WHERE MISSING=TRUE AND SHA1 IN (SELECT SHA1 FROM ARCHIVE_FILE WHERE MISSING = FALSE)")
    List<ArchiveFile> findMissingFromCollectionBySha1();

    @Query("UPDATE ARCHIVE_FILE SET MISSING=false, LAST_MODIFIED_DATE = NOW() WHERE ARCHIVE_PATH=:archivePath")
    @Modifying
    void setNotMissing(@Param("archivePath") String archivePath);

    long countByMissingIsFalse();

    Collection<ArchiveFile> findByDatPathAndMissingIsTrue(String datPath);

    Collection<ArchiveFile> findByLastModifiedDateAfter(Date startTime);

    @Query("UPDATE ARCHIVE_FILE SET MISSING=true, LAST_MODIFIED_DATE = NOW() WHERE ARCHIVE_FILE_ID=:id")
    @Modifying
    void setMissing(@Param("id") Long id);
}
