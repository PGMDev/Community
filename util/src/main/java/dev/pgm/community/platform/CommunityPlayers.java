package dev.pgm.community.platform;

import static dev.pgm.community.platform.Reflections.INSTANCE;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.skin.Skin;

public interface CommunityPlayers {
  CommunityPlayers PLAYERS = Platform.get(CommunityPlayers.class, INSTANCE);

  void setFakeDisplayName(Player player, CommandSender viewer, String displayName);

  void setFakeNameAndSkin(Player player, CommandSender viewer, String displayName, Skin skin);
}
