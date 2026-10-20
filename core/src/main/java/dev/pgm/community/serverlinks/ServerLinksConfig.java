package dev.pgm.community.serverlinks;

import static tc.oc.pgm.util.text.TextParser.parseComponent;
import static tc.oc.pgm.util.text.TextParser.parseEnum;
import static tc.oc.pgm.util.text.TextParser.parseUri;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import dev.pgm.community.serverlinks.types.ServerLink;
import dev.pgm.community.serverlinks.types.ServerLinkBuiltinType;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.Configuration;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ServerLinksConfig extends FeatureConfigImpl {
  private static final String KEY = "server-links";
  private static final String LINKS_KEY = "links";

  private static final String LINK_BUILTIN_KEY = "builtin";
  private static final String LINK_CUSTOM_TEXT_KEY = "text";
  private static final String LINK_URI_KEY = "uri";

  private @Nullable @Unmodifiable List<ServerLink> links;

  public ServerLinksConfig(Configuration config) {
    super(KEY, config);
  }

  public @Unmodifiable List<ServerLink> getLinks() {
    assert links != null;
    return links;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    links = config.getMapList(getKey() + "." + LINKS_KEY).stream()
        .map(this::readLink)
        .toList();
  }

  private ServerLink readLink(Map<?, ?> configData) {
    String builtIn = Objects.toString(configData.get(LINK_BUILTIN_KEY), null);
    String customText = Objects.toString(configData.get(LINK_CUSTOM_TEXT_KEY), null);
    String uri = Objects.toString(configData.get(LINK_URI_KEY), null);

    if ((builtIn == null) == (customText == null)) {
      throw new IllegalStateException(
          "A server link must have either built-in or custom text defined");
    }

    URI parsedUri = parseUri(uri);
    if (!parsedUri.getScheme().equals("http") && !parsedUri.getScheme().equals("https")) {
      throw new IllegalStateException("The URL " + uri + " is not a web URL");
    }

    return new ServerLink(
        builtIn != null ? parseEnum(builtIn, ServerLinkBuiltinType.class) : null,
        customText != null ? parseComponent(customText) : null,
        parsedUri);
  }
}
