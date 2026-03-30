package dev.pgm.community.utils;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.pgm.community.Community;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import tc.oc.pgm.util.skin.Skin;

public class WebUtils {

  // A big thanks to @Electroid for all these awesome APIs :)
  private static String RANDOM_NAME_API = "https://api.gamertag.dev/random";
  private static String USERNAME_API = "https://api.ashcon.app/mojang/v2/user/";

  public static void setRandomNameAPI(String address) {
    RANDOM_NAME_API = address;
  }

  public static void setUsernameAPI(String address) {
    USERNAME_API = address;
  }

  /** Fetch a list of random minecraft usernames */
  public static CompletableFuture<List<String>> getRandomNameList(int size) {
    return CompletableFuture.supplyAsync(() -> {
      List<String> names = Lists.newArrayList();
      for (int i = 0; i < size; i++) {
        names.add(getRandomName().join());
      }
      return names;
    });
  }

  /** Fetch a random minecraft username */
  public static CompletableFuture<String> getRandomName() {
    return CompletableFuture.supplyAsync(() -> {
      String response = "ERROR_404";
      HttpURLConnection url;
      try {
        url = (HttpURLConnection) new URI(RANDOM_NAME_API).toURL().openConnection();

        url.setRequestMethod("GET");
        url.setRequestProperty("User-Agent", "Community");
        url.setInstanceFollowRedirects(true);
        url.setConnectTimeout(10000);
        url.setReadTimeout(10000);

        try (final BufferedReader br = new BufferedReader(
            new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
          response = br.readLine().trim();
        }
      } catch (IOException | URISyntaxException e) {
        e.printStackTrace();
      }

      return response;
    });
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
