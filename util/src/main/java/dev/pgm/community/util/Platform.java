package dev.pgm.community.util;

import static org.reflections.scanners.Scanners.TypesAnnotated;

import dev.pgm.community.util.Supports.Variant;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.reflect.ReflectionUtils;
import tc.oc.pgm.util.text.TextParser;

@SuppressWarnings("unchecked")
public abstract class Platform {
  private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
      .addUrls(ClasspathHelper.forPackage("dev.pgm.community", Platform.class.getClassLoader()))
      .forPackage("dev.pgm.community.platform")
      .setScanners(TypesAnnotated));

  public static final Version MINECRAFT_VERSION;
  public static final Variant VARIANT;

  static {
    var sv = Bukkit.getServer();
    MINECRAFT_VERSION = TextParser.parseVersion(sv.getBukkitVersion().split("-")[0]);
    VARIANT = Arrays.stream(Supports.Variant.values())
        .filter(v -> v.matcher.test(sv))
        .findFirst()
        .orElse(null);
  }

  public static final @NotNull Manifest MANIFEST = get(Manifest.class);

  /**
   * Do a minimum sanity-check of the platform's viability and early-load some codepaths
   *
   * @throws Throwable could throw even class not found issues if loading in the wrong version
   */
  public static void init() throws Throwable {
    Effects.EFFECTS.dummy();
  }

  public static <T> @NotNull T get(Class<T> clazz) {
    return (T) Platform.getBestSupported(clazz);
  }

  private static <T> Iterable<Class<?>> getSupported(Class<T> parent) {
    return REFLECTIONS.get(TypesAnnotated.with(Supports.class, Supports.List.class)
        .asClass()
        .filter(parent::isAssignableFrom));
  }

  private static Object getBestSupported(Class<?> parent) {
    Class<?> result = null;
    Supports.Priority priority = null;
    for (Class<?> clazz : getSupported(parent)) {
      Supports[] supportList = clazz.getDeclaredAnnotationsByType(Supports.class);
      for (Supports sup : supportList) {
        if (VARIANT != sup.value()) continue;
        if (!sup.minVersion().isEmpty()
            && MINECRAFT_VERSION.isOlderThan(TextParser.parseVersion(sup.minVersion()))) continue;
        if (!sup.maxVersion().isEmpty()
            && TextParser.parseVersion(sup.maxVersion()).isOlderThan(MINECRAFT_VERSION)) continue;

        if (priority == null || priority.compareTo(sup.priority()) < 0) {
          priority = sup.priority();
          result = clazz;
        }
      }
    }
    if (result == null)
      throw new UnsupportedOperationException(
          "Current server software platform does not have an impl for: " + parent.getSimpleName());

    return ReflectionUtils.callConstructor(ReflectionUtils.getConstructor(result));
  }

  public interface Manifest {
    void onEnable(Plugin plugin);
  }
}
