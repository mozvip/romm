package com.github.mozvip.romm.core;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamBuilder {
    InputStream build() throws IOException, RommMissingFileException;
}
