package heartbeat;

import fallback.FallbackHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SocketHeartbeatMonitor implements IHeartbeatMonitor {

    private final int port;
    private volatile boolean running = false;
    private volatile long lastHeartbeatTime = System.currentTimeMillis();
    private long heartbeatTimeout = 3000; // Default 3 seconds
    
    // Enhanced monitoring capabilities
    private final ConcurrentHashMap<String, ComponentHealth> componentHealthMap = new ConcurrentHashMap<>();
    private final AtomicLong totalHeartbeats = new AtomicLong(0);
    private final AtomicLong missedHeartbeats = new AtomicLong(0);
    
    // Health thresholds
    private double cpuThreshold = 80.0;
    private double memoryThreshold = 85.0;
    private int errorThreshold = 5;
    
    // Fault detection
    private volatile boolean criticalFailureDetected = false;
    private volatile int consecutiveTimeouts = 0;

    public SocketHeartbeatMonitor(int port) {
        this.port = port;
    }

    @Override
    public void start() {
        running = true;

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[Monitor] Heartbeat monitoring started on port " + port);
                
                while (running) {
                    try (Socket socket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                        System.out.println("[Monitor] Connection established from " + socket.getRemoteSocketAddress());
                        startWatchdog();

                        String message;
                        while ((message = in.readLine()) != null && running) {
                            processHeartbeatMessage(message);
                        }
                    } catch (Exception e) {
                        System.err.println("[Monitor] Connection error: " + e.getMessage());
                        missedHeartbeats.incrementAndGet();
                    }
                }

            } catch (Exception e) {
                System.err.println("[Monitor] Server error: " + e.getMessage());
                criticalFailureDetected = true;
            }
        }, "HeartbeatListenerThread").start();
    }

    private void processHeartbeatMessage(String message) {
        if (message.startsWith("HEARTBEAT")) {
            lastHeartbeatTime = System.currentTimeMillis();
            totalHeartbeats.incrementAndGet();
            consecutiveTimeouts = 0;
            
            // Parse enhanced heartbeat data
            parseEnhancedHeartbeat(message);
            
            // Perform health analysis
            analyzeSystemHealth();
            
            System.out.println("[Monitor] Enhanced heartbeat received and analyzed.");
            
            // Deactivate fallback if system is healthy
            if (!criticalFailureDetected && isSystemHealthy()) {
                FallbackHandler.getInstance().deactivate();
            }
        }
    }

    private void parseEnhancedHeartbeat(String message) {
        try {
            String[] parts = message.split("\\|");
            double systemCpu = 0, systemMemory = 0;
            int systemFailures = 0;
            boolean systemHealthy = true;
            
            for (String part : parts) {
                if (part.startsWith("CPU:")) {
                    systemCpu = Double.parseDouble(part.substring(4));
                } else if (part.startsWith("MEM:")) {
                    systemMemory = Double.parseDouble(part.substring(4));
                } else if (part.startsWith("FAILURES:")) {
                    systemFailures = Integer.parseInt(part.substring(9));
                } else if (part.startsWith("HEALTHY:")) {
                    systemHealthy = Boolean.parseBoolean(part.substring(8));
                } else if (part.startsWith("COMP:")) {
                    parseComponentHealth(part);
                }
            }
            
            // Update system health
            componentHealthMap.put("SYSTEM", new ComponentHealth("SYSTEM", systemCpu, systemMemory, systemFailures));
            
            if (!systemHealthy || systemFailures >= errorThreshold) {
                criticalFailureDetected = true;
                System.out.println("[Monitor] CRITICAL: System health failure detected!");
            }
            
        } catch (Exception e) {
            System.err.println("[Monitor] Error parsing heartbeat data: " + e.getMessage());
            missedHeartbeats.incrementAndGet();
        }
    }

    private void parseComponentHealth(String componentData) {
        try {
            // Format: COMP:ComponentName:cpuUsage:memoryUsage:errorCount
            String[] parts = componentData.substring(5).split(":");
            if (parts.length >= 4) {
                String name = parts[0];
                double cpu = Double.parseDouble(parts[1]);
                double memory = Double.parseDouble(parts[2]);
                int errors = Integer.parseInt(parts[3]);
                
                componentHealthMap.put(name, new ComponentHealth(name, cpu, memory, errors));
            }
        } catch (Exception e) {
            System.err.println("[Monitor] Error parsing component health: " + e.getMessage());
        }
    }

    private void analyzeSystemHealth() {
        boolean overallHealthy = true;
        
        for (ComponentHealth health : componentHealthMap.values()) {
            if (health.cpuUsage > cpuThreshold || 
                health.memoryUsage > memoryThreshold || 
                health.errorCount > errorThreshold) {
                
                overallHealthy = false;
                System.out.println("[Monitor] WARNING: Component " + health.componentName + 
                                 " exceeds health thresholds (CPU:" + health.cpuUsage + 
                                 "%, MEM:" + health.memoryUsage + "%, ERR:" + health.errorCount + ")");
            }
        }
        
        if (!overallHealthy) {
            criticalFailureDetected = true;
        }
    }

    private boolean isSystemHealthy() {
        return !criticalFailureDetected && consecutiveTimeouts < 2;
    }

    private void startWatchdog() {
        new Thread(() -> {
            while (running) {
                long now = System.currentTimeMillis();
                long timeSinceLastHeartbeat = now - lastHeartbeatTime;
                
                if (timeSinceLastHeartbeat > heartbeatTimeout) {
                    consecutiveTimeouts++;
                    missedHeartbeats.incrementAndGet();
                    
                    System.err.println("[Monitor] HEARTBEAT TIMEOUT #" + consecutiveTimeouts + 
                                     " (Last heartbeat: " + timeSinceLastHeartbeat + "ms ago)");
                    
                    if (consecutiveTimeouts >= 2) {
                        System.err.println("[Monitor] CRITICAL FAILURE: Multiple heartbeat timeouts!");
                        criticalFailureDetected = true;
                        FallbackHandler.getInstance().activate();
                    }
                    
                    // Progressive timeout detection
                    if (consecutiveTimeouts >= 5) {
                        System.err.println("[Monitor] EMERGENCY: Complete system failure detected!");
                        triggerEmergencyProtocol();
                    }
                }
                
                // Print monitoring statistics
                if (totalHeartbeats.get() % 30 == 0 && totalHeartbeats.get() > 0) {
                    printMonitoringStats();
                }
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }, "WatchdogThread").start();
    }

    private void triggerEmergencyProtocol() {
        System.err.println("[Monitor] EMERGENCY PROTOCOL: Activating complete system shutdown safeguards!");
        FallbackHandler.getInstance().activate();
        // Additional emergency actions could be implemented here
    }

    private void printMonitoringStats() {
        long total = totalHeartbeats.get();
        long missed = missedHeartbeats.get();
        double reliability = total > 0 ? ((double)(total - missed) / total) * 100 : 0;
        
        System.out.println("[Monitor] Stats - Total: " + total + ", Missed: " + missed + 
                         ", Reliability: " + String.format("%.2f", reliability) + "%");
    }

    @Override
    public void setHeartbeatTimeout(long timeoutMs) {
        this.heartbeatTimeout = timeoutMs;
        System.out.println("[Monitor] Heartbeat timeout set to " + timeoutMs + "ms");
    }

    @Override
    public void setHealthThreshold(double cpuThreshold, double memoryThreshold, int errorThreshold) {
        this.cpuThreshold = cpuThreshold;
        this.memoryThreshold = memoryThreshold;
        this.errorThreshold = errorThreshold;
        System.out.println("[Monitor] Health thresholds updated - CPU:" + cpuThreshold + 
                         "%, MEM:" + memoryThreshold + "%, ERR:" + errorThreshold);
    }

    @Override
    public boolean isComponentHealthy(String componentName) {
        ComponentHealth health = componentHealthMap.get(componentName);
        if (health == null) return false;
        
        return health.cpuUsage <= cpuThreshold && 
               health.memoryUsage <= memoryThreshold && 
               health.errorCount <= errorThreshold;
    }

    @Override
    public void stop() {
        running = false;
        System.out.println("[Monitor] Monitoring stopped.");
    }

    private static class ComponentHealth {
        final String componentName;
        final double cpuUsage;
        final double memoryUsage;
        final int errorCount;

        ComponentHealth(String componentName, double cpuUsage, double memoryUsage, int errorCount) {
            this.componentName = componentName;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.errorCount = errorCount;
        }
    }
}