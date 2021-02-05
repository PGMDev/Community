package dev.pgm.community.nick.feature.types;

import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.nick.Nick;
import dev.pgm.community.nick.feature.NickFeatureBase;
import dev.pgm.community.nick.services.SQLNickService;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

public class SQLNickFeature extends NickFeatureBase {

  private final SQLNickService service;
  private final UsersFeature users;

  public SQLNickFeature(
      Configuration config, Logger logger, DatabaseConnection database, UsersFeature users) {
    super(config, logger, "Nicknames (SQL)");
    this.users = users;
    this.service = new SQLNickService(database, getNickConfig());
  }

  @Override
  public CompletableFuture<Nick> getNick(UUID playerId) {
    return service.query(playerId.toString());
  }

  @Override
  public CompletableFuture<List<Nick>> getNickHistory(UUID playerId) {
    return service.queryList(playerId.toString());
  }

  @Override
  public CompletableFuture<Boolean> setNick(UUID playerId, String nickName) {
    return isNameAvailable(nickName)
        .thenApplyAsync(
            free -> {
              if (!free) {
                return false;
              }
              clearNick(playerId)
                  .thenAcceptAsync(
                      success -> {
                        Nick newNick = Nick.of(playerId, nickName);
                        service.save(newNick);
                      });
              return true;
            });
  }

  @Override
  public CompletableFuture<Boolean> clearNick(UUID playerId) {
    return getNick(playerId)
        .thenApplyAsync(
            nick -> {
              if (nick == null) return false;
              nick.setValid(false);
              service.update(nick);
              return true;
            });
  }

  @Override
  public CompletableFuture<Boolean> isNameAvailable(String nickName) {
    return service
        .isNameAvailable(nickName)
        .thenApplyAsync(
            available -> {
              return available && users.getStoredProfile(nickName).join() == null;
            });
  }

  @Override
  public CompletableFuture<Boolean> toggleNick(UUID playerId) {
    return getNick(playerId)
        .thenApplyAsync(
            nick -> {
              if (nick == null) return false;

              nick.setEnabled(!nick.isEnabled());
              service.update(nick);
              return nick.isEnabled();
            });
  }

  @Override
  public CompletableFuture<List<Nick>> getNicksByName(String nick) {
    return service.queryByName(nick, false);
  }
}
