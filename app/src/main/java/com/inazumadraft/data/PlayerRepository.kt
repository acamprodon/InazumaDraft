package com.inazumadraft.data

import com.inazumadraft.R
import com.inazumadraft.model.Player

object PlayerRepository {

    val players = listOf(
        Player("Mark Evans", "PT", 85, 70, 80, R.drawable.mark),
        Player("Axel Blaze", "DL", 90, 85, 88, R.drawable.axel),
        Player("Jude Sharp", "MC", 78, 82, 91, R.drawable.jude),
        Player("Nathan Swift", "DF", 80, 95, 75, R.drawable.nathan),
        Player("Shawn Froste", "DL", 88, 84, 86, R.drawable.shawn),
        Player("Byron Love", "MC", 82, 80, 90, R.drawable.byron),
        Player("Jack Wallside", "DF", 85, 60, 70, R.drawable.jack),
        Player( "Joseph King", "PT", 80, 75, 79, R.drawable.king),
        Player ("Bobby Shearer", "DF", 80, 70, 80, R.drawable.bobby),
        Player ("Erik", "MC", 82,80,82, R.drawable.erik),
        Player ("Kevin Dragonfly" , "DL", 80, 75, 75, R.drawable.kevin),
        Player("Shadow", "DL", 90, 90, 90, R.drawable.shadow),
        Player("Caleb", "MC", 80, 79, 88, R.drawable.caleb),
        Player("Archer", "DF", 83, 73, 65, R.drawable.archer)

    )

    fun getPlayersByPosition(position: String): List<Player> {
        return players.filter { it.position.equals(position, ignoreCase = true) }
    }

    fun getRandomPlayers(count: Int): List<Player> {
        return players.shuffled().take(count)
    }
}
