package dev.pgm.community.freeze;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class FreezeConfig extends FeatureConfigImpl {

  private static final String KEY = "freeze";

  private boolean pgmIntegration;
  private int itemSlot;

  public FreezeConfig(Configuration config) {
    super(KEY, config);
  }

  public boolean isIntegrationEnabled() {
    return pgmIntegration;
  }

  public int getItemSlot() {
    return itemSlot;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.pgmIntegration = config.getBoolean(getKey() + ".pgm-integration");
    this.itemSlot = config.getInt(getKey() + ".item-slot");
  }
}
