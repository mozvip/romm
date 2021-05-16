package com.github.mozvip.romm.filetypes;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XmlDatTest {

    @Test
    public void loadTest() throws URISyntaxException, IOException {
        final URL resource = this.getClass().getClassLoader().getResource("xmldat.dat");
        final XmlDat xmlDat = XmlDat.read(Paths.get(resource.toURI()));
        assertEquals(382, xmlDat.getGames().size());
    }

    @Test
    public void loadTest2() throws URISyntaxException, IOException {
        final URL resource = this.getClass().getClassLoader().getResource("xmldat2.dat");
        final XmlDat xmlDat = XmlDat.read(Paths.get(resource.toURI()));
        assertEquals(21, xmlDat.getGames().size());
    }
}