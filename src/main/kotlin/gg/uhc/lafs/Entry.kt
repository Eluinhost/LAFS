package gg.uhc.lafs

import gg.uhc.lafs.kd.KdTree
import gg.uhc.lafs.rx.Events
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit

class Entry : JavaPlugin() {
    override fun onEnable() {
        config.options().copyDefaults(true)
        saveConfig()
        val events = Events(this)

        val maxTeamSize = config.getInt("max team size")

        // Create a tracker that automatically tracks online locations
        PlayerTracker(events)
            .observableLocations
            .observeOn(events.syncScheduler)
            // only trigger at most once per second (at end of the second)
            .throttleLast(1, TimeUnit.SECONDS)
            // Make into dimensionals + Map of dimensional to current team info
            // so we can use the team info on the async thread safely
            .map { mapping ->
                val teamInfo = mapping.values.associate {
                    val team = it.data.team
                    val full = if (team == null) true else team.size >= maxTeamSize

                    it to (team to full)
                }

                return@map mapping.values to teamInfo
            }
            // Hard calculations on async thread
            .observeOn(events.asyncScheduler)
            .map { prev ->
                val (dimensionals, teamInfo) = prev

                return@map dimensionals
                    .groupBy { it.coordinates.worldId }
                    .flatMap { entry ->
                        val (worldId, inWorld) = entry

                        // Make a k-d tree for nearest neighbour checking
                        val tree = KdTree(inWorld.toList())

                        // Create a list of Pair(players -> team) to now be assigned to
                        return@flatMap tree.items
                            // Only calculate for people not in a team
                            .filter { teamInfo[it] == null }
                            .mapNotNull { tree
                                // Find the closest player with a non-full team within 10 blocks
                                // Convert to null if none found, otherwise a pair of player+team to put them on
                                .nearestNeighbours(it, 1, { other, distance -> teamInfo[other]?.second /* second is 'full' */ == false }, 10.0)
                                .firstOrNull()
                                ?.let {
                                    val team = teamInfo[it.dimensional]

                                    it.dimensional.data to team!! // can't be null as we selected only non-full teams
                                }
                            }
                    }
            }
            // We now have a list of pairs of Player + Team to assign to
            // Invert into a map of Team -> List<Player> mapping for updating teams
            .map { it.groupBy(
                keySelector = { it.second.first!! /* second = teamInfo, first = team */},
                valueTransform = { it.first }
            )}
            // Jump back onto the main thread and update teams as needed
            .subscribeOn(events.syncScheduler)
            .subscribe { toProcess ->
                toProcess.forEach { entry ->
                    val (team, players) = entry

                    players.forEach { it.team = team }

                    server.broadcastMessage("${ChatColor.GOLD}Team ${team.name} now has ${team.size} members")
                }
            }
    }
}