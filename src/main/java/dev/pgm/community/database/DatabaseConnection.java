package dev.pgm.community.database;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.DatabaseOptions;
import co.aikar.idb.DatabaseOptions.DatabaseOptionsBuilder;
import co.aikar.idb.PooledDatabaseOptions;
import co.aikar.idb.PooledDatabaseOptions.PooledDatabaseOptionsBuilder;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import java.util.Map;

public class DatabaseConnection {

  private DatabaseConfig config;

  public DatabaseConnection(Community plugin) {
    this.config = new DatabaseConfig(plugin.getConfig());

    Map<String, Object> extraOptions = Maps.newHashMap();
    extraOptions.put("serverTimezone", config.getTimezone());

    DatabaseOptionsBuilder builder = DatabaseOptions.builder()
        .poolName(plugin.getDescription().getName() + " DB")
        .logger(plugin.getLogger());

    if (config.isEnabled()) {
      builder.mysql(
          config.getUsername(), config.getPassword(), config.getDatabaseName(), config.getHost());
    } else {
      builder.sqlite(config.getSQLiteFileName());
      builder.minAsyncThreads(1);
      builder.maxAsyncThreads(1);
    }

    PooledDatabaseOptionsBuilder poolBuilder = PooledDatabaseOptions.builder()
        .options(builder.build())
        .maxConnections(config.getMaxDatabaseConnections());

    // Apply extra MySQL options
    if (config.isEnabled()) {
      poolBuilder.dataSourceProperties(extraOptions);
    }

    // Setup the main global DB
    BukkitDB.createHikariDatabase(plugin, poolBuilder.build());
  }
}
