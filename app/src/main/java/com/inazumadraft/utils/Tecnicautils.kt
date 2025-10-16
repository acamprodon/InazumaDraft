package com.inazumadraft.utils

import com.inazumadraft.R
import com.inazumadraft.data.TecnicaRepository
import com.inazumadraft.model.Player
import com.inazumadraft.model.Tecnica

object Tecnicautils {


    // 🔹 Obtiene las técnicas combinadas del equipo
    fun getCombinedTecnica(team: List<Player>): List<Tecnica> {
        val nicknames = team.map { it.nickname }
        return TecnicaRepository.tecnicas.filter {
            it.combined && it.players.all { j -> j in nicknames }
        }
    }

    fun calculateTechniqueBonus(team: List<Player>): Int {
        var bonus = 0


        val combined = getCombinedTecnica(team)
        bonus += combined.sumOf { it.power * 2 } //  combinadas valen doble

        return bonus
    }
}