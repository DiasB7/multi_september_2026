import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class AtomicLongSum {

    public static void main(String[] args) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();

        System.out.println("=== SYSTEM INFO ===");

        System.out.println("Available processors: "
                + runtime.availableProcessors());

        System.out.println("Max memory (MB): "
                + runtime.maxMemory() / 1024 / 1024);

        System.out.println("Total memory (MB): "
                + runtime.totalMemory() / 1024 / 1024);

        System.out.println("Free memory (MB): "
                + runtime.freeMemory() / 1024 / 1024);

        System.out.println("Java version: "
                + System.getProperty("java.version"));

        Path dir = Path.of(System.getProperty("user.dir")).resolve("output").toAbsolutePath();
        Long begin = System.currentTimeMillis();

        ExecutorService ioPool = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService cpuPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        System.out.println(" Starting ...");
        AtomicLong total = new AtomicLong(0L);
        AtomicLong done = new AtomicLong(0L);

        try (Stream<Path> files = Files.list(dir)) {

            files.filter(Files::isRegularFile)
                    .forEach(file ->
                            ioPool.execute(() -> {
                                try {
                                    List<String> lines = Files.readAllLines(file);
                                    cpuPool.execute(() -> {
                                        total.addAndGet(sumLines(lines));
                                        long processed = done.incrementAndGet();
                                        if (processed % 1000 == 0) {
                                            System.out.printf("%d files been processed\n", processed);
                                        }

                                    });
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                    );
        }

        ioPool.shutdown();
        ioPool.awaitTermination(10, TimeUnit.MINUTES);
        cpuPool.shutdown();
        cpuPool.awaitTermination(10, TimeUnit.MINUTES);

        System.out.println("TOTAL = " + total.get());
        System.out.println("Took: " + (System.currentTimeMillis() - begin) / 1000f + " seconds");
    }

    private static long sumLines(List<String> lines) {
        long sum = 0L;
        for (int i = 0; i < lines.size(); i++) {
            for (String p : lines.get(i).split(",")) {
                sum += calculateCoefficient(Long.parseLong(p), 500);
            }
        }
        return sum;
    }

    public static long calculateCoefficient(long input, int iterations) {
        LongStream.range(0, iterations).forEach(i -> {
            double x = input + System.nanoTime();
            Math.sqrt(Math.sqrt(
                    (Math.cos(Math.sin(Math.log(Math.sqrt(x + 1.0)))) *
                            Math.sqrt(Math.cos(Math.sin(Math.log(Math.sqrt(x + 2.0))))))));
        });

        double x = input + System.nanoTime();
        return (long) Math.sqrt(10 * Math.sqrt(
                (Math.cos(Math.sin(Math.log(Math.sqrt(x + 1.0)))) *
                        Math.sqrt(Math.cos(Math.sin(Math.log(Math.sqrt(x + 2.0))))))));
    }



}