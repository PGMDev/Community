package dev.pgm.community.nick.feature.types;

import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.nick.Nick;
import dev.pgm.community.nick.feature.NickFeatureBase;
import dev.pgm.community.nick.services.SQLNickService;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;

public class SQLNickFeature extends NickFeatureBase {

  private final SQLNickService service;
  private final UsersFeature users;

  public SQLNickFeature(Configuration config, Logger logger, UsersFeature users) {
    super(config, logger, "Nicknames (SQL)");
    this.users = users;
    this.service = new SQLNickService(getNickConfig());
  }

  @Override
  public CompletableFuture<Nick> getNick(UUID playerId) {
    return service.query(playerId.toString());
  }

  @Override
  public CompletableFuture<Boolean> setNick(UUID playerId, String nickName) {
    return isNameAvailable(nickName)
        .thenApplyAsync(
            free -> {
              boolean override =
                  Bukkit.getPlayer(playerId) != null
                      && Bukkit.getPlayer(playerId).hasPermission(CommunityPermissions.ADMIN);
              if (!free && !override) {
                return false;
              }

              getNick(playerId)
                  .thenAcceptAsync(
                      nick -> {
                        if (nick == null) {
                          Nick newNick = Nick.of(playerId, nickName);
                          service.save(newNick);
                        } else {
                          nick.setName(nickName);
                          service.update(nick);
                        }
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
              nick.clear();
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
}
