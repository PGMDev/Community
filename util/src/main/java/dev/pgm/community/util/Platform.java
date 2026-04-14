package dev.pgm.community.util;

import static org.reflections.scanners.Scanners.TypesAnnotated;

import dev.pgm.community.util.Supports.Variant;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
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
  private static final Pattern VERSION_MATCHER =
      Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{1,2})");

  public static final Version MINECRAFT_VERSION;
  public static final Variant VARIANT;

  private static Version parseServerVersion(final @NonNull String versionName) {
    var matcher = VERSION_MATCHER.matcher(versionName);
    return matcher.find()
        ? new Version(
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3)))
        : new Version(0, 0, 0);
  }

  static {
    var sv = Bukkit.getServer();
    MINECRAFT_VERSION = parseServerVersion(sv.getBukkitVersion());
    VARIANT = Arrays.stream(Supports.Variant.values())
        .filter(v -> v.matcher.test(sv))
        .findFirst()
        .orElse(null);
  }

  public static final @NonNull Manifest MANIFEST = get(Manifest.class);

  /**
   * Do a minimum sanity-check of the platform's viability and early-load some codepaths
   *
   * @throws Throwable could throw even class not found issues if loading in the wrong version
   */
  public static void init() throws Throwable {
    Effects.EFFECTS.dummy();
  }

  public static <T> @NonNull T get(Class<T> clazz) {
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
        if (!sup.minVersion().isEmpty()) {
          Version min = TextParser.parseVersion(sup.minVersion());
          if (MINECRAFT_VERSION.isOlderThan(min)) continue;
        }

        if (!sup.maxVersion().isEmpty()) {
          Version max = TextParser.parseVersion(sup.maxVersion());
          if (max.isOlderThan(MINECRAFT_VERSION)) continue;
        }

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
    default void onEnable(Plugin plugin) {}

    default void onDisable() {}
  }
}
