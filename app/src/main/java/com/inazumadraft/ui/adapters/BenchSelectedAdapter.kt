package com.inazumadraft.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.model.Player
import com.inazumadraft.R

class BenchSelectedAdapter(
    private val benchPlayers: MutableList<Player?>,
    private val onTapSlot: (index: Int) -> Unit,   // ← un único callback por tap
    private val onChanged: () -> Unit = {}
) : RecyclerView.Adapter<BenchSelectedAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgPlayer)
        val elem: ImageView = v.findViewById(R.id.imgElement)
        val name: TextView = v.findViewById(R.id.txtPlayerNickname)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Misma carta que en el campo → apariencia idéntica
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_field, parent, false)
        return VH(view)
    }

    override fun getItemCount() = benchPlayers.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = benchPlayers[position]
        if (p != null) {
            holder.img.setImageResource(p.image)
            holder.elem.setImageResource(p.element)
            holder.name.text = p.nickname
        } else {
            holder.img.setImageResource(0)
            holder.elem.setImageResource(0)
            holder.name.text = "Vacío"
            holder.img.background = null
        }

        holder.itemView.setOnClickListener {
            onTapSlot(holder.bindingAdapterPosition)
            onChanged()
        }
    }
}
