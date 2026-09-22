package dev.pgm.community.utils;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.pgm.community.Community;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import tc.oc.pgm.util.skin.Skin;

public class WebUtils {

  private static String RANDOM_NAME_API = "https://random.pgm.fyi/random";
  private static String USERNAME_API = "https://api.ashcon.app/mojang/v2/user/";

  public static void setRandomNameAPI(String address) {
    RANDOM_NAME_API = address;
  }

  public static void setUsernameAPI(String address) {
    USERNAME_API = address;
  }

  /** Fetch and validate a batch of random minecraft usernames. */
  public static CompletableFuture<List<String>> getRandomNameList(int size) {
    return CompletableFuture.supplyAsync(() -> {
      HttpURLConnection url = null;
      try {
        String batchAPI = RANDOM_NAME_API.replaceFirst("/+$", "") + "/batch?count=" + size;
        url = (HttpURLConnection) new URI(batchAPI).toURL().openConnection();

        url.setRequestMethod("GET");
        url.setRequestProperty("User-Agent", "Community");
        url.setInstanceFollowRedirects(true);
        url.setConnectTimeout(10000);
        url.setReadTimeout(10000);

        int status = url.getResponseCode();
        if (status < 200 || status >= 300) {
          throw new IOException("Random name batch API returned HTTP " + status);
        }

        try (final BufferedReader br = new BufferedReader(
            new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
          JsonArray response = new Gson().fromJson(br, JsonArray.class);
          if (response == null || response.size() != size) {
            throw new IOException("Random name batch API must return exactly " + size + " names");
          }

          List<String> names = new ArrayList<>(size);
          Set<String> uniqueNames = new HashSet<>(size);
          for (JsonElement element : response) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
              throw new IOException("Random name batch API returned a non-string value");
            }

            String name = validateRandomName(element.getAsString(), "Random name batch API");
            if (!uniqueNames.add(name.toLowerCase(Locale.ROOT))) {
              throw new IOException("Random name batch API returned duplicate names");
            }
            names.add(name);
          }
          return names;
        }
      } catch (IOException | URISyntaxException | RuntimeException e) {
        throw new CompletionException("Unable to fetch random nicknames", e);
      } finally {
        if (url != null) url.disconnect();
      }
    });
  }

  /** Fetch a random minecraft username */
  public static CompletableFuture<String> getRandomName() {
    return CompletableFuture.supplyAsync(() -> {
      HttpURLConnection url = null;
      try {
        url = (HttpURLConnection) new URI(RANDOM_NAME_API).toURL().openConnection();

        url.setRequestMethod("GET");
        url.setRequestProperty("User-Agent", "Community");
        url.setInstanceFollowRedirects(true);
        url.setConnectTimeout(10000);
        url.setReadTimeout(10000);

        int status = url.getResponseCode();
        if (status < 200 || status >= 300) {
          throw new IOException("Random name API returned HTTP " + status);
        }

        try (final BufferedReader br = new BufferedReader(
            new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
          String response = br.readLine();
          if (response == null || response.isBlank()) {
            throw new IOException("Random name API returned an empty response");
          }
          return validateRandomName(response.trim(), "Random name API");
        }
      } catch (IOException | URISyntaxException e) {
        throw new CompletionException("Unable to fetch a random nickname", e);
      } finally {
        if (url != null) url.disconnect();
      }
    });
  }

  private static String validateRandomName(String name, String source) throws IOException {
    if (!NameUtils.isMinecraftName(name)) {
      throw new IOException(source + " returned an invalid Minecraft name");
    }
    return name;
  }

  public static CompletableFuture<Skin> getSkin(String input) {
    return getProfile(input).thenApplyAsync(profile -> {
      if (profile == null || !profile.get("textures").isJsonObject()) {
        return null;
      }
      JsonObject texture = profile.get("textures").getAsJsonObject();
      String data = texture.get("raw").getAsJsonObject().get("value").getAsString();
      String sign = texture.get("raw").getAsJsonObject().get("signature").getAsString();
      return new Skin(data, sign);
    });
  }

  /** Get profile data of provided username/uuid * */
  private static CompletableFuture<JsonObject> getProfile(String input) {
    return CompletableFuture.supplyAsync(() -> {
      JsonObject obj = null;
      HttpURLConnection url;
      try {
        url = (HttpURLConnection)
            new URI(USERNAME_API + assertNotNull(input)).toURL().openConnection();

        url.setRequestMethod("GET");
        url.setRequestProperty("User-Agent", "Community");
        url.setRequestProperty("Accept", "application/json");
        url.setInstanceFollowRedirects(true);
        url.setConnectTimeout(10000);
        url.setReadTimeout(10000);

        StringBuilder data = new StringBuilder();
        try (final BufferedReader br = new BufferedReader(
            new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
          String line;
          while ((line = br.readLine()) != null) {
            data.append(line.trim());
          }
          obj = new Gson().fromJson(data.toString(), JsonObject.class);
        }
      } catch (IOException | URISyntaxException e) {
        Community.log("%s", e.getMessage());
      }
      return obj;
    });
  }
}
