package com.inazumadraft.data

import com.inazumadraft.model.Tecnica
import com.inazumadraft.R


object TecnicaRepository {


    val tecnicas = listOf(
        Tecnica("Mano celestial", "PT",22 , R.drawable.earth, listOf("Mark")),
        Tecnica("Tornado de Fuego","DL", 33, R.drawable.fuego, listOf("Axel", "Turner", "Hephestus") ),
        Tecnica("Espejismo de balon", "MC", 27, R.drawable.bosque, listOf("Jude", "Erik", "Artemis", "Chronos", "Koala", "Max", "Moore", "Meenan", "Marvin", "Master", "Drent", "Eagle", "Simmons" )),
        Tecnica("Triangulo letal", "DL", 57, R.drawable.bosque, listOf("Samford", "Hatch", "Swing"), combined = true),
        Tecnica("Despeje de Fuego", "PT", 18, R.drawable.fuego, listOf("Mark", "Idol", "Franky")),
        Tecnica("Super Relampago", "DL",45, R.drawable.aire, listOf("Mark", "Axel"),combined = true ),


    )
}