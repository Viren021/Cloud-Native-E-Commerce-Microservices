package com.ecommerce.commonlib.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Logs method entry (with arguments) and exit (with result or exception).
 * Useful for service and repository methods during development/debugging.
 *
 * <pre>{@code
 * @Loggable
 * public CartDto findById(Integer cartId) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {

    /** Log only value-free argument metadata on entry. Off by default. */
    boolean logInput() default false;

    /** Log only value-free result metadata on exit. Off by default. */
    boolean logOutput() default false;
}
