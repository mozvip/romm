package com.github.mozvip.romm.model;

import org.springframework.data.annotation.Id;

import java.util.Objects;

public class OutputFolder {
    private @Id Long outputFolderId;
    private String relativePath;
    private java.sql.Timestamp scannedDate;

    public OutputFolder(String relativePath) {
        this.relativePath = relativePath;
    }

    public Long getOutputFolderId() {
        return outputFolderId;
    }

    public void setOutputFolderId(Long outputFolderId) {
        this.outputFolderId = outputFolderId;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public java.sql.Timestamp getScannedDate() {
        return scannedDate;
    }

    public void setScannedDate(java.sql.Timestamp scannedDate) {
        this.scannedDate = scannedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutputFolder that = (OutputFolder) o;
        return outputFolderId.equals(that.outputFolderId) && relativePath.equals(that.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outputFolderId, relativePath);
    }
}
