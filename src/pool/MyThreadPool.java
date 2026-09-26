package pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

public class MyThreadPool {
    private int poolSize;
    private int tasksSize;
    private volatile boolean shutdown;
    private boolean isDaemonPool;
    //ConcurrentLinkedQueue is unbounded, BlockingQueue is bounded and has multithreading oriented methods
    private BlockingQueue<Runnable> tasks;
    private BackPressureHandler handler;
    private TaskInterceptor taskInterceptor;

    private final TaskInterceptor defaultTaskInterceptor = new TaskInterceptor() {
        @Override
        public void beforeExecute(Thread thread, Runnable runnable) {}

        @Override
        public void afterExecute(Thread thread, Runnable runnable) {}
    };

    // Main constructor: the only place where fields are set
    public MyThreadPool(int poolSize, int tasksSize, boolean isDaemonPool, BackPressureHandler handler, TaskInterceptor taskInterceptor) {
    this.poolSize = poolSize;
    this.tasksSize = tasksSize;
    this.isDaemonPool = isDaemonPool;
    // Array: one lock and preallocated array; Linked: separate locks for put and take
    this.tasks = new LinkedBlockingQueue<>(tasksSize);
    this.handler = (handler == null) ? new Drop() : handler;
    this.taskInterceptor = (taskInterceptor == null) ? defaultTaskInterceptor : taskInterceptor;
    }

    // drop by default
    public MyThreadPool(int poolSize, int tasksSize, boolean isDaemonPool, TaskInterceptor taskInterceptor) {
        this(poolSize, tasksSize, isDaemonPool, new Drop(), taskInterceptor);
    }

    // non-daemon threads by default
    public MyThreadPool(int poolSize, int tasksSize, BackPressureHandler handler, TaskInterceptor taskInterceptor) {
        this(poolSize, tasksSize, false, handler, taskInterceptor);
    }

    public void start() {
        for (int i = 0; i < poolSize; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!shutdown) {
                        Runnable task = tasks.poll();
                        if (task != null) {
                            try {
                                taskInterceptor.beforeExecute(Thread.currentThread(), task);
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                            }
                            try {
                                task.run();
                                taskInterceptor.afterExecute(Thread.currentThread(), task);
                            } catch (Throwable throwable) {
                                taskInterceptor.afterExecute(Thread.currentThread(), task);
                                throwable.printStackTrace();
                            }
                        }
                    }
                }
            });
            thread.setDaemon(this.isDaemonPool);
            thread.start();
        }
    }

    public void submit(Runnable task) {
        if (!this.tasks.offer(task)) {
            handler.backPressure();
        }
    }

    public static class Drop implements BackPressureHandler {
        @Override
        public void backPressure() {}
    }

    public static class Reject implements BackPressureHandler {
        @Override
        public void backPressure() {
            throw new RejectedExecutionException("Tasks size is already full!");
        }
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getTasksSize() {
        return tasksSize;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public boolean isDaemonPool() {
        return isDaemonPool;
    }

    public void shutdown() {
        this.shutdown = true;
    }
}
