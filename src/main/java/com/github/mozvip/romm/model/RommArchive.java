package com.github.mozvip.romm.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;
import java.util.Objects;

@Table("ARCHIVE")
public class RommArchive {
    private @Id Long archiveId;
    private String archivePath;
    private String datPath;
    private ArchiveStatus status;
    private String contentSignatureSha1;
    @CreatedDate
    private Timestamp createdDate;
    @LastModifiedDate
    private Timestamp lastModifiedDate;

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public String getDatPath() {
        return datPath;
    }

    public void setDatPath(String datPath) {
        this.datPath = datPath;
    }

    public ArchiveStatus getStatus() {
        return status;
    }

    public void setStatus(ArchiveStatus status) {
        this.status = status;
    }

    public String getContentSignatureSha1() {
        return contentSignatureSha1;
    }

    public void setContentSignatureSha1(String contentSignatureSha1) {
        this.contentSignatureSha1 = contentSignatureSha1;
    }

    public Long getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(Long archiveId) {
        this.archiveId = archiveId;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public Timestamp getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public void setLastModifiedDate(Timestamp lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RommArchive rommArchive = (RommArchive) o;
        return Objects.equals(archiveId, rommArchive.archiveId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(archiveId);
    }
}
