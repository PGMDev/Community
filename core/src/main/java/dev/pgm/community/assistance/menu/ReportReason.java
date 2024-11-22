package dev.pgm.community.assistance.menu;

import dev.pgm.community.menu.MenuItem;
import java.util.List;
import org.bukkit.Material;

public class ReportReason extends MenuItem {

  public ReportReason(String name, List<String> description, Material icon) {
    super(icon, name, description);
  }
}
