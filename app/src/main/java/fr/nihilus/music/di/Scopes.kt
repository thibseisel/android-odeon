package fr.nihilus.music.di

import javax.inject.Scope

/**
 * Denote that the annotated class or component is alive as long the containing
 * activity instance is alive.
 * This annotation can especially be used to mark fragment classes.
 */
@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class ActivityScoped

/**
 * Denote that the annotated class or component is alive as long as the enclosing service
 * instance is alive.
 */
@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class ServiceScoped

@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class FragmentScoped