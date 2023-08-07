package dev.pgm.community.polls.ending.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.polls.ending.EndAction;
import java.util.Collection;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextFormatter;

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

  private Component getFancyMapName() {
    TextComponent.Builder hover = text();

    Collection<Gamemode> gamemodes = map.getGamemodes();

    if (map.getGamemode() != null) {
      hover.append(map.getGamemode().colorIfAbsent(NamedTextColor.AQUA)).appendNewline();
    } else if (!gamemodes.isEmpty()) {
      boolean acronyms = gamemodes.size() > 1;
      hover
          .append(
              TextFormatter.list(
                  gamemodes.stream()
                      .map(gm -> text(acronyms ? gm.getAcronym() : gm.getFullName()))
                      .collect(Collectors.toList()),
                  NamedTextColor.AQUA))
          .appendNewline();
    }

    hover.append(
        text(
            map.getTags().stream().map(MapTag::toString).collect(Collectors.joining(" ")),
            NamedTextColor.YELLOW));

    return text()
        .append(map.getStyledName(MapNameStyle.PLAIN).color(NamedTextColor.AQUA))
        .hoverEvent(HoverEvent.showText(hover))
        .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
        .build();
  }

  @Override
  public Component getDefaultQuestion() {
    return text()
        .append(text("Should we set next "))
        .append(getFancyMapName())
        .append(text("?"))
        .color(NamedTextColor.WHITE)
        .build();
  }
}
