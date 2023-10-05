package ovh.roro.libraries.componentutil;

import com.google.common.base.Preconditions;
import io.papermc.paper.adventure.PaperAdventure;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringDecomposer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ComponentUtil {

    public static final int DEFAULT_CHAT_BOX_WIDTH = 300; // It's actually 320px, but we remove 20 for people that reduce it slightly
    public static final int HALF_CHAT_BOX_WIDTH = ComponentUtil.DEFAULT_CHAT_BOX_WIDTH / 2;

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger("ComponentUtil");
    private static final @NotNull Int2ObjectMap<GlyphData> GLYPH_DATA = new Int2ObjectOpenHashMap<>();
    private static final int NEWLINE_CODE_POINT = '\n';

    public static void load(@NotNull JavaPlugin plugin) {
        try (InputStream inputStream = plugin.getClass().getResourceAsStream("/font_data")) {
            ComponentUtil.load(new DataInputStream(Objects.requireNonNull(inputStream, "font_data not found")));
        } catch (IOException ex) {
            ComponentUtil.LOGGER.error("An error occurred while loading ComponentUtil's font data", ex);
        }
    }

    public static float widthAdventure(@NotNull Component component) {
        return ComponentUtil.widthVanilla(PaperAdventure.asVanilla(component));
    }

    public static float widthVanilla(@NotNull net.minecraft.network.chat.Component component) {
        MutableFloat width = new MutableFloat();

        StringDecomposer.iterateFormatted(component, net.minecraft.network.chat.Style.EMPTY, (index, style, codePoint) -> {
            if (codePoint == ComponentUtil.NEWLINE_CODE_POINT) {
                return true;
            }

            GlyphData glyphData = ComponentUtil.GLYPH_DATA.get(codePoint);

            if (style.isBold()) {
                width.add(glyphData.boldWidth());
            } else {
                width.add(glyphData.normalWidth());
            }

            return true;
        });

        return width.floatValue();
    }

    public static @NotNull Component centerAdventure(@NotNull Component component) {
        net.minecraft.network.chat.Component componentVanilla = PaperAdventure.asVanilla(component);
        net.minecraft.network.chat.Component resultVanilla = ComponentUtil.centerVanilla(componentVanilla);

        return PaperAdventure.asAdventure(resultVanilla);
    }

    public static @NotNull net.minecraft.network.chat.Component centerVanilla(@NotNull net.minecraft.network.chat.Component component) {
        return new ComponentCenterer().apply(component);
    }

    public static @NotNull Component fillAdventure(
            @NotNull String characters,
            @Nullable ComponentStyleFillProcessor<Style> processor
    ) {
        return ComponentUtil.<Component, Style>fill(
                characters,
                processor,
                Component::empty,
                Component::text,
                ComponentUtil::widthAdventure,
                Component::style,
                Component::append
        );
    }

    public static @NotNull net.minecraft.network.chat.Component fillVanilla(
            @NotNull String characters,
            @Nullable ComponentStyleFillProcessor<net.minecraft.network.chat.Style> processor
    ) {
        return ComponentUtil.fill(
                characters,
                processor,
                net.minecraft.network.chat.Component::empty,
                net.minecraft.network.chat.Component::literal,
                ComponentUtil::widthVanilla,
                MutableComponent::setStyle,
                MutableComponent::append
        );
    }

    private static <T, U> @NotNull T fill(
            @NotNull String characters,
            @Nullable ComponentStyleFillProcessor<U> processor,
            @NotNull Supplier<T> emptyComponentSupplier,
            @NotNull Function<String, T> componentFromString,
            @NotNull ComponentWidthFunction<T> componentToWidth,
            @NotNull BiFunction<T, U, T> styleApplier,
            @NotNull BiFunction<T, T, T> componentAppender
    ) {
        Preconditions.checkArgument(!characters.isEmpty(), "There must be at least one character");

        char[] chars = characters.toCharArray();
        List<T> components = new ObjectArrayList<>();

        float currentWidth = 0.0F;
        int index = 0;

        while (currentWidth < ComponentUtil.DEFAULT_CHAT_BOX_WIDTH) {
            char value = chars[index++ % chars.length];
            T component = componentFromString.apply(Character.toString(value));

            if (processor != null) {
                U style = processor.process(value, index - 1);

                if (style != null) {
                    component = styleApplier.apply(component, style);
                }
            }

            currentWidth += componentToWidth.apply(component);

            components.add(component);
        }

        T empty = emptyComponentSupplier.get();
        T result = empty;

        for (int i = components.size() - 1; i >= 0; i--) {
            if (result == empty) {
                result = components.get(i);
            } else {
                result = componentAppender.apply(components.get(i), result);
            }
        }

        return result;
    }

    private static void load(@NotNull DataInputStream inputStream) throws IOException {
        inputStream.readUTF(); // Font's ResourceLocation

        int codePoints = ComponentUtil.readVarInt(inputStream);

        for (int i = 0; i < codePoints; i++) {
            int codePoint = ComponentUtil.readVarInt(inputStream);
            float boldWidth = ComponentUtil.readWidth(inputStream);
            float normalWidth = ComponentUtil.readWidth(inputStream);

            ComponentUtil.GLYPH_DATA.put(codePoint, new GlyphData(boldWidth, normalWidth));
        }

        // "Missing" data
        float boldWidth = ComponentUtil.readWidth(inputStream);
        float normalWidth = ComponentUtil.readWidth(inputStream);

        ComponentUtil.GLYPH_DATA.defaultReturnValue(new GlyphData(boldWidth, normalWidth));
    }

    private static float readWidth(@NotNull DataInputStream inputStream) throws IOException {
        byte whole = inputStream.readByte();
        byte decimal = inputStream.readByte();

        return whole + (decimal / 10.0F);
    }

    // https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/ProtocolUtils.java#L100C3-L111C4
    private static int readVarInt(@NotNull DataInputStream inputStream) throws IOException {
        int i = 0;
        int maxRead = Math.min(5, inputStream.available());
        for (int j = 0; j < maxRead; j++) {
            int k = inputStream.readByte();
            i |= (k & 0x7F) << j * 7;
            if ((k & 0x80) != 128) {
                return i;
            }
        }
        throw new IllegalStateException("Bad VarInt decoded");
    }
}
