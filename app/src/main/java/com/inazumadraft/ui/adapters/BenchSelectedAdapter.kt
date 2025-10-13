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

class BenchSelectedAdapter(
    private val benchPlayers: MutableList<Player?>,
    private val onChanged: () -> Unit,
    /** fromFieldIndex = índice del slot del campo desde el que se arrastra */
    private val onDropFromField: (toIndex: Int, fromFieldIndex: Int) -> Unit,
    /** Abrir picker sólo cuando el hueco está vacío */
    private val onClickSlot: (index: Int) -> Unit
) : RecyclerView.Adapter<BenchSelectedAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgPlayer)        // usa tu layout de item (p.ej. item_player_option)
        val elem: ImageView = v.findViewById(R.id.imgElement)
        val name: TextView = v.findViewById(R.id.txtPlayerNickname)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_field, parent, false)
        return VH(view)
    }


    override fun getItemCount(): Int = benchPlayers.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = benchPlayers[position]
        val d = holder.itemView.resources.displayMetrics.density
        holder.itemView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (100 * d).toInt())

        if (p != null) {
            holder.img.setImageResource(p.image)
            holder.elem.setImageResource(p.element)
            holder.name.text = p.nickname

            // Iniciar drag DESDE el banquillo -> para soltar en el campo
            holder.itemView.setOnLongClickListener {
                val clip = ClipData.newPlainText("benchPlayer", "bench")
                val shadow = View.DragShadowBuilder(it)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    it.startDragAndDrop(clip, shadow, position, 0)
                else
                    @Suppress("DEPRECATION") it.startDrag(clip, shadow, position, 0)
                it.alpha = 0.5f
                true
            }
        } else {
            // Slot vacío
            holder.img.setImageResource(0)
            holder.elem.setImageResource(0)
            holder.name.text = "Vacío"
            holder.itemView.setOnLongClickListener(null)
        }

        // Clic: sólo si está vacío → abre player pick
        holder.itemView.setOnClickListener {
            if (benchPlayers[holder.bindingAdapterPosition] == null) {
                onClickSlot(holder.bindingAdapterPosition)
            }
        }

        // Aceptar drop DESDE el campo -> intercambiar campo ↔ banquillo
        holder.itemView.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.8f; true }
                DragEvent.ACTION_DRAG_EXITED  -> { v.alpha = 1f; true }
                DragEvent.ACTION_DROP -> {
                    val fromField = event.clipDescription?.label == "fromIndex"
                    if (fromField) {
                        val fromFieldIndex = (event.localState as? Int) ?: return@setOnDragListener true
                        onDropFromField(holder.bindingAdapterPosition, fromFieldIndex)
                        onChanged()
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                else -> false
            }
        }
    }
}
