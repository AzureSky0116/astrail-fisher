package dev.astrail.client.ui.component;

/** Semantic Inter faces authored in design pixels, independent of GUI scale. */
public enum UiTextStyle {
    LOGO("LOGO"),
    CLIENT("CLIENT"),
    SEARCH("CLIENT"),
    MENU("MENU"),
    MENU_ACTIVE("MENU_ACTIVE"),
    TOP_TAB("MENU"),
    TOP_TAB_ACTIVE("MENU_ACTIVE"),
    MODULE_TITLE("MODULE_TITLE"),
    DESCRIPTION("DESCRIPTION"),
    SETTINGS_TITLE("SETTINGS_TITLE"),
    SETTING_LABEL("SETTING_LABEL"),
    AUXILIARY("AUXILIARY"),
    AUXILIARY_MEDIUM("AUXILIARY_MEDIUM"),
    AUXILIARY_SEMIBOLD("AUXILIARY_SEMIBOLD");

    private final String atlasName;

    UiTextStyle(String atlasName) {
        this.atlasName = atlasName;
    }

    String atlasName() {
        return atlasName;
    }
}
