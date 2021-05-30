package com.github.mozvip.romm.filetypes;

import com.github.mozvip.romm.model.ArchiveFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class XmlDat extends Dat {

    public static final Logger LOGGER = LoggerFactory.getLogger(XmlDat.class);

    private static InputStream checkForUtf8BOM(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), 3);
        byte[] bom = new byte[3];
        if (pushbackInputStream.read(bom) != -1) {
            if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
                pushbackInputStream.unread(bom);
            }
        }
        return pushbackInputStream;
    }

    public static XmlDat read(Path file) throws IOException {

        XmlDat dat = null;

        XMLInputFactory xmlif = XMLInputFactory.newInstance();

        xmlif.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE);
        xmlif.setProperty("javax.xml.stream.isReplacingEntityReferences", Boolean.TRUE);

        int eventType;
        try {
            try (final InputStream inputStream = Files.newInputStream(file)) {
                XMLStreamReader xmlsr = xmlif.createXMLStreamReader(checkForUtf8BOM(inputStream));

                DatGame currentArchive = null;
                DatFile currentArchiveFile;
                String currentSoftwareList = null;

                while (xmlsr.hasNext()) {
                    eventType = xmlsr.next();
                    if (eventType == XMLStreamConstants.START_ELEMENT) {
                        final String element = xmlsr.getName().toString();
                        switch (element) {
                            case "softwarelists":
                            case "datafile":
                            case "mame":
                                dat = new XmlDat();
                                break;
                            case "softwarelist":
                                currentSoftwareList = xmlsr.getAttributeValue(null, "name");
                                break;
                            case "name":
                                dat.setName(xmlsr.getElementText());
                                break;
                            case "machine":
                            case "software":
                            case "game":
                                String name = xmlsr.getAttributeValue(null, "name");
                                String cloneof = xmlsr.getAttributeValue(null, "cloneof");
                                String romof = xmlsr.getAttributeValue(null, "romof");

                                currentArchive = new DatGame(currentSoftwareList, name);
                                dat.addGame(currentArchive);
                                break;
                            case "rom":
                            case "disk":

                                String status = xmlsr.getAttributeValue(null, "status");
                                if ("nodump".equals(status)) {
                                    break;
                                }

                                String merge = xmlsr.getAttributeValue(null, "merge");
                                if (merge != null) {
                                    // split sets : we do not add merged roms to clone sets
                                    break;
                                }

                                currentArchiveFile = new DatFile();
                                currentArchiveFile.setDisk(element.equals("disk"));

                                name = xmlsr.getAttributeValue(null, "name");

                                currentArchiveFile.setName(currentArchiveFile.isDisk() ? name + ".chd" : name);

                                final String sizeStr = xmlsr.getAttributeValue(null, "size");
                                if (sizeStr != null && !sizeStr.trim().equals("")) {
                                    long size = Long.parseLong(sizeStr);
                                    currentArchiveFile.setSize(size);
                                    if (size > 0) {
                                        final String crcStr = xmlsr.getAttributeValue(null, "crc");
                                        if (crcStr != null && !crcStr.trim().equals("")) {
                                            try {
                                                currentArchiveFile.setCrc(Long.decode("0x" + crcStr));
                                            } catch (NumberFormatException e) {
                                                LOGGER.error(e.getMessage(), e);
                                            }
                                        }
                                    }
                                }

                                String sha1 = xmlsr.getAttributeValue(null, "sha1");
                                currentArchiveFile.setSha1(sha1);

                                String md5 = xmlsr.getAttributeValue(null, "md5");
                                currentArchiveFile.setMd5(md5);

                                currentArchive.addFile(name, currentArchiveFile);

                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }

        return dat;
    }
    
}

