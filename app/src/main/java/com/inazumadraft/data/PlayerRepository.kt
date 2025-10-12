package com.inazumadraft.data

import com.inazumadraft.R
import com.inazumadraft.model.Player

object PlayerRepository {

    val players = listOf(
        Player("Mark Evans","Mark", "PT",R.drawable.earth , 72, 68,75, 87,  R.drawable.mark, secondaryPositions = listOf("DF")),
        Player("Axel Blaze", "Axel", "DL", R.drawable.fuego,90, 80, 85,65,  R.drawable.axel),
        Player("Jude Sharp", "Jude","MC",R.drawable.aire, 75, 82, 91,80,  R.drawable.jude),
        Player("Nathan Swift", "Nathan", "DF", R.drawable.aire, 76, 95, 70,70,  R.drawable.nathan, secondaryPositions = listOf("MC", "DL")),
        Player("Shawn Froste", "Shawn", "DL", R.drawable.aire,88, 87, 80,78,  R.drawable.shawn, secondaryPositions = listOf("DF")),
        Player("Byron Love",  "Byron","MC", R.drawable.bosque,85, 80, 90, 68, R.drawable.byron, secondaryPositions = listOf("DL")),
        Player("Jack Wallside","Jack" ,"DF", R.drawable.earth, 62, 49, 70,85,  R.drawable.jack),
        Player( "Joseph King", "King","PT",R.drawable.fuego , 68, 75, 79,84,  R.drawable.king),
        Player ("Bobby Shearer", "Bobby","DF", R.drawable.bosque,70, 70, 78,78,  R.drawable.bobby),
        Player ("Erik Eagle","Erik", "MC",R.drawable.bosque,  82,80,82, 70, R.drawable.erik),
        Player ("Kevin Dragonfly" ,"Kevin", "DL",R.drawable.bosque, 80, 75, 75,62, R.drawable.kevin),
        Player("Shadow Cimmerian", "Shadow", "DL", R.drawable.bosque,90, 90, 90, 80, R.drawable.shadow, secondaryPositions = listOf("DF")),
        Player("Caleb Stonewall", "Caleb", "MC", R.drawable.fuego,80, 79, 88, 75,R.drawable.caleb),
        Player("Archer Hawkins","Archer", "DF", R.drawable.bosque,83, 73, 65,75,  R.drawable.archer),
        Player( "Jim Wraith", "Jim", "DF",R.drawable.bosque, 60, 55, 75, 60, R.drawable.jim ),
        Player("Maxwell Carson", "Max", "DL", R.drawable.aire, 70, 75, 78, 70, R.drawable.max, secondaryPositions = listOf("MC")),
        Player("Sam Kincaid", "Sam", "MC", R.drawable.fuego, 71, 70, 75, 71, R.drawable.sam),
        Player("Steve Grimm", "Steve", "MC", R.drawable.aire, 68, 72, 78, 70, R.drawable.steve),
        Player("Tim Saunders", "Timmy", "MC", R.drawable.bosque, 75, 75, 75, 70, R.drawable.timmy),
        Player("Tod Ironside", "Tod","DF", R.drawable.fuego, 68, 73, 74, 75, R.drawable.tod ),
       Player("William Glass", "Willy","DL", R.drawable.bosque, 60 ,55, 50, 2, R.drawable.willy),
        Player("Thomas Feldt","Feldt", "PT", R.drawable.bosque, 65, 70, 75, 84, R.drawable.feldt),
        Player("Nathan Jones", "Mask", "PT", R.drawable.aire, 63, 78, 75, 80, R.drawable.mask ),
        Player("Troy Moon", "Wolfy", "MC", R.drawable.fuego, 78, 82, 79, 70, R.drawable.wolfy, secondaryPositions = listOf("DL")),
        Player("Russell Walk", "Styx", "DF", R.drawable.bosque,62, 74, 70, 75, R.drawable.styx ),
        Player("Jason Jones", "Creepy", "DF", R.drawable.aire, 58, 72,68,71,R.drawable.creepy),
        Player("Ken Furan", "Franky", "DF", R.drawable.earth, 50, 35, 65, 77, R.drawable.franky) ,
        Player("Jerry Fulton", "Undead", "DF", R.drawable.fuego, 51, 51, 59, 71, R.drawable.undead),
        Player("Ray Mannigs", "Jiangshi", "MC", R.drawable.aire, 65, 72, 70, 62, R.drawable.jiangshi),
        Player("Robert Mayer", "Mummy", "MC", R.drawable.bosque, 66, 68, 71, 64, R.drawable.mummy),
        Player("Alexander Brave", "Grave", "MC", R.drawable.aire, 55 ,72, 67 ,75, R.drawable.grave),
        Player("Johan Tassman", "Talisman", "DL", R.drawable.bosque, 85, 80, 75, 65, R.drawable.talisman),
        Player("Burt Wolf", "Blood", "MC", R.drawable.earth, 79, 80 ,77, 75, R.drawable.blood, secondaryPositions = listOf("DL")),

        )

    fun getPlayersByPosition(position: String): List<Player> {
        return players.filter { it.position.equals(position, ignoreCase = true) }
    }

    fun getRandomPlayers(count: Int): List<Player> {
        return players.shuffled().take(count)
    }
}
