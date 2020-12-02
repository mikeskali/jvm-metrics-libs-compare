package metrics.prometheus;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import metrics.generator.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

public class PrometheusStarter implements MetricsListener{
    private static int listenerPort = 8701;
    static final Counter transactionsCounter = Counter.build()
                                                    .name("transactions")
                                                    .labelNames("instance")
                                                    .help("Transactions counter")
                                                    .register();
    static final Counter errorsCounter = Counter.build()
                                                    .name("errors")
                                                    .labelNames("instance", "error")
                                                    .help("Errors")
                                                    .register();

    static final Histogram processingTime = Histogram.build()
                                            .name("processing_time_ms")
                                            .labelNames("instance","is_successful")
                                            .buckets(1.0,5.0,20.0,50.0,100.0,200.0,500.0,1000.0)
                                            .help("Transaction processing time in ms")
                                            .register();

    static final Gauge lastState = Gauge.build()
                                                .name("last_processing_time")
                                                .labelNames("instance")
                                                .help("Last transaction state")
                                                .register();

    public static void main(String[] args) throws IOException {
        new PrometheusStarter();
    }

    public PrometheusStarter() throws IOException {
        DefaultExports.initialize();
        HTTPServer server = new HTTPServer(listenerPort);
        for (int i = 0; i<5;i++) {
            new Thread(new MetricsGeneratorInstance(Arrays.asList(this))).start();
        }
        System.out.println("PrometheusStarter is running, you can see the generated metrics at: http://localhost:" + listenerPort + "/metrics");
    }


    @Override
    public void increaseTransactionCount(String instance) {
        transactionsCounter.labels(instance).inc();
    }

    @Override
    public void increaseError(String instance, String error) {
        errorsCounter.labels(instance, error).inc();
    }

    @Override
    public void recordTransactionProcessingTime(long elapsedTimeMs, String instance, boolean isSuccessful) {
        processingTime.labels(instance, String.valueOf(isSuccessful)).observe(elapsedTimeMs);
    }

    @Override
    public void registerLastStateRetriever(String instance, Supplier<Double> valueFunc) {
        lastState.setChild(new Gauge.Child() {
            public double get() {
              return valueFunc.get();
            }
        },instance);
    }
}
