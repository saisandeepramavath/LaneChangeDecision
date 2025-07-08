# Lane Change System with Heartbeat Monitoring

## Overview
This project implements an autonomous vehicle lane change system with comprehensive fault detection and heartbeat monitoring. The system demonstrates critical software functionality monitoring in a distributed environment using Java socket-based communication.

## How to run it

### What you need
- Java (I used Java 11 but newer versions should work)
- A terminal
- Probably want multiple terminal windows open

### Compiling everything
```bash
java -d out ./*/*.java
```

### Starting the system

#### Terminal 1 - Start the monitor first
```bash
cd out
java main.MonitorMain
```
You should see something like:
```
[MonitorMain] Starting heartbeat monitor...
[Monitor] Heartbeat timeout set to 2500ms
[Monitor] Health thresholds updated - CPU:85.0%, MEM:90.0%, ERR:4
[Monitor] Heartbeat monitoring started on port 9000
```

#### Terminal 2 - Start the lane change system
```bash
cd out
java main.LaneChangeMain
```
Should connect to the monitor and start sending heartbeats:
```
[LaneChangeModule] Starting obstacle detection module...
[Emitter] Connected to monitor at localhost:9000
[LaneChangeModule] Evaluating lane change...
[Monitor] Connection established from /127.0.0.1:xxxxx
[Monitor] Heartbeat received and analyzed
```

#### Terminal 3 - Optional test emitter
```bash
cd out
java -cp . main.EmitterMain
```

This just sends test data to see if the monitoring works.

## What's in here?

The code is split into a few parts:

**Main classes:**
- `LaneChangeMain` - The main system that does the lane change logic
- `MonitorMain` - Watches for heartbeats and detects failures  
- `EmitterMain` - Just for testing, sends fake health data

**Interfaces:**
- `ILaneChangeDecisionSystem` - Contract for lane change stuff
- `IVehicleDetectionSystem` - Contract for detecting other cars
- `IHeartbeatEmitter/IHeartbeatMonitor` - Heartbeat communication

**The actual implementations:**
- `LaneChangeDecisionModule` - Does the actual lane change decisions and vehicle detection
- `FailureSimulator` - Simulates various things going wrong (sensors failing, memory corruption, etc.)
- `VehicleData` - Just holds info about detected vehicles
- `SocketHeartbeatEmitter/Monitor` - Handle the network communication
- `FallbackHandler` - Emergency fallback when things go really wrong

## How it works

The basic idea is:
1. Monitor starts and listens on port 9000
2. Lane change system connects and starts sending heartbeats every second
3. Each heartbeat includes CPU usage, memory usage, and error count
4. The FailureSimulator gradually makes things worse over time
5. Eventually something fails badly enough that the monitor notices
6. Monitor activates emergency mode

## What happens when you run it

**First minute or so:** Everything looks normal
```
[Monitor] Heartbeat received and analyzed
[LaneChangeModule] Evaluating lane change...
[Monitor] Stats - Total: 30, Missed: 0, Reliability: 100.00%
```

**After a couple minutes:** Things start going wrong
```
[FailureSimulator] WARNING: Sensor malfunction detected!
[FailureSimulator] WARNING: Memory corruption level: 65
[Monitor] WARNING: Component LaneChangeModule exceeds health thresholds
```

**Eventually:** Everything breaks
```
[LaneChangeModule] CRITICAL FAILURE: IndexOutOfBoundsException
[Monitor] EMERGENCY: No heartbeat received for 5+ seconds
[FallbackHandler] EMERGENCY MODE ACTIVATED
```

## What gets monitored

The system tracks a few basic health metrics:
- CPU usage (fails if over 85%)
- Memory usage (fails if over 90%)
- Error count (fails if more than 4 errors)
- Whether the component is responding at all

If heartbeats stop coming for 2.5 seconds, the monitor assumes something is wrong. If it keeps happening, it activates the fallback system.

You'll get status reports every 15 seconds or so showing how many heartbeats were received vs missed.


## Communication

The different parts talk to each other using TCP sockets on port 9000.

