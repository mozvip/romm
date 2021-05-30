package com.github.mozvip.romm.filetypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dat implements Serializable {

    public static final Logger log = LoggerFactory.getLogger(Dat.class);

    private String name;
    private final List<DatGame> games;
    private final Map<String, DatGame> gameMap;

    public Dat() {
        this.games = new ArrayList<>();
        gameMap = new HashMap<>();
    }

    public List<DatGame> getGames() {
        return games;
    }

    public void addGame(DatGame game) {
        final String gameId = game.getRelativePath().toString();
        if (gameMap.containsKey(gameId)) {
            log.error("Dat file {} contains duplicate game {}", this.name, gameId);
        } else {
            games.add(game);
            gameMap.put(gameId, game);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Dat read(Path file) throws IOException {
        Dat result = new Dat();

        try (BufferedReader r = Files.newBufferedReader(file)) {
            DatGame currentGame = null;
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.equals("")) {
                    continue;
                }
                if (line.startsWith("name ")) {
                    String name;
                    if (line.indexOf('"') >= 0) {
                        name = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
                    } else {
                        name = line.substring(line.indexOf(" ")+1);
                    }
                    if (currentGame != null) {
                        result.addGame(currentGame);
                    }
                    if (name.endsWith(".zip")) {
                        name = name.substring(0, name.lastIndexOf('.'));
                    }
                    currentGame = new DatGame(null, name);
                } else if (line.startsWith("file ") || line.startsWith("rom ")) {
                    assert currentGame != null;
                    String fileData = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')')).trim();
                    DatFile f = new DatFile();

                    final Matcher matcher = Pattern.compile("name (.*) size (\\d+) (.*)").matcher(fileData);
                    if (matcher.matches()) {
                        f.setName(matcher.group(1));
                        f.setSize(Long.parseLong(matcher.group(2)));
                        fileData = matcher.group(3);
                    } else {
                        log.error("Unable to correctly parse line {} in dat file {}", line, file);
                        continue;
                    }

                    final String[] split = fileData.split("\\s+");
                    String currentVariable = null;
                    String dateOnly = null;
                    for (int i = 0; i < split.length; i ++) {
                        String currentToken = split[i];
                        switch (currentToken) {
                            case "date":
                                dateOnly = null;
                            case "crc":
                            case "sha1":
                                currentVariable = currentToken;
                                break;
                            default:
                                if (currentVariable == null) {
                                    log.error("Unable to correctly parse line {} in dat file {}", line, file);
                                } else {
                                    switch (currentVariable) {
                                        case "date":
                                            if (dateOnly == null) {
                                                dateOnly = currentToken;
                                            } else {
                                                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                                                f.setDate(LocalDateTime.parse(dateOnly + " " + currentToken, df));
                                            }
                                            break;
                                        case "crc":
                                            f.setCrc(Long.decode("0x" + currentToken));
                                            break;
                                        case "sha1":
                                            f.setSha1(currentToken);
                                            break;
                                    }
                                }
                                break;
                        }
                    }
                    currentGame.addFile(f.getName(), f);
                }
            }
        }

        return result;
    }

}