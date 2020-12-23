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
    giveAllKits();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onSpawn(ParticipantKitApplyEvent event) {
    givePlayerKits(event.getPlayer()); // Apply all kits upon respawn
  }

  private void giveAllKits() {
    match.getParticipants().forEach(this::givePlayerKits);
  }

  private void givePlayerKits(MatchPlayer player) {
    kits.forEach(kit -> player.applyKit(kit, true));
  }
}
