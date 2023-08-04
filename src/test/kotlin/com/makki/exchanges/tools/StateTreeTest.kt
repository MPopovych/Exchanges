package com.makki.exchanges.tools

import com.makki.exchanges.nontesting.asyncTest
import kotlin.test.Test

class StateTreeTest {

	@Test
	fun testPooling() = asyncTest {
		val deepTree = StateTree()
			.track("time") { System.currentTimeMillis() }

		val midTree = StateTree()
			.merge("test", deepTree)
			.track("name") { Thread.currentThread().name }
			.track("state") { Thread.currentThread().state.name }

		val externalTree = StateTree()
			.merge("thread", midTree)

		println(externalTree)
	}

}