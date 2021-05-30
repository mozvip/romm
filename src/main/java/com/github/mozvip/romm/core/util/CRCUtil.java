package com.github.mozvip.romm.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public class CRCUtil {

    private static final int BUF_SIZE = 1 << 14; //16384

    public static long computeFileCrc(InputStream inputStream) throws IOException {

        byte[] buff = new byte[BUF_SIZE];
        CRC32 crc32 = new CRC32();

        int readLen;
        while ((readLen = inputStream.read(buff)) != -1) {
            crc32.update(buff, 0, readLen);
        }
        return crc32.getValue();
    }

    public static long computeFileCrc(byte[] bytes) throws IOException {
        CRC32 crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    public static long computeFileCrc(File inputFile) throws IOException {
        try(InputStream inputStream = new FileInputStream(inputFile)) {
            return computeFileCrc(inputStream);
        }
    }


}
