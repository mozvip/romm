package com.github.mozvip.romm.core;

import java.io.IOException;

public class RommMissingFileException extends IOException {
    public RommMissingFileException(String message) {
        super(message);
    }
}
