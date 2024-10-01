package org.tera201;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Cookies;
import io.restassured.response.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.tera201.FileUtil.*;
import static org.tera201.CookieFetcher.COOKIE_FILE;

public class YandexMusicClient {

    private static final Logger logger = LoggerFactory.getLogger(YandexMusicClient.class);
    private static final String LIBRARY_URL = "https://music.yandex.ru/handlers/library.jsx";
    private static final String REFERER_URL = "https://music.yandex.ru/users/%s/playlists";
    private static final String PLAYLIST_URL = "https://music.yandex.ru/handlers/playlist.jsx";
    private static final String SONG_URL = "https://music.yandex.ru/handlers/track.jsx";
    private static final String DOMAIN = "music.yandex.ru";
    private final String authToken;
    private final String user;
    private final ObjectMapper objectMapper;
    private final Cookies cookies;

    public YandexMusicClient(String username) throws IOException {
        this.objectMapper = new ObjectMapper();
        authToken = Files.lines(Path.of("token.txt")).findFirst().get();
        cookies = loadCookies();
        user = username;
    }

    public RequestSpecification baseRequest(String url) {
        return RestAssured
                .given()
                .baseUri(url)
                .queryParam("lang", "ru")
                .queryParam("external-domain", DOMAIN)
                .queryParam("overembed", "false")
                .contentType(ContentType.URLENC)
                .cookies(cookies);
    }

    public void fetchLibrary() {
        try {
            Response response = baseRequest(LIBRARY_URL)
                    .queryParam("owner", user)
                    .queryParam("filter", "playlists")
                    .queryParam("likeFilter", "all")
                    .queryParam("ncrnd", "0.17447934315877878")
                    .header("Authorization", "OAuth " + authToken)
                    .header("Referer", String.format(REFERER_URL, user))
                    .header("X-Retpath-Y", String.format(REFERER_URL, user))
                    .get();

            if (response.getStatusCode() == 200) {
                String jsonResponse = response.getBody().asString();
                processLibraryResponse(jsonResponse);
            } else {
                logger.info("Error while requesting: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public void fetchPlaylist(String id) throws Exception {
        Response response = baseRequest(PLAYLIST_URL)
                .queryParam("owner", user)
                .queryParam("kinds", id)
                .queryParam("light", "true")
                .queryParam("madeFor", "")
                .queryParam("withLikesCount", "true")
                .queryParam("forceLogin", "true")
                .queryParam("ncrnd", "0.5240693950447917")
                .header("Authorization", "OAuth " + authToken)
                .get();

        if (response.getStatusCode() == 200) {
            String jsonResponse = response.getBody().asString();
            processPlaylistResponse(jsonResponse);
        } else {
            logger.info("Error while requesting: {}", response.getStatusCode());
        }
    }

    public void fetchTrack(String trackId, File outputFile) throws IOException {
        Response response = baseRequest(SONG_URL)
                .queryParam("track", trackId)
                .get();

        if (response.getStatusCode() == 200) {
            String jsonResponse = response.getBody().asString();
            processTrackResponse(jsonResponse, outputFile);
        } else {
            logger.info("Error while requesting: {}", response.getStatusCode());
        }
    }

    private void processLibraryResponse(String jsonResponse) throws Exception {
        JsonNode playlistIds = objectMapper.readTree(jsonResponse).get("playlistIds");
        if (playlistIds.isArray()) {
            for (JsonNode playlistId : playlistIds) {
                String id = playlistId.asText();
                fetchPlaylist(id);
            }
        } else {
            logger.info("Collection not found.");
        }
    }

    private void processPlaylistResponse(String jsonResponse) throws Exception {
        JsonNode playlist = objectMapper.readTree(jsonResponse).get("playlist");
        if (playlist != null) {
            String playlistName = "playlist_" + playlist.get("title").asText() + ".txt";
            File outputFile = new File(playlistName);
            JsonNode trackIds = playlist.get("trackIds");
            if (trackIds.isArray()) {
                List<String> ids = StreamSupport.stream(trackIds.spliterator(), false)
                        .map(JsonNode::asText).map(it -> (it.contains(":"))? it.substring(0, it.lastIndexOf(":")) : it)
                        .filter(id -> !FileUtil.lineInFileStartsWith(outputFile, id)).toList();
                for (String id : ids) {
                    retryFetch(track -> fetchTrack(track, outputFile), id);
                }
            }
        } else {
            logger.info("Playlist not found.");
        }
    }

    private void processTrackResponse(String jsonResponse, File outputFile) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode track = rootNode.get("track");
        if (track != null) {
            String id = track.get("id").asText();
            String trackTitle =  track.get("title").asText();
            StringBuilder trackInfo = new StringBuilder(id + ": " + trackTitle + " - ");
            StreamSupport.stream(track.get("artists").spliterator(), false)
                    .map(it -> it.get("name").asText()).forEach(it -> trackInfo.append(it).append(" "));
            if (!isLineInFile(outputFile, trackInfo.toString())) {
                appendLineToFile(outputFile, trackInfo.toString());
                logger.info("Written: {}", trackInfo);
            } else {
                logger.info("Track already exists in file: {}", trackInfo);
            }

        } else {
            logger.info("Track not found.");
        }
    }

    private void retryFetch(ThrowConsumer<String> fetch, String param) throws Exception {
        for (int i = 0; i < 10; i++) {
            try {
                fetch.accept(param);
                break;
            } catch (JsonProcessingException e) {
                Thread.sleep(500);
                logger.info("Attempt {}", i);
            }
        }
    }

    public Cookies loadCookies() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File cookieFile = new File(COOKIE_FILE);
        if (cookieFile.exists()) {
            Set<CookieData> cookieSet = mapper.readValue(cookieFile, new TypeReference<>() {});
            return new Cookies(cookieSet.stream().map(CookieData::toRestAssuredCookie).toList());
        } else {
            logger.info("Cookie file does not exist.");
        }
        return new Cookies();
    }

    public static void main(String[] args) throws IOException {
        YandexMusicClient client = new YandexMusicClient("login");
        client.fetchLibrary();
    }
}