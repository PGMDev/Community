package dev.pgm.community.moderation.tools;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.tools.types.LookupSign;
import dev.pgm.community.moderation.tools.types.ModerationMenuTool;
import dev.pgm.community.moderation.tools.types.TeleportHook;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;

public class ModerationTools {

  private ModerationMenuTool menu;
  private TeleportHook tpHook;
  private LookupSign sign;

  public ModerationTools(ModerationConfig config) {
    // TODO: allow reloads to enable/disable tools
    this.menu = new ModerationMenuTool(config.isModMenuEnabled());
    this.tpHook = new TeleportHook(config.isPlayerHookEnabled());
    this.sign = new LookupSign(config.isLookupSignEnabled());
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
