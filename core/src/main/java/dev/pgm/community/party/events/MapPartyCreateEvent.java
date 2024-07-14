package dev.pgm.community.party.events;

import dev.pgm.community.party.MapParty;
import org.bukkit.command.CommandSender;

public class MapPartyCreateEvent extends MapPartyEvent {

  public MapPartyCreateEvent(MapParty party, CommandSender sender) {
    super(party, sender);
  }
}
