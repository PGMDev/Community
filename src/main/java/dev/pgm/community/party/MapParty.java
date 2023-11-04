package dev.pgm.community.party;

import dev.pgm.community.party.exceptions.MapPartySetupException;
import dev.pgm.community.party.hosts.MapPartyHosts;
import dev.pgm.community.party.settings.MapPartySettings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.api.map.MapInfo;

public interface MapParty {

  Component getStyledName();

  MapPartyType getEventType();

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);

  boolean shouldAutoScale();

  void setAutoScaling(boolean autoScaling);

  @Nullable
  Duration getLength();

  void setLength(Duration length);

  MapPartyHosts getHosts();

  boolean isRunning();

  Instant getStartTime();

  void start(CommandSender sender);

  void stop(CommandSender sender);

  void restart(CommandSender sender);

  void setup(CommandSender sender) throws MapPartySetupException;

  boolean isSetup();

  void setSetup(boolean setup);

  void addMap(MapInfo map);

  void removeMap(MapInfo map);

  boolean isMapAdded(MapInfo map);

  boolean canAddMaps();

  List<MapInfo> getCustomMaps();

  MapPartySettings getSettings();

  void onLogin(PlayerJoinEvent event);

  void onQuit(PlayerQuitEvent event);
}
