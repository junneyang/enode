package org.enodeframework.common.io

import com.ea.async.Async
import org.enodeframework.common.exception.EnodeInterruptException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch

/**
 * @author anruence@gmail.com
 */
object Task {
    @JvmField
    var completedTask: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)

    @JvmStatic
    fun await(latch: CountDownLatch) {
        try {
            latch.await()
        } catch (e: InterruptedException) {
            throw EnodeInterruptException(e)
        }
    }

    @JvmStatic
    fun <T> await(future: CompletableFuture<T>): T {
        return Async.await(future)
    }

    @JvmStatic
    fun sleep(sleepMilliseconds: Long) {
        try {
            Thread.sleep(sleepMilliseconds)
        } catch (e: InterruptedException) {
            throw EnodeInterruptException(e)
        }
    }
}