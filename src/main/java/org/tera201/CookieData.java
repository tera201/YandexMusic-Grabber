package org.tera201;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.openqa.selenium.Cookie;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CookieData {
    private String name;
    private String value;
    private String domain;
    private String path;
    private Long expiry;
    private String sameSite;
    private boolean isSecure;
    private boolean isHttpOnly;

    public CookieData(Cookie cookie) {
        this.name = cookie.getName();
        this.value = cookie.getValue();
        this.domain = cookie.getDomain();
        this.path = cookie.getPath();
        this.expiry = cookie.getExpiry() != null ? cookie.getExpiry().getTime() / 1000 : null;
        this.isSecure = cookie.isSecure();
        this.isHttpOnly = cookie.isHttpOnly();
        this.sameSite = cookie.getSameSite();
    }

    public CookieData() {
    }

    public Cookie toSeleniumCookie() {
        return new Cookie.Builder(this.name, this.value)
                .domain(this.domain)
                .path(this.path)
                .expiresOn(this.expiry != null ? new java.util.Date(this.expiry * 1000) : null)
                .isSecure(this.isSecure)
                .isHttpOnly(this.isHttpOnly)
                .sameSite(this.sameSite)
                .build();
    }

    public io.restassured.http.Cookie toRestAssuredCookie() {
        if (expiry == null)
            return new io.restassured.http.Cookie.Builder(this.name, this.value)
                .setDomain(this.domain)
                .setPath(this.path)
                .setSecured(this.isSecure)
                .setHttpOnly(this.isHttpOnly)
                .setSameSite(this.sameSite)
                .build();
        else  return new io.restassured.http.Cookie.Builder(this.name, this.value)
                .setDomain(this.domain)
                .setPath(this.path)
                .setExpiryDate(this.expiry != null ? new java.util.Date(this.expiry * 1000) : null)
                .setSecured(this.isSecure)
                .setHttpOnly(this.isHttpOnly)
                .setSameSite(this.sameSite)
                .build();
    }
}

