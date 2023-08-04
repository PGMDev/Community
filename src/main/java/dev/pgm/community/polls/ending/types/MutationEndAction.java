package dev.pgm.community.polls.ending.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.feature.MutationFeature;
import dev.pgm.community.polls.ending.EndAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MutationEndAction implements EndAction {

  private final MutationType mutation;
  private final MutationFeature mutationFeature;

  public MutationEndAction(MutationType mutation) {
    this.mutation = mutation;
    this.mutationFeature = Community.get().getFeatures().getMutations();
  }

  @Override
  public void execute(Player creator) {
    if (creator != null && creator.isOnline()) {
      boolean enable = !mutationFeature.hasMutation(mutation);
      Bukkit.dispatchCommand(creator, "mt " + (enable ? "add " : "remove ") + mutation.name());
    }
  }

  @Override
  public Component getName() {
    return text("Mutation")
        .hoverEvent(
            HoverEvent.showText(text("Toggle mutation upon completion", NamedTextColor.GRAY)));
  }

  @Override
  public Component getPreviewValue() {
    return mutation.getComponent();
  }

  @Override
  public Component getDefaultQuestion() {
    return text()
        .append(text("Should we toggle the "))
        .append(mutation.getComponent().color(NamedTextColor.GREEN))
        .append(text(" mutation"))
        .append(text("?"))
        .color(NamedTextColor.WHITE)
        .build();
  }
}
