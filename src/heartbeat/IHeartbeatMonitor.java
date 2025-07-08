package heartbeat;

public interface IHeartbeatMonitor {
    void start();
    void stop();
    void setHeartbeatTimeout(long timeoutMs);
    void setHealthThreshold(double cpuThreshold, double memoryThreshold, int errorThreshold);
    boolean isComponentHealthy(String componentName);
}