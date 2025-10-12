package com.inazumadraft.ui.adapters

import android.content.ClipData
import android.os.Build
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.model.Player
import java.util.Collections

/**
 * - Muestra SIEMPRE 5 huecos (lista con Player?).
 * - Click SIEMPRE llama a onClickSlot(index) para abrir el picker.
 * - Long press sólo si hay jugador => inicia drag (label "benchPlayer").
 * - Drop:
 *      · desde CAMPO -> callback onDropFromField(toIndex, fromFieldIndex)
 *      · desde BANQUILLO -> swap/move dentro de banquillo
 */
class BenchSelectedAdapter(
    private val benchPlayers: MutableList<Player?>,                     // tamaño 5 con nulls
    private val onChanged: () -> Unit,
    private val onDropFromField: (toIndex: Int, fromFieldIndex: Int) -> Unit,
    private val onClickSlot: (index: Int) -> Unit
) : RecyclerView.Adapter<BenchSelectedAdapter.VH>() {

    override fun getItemCount(): Int = 5

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_field, parent, false)
        val d = parent.resources.displayMetrics.density
        v.layoutParams = v.layoutParams.apply {
            width = (90 * d).toInt()
            height = (112 * d).toInt()
        }
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(benchPlayers.getOrNull(position), position)
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val img = itemView.findViewById<ImageView>(R.id.imgPlayer)
        private val name = itemView.findViewById<TextView>(R.id.txtPlayerNickname)
        private val elem = itemView.findViewById<ImageView>(R.id.imgElement)

        fun bind(p: Player?, index: Int) {
            // Click SIEMPRE abre picker del slot
            itemView.setOnClickListener { onClickSlot(index) }

            if (p == null) {
                img.setImageResource(0)
                elem.setImageResource(0)
                name.text = "Hueco"
                itemView.setOnLongClickListener(null)
            } else {
                img.setImageResource(p.image)
                elem.setImageResource(p.element)
                name.text = p.nickname
                itemView.setOnLongClickListener {
                    val clip = ClipData.newPlainText("benchPlayer", "benchPlayer")
                    val shadow = View.DragShadowBuilder(it)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) it.startDragAndDrop(clip, shadow, index, 0)
                    else @Suppress("DEPRECATION") it.startDrag(clip, shadow, index, 0)
                    true
                }
            }

            itemView.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.85f; true }
                    DragEvent.ACTION_DRAG_EXITED -> { v.alpha = 1f; true }
                    DragEvent.ACTION_DROP -> {
                        val fromBench = event.clipDescription?.label == "benchPlayer"
                        if (fromBench) {
                            val fromIdx = event.localState as Int
                            if (fromIdx == index) return@setOnDragListener true
                            val a = benchPlayers.getOrNull(fromIdx) ?: return@setOnDragListener true
                            val b = benchPlayers.getOrNull(index)
                            if (index >= benchPlayers.size) {
                                benchPlayers.removeAt(fromIdx)
                                benchPlayers.add(a)
                            } else {
                                // si había null en destino, es "mover"; si había jugador, swap
                                if (b == null) {
                                    benchPlayers[index] = a
                                    benchPlayers[fromIdx] = null
                                } else {
                                    Collections.swap(benchPlayers, fromIdx, index)
                                }
                            }
                            notifyDataSetChanged()
                            onChanged()
                        } else {
                            val fromFieldIndex = (event.localState as? Int) ?: return@setOnDragListener true
                            onDropFromField(index, fromFieldIndex)
                        }
                        v.alpha = 1f
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> { itemView.alpha = 1f; true }
                    else -> false
                }
            }
        }
    }
}
