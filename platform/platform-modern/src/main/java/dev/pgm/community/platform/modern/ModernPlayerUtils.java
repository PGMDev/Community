package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Variant.PAPER;

import dev.pgm.community.util.PlayerUtils;
import dev.pgm.community.util.Supports;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tc.oc.pgm.platform.modern.util.Skins;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernPlayerUtils implements PlayerUtils {
  @Override
  public Skin getPlayerSkin(Player player) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    return Skins.fromProfile(craftPlayer.getProfile());
  }

  private final Map<UUID, Map<UUID, Skin>> playerSkins = new HashMap<>();
  private final Map<UUID, Map<UUID, String>> playerNames = new HashMap<>();
  private final Map<UUID, Map<UUID, String>> playerDisplayNames = new HashMap<>();

  @Override
  public void setFakeNameAndSkin(
      Player player, Player viewer, String displayName, String nick, Skin skin) {
    playerSkins
        .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
        .put(viewer.getUniqueId(), skin);
    playerNames
        .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
        .put(viewer.getUniqueId(), nick);
    playerDisplayNames
        .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
        .put(viewer.getUniqueId(), displayName);
  }

  @Override
  public String getPlayerDisplayName(Player player, Player viewer) {
    if (playerDisplayNames.containsKey(player.getUniqueId())) {
      Map<UUID, String> uuidStringMap = playerDisplayNames.get(player.getUniqueId());
      String displayName = uuidStringMap.get(viewer.getUniqueId());
      return displayName == null ? player.getDisplayName() : displayName;
    }
    return player.getDisplayName();
  }

  @Override
  public String getPlayerName(Player player, Player viewer) {
    if (playerNames.containsKey(player.getUniqueId())) {
      Map<UUID, String> uuidStringMap = playerNames.get(player.getUniqueId());
      String name = uuidStringMap.get(viewer.getUniqueId());
      return name == null ? player.getName() : name;
    }
    return player.getName();
  }

  @Override
  public Skin getPlayerSkin(Player player, Player viewer) {
    return null;
    //    if (playerSkins.containsKey(player.getUniqueId())) {
    //      Map<UUID, Skin> uuidSkinMap = playerSkins.get(player.getUniqueId());
    //      Skin skin = uuidSkinMap.get(viewer.getUniqueId());
    //      if (skin == null) {
    //        return new Skin(player.getPlayerProfile().getTextures())
    //      }
    //      return skin == null ? Skin.EMPTY : skin;
    //    }
    //    return Skin.EMPTY;
  }
}
