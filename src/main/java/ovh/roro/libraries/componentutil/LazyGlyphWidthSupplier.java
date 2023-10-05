package ovh.roro.libraries.componentutil;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

class LazyGlyphWidthSupplier {

    private final @NotNull Component component;

    public LazyGlyphWidthSupplier(@NotNull Component component) {
        this.component = component;
    }

    private boolean initialized;
    private float width;

    public float getWidth() {
        if (!this.initialized) {
            this.initialized = true;
            this.width = ComponentUtil.widthVanilla(this.component);
        }

        return this.width;
    }
}
