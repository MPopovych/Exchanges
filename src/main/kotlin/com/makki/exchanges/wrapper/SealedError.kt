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
	data object InvalidAuth : SealedApiError, ErrorTags.Persisting

	/**
	 * Critical
	 */
	data object Banned : SealedApiError, ErrorTags.Persisting

	/**
	 * Can be retried right away or handled
	 */
	data object NonceRaceCondition : SealedApiError, ErrorTags.Temporary
	data object InternalExchangeError : SealedApiError, ErrorTags.Temporary

	/**
	 * May be internal or external, should do a retry with a longer delay
	 */
	data object RateLimited : SealedApiError, ErrorTags.Temporary, ErrorTags.LogicMiss

	/**
	 * Example cloud service is down
	 */
	data object ExchangeIsOutOfService : SealedApiError, ErrorTags.Temporary

	/**
	 * Usually temporary
	 */
	data object MarketBlocked : SealedApiError, ErrorTags.Temporary

	sealed interface Order : SealedApiError {
		data object ArgumentFail : Order, ErrorTags.LogicMiss
		data object OrderNotFound : Order
		data object AlreadyCancelled : Order
		data object InsufficientBalance : Order
		data object VolumeLessThanMinimum : Order, ErrorTags.LogicMiss
		data object PriceFillMiss : Order

		/**
		 * May need recovery
		 */
		data object OrderTimeout : Order, ErrorTags.Temporary
	}
}