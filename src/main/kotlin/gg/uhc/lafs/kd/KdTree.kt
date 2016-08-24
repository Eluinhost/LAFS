package gg.uhc.lafs.kd

import com.google.common.collect.HashBasedTable
import gg.uhc.lafs.pow
import gg.uhc.lafs.set
import java.util.*

class KdTree<T, C : DimensionalCoordinates>(val items: List<Dimensional<T, C>>) {
    val dimensions: Int

    private var root: KdNode<T, C>?

    private val cache = HashBasedTable.create<Dimensional<T, C>, Dimensional<T, C>, Double>()

    private fun findDistance(dim1: Dimensional<T, C>, dim2: Dimensional<T, C>) : Double {
        val cached = cache[dim1, dim2]

        if (cached != null) return cached

        val distanceSq = dim1 distanceSq dim2

        // Add the 2 dimensionals and their distance to the cache
        // + in reverse order for lookup later
        cache[dim1, dim2] = distanceSq
        cache[dim2, dim1] = distanceSq

        return distanceSq
    }

    init {
        if (items.size == 0) {
            dimensions = 0
            root = null
        } else {
            dimensions = items[0].dimensions
            root = KdNode.from(items)
        }
    }

    inner class DistanceEntry(val dimensional: Dimensional<T, C>, val distance: Double)

    fun nearestNeighbours(
        target: Dimensional<T, C>,
        count: Int,
        predicate: (Dimensional<T, C>, Double) -> Boolean = { x,y -> true },
        maxDist: Double = Double.POSITIVE_INFINITY
    ): PriorityQueue<DistanceEntry> {
        if (items.size == 0) return PriorityQueue()

        // Queue of dimensional+distance pairs sorted by distance
        val bestGuesses = PriorityQueue<DistanceEntry>(Comparator { e1, e2 -> e1.distance.compareTo(e2.distance) })

        val maxDistSq = maxDist.toDouble() pow 2.0

        fun recurse(node: KdNode<T, C>?) {
            if (node == null) return

            if (node.dimensional != target) {
                val distance = findDistance(node.dimensional, target)

                if (distance < maxDistSq && predicate(node.dimensional, distance)) {
                    bestGuesses.add(DistanceEntry(node.dimensional, target distanceSq node.dimensional))

                    // Limit to preset size
                    if (bestGuesses.size > count) bestGuesses.poll()
                }
            }

            val (closest, furthest) = if (node >= target) node.right to node.left else node.left to node.right

            // Recurse into closest subnode
            recurse(closest)

            val distanceToAxis = Math.abs(node.comparingCoordinate - target[node.depth])

            // Recurse the other node if:
            if (
                // The other plane is within maxDistSq and
                distanceToAxis <= maxDistSq && (
                    // Not found enough items yet or
                    bestGuesses.size != count ||
                    // There is potential to find a closer match
                    // than our furthest out within the other plane
                    distanceToAxis < bestGuesses.peek().distance
                )
            ) {
                recurse(furthest)
            }
        }

        recurse(root)

        return bestGuesses
    }
}