package dev.pgm.community.party.settings;

import dev.pgm.community.party.MapPartyConfig;
import org.bukkit.Material;

public class MapPartySettings {

  private PartyBooleanSetting showLoginMessage;
  private PartyBooleanSetting showPartyNotifications;

  public MapPartySettings(MapPartyConfig config) {
    this.showLoginMessage =
        new PartyBooleanSetting(
            "Login Message",
            "Display a login welcome when party is active",
            config.showLoginMessage(),
            Material.SIGN,
            Material.BARRIER);
    this.showPartyNotifications =
        new PartyBooleanSetting(
            "Notification",
            "Broadcast announcements related to party",
            config.showPartyNotifications(),
            Material.BOOK_AND_QUILL,
            Material.BARRIER);
  }

  public PartyBooleanSetting getShowLoginMessage() {
    return showLoginMessage;
  }

  public PartyBooleanSetting getShowPartyNotifications() {
    return showPartyNotifications;
  }
}
