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

  private volatile JedisPool pool;

  private volatile boolean shuttingDown;

  private final Set<NetworkSubscriber> subscribers;
  private final Set<NetworkSubscriber> activeSubscribers;
  private final Set<Jedis> subscriptionConnections;
  private final Object lifecycleLock;

  public RedisNetworkFeature(Configuration config, Logger logger) {
    super(config, logger, "Network (Redis)");
    this.subscribers = Sets.newHashSet();
    this.activeSubscribers = Sets.newHashSet();
    this.subscriptionConnections = Sets.newHashSet();
    this.lifecycleLock = new Object();

    if (getConfig().isEnabled()) enable();
  }

  public void registerSubscriber(NetworkSubscriber sub) {
    if (shuttingDown) return;
    this.subscribers.add(sub);
  }

  @Override
  public void enable() {
    super.enable();
    this.shuttingDown = false;
    this.pool = new JedisPool(
        new JedisPoolConfig(),
        getNetworkConfig().getHost(),
        getNetworkConfig().getPort(),
        Protocol.DEFAULT_TIMEOUT,
        getNetworkConfig().getPassword(),
        getNetworkConfig().isSSL());

    testConnection();

    // Delay subscriber so all features can register
    Community.get().getServer().getScheduler().runTaskLater(Community.get(), this::subscribe, 20L);
  }

  @Override
  public void disable() {
    Set<NetworkSubscriber> subscriptions;
    Set<Jedis> connections;
    synchronized (lifecycleLock) {
      if (shuttingDown) return;
      shuttingDown = true;
      subscriptions = Set.copyOf(activeSubscribers);
      connections = Set.copyOf(subscriptionConnections);
      subscriptionConnections.clear();
    }

    subscriptions.forEach(NetworkSubscriber::unsubscribe);
    connections.forEach(Jedis::close);

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
    if (shuttingDown) return;
    subscribers.forEach(this::asyncSubscribe);
  }

  private void asyncSubscribe(NetworkSubscriber sub) {
    Community.get().getServer().getScheduler().runTaskAsynchronously(Community.get(), () -> {
      Jedis jedi = null;
      boolean registered = false;
      try {
        jedi = pool.getResource();
        synchronized (lifecycleLock) {
          if (shuttingDown) return;
          subscriptionConnections.add(jedi);
          activeSubscribers.add(sub);
          registered = true;
        }

        jedi.subscribe(sub, sub.getChannel());
      } catch (RuntimeException e) {
        if (!shuttingDown) throw e;
      } finally {
        if (jedi != null) {
          boolean ours = true;
          if (registered) {
            synchronized (lifecycleLock) {
              ours = subscriptionConnections.remove(jedi);
              activeSubscribers.remove(sub);
            }
          }
          if (ours) jedi.close();
        }
      }
    });
  }

  @Override
  public void sendUpdate(NetworkUpdate update) {
    if (!isEnabled() || shuttingDown) return;
    Community.get().getServer().getScheduler().runTaskAsynchronously(Community.get(), () -> {
      if (!shuttingDown && pool != null && !pool.isClosed()) {
        try (Jedis jedi = pool.getResource()) {
          jedi.publish(
              update.getChannel(),
              String.format("%s;%s", getNetworkConfig().getNetworkId(), update.getData()));
        }
      }
    });
  }
}
