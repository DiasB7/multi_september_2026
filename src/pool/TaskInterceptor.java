package pool;

public interface TaskInterceptor {
    void beforeExecute(Thread thread, Runnable runnable);
    void afterExecute(Thread thread, Runnable runnable);
}
