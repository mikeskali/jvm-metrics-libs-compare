package metrics.generator;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class MetricsGeneratorInstance implements Runnable{
    List<MetricsListener> metricsListeners;
    Random rand = new Random();
    Long lastProcessingTime = 0l;
    public MetricsGeneratorInstance(List<MetricsListener> listeners){
        this.metricsListeners = listeners;
    }

    @Override
    public void run() {
        metricsListeners.forEach(it -> it.registerLastStateRetriever(Thread.currentThread().getName(),new Supplier<Double>() {
            @Override
            public Double get() {
                return lastProcessingTime.doubleValue();
            }
        }));
        while(!Thread.interrupted()){
            transactionArrived();
            long startProcessing = System.currentTimeMillis();
            try {
                processTransaction();
                transactionProcessed(System.currentTimeMillis() - startProcessing, true);
            } catch (Exception e) {
                transactionProcessed(System.currentTimeMillis() - startProcessing, false);
                onError(e.getClass().getSimpleName());
            }
        }
    }

    public void processTransaction() throws TimeoutException, InterruptedException {
        int processingTime = rand.nextInt(1000);
        TimeUnit.MILLISECONDS.sleep(processingTime);
        if (processingTime  > 950){
//            throw new NullPointerException();
        } else if (processingTime > 850){
//            throw new TimeoutException();
        }
    }

    public void onError(String error){
        metricsListeners.stream().forEach(it -> it.increaseError(Thread.currentThread().getName(), error));
    }

    public void transactionProcessed(long elapsedTime, boolean isSuccessful){
        metricsListeners.stream().forEach(it -> it.recordTransactionProcessingTime(elapsedTime, Thread.currentThread().getName(), isSuccessful));
    }

    public void transactionArrived(){
        metricsListeners.stream().forEach(it->it.increaseTransactionCount(Thread.currentThread().getName()));
    }
}
