package com.makki.exchanges.abtractions

sealed interface Frame<T> {
	data class Asset<T>(val data: T): Frame<T>
	class Connect<T>: Frame<T>
	class Disconnect<T>: Frame<T>

	fun unwrapAsset(): T? {
		return (this as? Asset)?.data
	}

	fun check(nonAsset: Boolean, block: (T) -> Boolean): Boolean {
		return when (this) {
			is Asset -> block(this.data)
			else -> nonAsset
		}
	}

	fun <N> mapAsset(block: (T) -> N): Frame<N> {
		return when(this) {
			is Asset -> Asset(block(this.data))
			is Connect -> Connect()
			is Disconnect -> Disconnect()
		}
	}
}