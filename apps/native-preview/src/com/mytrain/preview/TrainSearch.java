package com.mytrain.preview;
import java.util.Locale;
public final class TrainSearch {
  private TrainSearch() {}
  public static boolean matches(String number, String name, String query) {
    String term = query.trim().toLowerCase(Locale.ROOT);
    return !term.isEmpty() && (number.equals(term) || name.toLowerCase(Locale.ROOT).contains(term));
  }
  public static String time(int minute) {
    return String.format(Locale.ROOT, "%02d:%02d · Day %d", (minute / 60) % 24, minute % 60, minute / 1440 + 1);
  }
}
