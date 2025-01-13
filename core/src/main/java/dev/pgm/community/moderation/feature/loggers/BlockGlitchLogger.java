package dev.pgm.community.moderation.feature.loggers;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static tc.oc.pgm.util.nms.Packets.ENTITIES;
import static tc.oc.pgm.util.text.NumberComponent.number;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.BroadcastUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.nms.packets.FakeEntity;

public class BlockGlitchLogger implements Listener {

  private final Map<UUID, Incident> activeIncidents = new HashMap<>();
  private final List<Incident> pastIncidents = new ArrayList<>();

  public BlockGlitchLogger() {
    PGM.get()
        .getExecutor()
        .scheduleAtFixedRate(
            () -> activeIncidents.values().removeIf(incident -> {
              if (!incident.isOver()) return false;
              if (pastIncidents.size() > 30) pastIncidents.removeFirst();
              pastIncidents.add(incident);
              BroadcastUtils.sendAdminChatMessage(
                  incident.getDescription(), CommunityPermissions.BLOCK_GLITCH_BROADCASTS);
              return true;
            }),
            0,
            50L,
            TimeUnit.MILLISECONDS);
    Community.get().registerListener(this);
  }

  public List<Incident> getIncidents() {
    return pastIncidents;
  }

  public Incident getIncident(int id) {
    for (Incident i : pastIncidents) {
      if (i.id == id) return i;
    }
    return null;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBlockTransform(ParticipantBlockTransformEvent event) {
    if (!(event.getCause() instanceof BlockPlaceEvent bpe)) return;
    MatchPlayerState playerState = event.getPlayerState();
    if (!playerState.canInteract()) return;
    Player pl = bpe.getPlayer();

    boolean isBlockglitch = event.isCancelled()
        && (playerState.getLocation().getY() - 0.75) > event.getBlock().getY()
        && pl != null
        && !pl.isOnGround()
        && !pl.isFlying();

    Incident active = activeIncidents.get(event.getPlayerState().getId());
    if (active != null) {
      active.placed(BlockVectors.position(event.getBlock()), isBlockglitch);
    } else if (isBlockglitch) {
      activeIncidents.put(
          pl.getUniqueId(),
          new Incident(
              pl,
              playerState.getName(),
              playerState.getParty(),
              BlockVectors.position(event.getBlock())));
    }
  }

  @EventHandler
  public void onMatchLoad(MatchAfterLoadEvent event) {
    activeIncidents.clear();
    pastIncidents.clear();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    activeIncidents.remove(event.getPlayer().getUniqueId());
  }

  public static class Incident {
    private static final long CANCEL_AFTER_PLACE = TimeUtils.toTicks(2, TimeUnit.SECONDS);
    private static int MAX_ID = 0;

    private final int id = ++MAX_ID;
    private final Instant startedAt = Instant.now();
    private final List<Action> queue = new ArrayList<>(100);
    private final Player player;
    private final Component name;
    private final Party party;

    private int currTick = 0;
    private int lastPlace = 0;
    private int glitchBlocks = 1;
    private double maxHeight = 0;

    public Incident(Player player, Component name, Party party, BlockVector initial) {
      this.player = player;
      this.name = name;
      this.party = party;

      this.queue.add(new MoveAction(player.getLocation()));
      this.queue.add(new PlaceAction(initial));
    }

    public void play(Player viewer) {
      new BlockGlitchReplay(viewer, this);
    }

    public void placed(BlockVector vector, boolean isGlitching) {
      if (isGlitching) {
        lastPlace = currTick;
        glitchBlocks++;
      }
      queue.add(new PlaceAction(vector));
    }

    public boolean isOver() {
      if (!player.isOnline()) return true;
      Location loc = player.getLocation();
      if (!(queue.getLast() instanceof MoveAction move) || !move.to.equals(loc)) {
        maxHeight = Math.max(maxHeight, loc.getY());
        queue.add(new MoveAction(loc));
      }
      return loc.getY() < -64
          || (currTick++ - lastPlace >= CANCEL_AFTER_PLACE && player.isOnGround());
    }

    public Location getStart() {
      return ((MoveAction) queue.getFirst()).to;
    }

    public Location getEnd() {
      return ((MoveAction) queue.getLast()).to;
    }

    public Component getPlayerName() {
      return name;
    }

    public Component getWhen() {
      return translatable(
          "misc.timeAgo", duration(Duration.between(startedAt, Instant.now()), GOLD));
    }

    public Component getDescription() {
      return text()
          .append(getPlayerName())
          .append(text(" blockglitched for ", NamedTextColor.GRAY)
              .append(duration(TimeUtils.fromTicks(lastPlace + 10)).color(NamedTextColor.YELLOW))
              .hoverEvent(showText(getStatistics())))
          .append(text(" [View]", NamedTextColor.GREEN, TextDecoration.BOLD)
              .clickEvent(runCommand("/blockglitch replay " + id)))
          .build();
    }

    public Component getStatistics() {
      double from = getStart().getY();
      return join(
              newlines(),
              text("Started ").append(getWhen()),
              text("Placed ").append(text(glitchBlocks, GOLD)).append(text(" glitch blocks")),
              text("Climbed from ")
                  .append(number(from, GOLD))
                  .append(text(" to "))
                  .append(number(maxHeight, GOLD))
                  .append(text(" ("))
                  .append(number(maxHeight - from, GOLD))
                  .append(text(" blocks)")))
          .color(NamedTextColor.YELLOW);
    }

    record MoveAction(Location to) implements Action {
      @Override
      public void play(BlockGlitchReplay replay) {
        replay.fakeEntity.teleport(to).send(replay.viewer);
      }
    }

    record PlaceAction(BlockVector place) implements Action {
      private static final BlockMaterialData BLOCK =
          MaterialData.block(Materials.parse("STAINED_GLASS", "WHITE_STAINED_GLASS"));

      @Override
      public void play(BlockGlitchReplay replay) {
        BLOCK.sendBlockChange(replay.viewer, place.toLocation(replay.viewer.getWorld()));
      }

      @Override
      public void clear(Player viewer) {
        Location loc = place.toLocation(viewer.getWorld());
        MaterialData.block(loc.getBlock()).sendBlockChange(viewer, loc);
      }
    }
  }

  private interface Action {
    void play(BlockGlitchReplay replay);

    default void clear(Player viewer) {}
  }

  static class BlockGlitchReplay implements Runnable {
    private final Player viewer;
    private final List<BlockGlitchLogger.Action> queue;
    private final Future<?> task;
    private final FakeEntity fakeEntity;
    private int idx = 0;

    private BlockGlitchReplay(Player viewer, BlockGlitchLogger.Incident incident) {
      this.viewer = viewer;
      this.queue = incident.queue;

      this.fakeEntity = ENTITIES.fakePlayer(incident.player, incident.party.getColor());
      this.fakeEntity.spawn(incident.getStart()).send(viewer);

      var armorColor = incident.party.getFullColor();
      this.fakeEntity
          .wear(
              dyeLeather(Material.LEATHER_HELMET, armorColor),
              dyeLeather(Material.LEATHER_CHESTPLATE, armorColor),
              dyeLeather(Material.LEATHER_LEGGINGS, armorColor),
              dyeLeather(Material.LEATHER_BOOTS, armorColor))
          .send(viewer);

      // Instant play first tp and first place
      queue.get(idx++).play(this);
      queue.get(idx++).play(this);

      this.task =
          PGM.get().getExecutor().scheduleWithFixedDelay(this, 1000, 50, TimeUnit.MILLISECONDS);
    }

    private static ItemStack dyeLeather(Material material, Color color) {
      ItemStack item = new ItemStack(material);
      LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
      meta.setColor(color);
      item.setItemMeta(meta);
      return item;
    }

    @Override
    public void run() {
      if (!viewer.isOnline()) {
        task.cancel(true);
        return;
      }

      if (idx < queue.size()) queue.get(idx).play(this);
      int cleanup = idx++ - 20;
      if (cleanup >= 0 && cleanup < queue.size()) queue.get(cleanup).clear(viewer);
      else if (cleanup >= queue.size()) {
        fakeEntity.destroy().send(viewer);
        task.cancel(true);
      }
    }
  }
}
