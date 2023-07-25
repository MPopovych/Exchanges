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
	data class Unexpected(val description: String) : SealedApiError, ErrorTags.ShouldNotify
	data class ConnectionError(val description: String) : SealedApiError

	object SafeguardBlock : SealedApiError

	object InvalidAuth : SealedApiError, ErrorTags.Persisting, ErrorTags.ShouldNotify
	object Banned : SealedApiError, ErrorTags.Persisting, ErrorTags.ShouldNotify

	/**
	 * Can be retried right away or handled
	 */
	object NonceRaceCondition : SealedApiError, ErrorTags.Temporary
	object InternalExchangeError : SealedApiError, ErrorTags.Temporary
	object RateLimited : SealedApiError, ErrorTags.Temporary, ErrorTags.ShouldNotify
	object ExchangeIsOutOfService : SealedApiError, ErrorTags.Temporary
	object MarketBlocked : SealedApiError, ErrorTags.Temporary

	object PersistingHttpException : SealedApiError, ErrorTags.Persisting

	sealed interface Order : SealedApiError {
		object ArgumentFail : Order, ErrorTags.ShouldNotify
		object OrderNotFound : Order
		object AlreadyCancelled : Order
		object InsufficientBalance : Order
		object VolumeLessThanMinimum : Order, ErrorTags.ShouldNotify
		object PriceFillMiss : Order
	}
}