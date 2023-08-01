package dev.pgm.community.polls.ending.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.polls.ending.EndAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.util.named.MapNameStyle;

public class SetNextAction implements EndAction {

  private MapInfo map;

  public SetNextAction(MapInfo map) {
    this.map = map;
  }

  @Override
  public void execute(Player creator) {
    PGM.get().getMapOrder().setNextMap(map);
  }

  @Override
  public Component getName() {
    return text("Set Next Map")
        .hoverEvent(
            HoverEvent.showText(text("Sets the next map upon completion", NamedTextColor.GRAY)));
  }

  @Override
  public Component getPreviewValue() {
    return map.getStyledName(MapNameStyle.COLOR);
  }

  @Override
  public Component getDefaultQuestion() {
    return text()
        .append(text("Should we set next "))
        .append(map.getStyledName(MapNameStyle.PLAIN).color(NamedTextColor.AQUA))
        .append(text("?"))
        .color(NamedTextColor.WHITE)
        .build();
  }
}
