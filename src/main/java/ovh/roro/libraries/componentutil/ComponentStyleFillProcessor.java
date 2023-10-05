package ovh.roro.libraries.componentutil;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ComponentStyleFillProcessor<T> {

    @Nullable T process(char value, int index);

}
