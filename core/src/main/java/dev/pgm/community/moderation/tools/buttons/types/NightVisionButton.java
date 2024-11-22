package dev.pgm.community.moderation.tools.buttons.types;

import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.moderation.tools.buttons.TranslatableToolButton;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NightVisionButton extends TranslatableToolButton {

  private static final String NAME_KEY = "setting.nightvision";
  private static final String LORE_KEY = NAME_KEY + ".lore";
  private static final Material MATERIAL = Material.POTION;
  private static final NamedTextColor COLOR = NamedTextColor.DARK_PURPLE;

  public NightVisionButton(Player viewer) {
    super(viewer, NAME_KEY, COLOR, LORE_KEY, MATERIAL, 1);
  }

  @Override
  public Consumer<InventoryClickEvent> getClickEvent() {
    return c -> {
      toggleNightVision(getViewer());
      c.setCancelled(true);
      c.setCurrentItem(getIcon());
    };
  }

  @Override
  public Component getLoreComponent() {
    Component status =
        translatable(
            hasNightVision(getViewer()) ? "misc.on" : "misc.off",
            hasNightVision(getViewer()) ? NamedTextColor.GREEN : NamedTextColor.RED);
    return translatable("setting.nightvision.lore", NamedTextColor.GRAY, status);
  }

  private boolean hasNightVision(Player player) {
    return player.hasPotionEffect(PotionEffectType.NIGHT_VISION);
  }

  public void toggleNightVision(Player player) {
    if (hasNightVision(player)) {
      player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    } else {
      player.addPotionEffect(
          new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
    }
  }
}
