package dev.pgm.community.database;

import dev.pgm.community.Community;
import dev.pgm.community.database.dialect.MySqlDialect;
import dev.pgm.community.database.dialect.SqlDialect;
import dev.pgm.community.database.dialect.SqliteDialect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class DatabaseExecutor {

  private static final int DEFAULT_THREADS = 5;
  private static final ExecutorService SQL_EXECUTOR = Executors.newFixedThreadPool(DEFAULT_THREADS);
  private static final ExecutorService SQLITE_EXECUTOR = Executors.newSingleThreadExecutor();
  private static final SqlDialect MYSQL_DIALECT = new MySqlDialect();
  private static final SqlDialect SQLITE_DIALECT = new SqliteDialect();

  private DatabaseExecutor() {}

  public static CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
    return withConnection(() -> 0, conn -> {
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        bindParams(stmt, params);
        return stmt.executeUpdate();
      } catch (SQLException e) {
        logSqlError("update", sql, e);
        return 0;
      }
    });
  }

  public static <T> CompletableFuture<T> queryFirstAsync(
      String sql, RowMapper<T> mapper, Object... params) {
    return withConnection(() -> null, conn -> {
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        bindParams(stmt, params);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return mapper.map(rs);
          }
        }
      } catch (SQLException e) {
        logSqlError("query", sql, e);
      }
      return null;
    });
  }

  public static <T> CompletableFuture<List<T>> queryAsync(
      String sql, RowMapper<T> mapper, Object... params) {
    return withConnection(ArrayList::new, conn -> {
      List<T> results = new ArrayList<>();
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        bindParams(stmt, params);
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            results.add(mapper.map(rs));
          }
        }
      } catch (SQLException e) {
        logSqlError("query", sql, e);
      }
      return results;
    });
  }

  public static void shutdown() {
    SQL_EXECUTOR.shutdown();
    SQLITE_EXECUTOR.shutdown();
  }

  public static SqlDialect getDialect() {
    String backend = getDatabaseBackend();
    return "sqlite".equalsIgnoreCase(backend) ? SQLITE_DIALECT : MYSQL_DIALECT;
  }

  public static String describeBackend() {
    String backend = getDatabaseBackend();
    if ("sqlite".equalsIgnoreCase(backend)) {
      return "Features will use SQLITE for Database connections";
    }
    String name = getDatabaseName();
    if (name == null || name.isBlank()) {
      return "Features will use MYSQL for Database connections (Database: \"default\")";
    }
    return "Features will use MYSQL for Database connections (Database: \"" + name + "\")";
  }

  private static <T> CompletableFuture<T> withConnection(
      Supplier<T> fallback, ConnectionHandler<T> handler) {
    return CompletableFuture.supplyAsync(
        () -> {
          Optional<Connection> connOpt = getConnection();
          if (connOpt.isEmpty()) {
            return fallback.get();
          }
          try (Connection conn = connOpt.get()) {
            return handler.handle(conn);
          } catch (SQLException e) {
            logSqlError("connection", "n/a", e);
            return fallback.get();
          }
        },
        getExecutor());
  }

  private static Optional<Connection> getConnection() {
    try {
      String backend = getDatabaseBackend();
      if ("sqlite".equalsIgnoreCase(backend)) {
        return getSqliteConnection();
      }
      return getSqlConnection();
    } catch (Exception e) {
      Community.get().getLogger().warning("Failed to get database connection: " + e.getMessage());
      return Optional.empty();
    }
  }

  private static Optional<Connection> getSqlConnection() {
    String name = getDatabaseName();
    Optional<Connection> connOpt = resolveDatabaseConnection(name);
    if (connOpt.isEmpty()) {
      Community.get().getLogger().warning("Database plugin did not provide a connection.");
    }
    return connOpt;
  }

  private static Optional<Connection> getSqliteConnection() {
    try {
      Class.forName("org.sqlite.JDBC");
      Path databasePath = resolveSqlitePath();
      if (databasePath.getParent() != null) {
        Files.createDirectories(databasePath.getParent());
      }
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
      try (Statement statement = connection.createStatement()) {
        statement.execute("PRAGMA foreign_keys = ON");
        statement.execute("PRAGMA busy_timeout = 5000");
      }
      return Optional.of(connection);
    } catch (Exception e) {
      Community.get().getLogger().warning("Failed to open SQLite database: " + e.getMessage());
      return Optional.empty();
    }
  }

  private static Path resolveSqlitePath() {
    String file = Community.get().getConfig().getString("database.sqlite.file", "community.db");
    if (file == null || file.isBlank()) {
      file = "community.db";
    }
    Path path = Paths.get(file);
    if (!path.isAbsolute()) {
      path = Community.get().getDataFolder().toPath().resolve(path);
    }
    return path.toAbsolutePath().normalize();
  }

  private static Optional<Connection> resolveDatabaseConnection(String name) {
    try {
      Class<?> databaseClass = Class.forName("tc.oc.occ.database.Database");
      Object databaseInstance = databaseClass.getMethod("get").invoke(null);
      if (name == null || name.isBlank()) {
        @SuppressWarnings("unchecked")
        Optional<Connection> connection = (Optional<Connection>)
            databaseClass.getMethod("getConnection").invoke(databaseInstance);
        return connection;
      }
      @SuppressWarnings("unchecked")
      Optional<Connection> connection = (Optional<Connection>)
          databaseClass.getMethod("getConnection", String.class).invoke(databaseInstance, name);
      return connection;
    } catch (ClassNotFoundException e) {
      Community.get().getLogger().warning("Database plugin is not installed.");
      return Optional.empty();
    } catch (Exception e) {
      Community.get().getLogger().warning("Failed to access Database plugin: " + e.getMessage());
      return Optional.empty();
    }
  }

  private static String getDatabaseBackend() {
    String backend = Community.get().getConfig().getString("database.backend", "sql");
    return backend == null ? "sql" : backend;
  }

  private static ExecutorService getExecutor() {
    String backend = getDatabaseBackend();
    return "sqlite".equalsIgnoreCase(backend) ? SQLITE_EXECUTOR : SQL_EXECUTOR;
  }

  private static String getDatabaseName() {
    return Community.get().getConfig().getString("database.name", "");
  }

  private static void bindParams(PreparedStatement stmt, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      stmt.setObject(i + 1, params[i]);
    }
  }

  private static void logSqlError(String action, String sql, SQLException e) {
    Community.get()
        .getLogger()
        .warning("Database " + action + " failed (" + sql + "): " + e.getMessage());
  }

  @FunctionalInterface
  public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
  }

  @FunctionalInterface
  private interface ConnectionHandler<T> {
    T handle(Connection conn) throws SQLException;
  }
}
