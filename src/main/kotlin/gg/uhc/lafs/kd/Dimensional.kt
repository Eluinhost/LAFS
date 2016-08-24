package gg.uhc.lafs.kd

import gg.uhc.lafs.pow
import java.util.*

interface DimensionalCoordinates {
    operator fun get(depth: Int) : Double
    val count: Int
    override fun equals(other: Any?) : Boolean
    override fun hashCode(): Int
}

open class Dimensional<out T, C : DimensionalCoordinates>(val data: T, val coordinates: C) {
    val dimensions = coordinates.count

    /**
     * Fetches the coordinate at the provided 0 indexed depth.
     * If the depth > the number of coordinates it will 'wrap around'.
     * e.g. dimensions = 3, coordinateAtDimension(3) will fetch coordinates[0]
     */
    fun coordinateAtDimension(depth: Int) = coordinates[depth % dimensions]

    /**
     * Alias for coordinateAtDimension(Int)
     */
    operator fun get(depth: Int) = coordinateAtDimension(depth)

    /**
     * Calculates the distance from this dimensional to the other dimensional along the given axis.
     *
     * Should use axisDistanceSq to avoid extra sqrt operations where possible
     *
     * @param other dimensional to meature distance to
     * @param dimension the coordinate index to check along, 0 indexed
     */
    fun axisDistance(other: Dimensional<*, C>, dimension: Int) = Math.sqrt(axisDistanceSq(other, dimension))

    /**
     * Fetches the squared distance from this dimensional to the other dimensional along the given axis.
     *
     * @param other dimensional to meature distance to
     * @param dimension the coordinate index to check along, 0 indexed
     */
    fun axisDistanceSq(other: Dimensional<*, C>, dimension: Int) = (this[dimension] - other[dimension]) pow 2.0

    /**
     * Calculates the distance to the other dimensional along all axis.
     *
     * Should use distanceSq to avoid extra sqrt operations where possible
     *
     * @param other dimensional to measure distance to
     */
    infix fun distance(other: Dimensional<*, C>) = Math.sqrt(distanceSq(other))
    /**
     * Calculates the distance to the other dimensional along all axis.
     *
     * @param other dimensional to measure distance to
     */
    infix fun distanceSq(other: Dimensional<*, C>)
        = (0 until dimensions)
            .map { this.axisDistanceSq(other, it) }
            .sum()

    override fun equals(other: Any?) =
        other != null &&
        other is Dimensional<*, *> &&
        other.coordinates == this.coordinates &&
        other.data == this.data

    override fun hashCode() = Objects.hash(data, coordinates)
}