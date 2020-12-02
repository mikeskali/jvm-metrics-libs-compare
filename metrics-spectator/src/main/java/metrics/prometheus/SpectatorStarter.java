package metrics.prometheus;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.histogram.BucketDistributionSummary;
import com.netflix.spectator.api.histogram.BucketFunctions;
import com.netflix.spectator.api.patterns.PolledMeter;
import com.netflix.spectator.gc.GcLogger;
import com.netflix.spectator.jvm.Jmx;
import com.netflix.spectator.micrometer.MicrometerRegistry;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusRenameFilter;
import metrics.generator.MetricsGeneratorInstance;
import metrics.generator.MetricsListener;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SpectatorStarter implements MetricsListener{
    public static int listenerPort = 8703;
    CompositeMeterRegistry meterRegistry = new CompositeMeterRegistry();
    Registry registry = new MicrometerRegistry(meterRegistry);

    public static void main(String[] args) throws IOException {
        new SpectatorStarter();
    }

    public SpectatorStarter() throws IOException {
        PrometheusMeterRegistry promRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        promRegistry.config().meterFilter(new PrometheusRenameFilter());
        meterRegistry.add(promRegistry);
        meterRegistry.config().commonTags("application", "MetricsPoc-Spectator");

        HttpServer server = HttpServer.create(new InetSocketAddress(listenerPort), 0);
        server.createContext("/metrics", httpExchange -> {
            String response = promRegistry.scrape();
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        for (int i = 0; i<5;i++) {
            new Thread(new MetricsGeneratorInstance(Arrays.asList(this))).start();
        }

        new Thread(server::start).start();

        Jmx.registerStandardMXBeans(registry);
        new GcLogger().start(null);

        System.out.println("SpectatorStarter is running, you can see the generated metrics at: http://localhost:" + listenerPort + "/metrics");
    }



    public Counter getTransactionsCounter(String instance){
        return registry.counter("transactions", "instance", instance);
    }

    public Counter getErrorCounter(String instance, String error){
        return registry.counter("errors", "instance", instance, "error", error);
    }

    public DistributionSummary getProcessingTimer(String instance, boolean isSuccessful){
        return BucketDistributionSummary.get(registry, Id.create("processing_time_ms").withTags("instance", instance, "is_successful",String.valueOf(isSuccessful)), BucketFunctions.latency(1000, TimeUnit.MILLISECONDS));
    }

    @Override
    public void increaseTransactionCount(String instance) {
        getTransactionsCounter(instance).increment();
    }

    @Override
    public void increaseError(String instance, String error) {
        getErrorCounter(instance, error).increment();
    }

    @Override
    public void recordTransactionProcessingTime(long elapsedTimeMs, String instance, boolean isSuccessful) {
        getProcessingTimer(instance, isSuccessful).record(elapsedTimeMs);
    }

    @Override
    public void registerLastStateRetriever(String instance, Supplier<Double> valueFunc) {
        PolledMeter
                .using(registry)
                .withName("last_processing_time")
                .withTag("instance", instance).monitorValue(valueFunc, Supplier<Double>::get);

    }
}
