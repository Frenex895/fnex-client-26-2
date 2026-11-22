package dev.fnex.module;

public enum Category {
    VISUALS("VISUALS"), PLAYER("PLAYER"), WORLD("WORLD"), SETTINGS("SETTINGS");
    public final String label;
    Category(String l) { label = l; }
}
