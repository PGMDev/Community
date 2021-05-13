package dev.pgm.community.mutations.types;

import com.google.common.collect.Lists;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
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

  public List<Kit> getKits() {
    return kits;
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

  protected void giveAllKit(List<Kit> kits) {
    match.getParticipants().forEach(player -> givePlayerKit(player, kits));
  }

  protected void givePlayerKit(MatchPlayer player, List<Kit> kits) {
    kits.forEach(kit -> player.applyKit(kit, true));
  }
}
