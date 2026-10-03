package com.mamang.datameter.domain.model

data class NetworkUsage(
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L
) {
    val totalBytes: Long
        get() = rxBytes + txBytes

    operator fun plus(other: NetworkUsage): NetworkUsage {
        return NetworkUsage(
            rxBytes = this.rxBytes + other.rxBytes,
            txBytes = this.txBytes + other.txBytes
        )
    }

    companion object {
        val ZERO = NetworkUsage(0L, 0L)
    }
}
