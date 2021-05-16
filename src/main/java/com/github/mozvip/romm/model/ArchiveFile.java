package com.github.mozvip.romm.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.sql.Timestamp;
import java.util.Objects;

public class ArchiveFile {
    private @Id Long archiveFileId;
    private String datPath;
    private String archivePath;
    private boolean folder;
    private String filePath;
    private Long size;
    private Long crc32;
    private String md5;
    private String sha1;
    private boolean missing = true;
    @CreatedDate
    private Timestamp createdDate;
    @LastModifiedDate
    private Timestamp lastModifiedDate;

    public Long getArchiveFileId() {
        return archiveFileId;
    }

    public void setArchiveFileId(Long archiveFileId) {
        this.archiveFileId = archiveFileId;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getCrc32() {
        return crc32;
    }

    public void setCrc32(Long crc32) {
        this.crc32 = crc32;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getDatPath() {
        return datPath;
    }

    public void setDatPath(String datPath) {
        this.datPath = datPath;
    }

    public boolean isMissing() {
        return missing;
    }

    public void setMissing(boolean missing) {
        this.missing = missing;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public Timestamp getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Timestamp lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArchiveFile that = (ArchiveFile) o;
        return archiveFileId.equals(that.archiveFileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(archiveFileId);
    }
}
