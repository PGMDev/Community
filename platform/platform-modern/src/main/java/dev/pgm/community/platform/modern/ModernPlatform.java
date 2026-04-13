package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Priority.HIGHEST;
import static dev.pgm.community.util.Supports.Variant.PAPER;

import dev.pgm.community.util.Platform;
import dev.pgm.community.util.Supports;

@Supports(value = PAPER, minVersion = "1.21.11", priority = HIGHEST)
public class ModernPlatform implements Platform.Manifest {
  @Override
  public void onEnable() {
    new PacketManipulations();
  }
}
