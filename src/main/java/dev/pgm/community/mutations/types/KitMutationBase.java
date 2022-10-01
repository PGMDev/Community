package dev.pgm.community.mutations.types;

import com.google.common.collect.Lists;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;

/** KitMutation - A base for mutations which grant kits * */
public abstract class KitMutationBase extends MutationBase {

  private List<Kit> kits;

  public KitMutationBase(Match match, MutationType type, Kit... kits) {
    super(match, type);
    this.kits = Lists.newArrayList(kits);
  }

  @Override
  public void enable() {
    super.enable();
    giveAllKit(getKits());
  }

  @Override
  public void disable() {
    super.disable();
    kits.forEach(this::removeAllKit);
    if (getAllItems().length > 0) {
      removeItems(getAllItems());
    }
  }

  public List<Kit> getKits() {
    return kits;
  }

  public ItemStack[] getAllItems() {
    return new ItemStack[0];
  }

  public void spawn(MatchPlayer player) {
    givePlayerKit(player, getKits());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onSpawn(ParticipantKitApplyEvent event) {
    spawn(event.getPlayer());
  }

  protected void giveAllKit(Kit... kit) {
    giveAllKit(Lists.newArrayList(kit));
  }

  protected void removeAllKit(Kit kit) {
    if (kit instanceof ItemKit) return;
    match.getParticipants().forEach(player -> kit.remove(player));
  }

  protected void giveAllKit(List<Kit> kits) {
    match.getParticipants().forEach(player -> givePlayerKit(player, kits));
  }

  protected void givePlayerKit(MatchPlayer player, List<Kit> kits) {
    kits.forEach(kit -> player.applyKit(kit, true));
  }

  protected void removeItems(ItemStack... items) {
    match
        .getParticipants()
        .forEach(
            player -> {
              player.getInventory().removeItem(items);
            });
  }
}