If the connection drops, the emitter tries to reconnect automatically.


## What libraries it uses

- `java.net` for sockets
- `java.io` for reading/writing
- `java.util.concurrent` for thread-safe operations
- `java.util` for basic collections and random numbers


## Diagrams

### Class Diagram

```mermaid
classDiagram
    class IHeartbeatEmitter {
        <<interface>>
        +start() void
        +stop() void
        +sendHealthStatus() void
        +isHealthy() boolean
    }
    
    class IHeartbeatMonitor {
        <<interface>>
        +start() void
        +stop() void
        +setHeartbeatTimeout() void
        +isComponentHealthy() boolean
    }
    
    class ILaneChangeDecisionSystem {
        <<interface>>
        +start() void
        +stop() void
        +evaluateLaneChange() void
        +isRunning() boolean
    }
    
    class IVehicleDetectionSystem {
        <<interface>>
        +performObstacleDetection() List~String~
        +updateVehicleTracking() void
        +getVehicleData() VehicleData
        +getAllTrackedVehicles() List~VehicleData~
    }
    
    class SocketHeartbeatEmitter {
        -host: String
        -port: int
        -running: boolean
        -isHealthy: boolean
        +start() void
        +stop() void
        +sendHealthStatus() void
        +isHealthy() boolean
    }
    
    class SocketHeartbeatMonitor {
        -port: int
        -running: boolean
        -lastHeartbeatTime: long
        -heartbeatTimeout: long
        -componentHealthMap: Map
        +start() void
        +stop() void
        +setHeartbeatTimeout() void
        +isComponentHealthy() boolean
    }
    
    class LaneChangeDecisionModule {
        -heartbeatEmitter: IHeartbeatEmitter
        -running: boolean
        -detectedVehicles: ConcurrentHashMap
        -processingCycles: AtomicInteger
        -failureSimulator: FailureSimulator
        +start() void
        +stop() void
        +evaluateLaneChange() void
        +isRunning() boolean
        +performObstacleDetection() List~String~
        +updateVehicleTracking() void
        +getVehicleData() VehicleData
        +getAllTrackedVehicles() List~VehicleData~
    }
    
    class FailureSimulator {
        -random: Random
        -processingCycles: AtomicInteger
        -sensorMalfunction: boolean
        -memoryCorruption: int
        -systemLoad: double
        +simulateFailures() void
        +isSensorMalfunction() boolean
        +getMemoryCorruption() int
        +getSystemLoad() double
        +shouldSimulateDataCorruption() boolean
        +shouldSimulateProcessingDelay() boolean
        +getProcessingDelayMs() int
        +createCorruptedVehicleData() VehicleData
        +reset() void
    }
    
    class FallbackHandler {
        <<singleton>>
        -instance: FallbackHandler
        -isActivated: boolean
        +getInstance() FallbackHandler
        +activate() void
        +deactivate() void
        +engageSafetySystems() void
    }
    
    class ComponentHealth {
        +componentName: String
        +cpuUsage: double
        +memoryUsage: double
        +errorCount: int
        +isHealthy: boolean
    }
    
    class VehicleData {
        -id: String
        -distance: double
        -speed: double
        -angle: double
        +VehicleData() 
        +getId() String
        +getDistance() double
        +getSpeed() double
        +getAngle() double
        +isValid() boolean
        +toString() String
        +equals() boolean
        +hashCode() int
    }
    
    %% Interface Implementations
    IHeartbeatEmitter <|-- SocketHeartbeatEmitter
    IHeartbeatMonitor <|-- SocketHeartbeatMonitor
    ILaneChangeDecisionSystem <|-- LaneChangeDecisionModule
    IVehicleDetectionSystem <|-- LaneChangeDecisionModule
    
    %% Composition and Dependencies
    LaneChangeDecisionModule --> IHeartbeatEmitter
    LaneChangeDecisionModule --> FailureSimulator
    LaneChangeDecisionModule --> VehicleData
    SocketHeartbeatEmitter --> ComponentHealth
    SocketHeartbeatMonitor --> ComponentHealth
    SocketHeartbeatMonitor --> FallbackHandler
    FailureSimulator --> VehicleData
```

