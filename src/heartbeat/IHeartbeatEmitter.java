package heartbeat;

public interface IHeartbeatEmitter {
    void start();
    void stop();
    void sendHealthStatus(String componentName, double cpuUsage, double memoryUsage, int errorCount);
    boolean isHealthy();
}