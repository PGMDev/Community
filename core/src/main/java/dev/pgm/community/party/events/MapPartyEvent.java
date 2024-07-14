package dev.pgm.community.party.events;

import dev.pgm.community.events.CommunityEvent;
import dev.pgm.community.party.MapParty;
import org.bukkit.command.CommandSender;

public abstract class MapPartyEvent extends CommunityEvent {

  private final MapParty party;
  private final CommandSender sender;

  public MapPartyEvent(MapParty party, CommandSender sender) {
    this.party = party;
    this.sender = sender;
  }

  public MapParty getParty() {
    return party;
  }

  public CommandSender getSender() {
    return sender;
  }
}
