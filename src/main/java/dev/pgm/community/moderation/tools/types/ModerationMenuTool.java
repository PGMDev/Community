package dev.pgm.community.moderation.tools.types;

import com.google.common.collect.Lists;
import dev.pgm.community.moderation.tools.ToolBase;
import dev.pgm.community.moderation.tools.menu.ModerationToolsMenu;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;

public class ModerationMenuTool extends ToolBase {

  private static final Material MATERIAL = Material.EMERALD;

  private static final String NAME = "&a&lModerator Tools";
  private static final List<String> LORE =
      Lists.newArrayList("&7Click to open the mod tools menu.");

  private ModerationToolsMenu menu;

  public ModerationMenuTool(int slot, boolean enabled) {
    super(slot, enabled);
    this.menu = new ModerationToolsMenu();
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
    openMenu(event.getPlayer().getBukkit());
  }

  @Override
  public void onRightClick(ObserverInteractEvent event) {
    openMenu(event.getPlayer().getBukkit());
  }

  public void openMenu(Player viewer) {
    menu.open(viewer);
  }
}
