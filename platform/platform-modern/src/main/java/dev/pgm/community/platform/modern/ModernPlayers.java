package dev.pgm.community.platform.modern;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import dev.pgm.community.platform.CommunityPlayers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernPlayers implements CommunityPlayers {
  @Override
  public void setFakeDisplayName(Player player, CommandSender viewer, String displayName) {
    //    throw new
  }

  @Override
  public void setFakeNameAndSkin(
      Player player, CommandSender viewer, String displayName, Skin skin) {
    // hmm
  }
}
