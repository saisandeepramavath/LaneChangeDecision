package main;

import heartbeat.SocketHeartbeatEmitter;
import lanechange.LaneChangeDecisionModule;

public class LaneChangeMain {
    public static void main(String[] args) {
        SocketHeartbeatEmitter emitter = new SocketHeartbeatEmitter("localhost", 9000);
        LaneChangeDecisionModule module = new LaneChangeDecisionModule(emitter);
        module.start();

        Runtime.getRuntime().addShutdownHook(new Thread(module::stop));
    }
}