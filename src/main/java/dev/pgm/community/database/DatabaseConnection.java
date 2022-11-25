package dev.pgm.community.database;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.DatabaseOptions;
import co.aikar.idb.PooledDatabaseOptions;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import java.util.Map;

public class DatabaseConnection {

  private DatabaseConfig config;

  public DatabaseConnection(Community plugin) {
    this.config = new DatabaseConfig(plugin.getConfig());

    Map<String, Object> extraOptions = Maps.newHashMap();
    extraOptions.put("serverTimezone", config.getTimezone());

    DatabaseOptions options =
        DatabaseOptions.builder()
            .poolName(plugin.getDescription().getName() + " DB")
            .logger(plugin.getLogger())
            .mysql(
                config.getUsername(),
                config.getPassword(),
                config.getDatabaseName(),
                config.getHost())
            .build();

    PooledDatabaseOptions poolOptions =
        PooledDatabaseOptions.builder()
            .options(options)
            .maxConnections(config.getMaxDatabaseConnections())
            .dataSourceProperties(extraOptions)
            .build();

    // Setup the main global DB
    BukkitDB.createHikariDatabase(plugin, poolOptions);
  }
}
