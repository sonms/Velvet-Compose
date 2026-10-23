package com.sonms.systembars.internal

/**
 * Stack of per-screen bar specs. The most recently registered entry wins; [isInherit]
 * specs fall through to the entry below, independently for status and navigation bars.
 * Not thread-safe: use from the main thread. Deliberately not Compose state, so changes
 * never trigger recomposition.
 */
internal class BarRegistry<T : Any>(private val isInherit: (T) -> Boolean) {
    internal class Entry<T>(var status: T, var navigation: T)

    private val entries = mutableListOf<Entry<T>>()

    var onChanged: (() -> Unit)? = null

    inner class Handle internal constructor(private val entry: Entry<T>) {
        fun update(status: T, navigation: T) {
            if (entry !in entries) return
            entry.status = status
            entry.navigation = navigation
            onChanged?.invoke()
        }

        fun dispose() {
            if (entries.remove(entry)) onChanged?.invoke()
        }
    }

    fun register(status: T, navigation: T): Handle {
        val entry = Entry(status, navigation)
        entries += entry
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
