package dev.pgm.community.moderation.tools.obs;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import dev.pgm.community.moderation.tools.TranslatableTool;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import tc.oc.pgm.util.Audience;

public class GamemodeTool extends TranslatableTool {

  private static final String NAME_KEY = "setting.gamemode";
  private static final String LORE_KEY = NAME_KEY + ".lore";
  private static final Material ON_MATERIAL = Material.SEA_LANTERN;
  private static final Material OFF_MATERIAL = Material.PRISMARINE;
  private static final NamedTextColor COLOR = NamedTextColor.DARK_AQUA;

  public GamemodeTool(Player viewer) {
    super(viewer, NAME_KEY, COLOR, LORE_KEY, ON_MATERIAL, 1);
  }

  @Override
  public Material getMaterial() {
    return isCreative() ? ON_MATERIAL : OFF_MATERIAL;
  }

  @Override
  public Component getLoreComponent() {
    // TODO: This is broken for some reason? Investigate later
    // String gamemode = TextTranslations.translate("gameMode." +
    // getViewer().getGameMode().name().toLowerCase(), getViewer());
    Component gamemode =
        text(
            WordUtils.capitalize(getViewer().getGameMode().name().toLowerCase()),
            NamedTextColor.AQUA);
    return translatable("setting.gamemode.lore", NamedTextColor.GRAY, gamemode);
  }

  @Override
  public Consumer<InventoryClickEvent> getClickEvent() {
    return c -> {
      toggleObserverGameMode();
      c.setCancelled(true);
      c.setCurrentItem(getIcon());
    };
  }

  private void toggleObserverGameMode() {
    Player player = getViewer();
    player.setGameMode(getOppositeMode(player.getGameMode()));
    if (player.getGameMode() == GameMode.SPECTATOR) {
      Audience.get(player).sendWarning(getToggleMessage());
    } else if (isCreative()) {
      // Note: When WorldEdit is present, this executes a command to ensure the player is not stuck
      if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")
          && player.hasPermission("worldedit.navigation.unstuck")) {
        player.performCommand("worldedit:!");
      }
    }
  }

  private Component getToggleMessage() {
    Component command =
        text("/tools", NamedTextColor.AQUA)
            .hoverEvent(showText(translatable("setting.gamemode.hover", NamedTextColor.GRAY)))
            .clickEvent(runCommand("/tools"));
    return translatable("setting.gamemode.warning", NamedTextColor.GRAY, command);
  }

  private boolean isCreative() {
    return getViewer().getGameMode().equals(GameMode.CREATIVE);
  }

  private GameMode getOppositeMode(GameMode mode) {
    switch (mode) {
      case CREATIVE:
        return GameMode.SPECTATOR;
      case SPECTATOR:
        return GameMode.CREATIVE;
      default:
        return mode;
    }
  }
}
