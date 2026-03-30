package dev.pgm.community.party.settings;

import dev.pgm.community.party.MapPartyConfig;
import dev.pgm.community.utils.compatibility.Materials;
import org.bukkit.Material;

public class MapPartySettings {

  private final PartyBooleanSetting showLoginMessage;
  private final PartyBooleanSetting showPartyNotifications;
  private final PartyBooleanSetting autoscalingTeams;

  public MapPartySettings(MapPartyConfig config) {
    this.showLoginMessage = new PartyBooleanSetting(
        "Login Message",
        "Display a login welcome when party is active",
        config.showLoginMessage(),
        Materials.SIGN,
        Material.BARRIER);
    this.showPartyNotifications = new PartyBooleanSetting(
        "Notification",
        "Broadcast announcements related to party",
        config.showPartyNotifications(),
        Materials.BOOK_AND_QUILL,
        Material.BARRIER);
    this.autoscalingTeams = new PartyBooleanSetting(
        "Autoscaling Teams",
        "Automatically resize teams on match cycle",
        true,
        Materials.GOLD_PLATE,
        Materials.WOOD_PLATE);
  }

  public PartyBooleanSetting getShowLoginMessage() {
    return showLoginMessage;
  }

  public PartyBooleanSetting getShowPartyNotifications() {
    return showPartyNotifications;
  }

  public PartyBooleanSetting getAutoscalingTeams() {
    return autoscalingTeams;
  }
}
