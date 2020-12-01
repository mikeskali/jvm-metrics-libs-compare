package metrics.generator;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

public class MetricsGenerator {
    public static void main(String[] args){
        new MetricsGenerator();
    }

    public MetricsGenerator(){
        for (int i = 0; i<5;i++) {
            new Thread(new MetricsGeneratorInstance(Arrays.asList(new MetricsPrinter()))).start();
        }
    }

    class MetricsPrinter implements MetricsListener {

        @Override
        public void increaseTransactionCount(String instance) {
            System.out.println("tx +1, instance: "+ instance);
        }

        @Override
        public void increaseError(String instance, String error) {
            System.out.println("err +1,  " + instance + ", error: " + error);
        }

        @Override
        public void recordTransactionProcessingTime(long elapsedTimeMs, String instance, boolean isSuccessful) {
            System.out.println("tx took  "+ elapsedTimeMs + ", instance: " + instance + ", isSuccessful: " + isSuccessful);
        }

        @Override
        public void registerLastStateRetriever(String instance, Supplier<Double> valueFunc) {
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("Last Processing time:  " + valueFunc.get() + ", instance: " + instance );
                }
            },1000, 5000);


        }
    }

}
