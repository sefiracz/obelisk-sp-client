package cz.sefira.obelisk;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.Pair
 *
 * Created: 06.12.2021
 * Author: hlavnicka
 */

public class Pair<U, V> {

  private final U first;
  private final V second;

  public static <U, V> Pair<U, V> getInstance(U first, V second) {
    return new Pair<>(first, second);
  }

  private Pair(U first, V second) {
    this.first = first;
    this.second = second;
  }

  public U getFirst() {
    return first;
  }

  public V getSecond() {
    return second;
  }
}