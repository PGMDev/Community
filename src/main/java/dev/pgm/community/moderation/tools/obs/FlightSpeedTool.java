package dev.pgm.community.moderation.tools.obs;

import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.moderation.tools.TranslatableTool;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FlightSpeedTool extends TranslatableTool {

  private static final String NAME_KEY = "setting.flyspeed";
  private static final String LORE_KEY = NAME_KEY + ".lore";
  private static final Material MATERIAL = Material.FEATHER;
  private static final NamedTextColor COLOR = NamedTextColor.DARK_RED;

  public FlightSpeedTool(Player viewer) {
    super(viewer, NAME_KEY, COLOR, LORE_KEY, MATERIAL, 1);
  }

  @Override
  public Component getLoreComponent() {
    Component flySpeed = FlySpeed.of(getViewer().getFlySpeed()).getName();
    return translatable("setting.flyspeed.lore", NamedTextColor.GRAY, flySpeed);
  }

  @Override
  public Consumer<InventoryClickEvent> getClickEvent() {
    return c -> {
      FlySpeed speed = FlySpeed.of(getViewer().getFlySpeed());
      if (c.isRightClick()) {
        getViewer().setFlySpeed(speed.getPrev().getValue());
      } else {
        getViewer().setFlySpeed(speed.getNext().getValue());
      }
      c.setCancelled(true);
      c.setCurrentItem(getIcon());
    };
  }

  public enum FlySpeed {
    NORMAL(NamedTextColor.YELLOW, 0.1f),
    FAST(NamedTextColor.GOLD, 0.25f),
    FASTER(NamedTextColor.RED, 0.5f),
    HYPERSPEED(NamedTextColor.LIGHT_PURPLE, 0.9f);

    private final TextColor color;
    private final float value;

    private static FlySpeed[] speeds = values();

    FlySpeed(TextColor color, float value) {
      this.color = color;
      this.value = value;
    }

    public float getValue() {
      return value;
    }

    public Component getName() {
      return translatable(NAME_KEY + "." + this.name().toLowerCase(), color);
    }

    public FlySpeed getNext() {
      return speeds[(ordinal() + 1) % speeds.length];
    }

    public FlySpeed getPrev() {
      int index = (ordinal() == 0 ? speeds.length : ordinal()) - 1;
      return speeds[index % speeds.length];
    }

    public static FlySpeed of(float value) {
      for (FlySpeed speed : FlySpeed.values()) {
        if (speed.getValue() == value) {
          return speed;
        }
      }
      return NORMAL;
    }
  }
}
