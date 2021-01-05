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

public abstract class KitMutation extends MutationBase {

  private List<Kit> kits;

  public KitMutation(Match match, MutationType type, Kit... kits) {
    super(match, type);
    this.kits = Lists.newArrayList(kits);
  }

  @Override
  public void enable() {
    super.enable();
    giveAllKit(kits);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onSpawn(ParticipantKitApplyEvent event) {
    givePlayerKit(event.getPlayer(), kits); // Apply all kits upon respawn
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
