# Coherence messaging connector POC
## Build
### Coherence installation

 - Download and install Coherence 14.1.1 
https://www.oracle.com/middleware/technologies/coherence-downloads.html

 - Install Coherence to local maven repo
```bash
mvn install:install-file -Dfile=$COHERENCE_HOME/lib/coherence.jar      -DpomFile=$COHERENCE_HOME/plugins/maven/com/oracle/coherence/coherence/14.1.1/coherence.14.1.1.pom
```

### Build example
Build with jdk 11+
```bash
mvn install
```

### Run example
- Run Helidon
```bash
java -jar ./target/coherence-conn-example.jar
```
- Call REST resource to send message to coherence
```bash
curl 'http://localhost:7001/example/send/HelloWorld'
```
- Observe incoming messages
```bash
2020-04-02 15:19:01.152/8.628 Oracle Coherence GE 14.1.1.0.0 <Info> (thread=DistributedCache:PartitionedTopic, member=1): Partition ownership has stabilized with 1 nodes
[Thu Apr 02 15:19:02 CEST 2020] INFO: io.helidon.common.HelidonFeatures print - Helidon MP 2.0.0-M2 features: [CDI, Config, FaultTolerance, Health, JAX-RS, Messaging, Metrics, Security, Server, Tracing] 
Coherence channel 3 topic message: HelloWorld
Coherence channel 1 topic message: HelloWorld
Coherence channel 2 topic message: HelloWorld

```