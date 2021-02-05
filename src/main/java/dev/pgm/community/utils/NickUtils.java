package dev.pgm.community.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class NickUtils {

  private static final String RANDOM_NAME_API = "https://api.gamertag.dev/random"; // :)

  /** Fetch a random minecraft username */
  public static CompletableFuture<String> getRandomName() {
    return CompletableFuture.supplyAsync(
        () -> {
          String response = "Brokenowo12345";
          HttpURLConnection url;
          try {
            url = (HttpURLConnection) new URL(RANDOM_NAME_API).openConnection();

            url.setRequestMethod("GET");
            url.setRequestProperty("User-Agent", "Community");
            url.setInstanceFollowRedirects(true);
            url.setConnectTimeout(10000);
            url.setReadTimeout(10000);

            try (final BufferedReader br =
                new BufferedReader(
                    new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
              response = br.readLine().trim();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }

          return response;
        });
  }
}
