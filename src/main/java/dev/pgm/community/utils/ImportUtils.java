package dev.pgm.community.utils;

import co.aikar.commands.InvalidCommandArgument;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

public class ImportUtils {

  private static final Gson GSON = new Gson();

  public static List<BukkitBanEntry> getBukkitBans() throws InvalidCommandArgument {
    File file = new File("banned-players.json");
    if (file.exists()) {
      try {
        return Arrays.asList(
            GSON.fromJson(Files.newReader(file, Charset.defaultCharset()), BukkitBanEntry[].class));
      } catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
        e.printStackTrace();
        throw new InvalidCommandArgument(e.getMessage(), false);
      }
    } else {
      throw new InvalidCommandArgument("No banned-players.json file was found!", false);
    }
  }

  public static class BukkitBanEntry {

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    private UUID uuid;
    private String name;
    private String source;
    private String created;
    private String expires;
    private String reason;

    public UUID getUUID() {
      return uuid;
    }

    public String getName() {
      return name;
    }

    public String getSource() {
      return source;
    }

    public Instant getCreated() {
      return convertDateStr(created);
    }

    public @Nullable Duration getDuration() {
      if (expires.equalsIgnoreCase("forever")) {
        return null;
      }

      Instant startTime = getCreated();
      Instant endTime = convertDateStr(expires);

      if (endTime != null) {
        return Duration.between(startTime, endTime);
      }

      return null;
    }

    public String getReason() {
      return reason;
    }

    private @Nullable Instant convertDateStr(String date) {
      Instant time = null;
      try {
        Date parsedDate = DATE_FORMAT.parse(date);
        if (parsedDate != null) {
          time = parsedDate.toInstant();
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
      return time;
    }

    public Punishment toPunishment(ModerationConfig config, UsersFeature users) {
      return Punishment.of(
          UUID.randomUUID(),
          getUUID(),
          Optional.empty(),
          getReason(),
          getCreated(),
          getDuration(),
          getDuration() != null ? PunishmentType.TEMP_BAN : PunishmentType.BAN,
          true,
          getCreated(),
          Optional.empty(),
          config.getService());
    }
  }
}
