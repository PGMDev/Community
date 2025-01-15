package dev.pgm.community.moderation.feature.loggers;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;
import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;
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
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
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
    PGM.get().getExecutor().scheduleAtFixedRate(this::tickIncidents, 0, 50L, TimeUnit.MILLISECONDS);
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

  private void tickIncidents() {
    activeIncidents.values().removeIf(incident -> {
      if (!incident.isOver()) return false;
      archiveIncident(incident);
      return true;
    });
  }

  private void archiveIncident(Incident incident) {
    if (incident == null || incident.isInsignificant()) return;

    if (pastIncidents.size() > 30) pastIncidents.removeFirst();
    pastIncidents.add(incident);
    BroadcastUtils.sendAdminChatMessage(
        incident.getDescription(), CommunityPermissions.BLOCK_GLITCH_BROADCASTS);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBlockTransform(ParticipantBlockTransformEvent event) {
    if (!(event.getCause() instanceof BlockPlaceEvent bpe)) return;
    MatchPlayerState playerState = event.getPlayerState();
    if (!playerState.canInteract()) return;

    Incident active = activeIncidents.get(event.getPlayerState().getId());
    if (active != null) {
      active.placed(event.getBlock(), event.isCancelled());
      return;
    }

    Player pl = bpe.getPlayer();
    if (!event.isCancelled() || pl == null) return;

    // Block has to be under you or in your head
    Block block = event.getBlock();
    Location loc = playerState.getLocation();
    double heightDiff = block.getY() - loc.getY();
    boolean isBelow = !pl.isOnGround() && heightDiff < -1;
    boolean isAbove = pl.isSprinting() && heightDiff > 1.8 && heightDiff < 2.2;

    // Block is within your height, should be of no use
    if (!isAbove && !isBelow) return;

    double distance =
        Math.abs(block.getX() - loc.getX() + 0.5d) + Math.abs(block.getZ() - loc.getZ() + 0.5d);
    // Block is too far away to be relevant
    if (distance > 1.75) return;

    activeIncidents.put(pl.getUniqueId(), new Incident(pl, playerState, event.getBlock()));
  }

  @EventHandler
  public void onPlayerDespawn(ParticipantDespawnEvent event) {
    archiveIncident(activeIncidents.remove(event.getPlayer().getId()));
  }

  @EventHandler
  public void onMatchLoad(MatchAfterLoadEvent event) {
    activeIncidents.clear();
    pastIncidents.clear();
  }

  public static class Incident {
    private static final long CANCEL_AFTER_PLACE = TimeUtils.toTicks(2, TimeUnit.SECONDS);
    private static final long MAX_LENGTH = TimeUtils.toTicks(30, TimeUnit.SECONDS);
    private static int MAX_ID = 0;

    private final int id = ++MAX_ID;
    private final Instant startedAt = Instant.now();
    private final List<Action> queue = new ArrayList<>(100);
    private final Player player;
    private final Component name;
    private final Party party;

    private Location last;
    private int currTick = 0;
    private int lastPlace = 0;
    private int glitchBlocks = 0;
    private double maxHeight = 0;

    public Incident(Player player, MatchPlayerState pl, Block initial) {
      this.player = player;
      this.name = pl.getName();
      this.party = pl.getParty();

      this.queue.add(new MoveAction(last = player.getLocation()));
      placed(initial, true);
    }

    public void play(Player viewer) {
      new BlockGlitchReplay(viewer, this);
    }

    public void placed(Block block, boolean isGlitching) {
      if (isGlitching) {
        lastPlace = currTick;
        glitchBlocks++;
      }
      queue.add(new PlaceAction(BlockVectors.encodePos(block), isGlitching));
    }

    public boolean isOver() {
      Location loc = player.getLocation();
      if (!last.equals(loc)) {
        maxHeight = Math.max(maxHeight, loc.getY());
        queue.add(new MoveAction(last = loc));
      }
      return loc.getY() < -64
          || currTick > MAX_LENGTH
          || (currTick++ - lastPlace >= CANCEL_AFTER_PLACE && player.isOnGround());
    }

    public boolean isInsignificant() {
      // Any glitch over 3 blocks will always be reported
      if (glitchBlocks > 3) return false;

      // BG above void where you end up in the void are ignored
      if (isAboveVoid()) return getEnd().getY() < -1;

      // BG elsewhere if they don't gain height is also ignored
      return maxHeight - getStart().getY() < 0.4;
    }

    private boolean isAboveVoid() {
      Block block = BlockVectors.blockAt(player.getWorld(), ((PlaceAction) queue.get(1)).place());
      while (block.getY() > 0) {
        block = block.getRelative(BlockFace.DOWN);
        if (!block.isEmpty()) return false;
      }
      return true;
    }

    public Location getStart() {
      return ((MoveAction) queue.getFirst()).to.clone();
    }

    public Location getEnd() {
      return last.clone();
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
          .append(text(" blockglitched for ", GRAY)
              .append(duration(TimeUtils.fromTicks(lastPlace + 10)).color(YELLOW))
              .hoverEvent(showText(getStatistics())))
          .appendSpace()
          .append(text("[View]", NamedTextColor.GREEN, TextDecoration.BOLD)
              .clickEvent(runCommand("/blockglitch replay " + id))
              .hoverEvent(showText(text("Click to view replay #" + id, YELLOW))))
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
          .color(YELLOW);
    }

    record MoveAction(Location to) implements Action {
      @Override
      public void play(BlockGlitchReplay replay) {
        replay.fakeEntity.teleport(to).send(replay.viewer);
      }
    }

    @SuppressWarnings("deprecation")
    record PlaceAction(long place, boolean isGlitch) implements Action {
      private static final Material STAINED_GLASS =
          Materials.parse("STAINED_GLASS", "LEGACY_STAINED_GLASS");
      private static final BlockMaterialData BAD_BLOCK =
          MATERIAL_UTILS.fromLegacyBlock(STAINED_GLASS, DyeColor.WHITE.getWoolData());
      private static final BlockMaterialData GOOD_BLOCK =
          MATERIAL_UTILS.fromLegacyBlock(STAINED_GLASS, DyeColor.BROWN.getWoolData());

      @Override
      public void play(BlockGlitchReplay replay) {
        (isGlitch ? BAD_BLOCK : GOOD_BLOCK)
            .sendBlockChange(
                replay.viewer, BlockVectors.decodePos(place).toLocation(replay.viewer.getWorld()));
      }

      @Override
      public void clear(Player viewer) {
        Location loc = BlockVectors.decodePos(place).toLocation(viewer.getWorld());
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
