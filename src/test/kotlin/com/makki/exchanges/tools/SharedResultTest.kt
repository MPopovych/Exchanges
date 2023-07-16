package com.makki.exchanges.tools

import com.makki.exchanges.asyncTest
import kotlinx.coroutines.*
import java.util.HashSet
import kotlin.test.Test

class SharedResultTest {

	@Test
	fun testConcurrentLoadAny() = asyncTest {
		val sharedResultAny = SharedResultAny()

		val scope = CoroutineScope(Dispatchers.IO + Job())
		val buffer = HashSet<String>()

		repeat(15) { index ->
			delay(100)
			scope.launch {
				val result = sharedResultAny.shareResultByKey("k") {
					delay(1000)
					println("Launched action: $index")
					return@shareResultByKey "RESULT $index"
				}
				buffer.add(result)
			}
		}

		delay(2000) // let it finish
		println(buffer)
		assert(buffer.size == 2)
	}

	@Test
	fun testConcurrent() = asyncTest {
		val sharedResultAny = SharedResult<String>()

		val scope = CoroutineScope(Dispatchers.IO + Job())
		val buffer = HashSet<String>()

		repeat(15) { index ->
			delay(100)
			scope.launch {
				val result = sharedResultAny.shareResultByKey("k") {
					delay(1000)
					println("Launched action: $index")
					return@shareResultByKey "RESULT $index"
				}
				buffer.add(result)
			}
		}

		delay(2000) // let it finish
		println(buffer)
		assert(buffer.size == 2)
	}

}