package metrics.prometheus;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
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
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public class MicroMeterStarter implements MetricsListener{
    private static int listenerPort = 8702;

    static final CompositeMeterRegistry registry = new CompositeMeterRegistry();


    public static void main(String[] args) {
        new MicroMeterStarter();
    }

    public MicroMeterStarter() {
        startPrometheusEndpoint();
        registry.config().commonTags("application", "MetricsPoc-Micrometer");

        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ClassLoaderMetrics().bindTo(registry);

        for (int i = 0; i<5;i++) {
            new Thread(new MetricsGeneratorInstance(Arrays.asList(this))).start();
        }
    }

    private void startPrometheusEndpoint(){
        try {
            PrometheusMeterRegistry promRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            promRegistry.config().meterFilter(new PrometheusRenameFilter());

            registry.add(promRegistry);
            HttpServer server = HttpServer.create(new InetSocketAddress(listenerPort), 0);
            server.createContext("/metrics", httpExchange -> {
                String response = promRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            new Thread(server::start).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("MicrometerStarter is running, you can see the generated metrics at: http://localhost:" + listenerPort + "/metrics");
    }

    public Counter getTransactionsCounter(String instance){
        return Counter
                .builder("transactions")
                .baseUnit("tx")
                .description("Transactions counter")
                .tag("instance", instance)
                .register(registry);
    }

    public Counter getErrorCounter(String instance, String error){
        return Counter
                .builder("Errors")
                .tags("instance", instance, "error", error)
                .description("Errors")
                .register(registry);
    }

    public DistributionSummary getProcessingTimer(String instance, boolean isSuccessful){
        return DistributionSummary
                .builder("processing_time_ms")
                .baseUnit("ms")
                .serviceLevelObjectives(1,5,20,50,100,200,500,1000)
                .publishPercentileHistogram()
                .description("Transaction processing time in ms")
                .tags("instance",instance, "is_successful", String.valueOf(isSuccessful))
                .register(registry);
    }

    @Override
    public void increaseTransactionCount(String instance) {
        getTransactionsCounter(instance).increment();
    }

    @Override
    public void increaseError(String instance, String error) {
        getErrorCounter(instance,error).increment();
    }

    @Override
    public void recordTransactionProcessingTime(long elapsedTimeMs, String instance, boolean isSuccessful) {
        getProcessingTimer(instance, isSuccessful).record(elapsedTimeMs);
    }

    @Override
    public void registerLastStateRetriever(String instance, Supplier<Double> valueFunc) {
        registry.gauge("last_processing_time", Arrays.asList(new Tag(){
            @Override
            public String getKey() {
                return "instance";
            }

            @Override
            public String getValue() {
                return instance;
            }
        }), new ToDoubleFunction(){
            @Override
            public double applyAsDouble(Object value) {
                return valueFunc.get();
            }
        });
    }

}
