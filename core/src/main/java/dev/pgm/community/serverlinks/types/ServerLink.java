package dev.pgm.community.serverlinks.types;

import java.net.URI;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a Minecraft server link.
 *
 * @param builtinType The built-in type of the server link, or null if it's a custom link.
 * @param customText The custom text for the server link, or null if builtinType is set.
 * @param uri The URI of the server link.
 */
@NullMarked
public record ServerLink(
    @Nullable ServerLinkBuiltinType builtinType, @Nullable Component customText, URI uri) {}
