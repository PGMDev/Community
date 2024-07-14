package dev.pgm.community.platform;

import static org.reflections.scanners.Scanners.TypesAnnotated;

import org.reflections.util.ConfigurationBuilder;

public interface Reflections {
  org.reflections.Reflections INSTANCE = new org.reflections.Reflections(new ConfigurationBuilder()
      .forPackage("dev.pgm.community.platform")
      .setScanners(TypesAnnotated));
}
