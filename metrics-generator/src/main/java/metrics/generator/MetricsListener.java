package metrics.generator;

import java.util.function.Supplier;

public interface MetricsListener {
    public void increaseTransactionCount(String instance);
    public void increaseError(String instance, String error);
    public void recordTransactionProcessingTime(long elapsedTimeMs, String instance, boolean isSuccessful);
    public void registerLastStateRetriever(String instance, Supplier<Double> valueFunc);
}
