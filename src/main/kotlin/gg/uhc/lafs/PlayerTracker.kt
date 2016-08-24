package gg.uhc.lafs

import gg.uhc.lafs.kd.Dimensional
import gg.uhc.lafs.kd.LocationDimensionalCoordinates
import gg.uhc.lafs.rx.Events
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import rx.Observable
import rx.lang.kotlin.PublishSubject
import java.util.*

open class PlayerTracker(private val events: Events) {
    // Emits a player when they log in
    protected val playerLogins : Observable<Player> = events.on<PlayerJoinEvent>(priority = EventPriority.MONITOR, ignoreCancelled = true).map { it.player }
    // Emits a player when they log out
    protected val playerLogouts : Observable<Player> = events.on<PlayerQuitEvent>(priority = EventPriority.MONITOR).map { it.player }
    // Emits a move event when it happens
    protected val moves = events.on<PlayerMoveEvent>(priority = EventPriority.MONITOR, ignoreCancelled = true)

    // Emits a map of worlds to all online players to their location whenever lastKnownLocations updates
    val observableLocations = PublishSubject<Map<Player, Dimensional<Player, LocationDimensionalCoordinates>>>()
    // Map of current player->location mapping, changing this var updates the subject above
    var lastKnownLocations: Map<Player, Dimensional<Player, LocationDimensionalCoordinates>> by observableLocations.delegateProperty(initialValue = mapOf())
        protected set

    init {
        // Track a player's location when they're online
        // Automatically cleans up when player logs out
        playerLogins.subscribe {
            val moving = uniqueMoves(it.uniqueId)
                .subscribeOn(events.syncScheduler)
                .unsubscribeOn(events.syncScheduler)
                // On unsubscribe remove from the tracking map
                .doOnUnsubscribe { lastKnownLocations -= it }
                // On event update location in the tracking map
                .subscribe { lastKnownLocations += (it.data to it) }

            nextLogoff(it.uniqueId).subscribe {
                // Unsubscribe from their move events
                moving.unsubscribe()

                // Remove from location map
                lastKnownLocations -= it.player
            }
        }
    }

    // Emits when the given uuid next logs out and then ends
    fun nextLogoff(uuid: UUID) : Observable<Player> = playerLogouts.filter { it.uniqueId == uuid }.first()

    // Emits a dimensional whenever the uuid moves (ignoring moves to only pitch/yaw, x/y/z changes only)
    fun uniqueMoves(uuid: UUID) : Observable<Dimensional<Player, LocationDimensionalCoordinates>>
        = moves
        // Do the following on the async thread
        .observeOn(events.asyncScheduler)
        // Filter events for just this player
        .filter { it.player.uniqueId == uuid }
        // Convert into a dimensional for the map
        .map { Dimensional(it.player, it.to.toDimensionalCoordinates()) }
        // Only continue if the location has changed since the last check
        // we only use x/y/z so this stops pitch/yaw firing extra events needlessly
        .distinctUntilChanged { it.coordinates }

    fun x(list: List<String>) {
        list.sortedWith(Comparator { w1, w2 -> w1.compareTo(w2) })
    }
}