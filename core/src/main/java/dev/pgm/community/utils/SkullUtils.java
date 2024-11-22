package dev.pgm.community.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class SkullUtils {

  public static ItemStack customSkull(String url, String displayName, String... lore) {
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
    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ignored) {
      ignored.printStackTrace();
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

    byte[] encodedData =
        new Base64().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
    propertyMap.put("textures", new Property("textures", new String(encodedData)));

    return profile;
  }
}
