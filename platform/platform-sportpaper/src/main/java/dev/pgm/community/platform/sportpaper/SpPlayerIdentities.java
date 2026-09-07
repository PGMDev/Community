package dev.pgm.community.platform.sportpaper;

import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import dev.pgm.community.util.PlayerIdentities;
import dev.pgm.community.util.Supports;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.skin.Skin;

@NullMarked
@Supports(SPORTPAPER)
public class SpPlayerIdentities implements PlayerIdentities {

  @Override
  public void setIdentity(
      Player player,
      Player viewer,
      @Nullable String name,
      @Nullable String displayName,
      @Nullable Skin skin) {
    PlayerIdentities.validateName(name);

    player.setFakeDisplayName(viewer, displayName);
    player.setFakeNameAndSkin(viewer, name, convertToSpSkin(skin));
  }

  @Override
  public void clearIdentities(Player player) {
    player.clearFakeDisplayNames();
    player.clearFakeNamesAndSkins();
  }

  @Override
  public void forget(Player player) {
    // no-op, SportPaper handles this on its own
  }

  @Override
  public void clearAll() {
    for (Player player : Bukkit.getOnlinePlayers()) clearIdentities(player);
  }

  private static org.bukkit.@Nullable Skin convertToSpSkin(@Nullable Skin skin) {
    return skin == null || skin.isEmpty()
        ? null
        : new org.bukkit.Skin(skin.getData(), skin.getSignature());
  }
}
