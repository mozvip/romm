package com.github.mozvip.romm.filetypes;

import com.github.mozvip.romm.model.ArchiveFile;
import org.springframework.data.annotation.Id;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

public class DatGame implements Serializable {

    private final boolean folder = false;
    private final @Id String path;
    private final String name;
    private final Map<String, DatFile> archiveFiles;

    public DatGame(String path) {
        this.path = path.trim();
        final int beginIndex = path.lastIndexOf(File.separatorChar);
        this.name = path.substring(beginIndex == -1 ? 0 : beginIndex+1);
        this.archiveFiles = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Map<String, DatFile> getArchiveFiles() {
        return archiveFiles;
    }


    public boolean isFolder() {
        return folder;
    }

    public void addFile(String path, DatFile f) {
        archiveFiles.put(path, f);
    }
}
