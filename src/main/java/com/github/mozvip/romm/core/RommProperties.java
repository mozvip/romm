package com.github.mozvip.romm.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Set;

@ConfigurationProperties(prefix = "romm")
public class RommProperties {
    private Set<String> deleteExtensions;
    private Path datFolder;
    private Path inputFolder;
    private Path outputFolder;
    private Path unknownFolder;

    public Set<String> getDeleteExtensions() {
        return deleteExtensions;
    }

    public void setDeleteExtensions(Set<String> deleteExtensions) {
        this.deleteExtensions = deleteExtensions;
    }

    public Path getDatFolder() {
        return datFolder;
    }

    public void setDatFolder(Path datFolder) {
        this.datFolder = datFolder;
    }

    public Path getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(Path inputFolder) {
        this.inputFolder = inputFolder;
    }

    public Path getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(Path outputFolder) {
        this.outputFolder = outputFolder;
    }

    public Path getUnknownFolder() {
        return unknownFolder;
    }

    public void setUnknownFolder(Path unknownFolder) {
        this.unknownFolder = unknownFolder;
    }
}
