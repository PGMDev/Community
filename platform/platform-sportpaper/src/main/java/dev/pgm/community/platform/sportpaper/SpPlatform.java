package dev.pgm.community.platform.sportpaper;

import static dev.pgm.community.util.Supports.Priority.HIGHEST;
import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import dev.pgm.community.util.Platform;
import dev.pgm.community.util.Supports;

@Supports(value = SPORTPAPER, priority = HIGHEST)
public class SpPlatform implements Platform.Manifest {}
