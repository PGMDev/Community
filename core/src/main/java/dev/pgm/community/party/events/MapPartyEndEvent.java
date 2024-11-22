package dev.pgm.community.party.events;

import dev.pgm.community.party.MapParty;
import org.bukkit.command.CommandSender;

public class MapPartyEndEvent extends MapPartyEvent {

  public MapPartyEndEvent(MapParty party, CommandSender sender) {
    super(party, sender);
  }
}
