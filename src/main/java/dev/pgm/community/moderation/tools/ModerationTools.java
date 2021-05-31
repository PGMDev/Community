package dev.pgm.community.moderation.tools;

import dev.pgm.community.moderation.tools.types.LookupSign;
import dev.pgm.community.moderation.tools.types.ModerationMenuTool;
import dev.pgm.community.moderation.tools.types.TeleportHook;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;

public class ModerationTools {

  private ModerationMenuTool menu;
  private TeleportHook tpHook;
  private LookupSign sign;

  public ModerationTools() {
    this.menu = new ModerationMenuTool();
    this.tpHook = new TeleportHook();
    this.sign = new LookupSign();
  }

  public ModerationMenuTool getMenu() {
    return menu;
  }

  public TeleportHook getTeleportHook() {
    return tpHook;
  }

  public LookupSign getLookupSign() {
    return sign;
  }

  public void onInteract(ObserverInteractEvent event) {
    menu.onInteract(event);
    tpHook.onInteract(event);
    sign.onInteract(event);
  }

  public void giveTools(Player player) {
    getMenu().give(player);
    getTeleportHook().give(player);
    getLookupSign().give(player);
  }
}
