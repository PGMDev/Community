package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Variant.PAPER;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import dev.pgm.community.util.PlayerUtils;
import dev.pgm.community.util.Supports;
import dev.pgm.community.utils.MessageUtils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import tc.oc.pgm.platform.modern.util.Skins;
import tc.oc.pgm.util.bukkit.BukkitUtils;
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

  @Override
  public ItemStack customSkull(String url, String displayName, String... lore) {
    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
    if (url.isEmpty()) {
      return head;
    }

    SkullMeta headMeta = (SkullMeta) head.getItemMeta();
    PlayerProfile profile = new CraftPlayerProfile(UUID.randomUUID(), null);
    PlayerTextures textures = profile.getTextures();

    try {
      textures.setSkin(new URI(url).toURL());
      profile.setTextures(textures);
      headMeta.setPlayerProfile(profile);
    } catch (URISyntaxException | MalformedURLException e) {
      e.printStackTrace();
    }

    headMeta.setDisplayName(BukkitUtils.colorize(displayName));
    headMeta.setLore(MessageUtils.colorizeList(Arrays.asList(lore)));
    headMeta.addItemFlags(ItemFlag.values());
    head.setItemMeta(headMeta);
    return head;
  }
}
