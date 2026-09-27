package com.example.authserver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MaskedField {

  /**
   * The target field name(s) to be masked.
   * Default value is empty if used directly on a specific field.
   */
  String[] value() default {};

  /**
   * Optional character to use for masking (default is '*').
   */
  char maskChar() default '*';
}