package dev.pgm.community.party.events;

import dev.pgm.community.party.MapParty;
import org.bukkit.command.CommandSender;

public class MapPartyRestartEvent extends MapPartyEvent {

  public MapPartyRestartEvent(MapParty party, CommandSender sender) {
    super(party, sender);
  }
}
