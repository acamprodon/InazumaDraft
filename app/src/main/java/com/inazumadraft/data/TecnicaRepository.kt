package com.inazumadraft.data

import com.inazumadraft.model.Tecnica
import com.inazumadraft.R


object TecnicaRepository {


    val tecnicas = listOf(
        Tecnica("Triángulo letal", "DL", 57, R.drawable.bosque, listOf("Samford", "Hatch", "Swing"), combined = true),
        Tecnica("Súper Relámpago", "DL",45, R.drawable.aire, listOf("Mark", "Axel"),combined = true ),
        Tecnica("Supertrampolín Relámpago", "DL", 64, R.drawable.aire, listOf("Mark", "Axel", "Jack"), combined = true),
        Tecnica("Ruptura Relámpago", "DL",60, R.drawable.aire, listOf("Mark", "Axel", "Jude"), combined = true ),
        Tecnica("Fénix", "DL",63, R.drawable.fuego, listOf("Mark", "Erik", "Bobby"), combined = true),
        Tecnica("Defensa Triple", "PT", 65, R.drawable.earth, listOf("Mark", "Jack", "Tod"), combined = true),
        Tecnica("Triángulo letal 2", "DL",60, R.drawable.bosque, listOf("Mark", "Bobby", "Jude"), combined = true ),
        Tecnica("La Tierra", "DL", 67, R.drawable.earth, listOf("Mark", "Axel", "Shawn"), combined = true),
        Tecnica("Pájaro de Fuego", "DL", 49, R.drawable.fuego, listOf("Axel", "Nathan"), combined = true),
        Tecnica("Trampolín Relámpago", "DL",46, R.drawable.aire, listOf("Axel", "Jack"), combined = true ),
        Tecnica("Tornado Dragón", "DL", 45, R.drawable.fuego, listOf("Axel", "Kevin"), combined = true),
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
        Tecnica ("Remate Combinado", "DL", 42, R.drawable.fuego, listOf("Erik", "Jude"), combined = true),
        Tecnica ("Remate Combinado", "DL", 42, R.drawable.fuego, listOf("Samford", "Jude"), combined = true),
        Tecnica("Muralla Infinita", "PT", 60, R.drawable.earth, listOf("Greeny", "Hillvalley", "Sherman"), combined = true),
        Tecnica("Triángulo Z", "DL", 65, R.drawable.fuego, listOf("Marvin", "Tyler", "Thomas"), combined = true),
        Tecnica("Flecha Huracán", "DF", 61, R.drawable.aire, listOf("Night", "Meenan", "Mirthful"), combined = true),
        Tecnica("Remate en V", "DL",48, R.drawable.aire, listOf("Max", "Steve"), combined = true ),
        Tecnica("Bloqueo Doble", "PT", 49, R.drawable.bosque, listOf("Jim", "Feldt"), combined = true),
        Tecnica("Estrella Fugaz", "DF", 47, R.drawable.fuego, listOf("Timmy", "Sam"), combined = true),
        Tecnica("Remate Triple", "DL", 64, R.drawable.fuego, listOf("Nathan", "Sam", "Tod"), combined = true),
        Tecnica("Fénix Oscuro", "DL", 66, R.drawable.bosque, listOf("Nathan", "Max", "Kevin"), combined = true),
        Tecnica("Disparo Cósmico", "DL", 46,R.drawable.bosque, listOf("Janus", "Diam"), combined = true),
        Tecnica("Tirachinas", "DL", 64, R.drawable.earth, listOf("García", "Jiménez", "Bonachea"), combined = true),
        Tecnica("Tiro a Reacción", "DL", 65, R.drawable.earth, listOf("Mark", "Axel", "Austin"), combined = true),
        Tecnica("Torbellino Trampolín", "DL", 47, R.drawable.aire, listOf("Nathan", "Jack"), combined = true),
        Tecnica("El Huracán", "DL", 48, R.drawable.aire, listOf("Nathan", "Shawn"), combined = true),
        Tecnica("Campo de Fuerza", "MC", 49, R.drawable.bosque, listOf("Jude", "Caleb"), combined = true),
        Tecnica("Bestia del Trueno", "DL", 47, R.drawable.aire, listOf("Shawn", "Thor"), combined = true),
        Tecnica("La Aurora", "DL", 50, R.drawable.aire, listOf("Shawn", "Xavier"), combined = true),
        Tecnica("Big Bang", "DL", 66, R.drawable.fuego, listOf("Shawn", "Xavier", "Jude"), combined = true),
        Tecnica("Tormenta del Tigre", "DL", 46, R.drawable.fuego, listOf("Axel", "Austin"), combined = true),
        Tecnica("Fuego Total", "DL", 64, R.drawable.fuego, listOf("Axel", "Austin", "Xavier"), combined = true),
        Tecnica("Supernova", "DL", 65, R.drawable.bosque, listOf("Xene", "Bellatrix", "Wittz"), combined = true),
        Tecnica("Pingüino Espacial", "DL", 65, R.drawable.aire, listOf("Xene", "Bellatrix", "Wittz"), combined = true),
        Tecnica("Muralla Infinita 2", "PT", 65, R.drawable.earth, listOf("King", "Hillvalley", "Bargie"), combined = true),
        Tecnica("Segadora", "DF", 44, R.drawable.bosque, listOf("Zohen", "Bargie"), combined = true),
        Tecnica("Ciclón Doble", "DF", 44, R.drawable.aire, listOf("Bargie", "Hatch"), combined = true),
        Tecnica("Triángulo Z 2", "DL", 68, R.drawable.fuego, listOf("Quagmire", "Zell", "Wittz"), combined = true),




    )
}