package dev.pgm.community.platform.sportpaper;

import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import dev.pgm.community.util.EntityUtils;
import dev.pgm.community.util.Supports;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;

@Supports(SPORTPAPER)
public class SpEntityUtils implements EntityUtils {
  @Override
  public void follow(LivingEntity mob, Location location, float speed) {
    EntityInsentient nmsMob = ((EntityInsentient) ((CraftEntity) mob).getHandle());
    nmsMob.getNavigation().a(location.getX(), location.getY(), location.getZ(), speed);
  }
}
