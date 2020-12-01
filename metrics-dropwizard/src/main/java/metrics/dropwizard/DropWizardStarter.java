package metrics.dropwizard;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import metrics.generator.MetricsGeneratorInstance;
import metrics.generator.MetricsListener;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Not implemented. Latest released version 4.1.16 doesn't support tags, so it is a no go.
 * Version 5 seems to be actively developed and will support tags.
 */
public class DropWizardStarter implements MetricsListener{
    private final MetricRegistry metrics = new MetricRegistry();

    private final Counter transactionsCounter = metrics.counter("transactions_counter");



    public static void main(String[] args) throws IOException {
        new DropWizardStarter();
    }

    public DropWizardStarter() throws IOException {
        System.err.println("Dropwizard doesn't support labels/tags nor prometheus export. Not implemented.");

    }

    @Override
    public void increaseTransactionCount(String instance) {
        transactionsCounter.inc();
    }

    @Override
    public void increaseError(String instance, String error) {

    }

    @Override
    public void recordTransactionProcessingTime(long elapsedTimeMs, String instance, boolean isSuccessful) {

    }

    @Override
    public void registerLastStateRetriever(String instance, Supplier<Double> valueFunc) {

    }
}
