package com.inazumadraft.data

import com.inazumadraft.R
import com.inazumadraft.model.Player

object PlayerRepository {

    val players = listOf(
        Player("Mark Evans","Mark", "PT",R.drawable.earth , 74, 75,78, 89,  R.drawable.mark, secondaryPositions = listOf("DF")),
        Player("Axel Blaze", "Axel", "DL", R.drawable.fuego,92, 86, 85,65,  R.drawable.axel),
        Player("Jude Sharp", "Jude","MC",R.drawable.aire, 83, 80, 91,76,  R.drawable.jude),
        Player("Nathan Swift", "Nathan", "DF", R.drawable.aire, 81, 95, 75,79,  R.drawable.nathan, secondaryPositions = listOf("MC", "DL")),
        Player("Shawn Froste", "Shawn", "DL", R.drawable.aire,86, 91, 80,82,  R.drawable.shawn, secondaryPositions = listOf("DF")),
        Player("Byron Love",  "Byron","MC", R.drawable.bosque,88, 84, 90, 68, R.drawable.byron, secondaryPositions = listOf("DL")),
        Player("Jack Wallside","Jack" ,"DF", R.drawable.earth, 62, 55, 72,88,  R.drawable.jack),
        Player( "Joseph King", "King","PT",R.drawable.fuego , 68, 75, 79,85,  R.drawable.king),
        Player ("Bobby Shearer", "Bobby","DF", R.drawable.bosque,70, 73, 78,83,  R.drawable.bobby),
        Player ("Erik Eagle","Erik", "MC",R.drawable.bosque,  82,85,82, 70, R.drawable.erik),
        Player ("Kevin Dragonfly" ,"Kevin", "DL",R.drawable.bosque, 84, 79, 78,62, R.drawable.kevin),
        Player("Shadow Cimmerian", "Shadow", "DL", R.drawable.bosque,95, 95, 95, 95, R.drawable.shadow, secondaryPositions = listOf("DF")),
        Player("Caleb Stonewall", "Caleb", "MC", R.drawable.fuego,76, 84, 88, 81,R.drawable.caleb),
        Player("Archer Hawkins","Archer", "DF", R.drawable.bosque,79, 76, 62,82,  R.drawable.archer),
        Player( "Jim Wraith", "Jim", "DF",R.drawable.bosque, 60, 64, 60, 75, R.drawable.jim ),
        Player("Maxwell Carson", "Max", "DL", R.drawable.aire, 70, 80, 78, 70, R.drawable.max, secondaryPositions = listOf("MC")),
        Player("Sam Kincaid", "Sam", "MC", R.drawable.fuego, 71, 75, 75, 71, R.drawable.sam),
        Player("Steve Grimm", "Steve", "MC", R.drawable.aire, 68, 77, 78, 70, R.drawable.steve),
        Player("Tim Saunders", "Timmy", "MC", R.drawable.bosque, 75, 80, 76, 70, R.drawable.timmy),
        Player("Tod Ironside", "Tod","DF", R.drawable.fuego, 68, 78, 74, 75, R.drawable.tod ),
       Player("William Glass", "Willy","DL", R.drawable.bosque, 49 ,42, 43, 2, R.drawable.willy),
        Player("Thomas Feldt","Feldt", "PT", R.drawable.bosque, 65, 70, 75, 84, R.drawable.feldt),
        Player("Nathan Jones", "Mask", "PT", R.drawable.aire, 63, 79, 75, 80, R.drawable.mask ),
        Player("Troy Moon", "Wolfy", "MC", R.drawable.fuego, 78, 86, 79, 70, R.drawable.wolfy, secondaryPositions = listOf("DL")),
        Player("Russell Walk", "Styx", "DF", R.drawable.bosque,62, 77, 70, 79, R.drawable.styx ),
        Player("Jason Jones", "Creepy", "DF", R.drawable.aire, 58, 76,68,71,R.drawable.creepy),
        Player("Ken Furan", "Franky", "DF", R.drawable.earth, 50, 46, 65, 79, R.drawable.franky, secondaryPositions = listOf("PT")) ,
        Player("Jerry Fulton", "Undead", "DF", R.drawable.fuego, 51, 55, 59, 77, R.drawable.undead),
        Player("Ray Mannigs", "Jiangshi", "MC", R.drawable.aire, 65, 78, 70, 62, R.drawable.jiangshi),
        Player("Robert Mayer", "Mummy", "MC", R.drawable.bosque, 66, 73, 71, 64, R.drawable.mummy),
        Player("Alexander Brave", "Grave", "MC", R.drawable.aire, 55 ,77, 67 ,78, R.drawable.grave, secondaryPositions = listOf("DF")),
        Player("Johan Tassman", "Talisman", "DL", R.drawable.bosque, 85, 83, 75, 65, R.drawable.talisman, secondaryPositions = listOf("MC")),
        Player("Burt Wolf", "Blood", "MC", R.drawable.earth, 79, 83 ,77, 75, R.drawable.blood, secondaryPositions = listOf("DL")),
        Player("Paul Siddon", "Poseidon", "PT", R.drawable.earth, 68, 63, 78, 86, R.drawable.poseidon),
        Player("Apollo Light", "Apollo" ,"DF", R.drawable.bosque, 65, 85, 72, 80, R.drawable.apollo),
        Player("Jeff Iron", "Hephestus", "DF", R.drawable.fuego, 78, 87, 77, 74, R.drawable.jeff, secondaryPositions = listOf("MC")),
        Player("Lane War", "Ares", "DF", R.drawable.earth, 65, 79,75, 86, R.drawable.ares ),
        Player("Danny Wood", "Dionysus", "DF", R.drawable.aire, 65, 74, 78, 87, R.drawable.dionysus),
        Player("Artie Mishman", "Artemis", "MC", R.drawable.aire, 75, 82, 79, 75, R.drawable.artemis),
        Player("Arion Matlock", "Hermes", "MC", R.drawable.bosque, 73, 79, 80, 78, R.drawable.hermes),
        Player("Wesley Knox", "Athena", "MC", R.drawable.bosque, 79,85,79,72,R.drawable.athena),
        Player("Jonas Demetrius", "Demeter", "DL", R.drawable.fuego, 88,82,78,70,R.drawable.demeter),
        Player("Henry House", "Hera", "MC", R.drawable.fuego, 82,84,80,73,R.drawable.hera, secondaryPositions = listOf("DL")),
        Player("Peter Drent", "Drent", "DF", R.drawable.earth, 62,69, 72, 84 ,R.drawable.drent),
        Player("Ben Simmons", "Simmons", "DF", R.drawable.bosque, 60, 76, 74, 79, R.drawable.simmons),
        Player("Alan Master", "Master", "MC", R.drawable.aire, 68, 82, 78, 77, R.drawable.master, secondaryPositions = listOf("DF")),
        Player("Gus Martin", "Martin", "DF", R.drawable.bosque, 66, 78, 79, 79, R.drawable.martin),
        Player("Herman Waldon", "Waldon", "MC", R.drawable.aire, 67, 78, 78, 78, R.drawable.waldon, secondaryPositions = listOf("DF")),
        Player("John Bloom", "Bloom", "MC", R.drawable.fuego, 74, 84, 77, 70, R.drawable.bloom),
        Player("Derek Swing", "Swing", "MC", R.drawable.aire, 74 ,86, 77, 70 ,R.drawable.swing),
        Player("Daniel Hatch", "Hatch", "DL", R.drawable.bosque, 80, 80, 75, 75, R.drawable.hatch, secondaryPositions = listOf("DF")),
        Player("David Samford", "Samford", "DL", R.drawable.bosque, 84, 85, 80, 67, R.drawable.samford, secondaryPositions = listOf("MC")),
        Player("John Neville","Neville", "PT", R.drawable.fuego, 64,63, 73, 84, R.drawable.neville) ,
        Player("Malcolm Night", "Night", "DF", R.drawable.fuego, 75, 80, 77, 80, R.drawable.night),
        Player("Alfred Meenan", "Meenan", "DF", R.drawable.bosque, 64, 82, 76, 82, R.drawable.meenan),
        Player("Dan Mirthful", "Mirthful", "DF", R.drawable.bosque, 60, 72, 75, 79, R.drawable.mirthful),
        Player("Ricky Clover", "Clover", "DF", R.drawable.earth, 64, 77, 76, 81, R.drawable.clover),
        Player("Toby Damian", "Damian", "MC", R.drawable.aire, 74, 85, 79, 73, R.drawable.damian),
        Player("York Nashmith", "Nashmith", "MC", R.drawable.bosque, 73, 79, 80, 70, R.drawable.nashmith),
        Player("Zachary Moore", "Moore", "MC", R.drawable.bosque, 72, 80, 76, 72, R.drawable.moore),
        Player("Marvin Murdock", "Marvin", "DL", R.drawable.fuego, 86, 83, 79, 62, R.drawable.marvin),
        Player("Thomas Murdock", "Thomas", "DL", R.drawable.aire, 82, 80, 77, 70, R.drawable.thomas),
        Player("Tyler Murdock", "Tyler", "DL", R.drawable.earth, 84, 85, 76, 66, R.drawable.tyler),

        )

    fun getPlayersByPosition(position: String): List<Player> {
        return players.filter { it.position.equals(position, ignoreCase = true) }
    }

    fun getRandomPlayers(count: Int): List<Player> {
        return players.shuffled().take(count)
    }
}
