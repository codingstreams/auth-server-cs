package com.example.authserver;

public interface Maskable {
  /**
   * Generates a string representation of the object where sensitive
   * fields annotated with @MaskedField are masked.
   *
   * @return A masked string representation of the object.
   */
  String toMaskedString();
}