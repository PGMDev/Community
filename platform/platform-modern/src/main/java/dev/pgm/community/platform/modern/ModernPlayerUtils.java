package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Variant.PAPER;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import dev.pgm.community.util.PlayerUtils;
import dev.pgm.community.util.Supports;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jspecify.annotations.NonNull;

@Supports(value = PAPER, minVersion = "1.21.11")
public class ModernPlayerUtils implements PlayerUtils {
  @Override
  public ItemStack customSkull(@NonNull String url, String displayName, String... lore) {
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

    var displayNameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(displayName);
    var loreComponent = Arrays.stream(lore)
        .map(each -> LegacyComponentSerializer.legacyAmpersand().deserialize(each))
        .toList();

    headMeta.displayName(displayNameComponent);
    headMeta.lore(loreComponent);
    headMeta.addItemFlags(ItemFlag.values());
    head.setItemMeta(headMeta);
    return head;
  }
}
