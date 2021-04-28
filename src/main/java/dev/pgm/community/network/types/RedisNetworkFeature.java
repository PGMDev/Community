package dev.pgm.community.network.types;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.network.feature.NetworkFeatureBase;
import dev.pgm.community.network.subs.NetworkSubscriber;
import dev.pgm.community.network.updates.NetworkUpdate;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;

/** RedisNetworkFeature - Redis implementation of NetworkFeature * */
public class RedisNetworkFeature extends NetworkFeatureBase {

  private JedisPool pool;

  private Set<NetworkSubscriber> subscribers;

  public RedisNetworkFeature(Configuration config, Logger logger) {
    super(config, logger, "Network (Redis)");
    this.subscribers = Sets.newHashSet();
  }

  public void registerSubscriber(NetworkSubscriber sub) {
    this.subscribers.add(sub);
  }

  @Override
  public void enable() {
    super.enable();
    this.pool =
        new JedisPool(
            new JedisPoolConfig(),
            getNetworkConfig().getHost(),
            getNetworkConfig().getPort(),
            Protocol.DEFAULT_TIMEOUT,
            getNetworkConfig().getPassword(),
            getNetworkConfig().isSSL());

    testConnection();

    // Delay subscriber so all features can register
    Community.get().getServer().getScheduler().runTaskLater(Community.get(), this::subscribe, 20l);
  }

  @Override
  public void disable() {
    super.disable();
    if (pool != null && !pool.isClosed()) {
      pool.close();
    }
  }

  private void testConnection() {
    try (Jedis jedi = pool.getResource()) {
      logger.info("- REDIS: Connection status: " + jedi.ping());
    } catch (JedisConnectionException e) {
      logger.warning("- REDIS: Connection could not be established!");
      e.printStackTrace();
      logger.warning("NETWORK FEATURE WILL NOW DISABLE");
      disable();
    }
  }

  private void subscribe() {
    subscribers.forEach(this::asyncSubscribe);
  }

  private void asyncSubscribe(NetworkSubscriber sub) {
    Community.get()
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            Community.get(),
            () -> {
              try (Jedis jedi = pool.getResource()) {
                jedi.subscribe(sub, sub.getChannel());
              }
            });
  }

  @Override
  public void sendUpdate(NetworkUpdate update) {
    try (Jedis jedi = pool.getResource()) {
      jedi.publish(
          update.getChannel(),
          String.format("%s;%s", getNetworkConfig().getNetworkId(), update.getData()));
    }
  }
}
