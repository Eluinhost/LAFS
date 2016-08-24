package gg.uhc.lafs.rx

import org.bukkit.plugin.Plugin
import rx.Scheduler

class BukkitRxScheduler(private val thread: BukkitThread, private val plugin: Plugin) : Scheduler() {
    override fun createWorker(): Worker = BukkitRxWorker(thread, plugin)
}