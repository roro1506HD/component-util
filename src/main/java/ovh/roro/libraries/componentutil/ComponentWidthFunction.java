package ovh.roro.libraries.componentutil;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
interface ComponentWidthFunction<T> {

    float apply(@NotNull T component);

}
