package com.sonms.systembars.internal

/**
 * Stack of per-screen bar specs. Entries are ordered by `priority` (e.g. layout depth), then by
 * registration order; the last one wins; [isInherit]
 * specs fall through to the entry below, independently for status and navigation bars.
 * Not thread-safe: use from the main thread. Deliberately not Compose state, so changes
 * never trigger recomposition.
 */
internal class BarRegistry<T : Any>(private val isInherit: (T) -> Boolean) {
    internal class Entry<T>(var status: T, var navigation: T, var priority: Int)

    private val entries = mutableListOf<Entry<T>>()

    var onChanged: (() -> Unit)? = null

    inner class Handle internal constructor(private val entry: Entry<T>) {
        fun update(status: T, navigation: T) {
            if (entry !in entries) return
            entry.status = status
            entry.navigation = navigation
            onChanged?.invoke()
        }

        /** Re-sorts this entry (e.g. once its layout depth is known). */
        fun setPriority(priority: Int) {
            if (entry.priority == priority || entry !in entries) return
            entries.remove(entry)
            entry.priority = priority
            entries.add(entries.indexOfLast { it.priority <= priority } + 1, entry)
            onChanged?.invoke()
        }

        fun dispose() {
            if (entries.remove(entry)) onChanged?.invoke()
        }
    }

    fun register(status: T, navigation: T, priority: Int = 0): Handle {
        val entry = Entry(status, navigation, priority)
        val at = entries.indexOfLast { it.priority <= priority } + 1
        entries.add(at, entry)
        onChanged?.invoke()
        return Handle(entry)
    }

    fun resolveStatus(): T? = resolve { it.status }

    fun resolveNavigation(): T? = resolve { it.navigation }

    private inline fun resolve(pick: (Entry<T>) -> T): T? {
        for (i in entries.indices.reversed()) {
            val spec = pick(entries[i])
            if (!isInherit(spec)) return spec
        }
        return null
    }
}
