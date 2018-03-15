package com.mwsxh.commons.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 为提交的具有同一ID的任务使用一个独立的单线程池执行，适合耗时多的任务。
 * <p>
 * 也提供了一个共享的多线程池用于执行没有ID的任务，适合耗时少的任务。
 *
 * @author mwsxh
 */
@Slf4j
public class ExecutorUtils {

    /**
     * 共享线程池，最多线程数：200
     */
    private static final ThreadPoolExecutor _executor = new ThreadPoolExecutor(10, 200, 0L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    /**
     * 利用HashMap作为任务队列，同一ID号只会有一个任务进入队列
     */
    private static final ConcurrentHashMap<String, ThreadPoolExecutor> _executors = new ConcurrentHashMap<>();

    /**
     * 缓存的单例任务状态
     */
    private static final Map<String, WeakReference<Future<?>>> _tasks = new ConcurrentHashMap<>();

    /**
     * 状态。
     */
    public static String status() {
        StringBuilder buf = new StringBuilder();

        buf.append(ExecutorUtils.class.getSimpleName()).append(IOUtils.LINE_SEPARATOR);
        buf.append("Thread Pool (Shared):\t").append(_executor).append(IOUtils.LINE_SEPARATOR);

        // buf.append(String.format("Thread Pool: total=%d, completed=%d, active=%d, queue=%d",
        // _executor.getTaskCount(),
        // _executor.getCompletedTaskCount(), _executor.getActiveCount(), _executor.getQueue().size()))
        // .append(IOUtils.LINE_SEPARATOR);

        final Enumeration<String> keys = _executors.keys();
        while (keys.hasMoreElements()) {
            String id = keys.nextElement();
            ThreadPoolExecutor executor = _executors.get(id);
            buf.append("Thread Pool for ").append(id).append(":\t").append(executor).append(IOUtils.LINE_SEPARATOR);
        }

        // 等待的任务
        // for (String id : _queue.keySet()) {
        // buf.append("Waiting/等待\t\t").append(id).append(IOUtils.LINE_SEPARATOR);
        // }
        buf.append(IOUtils.LINE_SEPARATOR);

        // 曾经的任务
        StringBuilder preceding = new StringBuilder();
        for (String id : _tasks.keySet()) {
            WeakReference<Future<?>> r = _tasks.get(id);
            if (r != null) {
                Future<?> f = r.get();
                if (f != null) {
                    if (f.isDone()) {
                        buf.append("Done/完成\t\t").append(id).append(IOUtils.LINE_SEPARATOR);
                    } else if (f.isCancelled()) {
                        buf.append("Cancelled/取消\t\t").append(id).append(IOUtils.LINE_SEPARATOR);
                    } else {
                        buf.append("Ongoing/正在\t\t").append(id).append(IOUtils.LINE_SEPARATOR);
                    }
                } else {
                    preceding.append("Preceding/之前的（Future is null）\t\t").append(id).append(IOUtils.LINE_SEPARATOR);
                }
            } else {
                preceding.append("Preceding/之前的（WeakReference is null）\t\t").append(id).append(IOUtils.LINE_SEPARATOR);
            }
        }

        return buf.append(IOUtils.LINE_SEPARATOR).append(preceding).toString();
    }

    /**
     * 提交一个任务。
     *
     * @param task 任务
     */
    public static void execute(Runnable task) {
        if (task != null) {
            _executor.execute(task);
        }
    }

    /**
     * 提交一个任务。
     *
     * @param task 任务
     */
    public static Future<?> submit(Runnable task) {
        if (task != null) {
            return _executor.submit(task);
        }
        return null;
    }

    public static Future<?> submit(Callable<?> task) {
        if (task != null) {
            return _executor.submit(task);
        }
        return null;
    }

    /**
     * 提交一个任务。
     *
     * @param task 任务
     */
    static <T> Future<T> submit(Runnable task, T result) {
        if (task != null) {
            return _executor.submit(task, result);
        }
        return null;
    }

    /**
     * 提交一个任务。
     *
     * @param id   任务ID
     * @param task 任务
     */
    public static Future<?> submit(String id, Runnable task) {
        return submit(id, task, Integer.MAX_VALUE);
    }

    /**
     * 提交一个任务，同一时间下，同一ID号的任务运行数不能超过指定的数目。
     *
     * @param id              任务ID
     * @param task            任务
     * @param maxWaitingCount 允许排队等待的最大任务数，超过的将被忽略
     */
    public static Future<?> submit(String id, Runnable task, int maxWaitingCount) {
        if (StringUtils.isBlank(id)) {
            return submit(task);
        }

        if (task != null) {
            if (_tasks.containsKey(id)) {
                WeakReference<Future<?>> r = _tasks.get(id);
                if (r != null) {
                    Future<?> f = r.get();
                    if (f != null && !f.isDone() && maxWaitingCount < 1) {
                        log.info("Ignore task to {}, since it is running and NOT waiting.", id);
                        return r.get();
                    }
                }
            }

            if (!_executors.containsKey(id)) {
                synchronized (ExecutorUtils.class) {
                    if (!_executors.containsKey(id)) { // Double-Check
                        _executors.put(id, createExecutor());
                    }
                }
            }

            final int queueSize = _executors.get(id).getQueue().size();
            if (queueSize == 0 || queueSize < maxWaitingCount) {
                final Future<?> future = _executors.get(id).submit(task);
                log.info("Submit task to {}(queueSize = {}, maxWaitingCount={}).",
                        id, queueSize, maxWaitingCount);
                _tasks.put(id, new WeakReference<Future<?>>(future));
                return future;
            } else {
                log.info("Ignore task to {}, since {} tasks in the queue and reaches the maxWaitingCount {}.",
                        id, queueSize, maxWaitingCount);
            }
        }

        return null;
    }

    /**
     * 指定的任务是否正在执行。
     *
     * @param id 任务ID
     */
    public static boolean isRunning(String id) {
        if (StringUtils.isNotBlank(id) && _tasks.containsKey(id)) {
            WeakReference<Future<?>> r = _tasks.get(id);
            if (r != null) {
                Future<?> f = r.get();
                if (f != null) {
                    return !f.isDone();
                }
            }
        }
        return false;
    }

    /**
     * 将队列处理完，关闭线程。
     */
    public static void shutdown() {
        log.info("shutdown {} ...", ExecutorUtils.class);

        shutdown(StringUtils.EMPTY, _executor);

        _executors.forEach(ExecutorUtils::shutdown);
    }

    private static ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    private static void shutdown(String id, ThreadPoolExecutor executor) {
        if (executor != null && !executor.isShutdown()) {
            log.info("shutdown {} (id={}) ...", executor.getClass().getSimpleName(), id);
            executor.shutdown();
        }
    }

}
