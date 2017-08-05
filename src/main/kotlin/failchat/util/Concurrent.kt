package failchat.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition

private val log: Logger = LoggerFactory.getLogger("failchat.util.ConcurrentKt")

inline fun sleep(duration: Duration) = Thread.sleep(duration.toMillis())

inline fun Condition.await(duration: Duration) = this.await(duration.toMillis(), TimeUnit.MILLISECONDS)

inline fun ScheduledExecutorService.schedule(delay: Duration, noinline command: () -> Unit): ScheduledFuture<*> {
    return this.schedule(command, delay.toMillis(), TimeUnit.MILLISECONDS)
}

fun ScheduledExecutorService.scheduleWithCatch(delay: Duration, command: () -> Unit): ScheduledFuture<*> {
    val wrapperCommand = {
        try {
            command.invoke()
        } catch (t: Throwable) {
            log.warn("Uncaught exception during executing scheduled task $command", t)
        }
    }

    return schedule(delay, wrapperCommand)
}
