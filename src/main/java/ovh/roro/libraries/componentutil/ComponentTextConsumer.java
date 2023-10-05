package ovh.roro.libraries.componentutil;

import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
interface ComponentTextConsumer {

    void accept(@NotNull Style style, @Nullable String text);

}
