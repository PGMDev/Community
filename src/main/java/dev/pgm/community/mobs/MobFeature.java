package dev.pgm.community.mobs;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.feature.FeatureBase;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;

public class MobFeature extends FeatureBase {

  public static final float DEFAULT_SPEED = 1.2f;

  private Map<UUID, UUID> followTargets;
  private Set<UUID> attackers;

  private BukkitTask task;
  private float speed;

  public MobFeature(Configuration config, Logger logger) {
    super(new MobConfig(config), logger, "Mobs");
    this.followTargets = Maps.newHashMap();
    this.attackers = Sets.newHashSet();
    this.speed = DEFAULT_SPEED;
    if (getMobConfig().isEnabled()) {
      enable();
    }
  }

  @Override
  public void enable() {
    super.enable();
    this.task =
        Community.get()
            .getServer()
            .getScheduler()
            .runTaskTimer(Community.get(), this::updateFollows, 0L, 10L);
  }

  @Override
  public void disable() {
    super.disable();
    if (this.task != null) {
      this.task.cancel();
      this.task = null;
    }
  }

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() != null
        && event.getDamager() instanceof Player
        && event.getEntity() instanceof Player) {
      Player damager = (Player) event.getDamager();
      Player target = (Player) event.getEntity();

      if (isAttacker(damager.getUniqueId())) {
        setTarget(damager, target);
      }
    }
  }

  public void updateFollows() {
    this.followTargets
        .entrySet()
        .forEach(
            entry -> {
              Player owner = Bukkit.getPlayer(entry.getKey());
              Player target = Bukkit.getPlayer(entry.getValue());
              if (owner != null && target != null) {
                this.getOwnedMobs(owner)
                    .forEach(
                        mob -> {
                          follow(mob, target.getLocation());
                          if (mob instanceof Creature && attackers.contains(owner.getUniqueId())) {
                            Creature creature = (Creature) mob;
                            creature.setTarget(target);
                          }
                        });
              }
            });
  }

  public boolean isFollower(UUID playerId) {
    return followTargets.containsKey(playerId);
  }

  public boolean isAttacker(UUID playerId) {
    return attackers.contains(playerId);
  }

  public void setSpeed(float speed) {
    this.speed = speed;
  }

  public void setTarget(Player player, Player target) {
    if (target == null) {
      this.followTargets.remove(player.getUniqueId());
    } else {
      this.followTargets.put(player.getUniqueId(), target.getUniqueId());
    }
  }

  private void follow(LivingEntity mob, Location location) {
    EntityInsentient nmsMob = ((EntityInsentient) ((CraftEntity) mob).getHandle());
    nmsMob.getNavigation().a(location.getX(), location.getY(), location.getZ(), speed);
  }

  public MobConfig getMobConfig() {
    return (MobConfig) getConfig();
  }

  public void spawn(Player sender, EntityType type, int amount, boolean canDie) {
    for (int i = 0; i < amount; i++) {
      spawn(sender, type, canDie);
    }
  }

  public void spawn(Player sender, EntityType type, boolean canDie) {
    Entity entity = sender.getLocation().getWorld().spawnEntity(sender.getLocation(), type);

    if (entity instanceof LivingEntity) {
      LivingEntity mob = (LivingEntity) entity;
      if (!canDie) {
        mob.setMaxHealth(Integer.MAX_VALUE);
        mob.setHealth(mob.getMaxHealth());
      }
      mob.setMetadata("owner", new FixedMetadataValue(Community.get(), sender.getName()));
    }
  }

  public List<LivingEntity> getOwnedMobs(Player sender) {
    return sender.getWorld().getLivingEntities().stream()
        .filter(le -> le.hasMetadata("owner"))
        .filter(
            le -> {
              List<MetadataValue> values = le.getMetadata("owner");
              Optional<MetadataValue> value =
                  values.stream()
                      .filter(mv -> mv.getOwningPlugin().equals(Community.get()))
                      .findAny();
              return value.isPresent() && value.get().asString().equalsIgnoreCase(sender.getName());
            })
        .collect(Collectors.toList());
  }

  public int tphere(Player sender) {
    List<LivingEntity> mobs = getOwnedMobs(sender);
    mobs.forEach(mob -> mob.teleport(sender));
    return mobs.size();
  }

  public int heal(Player sender) {
    List<LivingEntity> mobs = getOwnedMobs(sender);
    mobs.forEach(mob -> mob.setHealth(mob.getMaxHealth()));
    return mobs.size();
  }

  public int remove(Player sender) {
    List<LivingEntity> mobs = getOwnedMobs(sender);
    mobs.forEach(mob -> mob.remove());
    return mobs.size();
  }

  public boolean toggleFollow(Player sender) {
    UUID playerId = sender.getUniqueId();

    // Unset attackers when switching to follow
    if (this.attackers.contains(playerId)) {
      this.attackers.remove(playerId);
    }

    if (this.followTargets.containsKey(playerId)) {
      this.followTargets.remove(playerId);
    } else {
      this.followTargets.put(playerId, playerId);
    }

    return this.followTargets.containsKey(playerId);
  }

  public void attack(Player sender, Player target) {
    setTarget(sender, target);
    this.attackers.add(sender.getUniqueId());
  }

  public boolean toggleAutoAttack(Player sender) {
    UUID playerId = sender.getUniqueId();

    if (this.attackers.contains(sender.getUniqueId())) {
      this.attackers.remove(playerId);
    } else {
      this.attackers.add(playerId);
    }
    return attackers.contains(playerId);
  }
}
