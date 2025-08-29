package ovh.roro.libraries.componentutil;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

class ComponentCenterer {

    private static final @NotNull LazyGlyphWidthSupplier SPACE_WIDTH = new LazyGlyphWidthSupplier(Component.literal(" "));

    private @Nullable MutableComponent result;

    public @NotNull net.minecraft.network.chat.Component apply(@NotNull net.minecraft.network.chat.Component input) {
        net.minecraft.network.chat.Component lastComponent = this.splitAndCollectNewlines(
                input,
                input.getStyle(),
                null,
                component -> {
                    this.result = this.setOrAppend(this.result, this.centerSingleLine(component));
                }
        );

        if (lastComponent != null) {
            this.result = this.setOrAppend(this.result, this.centerSingleLine(lastComponent));
        }

        return Objects.requireNonNullElseGet(this.result, net.minecraft.network.chat.Component::empty);
    }

    private @Nullable MutableComponent splitAndCollectNewlines(
            @NotNull net.minecraft.network.chat.Component component,
            @NotNull Style componentStyle,
            @Nullable MutableComponent currentComponent,
            @NotNull Consumer<Component> consumer
    ) {
        if (component.getContents() instanceof PlainTextContents contents) {
            String text = contents.text();
            String[] lines = this.splitNewline(text);

            for (String line : lines) {
                currentComponent = this.setOrAppend(currentComponent, Component.literal(line).withStyle(componentStyle).append("\n"));

                consumer.accept(currentComponent);
                currentComponent = null;
            }
        } else {
            currentComponent = this.setOrAppend(currentComponent, MutableComponent.create(component.getContents()).withStyle(componentStyle));
        }

        for (net.minecraft.network.chat.Component sibling : component.getSiblings()) {
            currentComponent = this.splitAndCollectNewlines(sibling, sibling.getStyle().applyTo(componentStyle), currentComponent, consumer);
        }

        return currentComponent;
    }

    private @NotNull MutableComponent setOrAppend(
            @Nullable MutableComponent currentComponent,
            @NotNull MutableComponent toAppend
    ) {
        if (currentComponent == null) {
            return toAppend;
        }

        return currentComponent.append(toAppend);
    }

    private @NotNull MutableComponent centerSingleLine(@NotNull net.minecraft.network.chat.Component component) {
        float componentHalfWidth = ovh.roro.libraries.componentutil.ComponentUtil.widthVanilla(component) / 2.0F;
        float toCompensate = ovh.roro.libraries.componentutil.ComponentUtil.HALF_CHAT_BOX_WIDTH - componentHalfWidth;
        float spaceWidth = ComponentCenterer.SPACE_WIDTH.getWidth();
        float compensated = 0.0F;
        int spaceCount = 0;

        while (compensated < toCompensate) {
            spaceCount++;
            compensated += spaceWidth;
        }

        return net.minecraft.network.chat.Component.literal(" ".repeat(spaceCount)).append(component);
    }

    private @NotNull String @NotNull [] splitNewline(@NotNull String input) {
        Preconditions.checkNotNull(input);

        List<String> list = new ObjectArrayList<>();

        int index = input.indexOf('\n');
        int lastIndex = 0;
        while (index != -1) {
            list.add(input.substring(lastIndex, index));

            lastIndex = index + 1;
            index = input.indexOf('\n', index + 1);
        }

        if (lastIndex != input.length()) {
            list.add(input.substring(lastIndex));
        }

        return list.toArray(String[]::new);
    }
}
