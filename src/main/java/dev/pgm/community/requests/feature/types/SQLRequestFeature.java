package dev.pgm.community.requests.feature.types;

import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.feature.RequestFeatureBase;
import dev.pgm.community.requests.services.SQLRequestService;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.player.PlayerJoinEvent;

public class SQLRequestFeature extends RequestFeatureBase {

  private SQLRequestService service;

  public SQLRequestFeature(
      Configuration config, Logger logger, DatabaseConnection database, UsersFeature users) {
    super(new RequestConfig(config), logger, "Requests (SQL)", users);
    this.service = new SQLRequestService(database);
  }

  @Override
  public CompletableFuture<RequestProfile> onLogin(PlayerJoinEvent event) {
    return service.login(event.getPlayer().getUniqueId());
  }

  @Override
  public void update(RequestProfile profile) {
    service.update(profile);
  }

  @Override
  public CompletableFuture<RequestProfile> getRequestProfile(UUID playerId) {
    return service.query(playerId.toString());
  }

  @Override
  public RequestProfile getCached(UUID playerId) {
    return service.getCached(playerId);
  }
}
