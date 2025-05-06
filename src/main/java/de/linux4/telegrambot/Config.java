package de.linux4.telegrambot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Config {

    @JsonProperty("BotToken")
    public String botToken;
    @JsonProperty("OwnerUserName")
    public String ownerUserName;

    @JsonProperty("MariaDBHost")
    public String mariaDbHost;
    @JsonProperty("MariaDBPort")
    public int mariaDbPort;
    @JsonProperty("MariaDBDatabase")
    public String mariaDbDatabase;
    @JsonProperty("MariaDBUserName")
    public String mariaDbUserName;
    @JsonProperty("MariaDBPassword")
    public String mariaDbPassword;

    @JsonProperty("ChatGPTAccessToken")
    public String chatGptAccessToken;

    public static Config loadFromFile(File file) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append('\n');
            }
        }

        return new ObjectMapper().readValue(contentBuilder.toString(), Config.class);
    }

}
