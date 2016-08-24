package gg.uhc.lafs.kd

import gg.uhc.lafs.splitAtExcludingIndex

internal data class KdNode<T, C : DimensionalCoordinates>(val left: KdNode<T, C>?, val right: KdNode<T, C>?, val dimensional: Dimensional<T, C>, val depth: Int) {
    val comparingCoordinate = dimensional[depth]

    operator fun compareTo(other: Dimensional<T, C>) = comparingCoordinate.compareTo(other[depth])

    companion object {
        private fun <T, C : DimensionalCoordinates> List<Dimensional<T, C>>.toNode(dimensions: Int, depth: Int) : KdNode<T, C>? {
            val length = this.size

            if (length == 0) return null

            // Leaf, no children
            if (length == 1) {
                if (this[0].dimensions != dimensions) throw IllegalArgumentException("Not all items are of the same dimensional length")

                return KdNode(null, null, this[0], depth)
            }

            // Sort by the dimensions we are currently matching against
            val sortedInDimension = this.sortedBy { it[depth] }

            // Find the median value to split around
            val medianIndex = length / 2
            val median = sortedInDimension[medianIndex]
            if (median.dimensions != dimensions) throw IllegalArgumentException("Not all items are of the same dimensional length")

            val (left, right) = sortedInDimension splitAtExcludingIndex medianIndex

            // Construct child trees if needed
            return KdNode(if (left.size > 0) left.toNode(dimensions, depth + 1) else null, if (right.size > 0) right.toNode(dimensions, depth + 1) else null, median, depth)
        }

        fun <T, C : DimensionalCoordinates> from(items: List<Dimensional<T, C>>) : KdNode<T, C> {
            val length = items.size

            if (length == 0) throw IllegalArgumentException("Must provide at least 1 item")

            return items.toNode(items[0].dimensions, 0)!!
        }
    }
}