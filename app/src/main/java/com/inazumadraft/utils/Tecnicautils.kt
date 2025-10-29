package com.inazumadraft.utils

import com.inazumadraft.data.TecnicaRepository
import com.inazumadraft.model.Player
import com.inazumadraft.model.Tecnica

object Tecnicautils {


    // 🔹 Obtiene las técnicas combinadas del equipo
    suspend fun getCombinedTecnica(team: List<Player>): List<Tecnica> {
        val nicknames = team.map { it.nickname }.toSet()
        val techniques = TecnicaRepository.getTechniques()
        return techniques.filter { tecnica ->
            tecnica.combined && tecnica.players.all { player -> player in nicknames }
        }
    }

    suspend fun calculateTechniqueBonus(team: List<Player>): Int {
        val combined = getCombinedTecnica(team)
        return combined.sumOf { it.power * 2 } //  combinadas valen doble
    }
}