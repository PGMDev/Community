package dev.pgm.community.platform.sportpaper;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGH;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import dev.pgm.community.platform.CommunityPlayers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = SPORTPAPER, priority = HIGH)
public class SpPlayers implements CommunityPlayers {
  @Override
  public void setFakeDisplayName(Player player, CommandSender viewer, String displayName) {
    player.setFakeDisplayName(viewer, displayName);
  }

  @Override
  public void setFakeNameAndSkin(
      Player player, CommandSender viewer, String displayName, Skin skin) {
    player.setFakeNameAndSkin(
        viewer,
        displayName,
        skin == null ? null : new org.bukkit.Skin(skin.getData(), skin.getData()));
  }
}
