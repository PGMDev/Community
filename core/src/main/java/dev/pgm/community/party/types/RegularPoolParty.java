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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.pools.MapPool;

/** A regular party where a "party" pool is set and announcements are made */
public class RegularPoolParty extends MapPartyBase {

  private MapPool pool;

  public RegularPoolParty(Player creator, MapPartyConfig config) {
    this(
        formatDefaultName(creator.getName()),
        formatDefaultDescription(),
        config.getDuration(),
        new MapPartyHosts(creator, config.getHostPermissions()),
        new MapPartySettings(config),
        null);
  }

  public RegularPoolParty(
      String name,
      String description,
      Duration length,
      MapPartyHosts hosts,
      MapPartySettings settings,
      MapPool pool) {
    super(name, description, length, hosts, settings);
    this.pool = pool;
  }

  public MapPool getMapPool() {
    return pool;
  }

  public void setMapPool(MapPool pool) {
    this.pool = pool;
  }

  @Override
  public MapPartyType getEventType() {
    return MapPartyType.REGULAR;
  }

  @Override
  public void setup(CommandSender sender) throws MapPartySetupException {
    if (pool == null) {
      throw new MapPartySetupException("No map pool defined! Use &e/party setpool [pool]", this);
    }
    PGMUtils.setMapPool(sender, pool);
  }

  @Override
  public void addMap(MapInfo map) {
    // No-op
  }

  @Override
  public void removeMap(MapInfo map) {
    // No-op
  }

  @Override
  public boolean isMapAdded(MapInfo map) {
    return false;
  }

  @Override
  public boolean canAddMaps() {
    return false;
  }

  @Override
  public List<MapInfo> getCustomMaps() {
    return Lists.newArrayList();
  }
}
