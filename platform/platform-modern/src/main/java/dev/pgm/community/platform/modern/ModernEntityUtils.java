package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Variant.PAPER;

import dev.pgm.community.util.EntityUtils;
import dev.pgm.community.util.Supports;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernEntityUtils implements EntityUtils {
  @Override
  public void follow(LivingEntity mob, Location location, float speed) {
    Entity handle = ((CraftEntity) mob).getHandle();
    if (handle instanceof PathfinderMob pathfinderMob) {
      pathfinderMob
          .getNavigation()
          .moveTo(location.getX(), location.getY(), location.getZ(), speed);
    }
  }
}
