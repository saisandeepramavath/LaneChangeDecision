package heartbeat;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketHeartbeatEmitter implements IHeartbeatEmitter {

    private final String host;
    private final int port;
    private volatile boolean running = false;
    private final ConcurrentHashMap<String, ComponentHealth> componentHealthMap = new ConcurrentHashMap<>();
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private Socket socket;
    private PrintWriter out;

    public SocketHeartbeatEmitter(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void start() {
        running = true;
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                
                System.out.println("[Emitter] Connected to monitor at " + host + ":" + port);

                while (running) {
                    try {
                        sendHeartbeatWithHealthData();
                        consecutiveFailures.set(0);
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        int failures = consecutiveFailures.incrementAndGet();
                        System.err.println("[Emitter] Heartbeat send failure #" + failures + ": " + e.getMessage());
                        
                        if (failures >= 3) {
                            isHealthy.set(false);
                            System.err.println("[Emitter] CRITICAL: Multiple heartbeat failures detected!");
                        }
                        
                        // Try to reconnect
                        if (failures >= 5) {
                            reconnect();
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("[Emitter] Connection error: " + e.getMessage());
                isHealthy.set(false);
            } finally {
                closeConnection();
            }
        }, "HeartbeatEmitterThread").start();
    }

    private void sendHeartbeatWithHealthData() {
        if (out == null) return;
        
        // Get system metrics
        double cpuUsage = getSystemCpuUsage();
        double memoryUsage = getMemoryUsage();
        
        // Create comprehensive heartbeat message
        StringBuilder heartbeatMsg = new StringBuilder();
        heartbeatMsg.append("HEARTBEAT|")
                   .append("CPU:").append(String.format("%.2f", cpuUsage)).append("|")
                   .append("MEM:").append(String.format("%.2f", memoryUsage)).append("|")
                   .append("FAILURES:").append(consecutiveFailures.get()).append("|")
                   .append("HEALTHY:").append(isHealthy.get());
        
        // Add component health data
        for (ComponentHealth health : componentHealthMap.values()) {
            heartbeatMsg.append("|COMP:").append(health.componentName)
                       .append(":").append(health.cpuUsage)
                       .append(":").append(health.memoryUsage)
                       .append(":").append(health.errorCount);
        }
        
        out.println(heartbeatMsg.toString());
        System.out.println("[Emitter] Sent enhanced heartbeat with health data.");
    }

    private void reconnect() {
        try {
            closeConnection();
            Thread.sleep(2000); // Wait before reconnecting
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("[Emitter] Reconnected to monitor.");
        } catch (Exception e) {
            System.err.println("[Emitter] Reconnection failed: " + e.getMessage());
        }
    }

    private void closeConnection() {
        try {
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            System.err.println("[Emitter] Error closing connection: " + e.getMessage());
        }
    }

    private double getSystemCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return osBean.getProcessCpuLoad() * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return ((double) (totalMemory - freeMemory) / totalMemory) * 100;
    }

    @Override
    public void sendHealthStatus(String componentName, double cpuUsage, double memoryUsage, int errorCount) {
        componentHealthMap.put(componentName, new ComponentHealth(componentName, cpuUsage, memoryUsage, errorCount));
        
        // Update overall health status
        boolean componentHealthy = cpuUsage < 80 && memoryUsage < 85 && errorCount < 5;
        if (!componentHealthy) {
            isHealthy.set(false);
            System.out.println("[Emitter] WARNING: Component " + componentName + " health degraded!");
        }
    }

    @Override
    public boolean isHealthy() {
        return isHealthy.get() && consecutiveFailures.get() < 3;
    }

    @Override
    public void stop() {
        running = false;
        closeConnection();
        System.out.println("[Emitter] Stopped.");
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