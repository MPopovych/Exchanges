package com.makki.exchanges.wrapper

/**
 * Definitions of API errors
 * Should be mapped on the wrapper component for each exchange
 * Thus an abstraction from an exchange is reached
 */
sealed interface SealedApiError {

	/**
	 * This should be returned when an unrecognized error is received or in a method not expecting that case
	 * Recommended way to handle - log + recovery + crash if involves trading
	 */
	data class Unexpected(val description: String) : SealedApiError

	/**
	 * Api wraps should prevent from trading if such was not allowed
	 */
	data object SafeguardBlock : SealedApiError

	/**
	 * Needs new auth api key
	 */
	data object InvalidAuth : SealedApiError

	/**
	 * Critical
	 */
	data object Banned : SealedApiError

	/**
	 * Can be retried right away or handled
	 */
	data object NonceRaceCondition : SealedApiError
	data object InternalExchangeError : SealedApiError

	/**
	 * May be internal or external, should do a retry with a longer delay
	 */
	data class RateLimited(val internal: Boolean) : SealedApiError

	/**
	 * Example cloud service is down
	 */
	data object ExchangeIsOutOfService : SealedApiError

	/**
	 * Usually temporary
	 */
	data class MarketBlocked(val market: String) : SealedApiError

	sealed interface Order : SealedApiError {
		val orderId: String

		data class ArgumentFail(override val orderId: String) : Order
		data class OrderNotFound(override val orderId: String) : Order
		data class AlreadyCancelled(override val orderId: String) : Order
		data class InsufficientBalance(override val orderId: String) : Order
		data class VolumeLessThanMinimum(override val orderId: String) : Order
		data class PriceFillMiss(override val orderId: String) : Order
		/**
		 * May need recovery
		 */
		data class OrderTimeout(override val orderId: String) : Order
	}


}