package dev.pgm.community.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.pgm.community.Community;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.bukkit.Skin;

public class WebUtils {

  private static final String RANDOM_NAME_API = "https://api.gamertag.dev/random"; // :)
  private static final String USERNAME_API = "https://api.ashcon.app/mojang/v2/user/";;

  /** Fetch a random minecraft username */
  public static CompletableFuture<String> getRandomName() {
    return CompletableFuture.supplyAsync(
        () -> {
          String response = "ERROR_404";
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

  public static CompletableFuture<Skin> getSkin(String input) {
    return getProfile(input)
        .thenApplyAsync(
            profile -> {
              if (profile == null || !profile.get("textures").isJsonObject()) {
                return null;
              }
              JsonObject texture = profile.get("textures").getAsJsonObject();
              String data = texture.get("raw").getAsJsonObject().get("value").getAsString();
              String sign = texture.get("raw").getAsJsonObject().get("signature").getAsString();
              return new Skin(data, sign);
            });
  }

  public static CompletableFuture<UsernameHistory> getUsernameHistory(String input) {
    return getProfile(input).thenApplyAsync(UsernameHistory::of);
  }

  public static class UsernameHistory {
    private String username;
    private UUID playerId;
    private List<NameEntry> history;

    public static UsernameHistory of(JsonObject profile) {
      String uuid = profile.get("uuid").getAsString();
      String current = profile.get("username").getAsString();

      List<NameEntry> history = Lists.newArrayList();
      JsonArray names = profile.get("username_history").getAsJsonArray();
      names.forEach(
          name -> {
            if (name.isJsonObject() && name.getAsJsonObject().entrySet().size() > 1) {
              history.add(new NameEntry(name.getAsJsonObject()));
            }
          });
      return new UsernameHistory(current, UUID.fromString(uuid), history);
    }

    public UsernameHistory(String username, UUID playerId, List<NameEntry> history) {
      this.username = username;
      this.playerId = playerId;
      this.history = history;
    }

    public String getCurrentName() {
      return username;
    }

    public UUID getId() {
      return playerId;
    }

    public List<NameEntry> getHistory() {
      return history;
    }
  }

  public static class NameEntry {
    private String username;
    private @Nullable Instant dateChanged;

    public NameEntry(JsonObject name) {
      this.username = name.get("username").getAsString();
      String date = name.get("changed_at").getAsString();
      this.dateChanged = Instant.parse(date);
    }

    public NameEntry(String username, Instant dateChanged) {
      this.username = username;
      this.dateChanged = dateChanged;
    }

    public String getUsername() {
      return username;
    }

    @Nullable
    public Instant getDateChanged() {
      return dateChanged;
    }
  }

  /** Get profile data of provided username/uuid * */
  private static CompletableFuture<JsonObject> getProfile(String input) {
    return CompletableFuture.supplyAsync(
        () -> {
          JsonObject obj = null;
          HttpURLConnection url;
          try {
            url = (HttpURLConnection) new URL(USERNAME_API + checkNotNull(input)).openConnection();

            url.setRequestMethod("GET");
            url.setRequestProperty("User-Agent", "Community");
            url.setRequestProperty("Accept", "application/json");
            url.setInstanceFollowRedirects(true);
            url.setConnectTimeout(10000);
            url.setReadTimeout(10000);

            StringBuilder data = new StringBuilder();
            try (final BufferedReader br =
                new BufferedReader(
                    new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
              String line;
              while ((line = br.readLine()) != null) {
                data.append(line.trim());
              }
              obj = new Gson().fromJson(data.toString(), JsonObject.class);
            }
          } catch (IOException e) {
            Community.log("%s", e.getMessage());
          }
          return obj;
        });
  }
}
