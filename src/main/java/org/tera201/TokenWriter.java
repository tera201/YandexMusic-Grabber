package org.tera201;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TokenWriter {

    private static WebDriver driver;
    private static String token;
    private static ChromeOptions options;
    private static final int sleepTime = 1000;
    private static int timeCounter = 120;
    private static final String TOKEN_FILEPATH = "token.txt";
    private static final String OAUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id=" +
            "23cabbbdc6cd418abb4b39c32c41195d";

    private static void settingDriver() {
        Map<String, Object> loggingPrefs = new HashMap<>();
        loggingPrefs.put("performance", "ALL");
        options.setCapability("goog:loggingPrefs", loggingPrefs);
    }

    public static void main(String[] args) {
        if (!checkWriteToken()) {
            options = new ChromeOptions();
            settingDriver();
            driver = new ChromeDriver(options);
            openUrl();
            getToken();
        }
    }

    private static void openUrl() {
        driver.get(OAUTH_URL);
    }

    private static void getToken() {
        ObjectMapper objectMapper = new ObjectMapper();
        while (token == null && isActive()) {
            checkTime();
            sleep();
            List<LogEntry> logsRaw = getPerformanceLog();
            for (LogEntry logEntry : logsRaw) {
                try {
                    JsonNode logJson = objectMapper.readTree(logEntry.getMessage());
                    JsonNode message = logJson.get("message");

                    if (message.has("params")) {
                        JsonNode params = message.get("params");
                        if (params.has("frame")) {
                            JsonNode frame = params.get("frame");
                            if (frame.has("urlFragment")) {
                                String urlFragment = frame.get("urlFragment").asText();
                                token = urlFragment.split("&")[0].split("=")[1];
                                log.info(token);
                                writeToken();
                                closeDriver();
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    private static boolean checkWriteToken() {
        Path path = Paths.get(TOKEN_FILEPATH);
        if (Files.exists(path)) {
            try {
                String content = Files.readString(path);
                if (!content.isEmpty()) {
                    log.info("Token already exist {}", TOKEN_FILEPATH);
                } else {
                    log.info("File {} is existed, but it is empty.", TOKEN_FILEPATH);
                }
                return true;
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        } else {
            log.info("File {} not found.", TOKEN_FILEPATH);
        }
        return false;
    }

    private static void writeToken() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TOKEN_FILEPATH))) {
            if (token != null) {
                writer.write(token);
            } else {
                log.info("Токен не был найден.");
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private static List<LogEntry> getPerformanceLog() {
        try {
            return driver.manage().logs().get("performance").getAll();
        } catch (Exception e) {
            log.error(e.getMessage());
            return List.of();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(e.getMessage());
        }
    }

    private static void checkTime() {
        if (timeCounter-- <= 0) {
            log.info("Time out");
            closeDriver();
            System.exit(0);
        }
    }

    private static boolean isActive() {
        try {
            ((RemoteWebDriver) driver).manage().getCookies();
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    private static void closeDriver() {
        if (driver != null) {
            driver.close();
            driver.quit();
        }
    }
}
