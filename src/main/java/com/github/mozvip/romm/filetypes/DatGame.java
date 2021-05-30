package com.github.mozvip.romm.filetypes;

import org.springframework.data.annotation.Id;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DatGame implements Serializable {

    private final boolean folder = false;
    private final @Id String path;
    private final String list;
    private final String name;
    private final Map<String, DatFile> archiveFiles;

    public DatGame(String list, String path) {
        this.list = list;
        this.path = path.trim();
        final int beginIndex = path.lastIndexOf(File.separatorChar);
        this.name = path.substring(beginIndex == -1 ? 0 : beginIndex+1);
        this.archiveFiles = new HashMap<>();
    }

    public String getList() {
        return list;
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

    public Path getRelativePath() {
        return list != null ? Paths.get(list).resolve(path) : Paths.get(path);
    }
}
