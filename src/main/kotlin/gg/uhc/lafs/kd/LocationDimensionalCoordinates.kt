package gg.uhc.lafs.kd

import org.bukkit.Location
import java.util.*

/**
 * A dimensional that represents a location based on its X/Y/Z coordinates only.
 * Only equal to another LocationDimensionalCoordinates if the X/Y/Z/World are all equal
 */
class LocationDimensionalCoordinates(location: Location) : DimensionalCoordinates {
    val x = location.x
    val y = location.y
    val z = location.z
    val worldId : UUID = location.world.uid

    override fun get(depth: Int) : Double = when (depth) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException()
    }

    override val count: Int = 3

    override fun equals(other: Any?): Boolean =
        other is LocationDimensionalCoordinates &&
            other.worldId == worldId &&
            other.x == x &&
            other.y == y &&
            other.z == z

    override fun hashCode(): Int = Objects.hash(worldId, x, y, z)
}