package dev.pgm.community.party.exceptions;

import dev.pgm.community.party.MapParty;

public class MapPartySetupException extends MapPartyException {

  public MapPartySetupException(String error, MapParty party) {
    super(party, error);
  }
}
