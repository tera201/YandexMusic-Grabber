package org.tera201;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
public class CookieFetcher {

    private static final String BASE_URL = "https://music.yandex.ru/";
    public static final String COOKIE_FILE = "cookies.json";

    public void fetchPlaylist() {
        WebDriver driver = new ChromeDriver();

        try {
            String url = BASE_URL + "?" +
                    "owner=" + URLEncoder.encode("tera204", StandardCharsets.UTF_8) +
                    "&kinds=" + URLEncoder.encode("3", StandardCharsets.UTF_8) +
                    "&light=" + URLEncoder.encode("true", StandardCharsets.UTF_8) +
                    "&madeFor=" + URLEncoder.encode("", StandardCharsets.UTF_8) +
                    "&withLikesCount=" + URLEncoder.encode("true", StandardCharsets.UTF_8) +
                    "&forceLogin=" + URLEncoder.encode("true", StandardCharsets.UTF_8) +
                    "&lang=" + URLEncoder.encode("ru", StandardCharsets.UTF_8) +
                    "&external-domain=" + URLEncoder.encode("music.yandex.ru", StandardCharsets.UTF_8) +
                    "&overembed=" + URLEncoder.encode("false", StandardCharsets.UTF_8) +
                    "&ncrnd=" + URLEncoder.encode("0.5240693950447917", StandardCharsets.UTF_8);

            driver.get(url);
            Thread.sleep(5000);
            saveCookies(driver);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            driver.quit();
        }
    }

    public void saveCookies(WebDriver driver) throws IOException {
        Set<Cookie> cookies = driver.manage().getCookies();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(COOKIE_FILE), cookies);
        log.info("Cookies saved to " + COOKIE_FILE);
    }

    public void loadCookies(WebDriver driver) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File cookieFile = new File(COOKIE_FILE);

        if (cookieFile.exists()) {
            Set<CookieData> cookies = mapper.readValue(cookieFile, new TypeReference<>() {});
            for (CookieData cookie : cookies) {
                driver.manage().addCookie(cookie.toSeleniumCookie());
            }
            log.info("Cookies loaded from {}", COOKIE_FILE);
        } else {
            log.info("Cookie file does not exist.");
        }
    }

    public static void main(String[] args) {
        new CookieFetcher().fetchPlaylist();
    }
}
