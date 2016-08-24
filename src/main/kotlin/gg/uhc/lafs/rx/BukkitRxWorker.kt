package gg.uhc.lafs.rx

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import rx.Scheduler
import rx.Subscription
import rx.functions.Action0
import rx.internal.schedulers.ScheduledAction
import rx.subscriptions.CompositeSubscription
import rx.subscriptions.Subscriptions
import java.util.concurrent.TimeUnit

class BukkitRxWorker(val thread: BukkitThread, val plugin: Plugin) : Scheduler.Worker() {
    private val runTaskLater = when (thread) {
        BukkitThread.SYNC -> BukkitRunnable::runTaskLater.apply {} // Added .apply{} to work around language bug
        else -> BukkitRunnable::runTaskLaterAsynchronously.apply {}
    }
    private val compositeSubscription = CompositeSubscription()

    fun Action0.toBukkitRunnable() = object : BukkitRunnable() { override fun run() { call() }}
    fun Long.toTicks() : Long = Math.round(this / 50.0)

    override fun schedule(action: Action0): Subscription = schedule(action, 0, TimeUnit.MILLISECONDS)
    override fun schedule(action: Action0, delayTime: Long, unit: TimeUnit): Subscription {
        // Run immediately if required and possible
        if (thread == BukkitThread.SYNC && unit.toMillis(delayTime) == 0L && Bukkit.getServer().isPrimaryThread) {
            action.call()
            return Subscriptions.unsubscribed()
        }

        val task = runTaskLater(action.toBukkitRunnable(), plugin, unit.toMillis(delayTime).toTicks())

        return ScheduledAction(action, compositeSubscription).apply {
            // cancel the task on unsubscribe
            add(Subscriptions.create { task.cancel() })
        }
    }

    override fun unsubscribe() = compositeSubscription.unsubscribe()
    override fun isUnsubscribed(): Boolean = compositeSubscription.isUnsubscribed
}