package com.makki.exchanges.tools

class RetryTimer(private val delay: Long,
                 private val failInterval: Long = delay * 5) {

    private var lastRetry = 0L
    private var lastDelay = 0L
    private var sequentRetryCount = 1

    fun getNextRetryDelay(): Long {
        if (lastRetry + delay * sequentRetryCount + failInterval > System.currentTimeMillis()) {
            sequentRetryCount++
        } else {
            sequentRetryCount = 1
        }
        lastRetry = System.currentTimeMillis()
        lastDelay = delay * sequentRetryCount
        return lastDelay
    }

    fun reset() {
        sequentRetryCount = 1
        lastDelay = delay
    }

}