package dev.pgm.community.moderation.tools.buttons.types;

import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.moderation.tools.buttons.TranslatableToolButton;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;

public class ObserverVisibilityButton extends TranslatableToolButton {

  private static final String NAME_KEY = "setting.visibility";
  private static final String LORE_KEY = NAME_KEY + ".lore";
  private static final Material ON_MATERIAL = Material.EYE_OF_ENDER;
  private static final Material OFF_MATERIAL = Material.ENDER_PEARL;
  private static final NamedTextColor COLOR = NamedTextColor.YELLOW;
  private static final NamedTextColor ON_COLOR = NamedTextColor.GREEN;
  private static final NamedTextColor OFF_COLOR = NamedTextColor.RED;

  public ObserverVisibilityButton(Player viewer) {
    super(viewer, NAME_KEY, COLOR, LORE_KEY, ON_MATERIAL, 1);
  }

  @Override
  public Material getMaterial() {
    return isVisible() ? ON_MATERIAL : OFF_MATERIAL;
  }

  public NamedTextColor getStatusColor() {
    return isVisible() ? ON_COLOR : OFF_COLOR;
  }

  @Override
  public Component getLoreComponent() {
    return translatable(
        "setting.visibility.lore",
        NamedTextColor.GRAY,
        translatable(getStatusKey(), getStatusColor()));
  }

  @Override
  public Consumer<InventoryClickEvent> getClickEvent() {
    return c -> {
      toggleObserverVisibility(getViewer());
      c.setCancelled(true);
      c.setCurrentItem(getIcon());
    };
  }

  public boolean isVisible() {
    return PGM.get()
            .getMatchManager()
            .getPlayer(getViewer())
            .getSettings()
            .getValue(SettingKey.OBSERVERS)
        != SettingValue.OBSERVERS_OFF;
  }

  private String getStatusKey() {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(getViewer());
    SettingValue value = matchPlayer.getSettings().getValue(SettingKey.OBSERVERS);
    switch (value) {
      case OBSERVERS_FRIEND:
        return "misc.friends";
      case OBSERVERS_ON:
        return "misc.all";
      default:
        return "misc.none";
    }
  }

  public void toggleObserverVisibility(Player player) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    Settings setting = matchPlayer.getSettings();
    setting.toggleValue(SettingKey.OBSERVERS);
    SettingKey.OBSERVERS.update(matchPlayer);
  }
}
