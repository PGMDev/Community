package dev.pgm.community.menu;

import java.util.List;
import org.bukkit.Material;

public class StaticMenuItem extends MenuItem {

  public StaticMenuItem(Material icon, String name, List<String> description) {
    super(icon, name, description);
  }
}
