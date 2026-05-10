package dev.pgm.community.platform.modern;

import static dev.pgm.community.nick.identity.PlayerIdentity.PLAYER_IDENTITY;
import static dev.pgm.community.util.Supports.Variant.PAPER;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode;
import dev.pgm.community.util.PlayerUtils;
import dev.pgm.community.util.Supports;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jspecify.annotations.NonNull;
import tc.oc.pgm.platform.modern.util.Skins;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "1.21.11")
public class ModernPlayerUtils implements PlayerUtils {
  @Override
  public Skin getPlayerSkin(Player player) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    return Skins.fromProfile(craftPlayer.getProfile());
  }

  @Override
  public void setFakeNameAndSkin(
      Player player, Player viewer, String displayName, String nick, Skin skin) {
    String oldName = PLAYER_IDENTITY.getName(player, viewer);

    PLAYER_IDENTITY.set(player, viewer, displayName, nick, skin);

    String newName = PLAYER_IDENTITY.getName(player, viewer);

    if (!oldName.equals(newName)) {
      updateTeamEntry(viewer, oldName, newName);
    }
  }

  private void updateTeamEntry(@NonNull Player viewer, String oldName, String newName) {
    UUID viewerId = viewer.getUniqueId();
    String teamName = PLAYER_IDENTITY.getTeamName(viewerId, oldName);
    if (teamName == null) return;

    sendTeamPacketSilently(viewer, teamName, TeamMode.REMOVE_ENTITIES, List.of(oldName));
    PLAYER_IDENTITY.removeTeamEntries(viewerId, teamName, List.of(oldName));

    sendTeamPacketSilently(viewer, teamName, TeamMode.ADD_ENTITIES, List.of(newName));
    PLAYER_IDENTITY.addTeamEntries(viewerId, teamName, List.of(newName));
  }

  private void sendTeamPacketSilently(
      Player viewer, String teamName, TeamMode mode, java.util.Collection<String> names) {
    PacketEvents.getAPI()
        .getPlayerManager()
        .sendPacketSilently(
            viewer,
            new WrapperPlayServerTeams(
                teamName, mode, (WrapperPlayServerTeams.ScoreBoardTeamInfo) null, names));
  }

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
