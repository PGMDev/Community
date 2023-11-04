package dev.pgm.community.party.menu.settings;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import dev.pgm.community.party.MapParty;
import dev.pgm.community.party.feature.MapPartyFeature;
import dev.pgm.community.party.menu.MapPartyMenu;
import dev.pgm.community.party.settings.MapPartySettings;
import dev.pgm.community.party.settings.PartyBooleanSetting;
import dev.pgm.community.party.types.CustomPoolParty;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.text.TextTranslations;

public class MapPartySettingsMenu extends MapPartyMenu {

  private static final String TITLE = "&3&lSettings";
  private static final int ROWS = 6;
  private static final boolean HOST_ONLY = true;

  public MapPartySettingsMenu(MapPartyFeature feature, Player viewer) {
    super(feature, TITLE, ROWS, HOST_ONLY, viewer);
    open();
  }

  private void render(Player player, InventoryContents contents) {
    contents.fillBorders(getBorderItem());

    if (getFeature().getParty() == null) {
      return;
    }

    MapParty party = getFeature().getParty();
    MapPartySettings settings = party.getSettings();

    contents.set(1, 1, getAutoScalingItem(settings.getAutoscalingTeams()));
    contents.set(3, 1, getNameItem(party));
    contents.set(3, 3, getTimelimitItem(party));
    contents.set(3, 5, getSettingItem(settings.getShowLoginMessage()));
    contents.set(3, 7, getSettingItem(settings.getShowPartyNotifications()));

    if (party instanceof CustomPoolParty) {
      CustomPoolParty customParty = (CustomPoolParty) getFeature().getParty();
      contents.set(1, 4, getPoolModeItem(customParty));
    }

    contents.set(5, 4, getMainMenuIcon());
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    render(player, contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    render(player, contents);
  }

  private ClickableItem getTimelimitItem(MapParty party) {
    boolean hasLimit = party.getLength() != null;
    Component limitRender =
        !hasLimit
            ? text("No limit", NamedTextColor.YELLOW)
            : duration(party.getLength(), NamedTextColor.YELLOW);

    ItemStack item =
        new ItemBuilder()
            .material(Material.WATCH)
            .name(colorize("&2&lTimelimit"))
            .lore(
                colorize(
                    "&7Current: "
                        + TextTranslations.translateLegacy(
                            limitRender.color(NamedTextColor.GRAY), getViewer())),
                colorize("&7Click to edit"))
            .flags(ItemFlag.values())
            .build();

    return ClickableItem.of(
        item,
        c -> {
          close();
          Audience viewer = Audience.get(getViewer());
          viewer.sendMessage(
              text()
                  .append(text("Current party timelimit: "))
                  .append(limitRender)
                  .color(NamedTextColor.GRAY));

          // TODO: make presets customizable via config?
          // [30 minutes] [1 hour] [3 hours] [Custom]
          Component preset1 = renderTimePreset(Duration.ofMinutes(30));
          Component preset2 = renderTimePreset(Duration.ofHours(1));
          Component preset3 = renderTimePreset(Duration.ofHours(3));
          Component custom =
              text()
                  .append(text("["))
                  .append(text("Custom", NamedTextColor.YELLOW, TextDecoration.BOLD))
                  .append(text("]"))
                  .color(NamedTextColor.GRAY)
                  .hoverEvent(
                      HoverEvent.showText(
                          text("Click to set a custom timelimit", NamedTextColor.GRAY)))
                  .clickEvent(ClickEvent.suggestCommand("/event timelimit "))
                  .build();
          viewer.sendMessage(
              text()
                  .append(preset1)
                  .append(space())
                  .append(preset2)
                  .append(space())
                  .append(preset3)
                  .append(space())
                  .append(custom));
        });
  }

  private Component renderTimePreset(Duration time) {
    return text()
        .append(text("["))
        .append(duration(time, NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD))
        .append(text("]"))
        .color(NamedTextColor.GRAY)
        .hoverEvent(
            HoverEvent.showText(text("Click to select new timelimit preset", NamedTextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand("/event timelimit " + time.getSeconds() + "s"))
        .build();
  }

  private ClickableItem getNameItem(MapParty party) {
    ItemStack item =
        new ItemBuilder()
            .material(Material.NAME_TAG)
            .name(colorize("&3&lParty Name"))
            .lore(colorize("&7Current: " + party.getName()), colorize("&7Click to edit"))
            .flags(ItemFlag.values())
            .build();

    return ClickableItem.of(
        item,
        c -> {
          close();
          Audience viewer = Audience.get(getViewer());
          Component edit =
              text()
                  .append(text(" ["))
                  .append(text("Edit", NamedTextColor.YELLOW, TextDecoration.BOLD))
                  .append(text("]"))
                  .color(NamedTextColor.GRAY)
                  .hoverEvent(
                      HoverEvent.showText(text("Click to edit party name", NamedTextColor.GRAY)))
                  .clickEvent(ClickEvent.suggestCommand("/event setname "))
                  .build();
          viewer.sendMessage(
              text()
                  .append(text("Current party name: ", NamedTextColor.GRAY))
                  .append(party.getStyledName())
                  .append(edit)
                  .build());
        });
  }

  private ClickableItem getPoolModeItem(CustomPoolParty party) {
    ItemBuilder builder =
        new ItemBuilder()
            .material(party.isVoted() ? Material.PAPER : Material.RAILS)
            .name(colorize("&d&lPool Mode"))
            .lore(
                colorize("&7Current: &6" + (party.isVoted() ? "Voted" : "Rotation")),
                colorize("&7Click to toggle mode"))
            .flags(ItemFlag.values());

    return ClickableItem.of(
        builder.build(),
        c -> {
          Bukkit.dispatchCommand(getViewer(), "event mode");
        });
  }

  private ClickableItem getSettingItem(PartyBooleanSetting setting) {
    return ClickableItem.of(settingItemBuilder(setting), c -> setting.toggle());
  }

  private ClickableItem getAutoScalingItem(PartyBooleanSetting setting) {
    return ClickableItem.of(
        settingItemBuilder(setting),
        c -> {
          setting.toggle();
          Bukkit.dispatchCommand(getViewer(), "event autoscaling " + setting.getValue());
        });
  }

  private static ItemStack settingItemBuilder(PartyBooleanSetting setting) {
    return new ItemBuilder()
        .material(setting.getIcon())
        .name(colorize((setting.getValue() ? "&a&l" : "&c&l") + setting.getName()))
        .lore(
            colorize("&7" + setting.getDescription()),
            colorize("&7Current: " + (setting.getValue() ? "&aEnabled" : "&cDisabled")),
            colorize("&7Click to toggle"))
        .flags(ItemFlag.values())
        .build();
  }
}
