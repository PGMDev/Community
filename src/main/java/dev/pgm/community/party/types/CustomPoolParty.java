package dev.pgm.community.party.types;

import com.google.common.collect.Lists;
import dev.pgm.community.party.MapPartyBase;
import dev.pgm.community.party.MapPartyConfig;
import dev.pgm.community.party.MapPartyType;
import dev.pgm.community.party.exceptions.MapPartySetupException;
import dev.pgm.community.party.hosts.MapPartyHosts;
import dev.pgm.community.party.settings.MapPartySettings;
import dev.pgm.community.utils.PGMUtils;
import java.time.Duration;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.Rotation;
import tc.oc.pgm.rotation.VotingPool;

/* A custom map party where host provides a list of maps,*/
public class CustomPoolParty extends MapPartyBase {

  private boolean voted;

  private List<MapInfo> maps;

  public CustomPoolParty(Player creator, Duration length, MapPartyConfig config) {
    this(
        formatDefaultName(creator.getName()),
        formatDefaultDescription(),
        length,
        new MapPartyHosts(creator, config.getHostPermissions()),
        new MapPartySettings(config),
        Lists.newArrayList());
  }

  public CustomPoolParty(
      String name,
      String description,
      Duration length,
      MapPartyHosts hosts,
      MapPartySettings settings,
      List<MapInfo> maps) {
    super(name, description, length, hosts, settings);
    this.maps = maps;
    this.voted = true;
  }

  public List<MapInfo> getMaps() {
    return maps;
  }

  @Override
  public void addMap(MapInfo map) {
    this.maps.add(map);
  }

  @Override
  public void removeMap(MapInfo map) {
    this.maps.remove(map);
  }

  @Override
  public boolean isMapAdded(MapInfo map) {
    return maps.contains(map);
  }

  @Override
  public boolean canAddMaps() {
    return true;
  }

  public boolean isVoted() {
    return voted;
  }

  public void setVoted(boolean voted) throws MapPartySetupException {
    this.voted = voted;
    if (this.isRunning()) {
      setMapPool(Bukkit.getConsoleSender());
    }
  }

  @Override
  public MapPartyType getEventType() {
    return MapPartyType.CUSTOM;
  }

  @Override
  public List<MapInfo> getCustomMaps() {
    return maps;
  }

  @Override
  public void setup(CommandSender sender) throws MapPartySetupException {
    setMapPool(sender);
  }

  private void setMapPool(CommandSender sender) throws MapPartySetupException {
    if (getMaps().isEmpty()) {
      throw new MapPartySetupException("No maps defined! Use &e/party addmap [map]", this);
    }

    MapPoolManager manager = PGMUtils.getMapPoolManager();
    if (manager == null) {
      throw new MapPartySetupException("Error finding the map pool manager!", this);
    }

    MapPool customPool;
    if (isVoted()) {
      customPool =
          new VotingPool(
              manager, "party-maps", this.getName(), true, 1, false, Duration.ofSeconds(30), maps);
    } else {
      customPool =
          new Rotation(
              manager, "party-maps", this.getName(), true, 1, false, Duration.ofSeconds(30), maps);
    }

    PGMUtils.setMapPool(sender, customPool);
  }
}
