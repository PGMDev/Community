package dev.pgm.community.platform.sportpaper;

import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import dev.pgm.community.util.PlayerUtils;
import dev.pgm.community.util.Supports;
import dev.pgm.community.utils.MessageUtils;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.platform.sportpaper.utils.Skins;
import tc.oc.pgm.util.bukkit.BukkitUtils;
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

  @Override
  public ItemStack customSkull(String url, String displayName, String... lore) {
    ItemStack head = new ItemStack(Material.SKULL_ITEM);
    head.setDurability((short) SkullType.PLAYER.ordinal());
    if (url.isEmpty()) {
      return head;
    }

    SkullMeta headMeta = (SkullMeta) head.getItemMeta();
    GameProfile profile = createGameProfile(url);
    Field profileField;
    try {
      profileField = headMeta.getClass().getDeclaredField("profile");
      profileField.setAccessible(true);
      profileField.set(headMeta, profile);
    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
    headMeta.setDisplayName(BukkitUtils.colorize(displayName));
    headMeta.setLore(MessageUtils.colorizeList(Arrays.asList(lore)));
    headMeta.addItemFlags(ItemFlag.values());
    head.setItemMeta(headMeta);
    return head;
  }

  private static GameProfile createGameProfile(String url) {
    GameProfile profile = new GameProfile(UUID.randomUUID(), null);
    PropertyMap propertyMap = profile.getProperties();
    if (propertyMap == null) {
      return null;
    }

    byte[] encodedData = Base64.getEncoder()
        .encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
    propertyMap.put("textures", new Property("textures", new String(encodedData)));

    return profile;
  }
}
