package fr.nihilus.mymusic.di;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/**
 * Denote that the annotated class or component is alive as long the containing
 * activity instance is alive.
 * This annotation can especially be used to mark fragment classes.
 */
@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivityScoped {
}
