package dev.pgm.community.assistance.menu;

import dev.pgm.community.menu.MenuItem;
import java.util.List;
import org.bukkit.Material;

public class ReportCategory extends MenuItem {

  private final List<ReportReason> reasons;

  public ReportCategory(
      String name, List<String> description, Material icon, List<ReportReason> reasons) {
    super(icon, name, description);
    this.reasons = reasons;
  }

  public List<ReportReason> getReasons() {
    return reasons;
  }
}
