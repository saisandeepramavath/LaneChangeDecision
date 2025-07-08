package main;

import heartbeat.SocketHeartbeatEmitter;

public class EmitterMain {
    public static void main(String[] args) {
        System.out.println("[EmitterMain] Starting enhanced heartbeat emitter...");
        
        SocketHeartbeatEmitter emitter = new SocketHeartbeatEmitter("localhost", 9000);
        emitter.start();

        // Simulate health status updates
        new Thread(() -> {
            int cycle = 0;
            while (true) {
                try {
                    Thread.sleep(5000);
                    cycle++;
                    
                    // Simulate varying component health
                    double cpu = 30 + (cycle * 2) + (Math.random() * 20);
                    double memory = 40 + (cycle * 1.5) + (Math.random() * 15);
                    int errors = cycle > 10 ? (int)(Math.random() * 3) : 0;
                    
                    emitter.sendHealthStatus("TestComponent", cpu, memory, errors);
                    
                    if (cycle > 20) {
                        System.out.println("[EmitterMain] Simulating component degradation...");
                        emitter.sendHealthStatus("TestComponent", 95, 90, 6);
                    }
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "HealthSimulationThread").start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[EmitterMain] Shutting down...");
            emitter.stop();
        }));
    }
}