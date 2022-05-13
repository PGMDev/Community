package dev.pgm.community.party.exceptions;

import dev.pgm.community.party.MapParty;

public abstract class MapPartyException extends Exception {

  private MapParty party;

  public MapPartyException(MapParty party, String error) {
    super(error);
    this.party = party;
  }

  public MapParty getParty() {
    return party;
  }
}
