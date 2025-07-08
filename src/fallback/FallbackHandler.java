package fallback;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FallbackHandler {

    private static final FallbackHandler instance = new FallbackHandler();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicInteger activationCount = new AtomicInteger(0);
    private volatile long lastActivationTime = 0;
    private volatile String lastFailureReason = "";
    
    // Emergency protocols
    private final AtomicBoolean emergencyMode = new AtomicBoolean(false);
    private volatile boolean safetySystemsEngaged = false;

    private FallbackHandler() {}

    public static FallbackHandler getInstance() {
        return instance;
    }

    public void activate() {
        activate("System failure detected");
    }

    public void activate(String reason) {
        if (!active.get()) {
            active.set(true);
            lastActivationTime = System.currentTimeMillis();
            lastFailureReason = reason;
            int count = activationCount.incrementAndGet();
            
            System.out.println("[Fallback] ACTIVATED (#" + count + "): " + reason);
            System.out.println("[Fallback] Engaging safety protocols...");
            
            // Engage safety systems
            engageSafetySystems();
            
            // Check for emergency conditions
            if (count >= 3) {
                activateEmergencyMode();
            }
        }
    }

    private void engageSafetySystems() {
        safetySystemsEngaged = true;
        System.out.println("[Fallback] Safety systems engaged:");
        System.out.println("  - Lane holding mode activated");
        System.out.println("  - Speed reduction protocol initiated");
        System.out.println("  - Hazard lights activated");
        System.out.println("  - Emergency brake preparation enabled");
    }

    private void activateEmergencyMode() {
        if (!emergencyMode.get()) {
            emergencyMode.set(true);
            System.err.println("[Fallback] EMERGENCY MODE ACTIVATED!");
            System.err.println("[Fallback] Multiple system failures detected - initiating emergency protocols:");
            System.err.println("  - Controlled emergency stop sequence");
            System.err.println("  - Emergency communication to traffic management");
            System.err.println("  - Passenger notification system activated");
            System.err.println("  - Manual control transfer requested");
        }
    }

    public void deactivate() {
        if (active.get()) {
            active.set(false);
            safetySystemsEngaged = false;
            System.out.println("[Fallback] Deactivated: Resuming normal operation.");
            System.out.println("[Fallback] System recovery confirmed - safety systems disengaged.");
        }
    }

    public boolean isActive() {
        return active.get();
    }

    public boolean isEmergencyMode() {
        return emergencyMode.get();
    }

    public int getActivationCount() {
        return activationCount.get();
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }

    public long getTimeSinceLastActivation() {
        return lastActivationTime > 0 ? System.currentTimeMillis() - lastActivationTime : -1;
    }

    public void printSystemStatus() {
        System.out.println("[Fallback] System Status:");
        System.out.println("  - Active: " + active.get());
        System.out.println("  - Emergency Mode: " + emergencyMode.get());
        System.out.println("  - Safety Systems: " + (safetySystemsEngaged ? "ENGAGED" : "DISENGAGED"));
        System.out.println("  - Total Activations: " + activationCount.get());
        System.out.println("  - Last Failure: " + lastFailureReason);
    }
}