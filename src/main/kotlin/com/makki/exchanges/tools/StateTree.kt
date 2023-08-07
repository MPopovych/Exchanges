package com.makki.exchanges.tools

import com.makki.exchanges.abtractions.StateFormat
import com.makki.exchanges.abtractions.StateJson
import com.makki.exchanges.abtractions.StateObservable
import kotlinx.serialization.json.*

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
		merge(key, other.stateTree())
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

	fun toJson(): JsonElement {
		return JsonObject(
			this.cache.mapNotNull {
				Pair(it.key, handleAny(it.value.toJson()))
			}.toMap()
		)
	}

	override fun toString(): String {
		val sb = StringBuilder()
		for (s in get()) {
			when (s) {
				is Entries.Header -> sb.appendLine("${"\t".repeat(s.depth)}${s.key}:")
				is Entries.State -> {
					if (s.value is StateFormat) {
						sb.appendLine("${"\t".repeat(s.depth)}${s.key}:'${s.value.format()}'")
					} else {
						sb.appendLine("${"\t".repeat(s.depth)}${s.key}:'${s.value}'")
					}
				}
			}
		}
		return sb.toString()
	}

	private sealed class Node {
		class Generic(internal val block: (() -> Any?)) : Node() {
			fun invoke(depth: Int, key: String): Entries = Entries.State(depth, key, block())
		}

		class Tree(internal val tree: StateTree) : Node() {
			fun invoke(depth: Int): List<Entries> = tree.get(depth)
		}

		fun toJson(): JsonElement {
			return when (this) {
				is Generic -> handleAny(this.block())
				is Tree -> return this.tree.toJson()
			}
		}
	}

	sealed interface Entries {
		class Header(val depth: Int, val key: String) : Entries
		data class State(val depth: Int, val key: String, val value: Any?) : Entries
	}

	companion object {
		private fun handleAny(v: Any?): JsonElement {
			return when (v) {
				is StateObservable -> v.stateTree().toJson()
				is StateJson -> v.toJson()
				is StateTree -> v.toJson()
				null -> JsonNull
				is JsonElement -> v
				is String -> JsonPrimitive(v)
				is Number -> JsonPrimitive(v)
				is Boolean -> JsonPrimitive(v)
				is Array<*> -> JsonArray(v.map { handleAny(it) })
				is Collection<*> -> JsonArray(v.map { handleAny(it) })
				is Map<*, *> -> {
					v.mapNotNull {
						Pair(
							it.key?.toString() ?: return@mapNotNull null,
							handleAny(it.value)
						)
					}.toMap().let { JsonObject(it) }
				}

				else -> JsonPrimitive(v.toString())
			}
		}
	}
}