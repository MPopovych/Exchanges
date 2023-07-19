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
	data class ConnectionError(val description: String): SealedApiError

	data object SafeguardBlock : SealedApiError

	data object InvalidAuth : SealedApiError, ErrorTags.Persisting, ErrorTags.ShouldNotify
	data object Banned : SealedApiError, ErrorTags.Persisting, ErrorTags.ShouldNotify

	/**
	 * Can be retried right away or handled
	 */
	data object NonceRaceCondition : SealedApiError, ErrorTags.Temporary
	data object InternalExchangeError : SealedApiError, ErrorTags.Temporary
	data object RateLimited : SealedApiError, ErrorTags.Temporary, ErrorTags.ShouldNotify
	data object ExchangeIsOutOfService : SealedApiError, ErrorTags.Temporary
	data object MarketBlocked : SealedApiError, ErrorTags.Temporary

	data object PersistingHttpException: SealedApiError, ErrorTags.Persisting

	sealed interface Order : SealedApiError {
		data object ArgumentFail : Order, ErrorTags.ShouldNotify
		data object OrderNotFound : Order
		data object AlreadyCancelled : Order
		data object InsufficientBalance : Order
		data object VolumeLessThanMinimum : Order, ErrorTags.ShouldNotify
		data object PriceFillMiss : Order
	}
}