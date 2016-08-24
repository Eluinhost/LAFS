package gg.uhc.lafs.rx

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.Plugin
import rx.Observable
import rx.exceptions.Exceptions
import rx.subscriptions.Subscriptions

class Events(private val plugin: Plugin) {
    val syncScheduler: BukkitRxScheduler by lazy { BukkitRxScheduler(BukkitThread.SYNC, plugin) }
    val asyncScheduler : BukkitRxScheduler by lazy { BukkitRxScheduler(BukkitThread.ASYNC, plugin) }

    /**
     * Listen for the specified event/s on the sync thread
     */
    fun <EVENT_TYPE : Event> onAny(
        vararg events: Class<out EVENT_TYPE>,
        priority: EventPriority = EventPriority.NORMAL,
        ignoreCancelled: Boolean = false
    ) : Observable<EVENT_TYPE> = onAny(events = * events, priority = priority, ignoreCancelled = ignoreCancelled).subscribeOn(syncScheduler)

    /**
     * Listen for the events on non-specified thread
     */
    fun <EVENT_TYPE : Event> onAnyRaw(
        vararg events: Class<out EVENT_TYPE>,
        priority: EventPriority = EventPriority.NORMAL,
        ignoreCancelled: Boolean = false
    ) : Observable<EVENT_TYPE> = Observable.create { subscriber ->

        val executor = EventExecutor { listener, event ->
            // Check class is valid, bukkit can be weird sometimes
            if (!events.any { it.isAssignableFrom(event.javaClass) }) return@EventExecutor

            try {
                subscriber.onNext(event as EVENT_TYPE)
            } catch (ex: Throwable) {
                Exceptions.throwOrReport(ex, subscriber)
            }
        }

        val manager = Bukkit.getPluginManager()
        // Dummy object to listen with
        val listener = object : Listener{}

        // Register with bukkit
        events.forEach { manager.registerEvent(it, listener, priority, executor, plugin, ignoreCancelled) }

        // Unregister all events for unsubscriptions
        subscriber.add(Subscriptions.create { HandlerList.unregisterAll(listener) })

        // Complete on plugin disabling
        manager.registerEvent(PluginDisableEvent::class.java, listener, EventPriority.MONITOR, { listener, event ->
            if ((event !!as PluginDisableEvent).plugin === plugin) {
                subscriber.onCompleted()
            }
        }, plugin, false)
    }

    /**
     * Listen for a specific event on the sync thread
     */
    inline fun <reified EVENT_TYPE : Event> on(
        priority: EventPriority = EventPriority.NORMAL,
        ignoreCancelled: Boolean = false
    ) : Observable<EVENT_TYPE> = onAny(events = EVENT_TYPE::class.java, priority = priority, ignoreCancelled = ignoreCancelled)

    /**
     * Listen for the event on non-specified thread
     */
    inline fun <reified EVENT_TYPE : Event> onAnyRaw(
        priority: EventPriority = EventPriority.NORMAL,
        ignoreCancelled: Boolean = false
    ) : Observable<EVENT_TYPE> = onAnyRaw(events = EVENT_TYPE::class.java, priority = priority, ignoreCancelled = ignoreCancelled)
}