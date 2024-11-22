package dev.pgm.community.party.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.Lists;
import dev.pgm.community.party.MapParty;
import dev.pgm.community.party.MapPartyMessages;
import dev.pgm.community.party.MapPartyType;
import dev.pgm.community.party.feature.MapPartyFeature;
import dev.pgm.community.party.hosts.MapPartyHosts;
import dev.pgm.community.party.menu.hosts.HostMenu;
import dev.pgm.community.party.menu.modifiers.MapPartyModifierMenu;
import dev.pgm.community.party.menu.settings.MapPartySettingsMenu;
import dev.pgm.community.party.presets.MapPartyPreset;
import dev.pgm.community.utils.SkullUtils;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class MapPartyMainMenu extends MapPartyMenu {

  private static final String TITLE = "&d&lMapParty Manager";
  private static final int ROWS = 6;
  private static final boolean HOST_ONLY = false;

  public MapPartyMainMenu(MapPartyFeature feature, Player viewer) {
    super(feature, TITLE, ROWS, HOST_ONLY, viewer);
    open();
  }

  private void render(Player player, InventoryContents contents) {
    contents.fillBorders(getBorderItem());

    if (getFeature().getParty() == null) {
      renderPartyCreation(player, contents);
      return;
    }

    MapParty party = getFeature().getParty();

    // Info item
    ItemStack info =
        new ItemBuilder()
            .material(Material.CAKE)
            .name(colorize(party.getName()))
            .lore(
                colorize(" &f* &7Description: &6" + party.getDescription()),
                colorize(" &f* &7Type: &6" + party.getEventType()),
                colorize(" &f* &7Started: " + (party.isRunning() ? "&aYes" : "&cNo")),
                colorize(" &f* &7Time Remaining: &a" + MapPartyMessages.formatTime(party)))
            .build();

    ClickableItem infoIcon = ClickableItem.empty(info);

    contents.set(1, 4, infoIcon);

    contents.set(2, 1, getHostIcon(party.getHosts()));

    contents.set(2, 3, getMapsIcon(party.getEventType()));

    contents.set(2, 5, getModifierMenu());

    contents.set(2, 7, getSettingsIcon());

    if (!party.isRunning()) {
      contents.set(4, 3, getStartIcon());
    } else {
      contents.set(4, 3, getRestartIcon());
    }

    contents.set(4, 5, getEndIcon());
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    render(player, contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    render(player, contents);
  }

  private void renderPartyCreation(Player player, InventoryContents contents) {
    MapPartyTypeSelectionMenu selectTypeSub = new MapPartyTypeSelectionMenu(getFeature(), player);
    contents.set(
        1,
        4,
        ClickableItem.of(
            new ItemBuilder()
                .material(Material.CAKE)
                .name(colorize("&aCreate a new map party"))
                .build(),
            c -> {
              selectTypeSub.open(this);
            }));

    List<MapPartyPreset> presets = getFeature().getPresets();
    int[] SLOTS = {1, 3, 5, 7};
    for (int i = 0; i < SLOTS.length; i++) {
      ClickableItem item = getEmptyItem(i);

      if (i < presets.size()) {
        item = getPresetIcon(presets.get(i));
        contents.set(3, SLOTS[i], getBorderItem());
      }
      contents.set(3, SLOTS[i], item);
    }
  }

  private Optional<MapParty> getParty() {
    if (getFeature().getParty() != null) {
      return Optional.of(getFeature().getParty());
    } else {
      close();
      return Optional.empty();
    }
  }

  private ClickableItem getStartIcon() {
    return ClickableItem.of(
        START_ITEM,
        c -> {
          getParty()
              .ifPresent(
                  party -> {
                    Bukkit.dispatchCommand(
                        getViewer(), "event start " + (c.isLeftClick() ? "false" : "true"));
                  });
        });
  }

  private ClickableItem getRestartIcon() {
    return ClickableItem.of(
        RESTART_ITEM,
        c -> {
          getParty()
              .ifPresent(
                  party -> {
                    Bukkit.dispatchCommand(getViewer(), "event restart");
                  });
        });
  }

  private ClickableItem getEndIcon() {
    return ClickableItem.of(
        END_ITEM,
        c -> {
          getParty()
              .ifPresent(
                  party -> {
                    Bukkit.dispatchCommand(getViewer(), "event stop");
                    close();
                  });
        });
  }

  private ClickableItem getHostIcon(MapPartyHosts hosts) {
    return ClickableItem.of(
        new ItemBuilder()
            .material(Material.SKULL_ITEM)
            .durability(3)
            .name(colorize("&a&lHosts"))
            .lore(colorize("&7Click to manage party hosts"))
            .flags(ItemFlag.values())
            .build(),
        c -> {
          new HostMenu(getFeature(), getViewer());
        });
  }

  private ClickableItem getMapsIcon(MapPartyType type) {
    return ClickableItem.of(
        new ItemBuilder()
            .material(Material.MAP)
            .name(colorize(type == MapPartyType.REGULAR ? "&6&lPool" : "&6&lMaps"))
            .lore(
                colorize(
                    "&7Click to manage party " + (type == MapPartyType.REGULAR ? "pool" : "maps")))
            .flags(ItemFlag.values())
            .build(),
        c -> {
          Bukkit.dispatchCommand(getViewer(), "event maps");
        });
  }

  private ClickableItem getSettingsIcon() {
    return ClickableItem.of(
        new ItemBuilder()
            .material(Material.REDSTONE_COMPARATOR)
            .name(colorize("&3&lSettings"))
            .lore(colorize("&7Click to manage party settings"))
            .flags(ItemFlag.values())
            .build(),
        c -> {
          new MapPartySettingsMenu(getFeature(), getViewer());
        });
  }

  private ClickableItem getModifierMenu() {
    return ClickableItem.of(
        new ItemBuilder()
            .material(Material.TNT)
            .name(colorize("&5&lModifiers"))
            .lore(colorize("&7Click to manage party modifiers"))
            .flags(ItemFlag.values())
            .build(),
        c -> {
          new MapPartyModifierMenu(getFeature(), getViewer());
        });
  }

  private ClickableItem getPresetIcon(MapPartyPreset preset) {
    List<String> lore = Lists.newArrayList();
    lore.add(colorize("&6Description&7: &3" + preset.getDescription()));
    lore.add(colorize("&6Mode&7: &a" + preset.getType().getName()));

    if (preset.getType() == MapPartyType.REGULAR) {
      lore.add(colorize("&6Pool&7: &b" + preset.getPool()));
    } else {
      lore.add(colorize("&6Maps: &b" + preset.getMaps().size()));
    }

    lore.add("");
    lore.add(colorize("&aClick to create new party"));

    return ClickableItem.of(
        new ItemBuilder()
            .material(Material.STAINED_GLASS_PANE)
            .color(DyeColor.CYAN)
            .name(colorize(preset.getName()))
            .lore(lore.toArray(new String[lore.size()]))
            .build(),
        c -> {
          Bukkit.dispatchCommand(getViewer(), "event preset " + preset.getName());
        });
  }

  private ClickableItem getEmptyItem(int i) {
    return ClickableItem.empty(
        new ItemBuilder()
            .material(Material.STAINED_GLASS_PANE)
            .color(DyeColor.GRAY)
            .name(colorize("&cEmpty Preset #" + (i + 1)))
            .build());
  }

  private static final String START_PARTY_SKIN =
      "http://textures.minecraft.net/texture/4ae29422db4047efdb9bac2cdae5a0719eb772fccc88a66d912320b343c341";
  private static final String RESTART_PARTY_SKIN =
      "http://textures.minecraft.net/texture/479e8cf21b839b255a2836e251941c5fdc99af01559e3733d5325ccfa3d922aa";
  private static final String END_PARTY_SKIN =
      "http://textures.minecraft.net/texture/e9cdb9af38cf41daa53bc8cda7665c509632d14e678f0f19f263f46e541d8a30";

  private static final ItemStack START_ITEM =
      SkullUtils.customSkull(
          START_PARTY_SKIN,
          "&a&lStart Event",
          "&2Left-Click&7 to start the event now",
          "&2Right-Click&7 to start event after current match ends");

  private static final ItemStack RESTART_ITEM =
      SkullUtils.customSkull(
          RESTART_PARTY_SKIN, "&2&lRestart Event", "&7Click to restart the event");

  private static final ItemStack END_ITEM =
      SkullUtils.customSkull(END_PARTY_SKIN, "&4&lEnd Event", "&7Click to end the event");
}