### Sequence Diagram


```mermaid
sequenceDiagram
    participant Monitor as HeartbeatMonitor
    participant Module as LaneChangeModule
    participant Emitter as HeartbeatEmitter
    participant FailSim as FailureSimulator
    participant Fallback as FallbackHandler
    
    Note over Monitor, Fallback: System Initialization
    Monitor->>Monitor: Start on port 9000
    Module->>Module: Create FailureSimulator
    Module->>Emitter: Create connection
    Emitter->>Monitor: Establish TCP connection
    
    Note over Monitor, Fallback: Normal Operation
    loop Every 1 second
        Module->>Module: Evaluate lane change
        Module->>FailSim: simulateFailures()
        FailSim->>FailSim: Update system load
        FailSim->>FailSim: Check sensor malfunction
        FailSim->>FailSim: Simulate memory corruption
        Module->>FailSim: getSystemLoad()
        Module->>FailSim: getMemoryCorruption()
        Module->>FailSim: isSensorMalfunction()
        Module->>Emitter: Send health status
        Emitter->>Monitor: Send heartbeat with health data
        Monitor->>Monitor: Validate health metrics
        Note right of Monitor: System healthy
    end
    
    Note over Monitor, Fallback: Failure Scenario
    Module->>FailSim: simulateFailures()
    FailSim->>FailSim: Increase failure rates
    Note right of FailSim: Sensor/memory issues
    Module->>FailSim: shouldSimulateDataCorruption()
    FailSim-->>Module: true
    Module->>Module: Create corrupted vehicle data
    Module->>Emitter: Send degraded health status
    Monitor->>Monitor: Detect warning conditions
    
    Note over Monitor, Fallback: Critical Failure
    Module->>FailSim: getMemoryCorruption()
    FailSim-->>Module: >100
    Module->>Module: Critical failure occurs
    Note right of Module: IndexOutOfBoundsException
    
    Note over Monitor, Fallback: Failure Detection
    Monitor->>Monitor: Timeout detected (no heartbeat)
    Monitor->>Monitor: Multiple consecutive timeouts
    Monitor->>Fallback: Activate safety systems
    Fallback->>Fallback: Engage emergency protocols
    Note right of Fallback: Lane holding, speed reduction<br/>Hazard lights, emergency brake
    
    Note over Monitor, Fallback: Recovery
    alt System Recovery
        Module->>FailSim: reset()
        FailSim->>FailSim: Reset all failure states
        Module->>Emitter: Restart and reconnect
        Emitter->>Monitor: Send healthy heartbeat
        Monitor->>Monitor: Reset failure counters
        Monitor->>Fallback: Deactivate safety systems
        Fallback->>Fallback: Resume normal operation
        Note right of Fallback: System restored
    else Permanent Failure
        Note over Monitor, Fallback: Emergency mode continues<br/>Manual intervention required
    end
```




## Files and directories
```
├── README.md                              # Project documentation
└── src/
    ├── main/
    │   ├── LaneChangeMain.java           # Primary application
    │   ├── MonitorMain.java              # Monitoring process
    │   └── EmitterMain.java              # Standalone emitter
    ├── lanechange/
    │   ├── ILaneChangeDecisionSystem.java # Lane change interface
    │   ├── IVehicleDetectionSystem.java  # Vehicle detection interface
    │   ├── LaneChangeDecisionModule.java # Main decision module
    │   ├── VehicleData.java              # Vehicle data model
    │   └── FailureSimulator.java         # Failure simulation component
    ├── heartbeat/
    │   ├── IHeartbeatEmitter.java        # Heartbeat emitter interface
    │   ├── IHeartbeatMonitor.java        # Heartbeat monitor interface
    │   ├── SocketHeartbeatEmitter.java   # TCP heartbeat emitter
    │   └── SocketHeartbeatMonitor.java   # TCP heartbeat monitor
    └── fallback/
        └── FallbackHandler.java          # Emergency fallback system
```


