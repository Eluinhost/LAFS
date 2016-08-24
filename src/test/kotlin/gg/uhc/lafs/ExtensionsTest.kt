package gg.uhc.lafs

import com.google.common.collect.HashBasedTable
import gg.uhc.lafs.kd.LocationDimensionalCoordinates
import org.bukkit.Location
import org.bukkit.World
import org.jetbrains.spek.api.Spek
import java.util.*
import kotlin.test.assertEquals

class ExtensionsTest : Spek({
    describe("pow") {
        it("should raise left to the power of the right") {
            val value = 2.0 pow 3.0
            assertEquals(8.0, value)
        }
    }

    describe("splitAtExcludingIndex") {
        it("should return empty lists for an empty list") {
            val (left, right) = emptyList<String>().splitAtExcludingIndex(20)

            assert(left.size == 0 && right.size == 0)
        }

        it("should exclude middle result") {
            val (left, right) = listOf("left", "middle", "right").splitAtExcludingIndex(1)

            assert(left.size == 1)
            assertEquals("left", left[0])
            assert(right.size == 1)
            assertEquals("right", right[0])
        }

        it("should have an empty right list at end index") {
            val (left, right) = listOf("left", "right").splitAtExcludingIndex(1)

            assert(left.size == 1)
            assertEquals("left", left[0])
            assert(right.size == 0)
        }

        it("should have an empty left list at start index") {
            val (left, right) = listOf("left", "right").splitAtExcludingIndex(0)

            assert(left.size == 0)
            assert(right.size == 1)
            assertEquals("right", right[0])
        }

        it("should work with out of bound upper index") {
            val (left, right) = listOf("middle").splitAtExcludingIndex(1)

            assert(left.size == 1)
            assertEquals("middle", left[0])
            assert(right.size == 0)
        }

        it("should work with out of bound lower index") {
            val (left, right) = listOf("middle").splitAtExcludingIndex(-1)

            assert(left.size == 0)
            assert(right.size == 1)
            assertEquals("middle", right[0])
        }
    }

    describe("Location.toDimensionalCoordinates") {
        it("should convert x/y/z values") {
            val world = mock<World>()
            val worldUid = UUID.randomUUID()
            world.uid.whenCalled.thenReturn(worldUid)

            val loc = mock<Location>()
            loc.x.whenCalled.thenReturn(1.0)
            loc.y.whenCalled.thenReturn(2.0)
            loc.z.whenCalled.thenReturn(3.0)
            loc.world.whenCalled.thenReturn(world)

            val dim = loc.toDimensionalCoordinates()

            assert(dim.count == 3)
            assert(dim.x == 1.0)
            assert(dim.y == 2.0)
            assert(dim.z == 3.0)

            assertEquals(LocationDimensionalCoordinates(loc), dim)
        }
    }

    describe("Map.minus") {
        it("should remove an item without modifying the original map") {
            val map = mapOf("a" to 1, "b" to 2, "c" to 3)
            var processed = map

            processed -= "b"

            assert(map.size == 3)
            assert(processed.size == 2)
            assertEquals(2, map["b"])
            assertEquals(null, processed["b"])
        }
    }

    describe("Player.team variable") {
        xit("Should pull team from the main scoreboard on access") {}
    }

    describe("Table[x,y] = z") {
        it("should set row and column via operation function") {
            val table = HashBasedTable.create<Int, Int, String>()

            table[0, 1] = "first"
            table[1, 0] = "second"

            assertEquals("first", table[0, 1])
            assertEquals("second", table[1, 0])
        }
    }
})