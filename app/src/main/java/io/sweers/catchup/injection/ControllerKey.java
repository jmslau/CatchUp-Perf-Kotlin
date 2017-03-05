package io.sweers.catchup.injection;

import com.bluelinelabs.conductor.Controller;
import dagger.MapKey;
import dagger.internal.Beta;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/** {@link MapKey} annotation to key bindings by a type of a {@link Controller}. */
@Beta
@MapKey
@Target(METHOD)
public @interface ControllerKey {
  Class<? extends Controller> value();
}