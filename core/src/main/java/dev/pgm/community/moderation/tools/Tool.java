package dev.pgm.community.moderation.tools;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;

public interface Tool {

  void onInteract(ObserverInteractEvent event);

  void give(Player player);

  String getName();

  List<String> getLore();

  Material getMaterial();

  ItemStack getItem();
}
