package io.github.zap.party.audience;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * An {@link Audience} that renders {@link Component}s before sending messages.
 */
public class PreRenderedAudience implements Audience {

    private final Audience delegate;

    private final TranslatableComponentRenderer<Locale> renderer;

    private final Locale locale;

    public PreRenderedAudience(@NotNull Audience audience,
                               @NotNull TranslatableComponentRenderer<Locale> renderer, @NotNull Locale locale) {
        this.delegate = audience;
        this.renderer = renderer;
        this.locale = locale;
    }

    @Override
    public void sendMessage(final @NonNull Identity source, final @NonNull Component message,
                            final @NonNull MessageType type) {
        this.delegate.sendMessage(source, this.renderer.render(message, locale), type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PreRenderedAudience that = (PreRenderedAudience) o;

        return this.delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }
}
