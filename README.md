# Java Metrics Libraries 
Compares the common Java telemetry instrumentation libraries. Kind of a playground to try out the different libraries. 

The project consists of:
* metrics-generator. Mock of a transactions processing system. `MetricsGeneratorInstance` mocks a processing thread. 
It updates it's listeners with transaction arrived, transaction processing time, errors and exposes a lamda function to retrieve the last processing time of a transaction.

* specific metrics libraries implementations. Each implementation starts 5 `MetricsGeneratorInstance` threads, registers to the metrics library and sets up an http endpoint. 

# Build and run
 * build: `gradlew clean install`
 * run a specific implementation: 
    * `gradlew :metrics-spectator:run` 
    * `gradlew :metrics-micrometer:run`
    * `gradlew :metrics-spectatore:run`
    
  
 
 