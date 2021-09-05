package io.github.zap.party.list;

import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.MessageFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.text.AttributedCharacterIterator;
import java.util.List;
import java.util.Locale;

/**
 * Uses ICU's formatting to create lists of {@link Component}s
 */
public final class ListFormatUtil {

    /**
     * Puts {@link Component}s in a list.
     * @param locale The {@link Locale} used to format the list
     * @param parts The {@link Component}s to put in the list
     * @return A new {@link Component} that has the formatted {@link Component}s
     */
    public static @NotNull Component list(@NotNull Locale locale, @NotNull List<Component> parts) {
        if (parts.isEmpty()) {
            return Component.empty();
        }

        ListFormatter listFormatter = ListFormatter.getInstance(locale);
        String format = listFormatter.getPatternForNumItems(parts.size());

        Object[] nulls = new Object[parts.size()];
        MessageFormat messageFormat = new MessageFormat(format);
        StringBuffer sb = messageFormat.format(nulls, new StringBuffer(), null);
        AttributedCharacterIterator it = messageFormat.formatToCharacterIterator(nulls);

        TextComponent.Builder builder = Component.text();
        while (it.getIndex() < it.getEndIndex()) {
            int end = it.getRunLimit();
            Integer index = (Integer) it.getAttribute(MessageFormat.Field.ARGUMENT);
            if (index != null) {
                builder.append(parts.get(index));
            } else {
                builder.append(Component.text(sb.substring(it.getIndex(), end)));
            }
            it.setIndex(end);
        }

        return builder.build();
    }

}
