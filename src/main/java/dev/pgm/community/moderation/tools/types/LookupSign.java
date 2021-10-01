package dev.pgm.community.moderation.tools.types;

import com.google.common.collect.Lists;
import dev.pgm.community.moderation.tools.ToolBase;
import java.util.List;
import org.bukkit.Material;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;

public class LookupSign extends ToolBase {

  private static final Material MATERIAL = Material.SIGN;

  private static final String NAME = "&c&lLookup Sign";
  private static final List<String> LORE =
      Lists.newArrayList("&7Click &bplayer &7to view punishment info");

  public LookupSign(int slot, boolean enabled) {
    super(slot, enabled);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<String> getLore() {
    return LORE;
  }

  @Override
  public Material getMaterial() {
    return MATERIAL;
  }

  @Override
  public void onLeftClick(ObserverInteractEvent event) {
    lookup(event);
  }

  @Override
  public void onRightClick(ObserverInteractEvent event) {
    lookup(event);
  }

  private void lookup(ObserverInteractEvent event) {
    if (event.getClickedPlayer() != null) {
      MatchPlayer target = event.getClickedPlayer();
      event.getPlayer().getBukkit().performCommand("l " + target.getId().toString());
    }
  }
}
