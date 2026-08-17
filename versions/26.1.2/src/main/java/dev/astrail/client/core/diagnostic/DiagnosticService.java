package dev.astrail.client.core.diagnostic;

import java.util.LinkedHashMap;
import java.util.Map;

/** Observable capability-ownership store shared by the input and rotation services. */
public final class DiagnosticService {
    private final Map<String, String> capabilityOwners = new LinkedHashMap<>();

    public synchronized void setCapabilityOwner(String capability, String owner) {
        if (owner == null || owner.isBlank()) {
            capabilityOwners.remove(capability);
        } else {
            capabilityOwners.put(capability, owner);
        }
    }

    public synchronized Map<String, String> capabilityOwners() {
        return Map.copyOf(capabilityOwners);
    }

    public synchronized void clear() {
        capabilityOwners.clear();
    }
}
