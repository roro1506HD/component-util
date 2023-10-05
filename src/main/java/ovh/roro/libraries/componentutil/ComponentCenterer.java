package ovh.roro.libraries.componentutil;

import com.google.common.base.Preconditions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class ComponentCenterer {

    private static final @NotNull LazyGlyphWidthSupplier SPACE_WIDTH = new LazyGlyphWidthSupplier(Component.literal(" "));

    private @Nullable MutableComponent result;
    private @Nullable MutableComponent currentLine;

    private void visit(@NotNull Component component, ComponentTextConsumer consumer) {
        ComponentContents contents = component.getContents();

        if (contents == ComponentContents.EMPTY) {
            consumer.accept(Style.EMPTY, null);
        } else if (contents instanceof LiteralContents literalContents) {
            String text = literalContents.text();

            if (text.isEmpty()) {
                consumer.accept(component.getStyle(), null);
            } else {
                consumer.accept(component.getStyle(), text);
            }
        } else {
            throw new IllegalArgumentException("Invalid component type, cannot center component (impossible to guess width): " + contents);
        }

        for (Component sibling : component.getSiblings()) {
            this.visit(sibling, consumer);
        }
    }

    public @NotNull Component apply(@NotNull Component component) {
        this.visit(component, (style, text) -> {
            if (text == null) {
                this.applyCurrentLine(Component.empty().setStyle(style));
                return;
            }

            boolean endsWithNewLine = text.endsWith("\n");
            String[] lines = text.split("\n");

            if (lines.length > 0) {
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];

                    if (this.isNotEmpty(this.currentLine, false)) {
                        this.applyResult();

                        if (endsWithNewLine || i != lines.length - 1) {
                            line += "\n";
                        }
                    }

                    this.applyCurrentLine(Component.literal(line).setStyle(style));
                }
            } else if (endsWithNewLine) {
                this.applyCurrentLine(Component.literal("\n"));
                this.applyResult();
            }
        });

        if (this.currentLine != null) {
            this.applyResult();
        }

        return Objects.requireNonNullElseGet(this.result, Component::empty);
    }

    private boolean isNotEmpty(@Nullable Component component, boolean countNewlineAsEmpty) {
        if (component == null) {
            return false;
        }

        if (component.getContents() != ComponentContents.EMPTY) {
            return !countNewlineAsEmpty ||
                    !(component.getContents() instanceof LiteralContents literalContents) ||
                    !"\n".equals(literalContents.text());
        }

        return !component.getSiblings().isEmpty();
    }

    private void applyCurrentLine(@NotNull MutableComponent component) {
        if (this.currentLine != null) {
            this.currentLine.append(component);
        } else {
            this.currentLine = component;
        }
    }

    private void applyResult() {
        Preconditions.checkNotNull(this.currentLine);

        if (this.isNotEmpty(this.currentLine, true)) {
            this.currentLine = this.centerSingleLine(this.currentLine);
        }

        if (this.result != null) {
            this.result.append(this.currentLine);
        } else {
            this.result = this.currentLine;
        }

        this.currentLine = null;
    }

    private @NotNull MutableComponent centerSingleLine(@NotNull Component component) {
        float componentHalfWidth = ComponentUtil.widthVanilla(component) / 2.0F;
        float toCompensate = ComponentUtil.HALF_CHAT_BOX_WIDTH - componentHalfWidth;
        float spaceWidth = ComponentCenterer.SPACE_WIDTH.getWidth();
        float compensated = 0.0F;
        int spaceCount = 0;

        while (compensated < toCompensate) {
            spaceCount++;
            compensated += spaceWidth;
        }

        return Component.literal(" ".repeat(spaceCount)).append(component);
    }
}
