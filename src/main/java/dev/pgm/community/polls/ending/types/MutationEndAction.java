package dev.pgm.community.polls.ending.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.polls.ending.EndAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MutationEndAction implements EndAction {

  private final MutationType mutation;

  public MutationEndAction(MutationType mutation) {
    this.mutation = mutation;
  }

  @Override
  public void execute(Player creator) {
    if (creator != null && creator.isOnline()) {
      Bukkit.dispatchCommand(creator, "mt add " + mutation.name());
    }
  }

  @Override
  public Component getName() {
    return text("Mutation")
        .hoverEvent(
            HoverEvent.showText(text("Enable a mutation upon completion", NamedTextColor.GRAY)));
  }

  @Override
  public Component getPreviewValue() {
    return text(mutation.getDisplayName(), NamedTextColor.GREEN);
  }

  @Override
  public Component getDefaultQuestion() {
    return text()
        .append(text("Should we enable the "))
        .append(text(mutation.getDisplayName(), NamedTextColor.GREEN))
        .append(text(" mutation"))
        .append(text("?"))
        .color(NamedTextColor.WHITE)
        .build();
  }
}
