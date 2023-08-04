package com.makki.exchanges.tools

import com.makki.exchanges.abtractions.StateObservable

class StateTree {
	private val cache = LinkedHashMap<String, Node>()

	fun track(key: String, block: (() -> Any?)): StateTree {
		cache[key] = Node.Generic(block)
		return this
	}

	fun merge(key: String, other: StateTree): StateTree {
		cache[key] = Node.Tree(other)
		return this
	}

	fun merge(key: String, other: StateObservable): StateTree {
		merge(key, other.state())
		return this
	}

	fun get(depth: Int = 0): List<Entries> {
		val buffer = ArrayList<Entries>()
		for ((k, v) in cache) {
			when (v) {
				is Node.Generic -> buffer.add(v.invoke(depth, k))
				is Node.Tree -> {
					buffer.add(Entries.Header(depth, k))
					buffer.addAll(v.invoke(depth + 1))
				}
			}
		}
		return buffer
	}

	override fun toString(): String {
		val sb = StringBuilder()
		for (s in get()) {
			when (s) {
				is Entries.Header -> sb.appendLine("${"\t".repeat(s.depth)}${s.key}:")
				is Entries.State -> sb.appendLine("${"\t".repeat(s.depth)}${s.key}:'${s.value}'")
			}
		}
		return sb.toString()
	}

	private sealed class Node {
		class Generic(private val block: (() -> Any?)) : Node() {
			fun invoke(depth: Int, key: String): Entries = Entries.State(depth, key, block())
		}

		class Tree(private val tree: StateTree) : Node() {
			fun invoke(depth: Int): List<Entries> = tree.get(depth)
		}
	}

	sealed interface Entries {
		class Header(val depth: Int, val key: String) : Entries
		data class State(val depth: Int, val key: String, val value: Any?) : Entries
	}
}