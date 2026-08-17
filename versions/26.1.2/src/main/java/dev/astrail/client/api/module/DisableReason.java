package dev.astrail.client.api.module;

public enum DisableReason {
    USER,
    WORLD_CHANGED,
    CLIENT_STOPPING,
    FAILURE,
    /**
     * Transient auto-shutdown performed by Macro Protect. Not a user choice,
     * so the module's persisted enable intent survives and can be restored.
     */
    PROTECTED
}
