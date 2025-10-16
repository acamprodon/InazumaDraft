package com.inazumadraft.data

import com.inazumadraft.model.Tecnica
import com.inazumadraft.R


object TecnicaRepository {


    val tecnicas = listOf(
        Tecnica("Triangulo letal", "DL", 57, R.drawable.bosque, listOf("Samford", "Hatch", "Swing"), combined = true),
        Tecnica("Súper Relámpago", "DL",45, R.drawable.aire, listOf("Mark", "Axel"),combined = true ),
        Tecnica("Supertrampolín Relámpago", "DL", 64, R.drawable.aire, listOf("Mark", "Axel", "Jack"), combined = true),
        Tecnica("Ruptura Relámpago", "DL",60, R.drawable.aire, listOf("Mark", "Axel", "Jude"), combined = true ),
        Tecnica("Fénix", "DL",63, R.drawable.fuego, listOf("Mark", "Erik", "Bobby"), combined = true),
        Tecnica("Defensa Triple", "PT", 65, R.drawable.earth, listOf("Mark", "Jack", "Tod"), combined = true),
        Tecnica("Triangulo letal 2", "DL",60, R.drawable.bosque, listOf("Mark", "Bobby", "Jude"), combined = true ),
        Tecnica("La Tierra", "DL", 67, R.drawable.earth, listOf("Mark", "Axel", "Shawn"), combined = true),
        Tecnica("Pájaro de Fuego", "DL", 49, R.drawable.fuego, listOf("Axel", "Nathan"), combined = true),
        Tecnica("Trampolín Relámpago", "DL",46, R.drawable.aire, listOf("Axel", "Jack"), combined = true ),
        Tecnica("Tornado Dragón", "DL", 50, R.drawable.fuego, listOf("Axel", "Kevin"), combined = true),
        Tecnica("Empuje Gemelo F", "DL", 50, R.drawable.fuego, listOf("Axel", "Jude"), combined = true),
        Tecnica("Fuego Cruzado", "DL", 50, R.drawable.fuego, listOf("Axel", "Shawn"), combined = true),
        Tecnica("Remate Gafas", "DL",25,R.drawable.earth, listOf("Kevin", "Willy"), combined = true ),
        Tecnica("Ventisca Guiverno", "DL",50, R.drawable.aire, listOf("Kevin", "Shawn",), combined = true),
        Tecnica("Pingüino Emperador N2", "DL",62, R.drawable.bosque, listOf("Jude", "Samford", "Hatch"), combined = true),
        Tecnica("Pingüino Emperador N3", "DL",65, R.drawable.bosque, listOf("Jude", "Samford", "Caleb"), combined = true),
        Tecnica("Torre Perfecta", "DF",63, R.drawable.aire, listOf("Tori", "Scotty", "Hurley"), combined = true ),
        Tecnica("Baile de Mariposas", "DL", 44, R.drawable.bosque, listOf("Tori", "Sue"), combined = true),
        Tecnica("Remate Halcón", "DL", 42, R.drawable.aire, listOf("Chiken", "Eagle"), combined = true),
        Tecnica("Bateo Total", "DL", 40, R.drawable.fuego, listOf("Gamer", "Artist"), combined = true),
        Tecnica ("Remate Combinado", "DL", 44, R.drawable.fuego, listOf("Erik", "Jude"), combined = true),
        Tecnica("Muralla Infinita", "PT", 60, R.drawable.earth, listOf("Greeny", "Hillvalley", "Sherman"), combined = true),
        Tecnica("Triángulo Z", "DL", 65, R.drawable.fuego, listOf("Marvin", "Tyler", "Thomas"), combined = true),
        Tecnica("Flecha Huracán", "DF", 61, R.drawable.aire, listOf("Night", "Meenan", "Mirthful"), combined = true),



    )
}