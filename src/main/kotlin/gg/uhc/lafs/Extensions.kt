package gg.uhc.lafs

import com.google.common.collect.Table
import gg.uhc.lafs.kd.LocationDimensionalCoordinates
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import rx.Observer
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// Simple 'x pow y'
infix fun Double.pow(other: Double) = Math.pow(this, other)

/**
 * Splits the list into two lists at the given index, neither list will contain the item at the index
 */
infix fun <T> List<T>.splitAtExcludingIndex(index: Int) : Pair<List<T>, List<T>> {
    // Exit early
    if (this.size == 0) return listOf<T>() to listOf<T>()
    if (index < 0) return listOf<T>() to this
    if (index >= this.size) return this to listOf<T>()

    val left = this.subList(0, index)
    // If there is no right list return empty instead
    val right = if (index + 1 <= this.size) subList(index + 1, this.size) else listOf()

    return left to right
}

fun Location.toDimensionalCoordinates() = LocationDimensionalCoordinates(this)

operator fun <K, V> Map<K, V>.minus(key: K) : Map<K, V> = LinkedHashMap<K, V>(this).apply { remove(key) }

var Player.team : Team?
    get() = Bukkit.getServer().scoreboardManager.mainScoreboard.getPlayerTeam(this)
    set(new) {
        if (new == null)
            this.team?.removePlayer(this)
        else
            new.addPlayer(this)
    }

class RxObservableProperty<T>(private val observable: Observer<T>, initialValue: T) : ReadWriteProperty<Any?, T> {
    private var currentValue = initialValue

    init {
        observable.onNext(currentValue)
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = currentValue
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        currentValue = value
        observable.onNext(value)
    }
}

fun <T> Observer<T>.delegateProperty(initialValue: T) = RxObservableProperty(this, initialValue)

operator fun <R, C, V> Table<R, C, V>.set(row: R, column: C, value: V) : V = this.put(row, column, value)
