package dev.pgm.community.platform.sportpaper;

import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import dev.pgm.community.util.PlayerUtils;
import dev.pgm.community.util.Supports;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tc.oc.pgm.platform.sportpaper.utils.Skins;
import tc.oc.pgm.util.skin.Skin;

@Supports(SPORTPAPER)
public class SpPlayerUtils implements PlayerUtils {

  @Override
  public Skin getPlayerSkin(Player player) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    return Skins.fromProfile(craftPlayer.getProfile());
  }

  @Override
  public void setFakeNameAndSkin(
      Player player, Player viewer, String displayName, String nick, Skin skin) {
    player.setFakeDisplayName(viewer, displayName);
    player.setFakeNameAndSkin(viewer, nick, player.getSkin(viewer));
  }

  @Override
  public String getPlayerDisplayName(Player player, Player viewer) {
    return player.getDisplayName(viewer);
  }

  @Override
  public String getPlayerName(Player player, Player viewer) {
    return player.getName(viewer);
  }

  @Override
  public Skin getPlayerSkin(Player player, Player viewer) {
    org.bukkit.Skin skin = player.getSkin(viewer);
    return new Skin(skin.getData(), skin.getSignature());
  }
}
