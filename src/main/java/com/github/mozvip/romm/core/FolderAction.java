package com.github.mozvip.romm.core;

import java.nio.file.Path;
import java.sql.SQLException;

public interface FolderAction {
    void run(Path folder) throws Exception;
}
