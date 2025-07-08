package main;

import fallback.FallbackHandler;
import heartbeat.SocketHeartbeatMonitor;

public class MonitorMain {
    public static void main(String[] args) {
        System.out.println("[MonitorMain] Starting heartbeat monitor...");
        
        SocketHeartbeatMonitor monitor = new SocketHeartbeatMonitor(9000);
        
        // Configure enhanced monitoring parameters
        monitor.setHeartbeatTimeout(2500); // 2.5 seconds timeout
        monitor.setHealthThreshold(85.0, 90.0, 4); // CPU, Memory, Error thresholds
        
        monitor.start();

        // Status monitoring thread
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(15000); // Print status every 15 seconds
                    
                    System.out.println("\n[MonitorMain] === SYSTEM STATUS REPORT ===");
                    FallbackHandler.getInstance().printSystemStatus();
                    
                    // Check specific component health
                    boolean laneChangeHealthy = monitor.isComponentHealthy("LaneChangeModule");
                    boolean systemHealthy = monitor.isComponentHealthy("SYSTEM");
                    
                    System.out.println("[MonitorMain] Component Health Status:");
                    System.out.println("  - Lane Change Module: " + (laneChangeHealthy ? "HEALTHY" : "DEGRADED"));
                    System.out.println("  - System Overall: " + (systemHealthy ? "HEALTHY" : "DEGRADED"));
                    System.out.println("=== END STATUS REPORT ===\n");
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "StatusMonitorThread").start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[MonitorMain] Shutting down monitoring system...");
            monitor.stop();
        }));
    }
}