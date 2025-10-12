package com.inazumadraft.ui

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.model.Player

class OptionAdapter(
    private val players: List<Player>,
    private val onClick: (Player) -> Unit,
    private val onLongClick: ((Player) -> Unit)? = null,
    private val onReleaseAfterLongClick: ((Player) -> Unit)? = null
) : RecyclerView.Adapter<OptionAdapter.OptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtPlayerName: TextView = itemView.findViewById(R.id.txtPlayerName)
        private val imgPlayer: ImageView = itemView.findViewById(R.id.imgPlayer)
        private val imgElement: ImageView = itemView.findViewById(R.id.imgElement)

        private var wasLongPressed = false

        fun bind(player: Player) {
            txtPlayerName.text = player.name
            imgPlayer.setImageResource(player.image)
            imgElement.setImageResource(player.element)

            itemView.setOnClickListener { onClick(player) }

            itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        if (wasLongPressed) {
                            onReleaseAfterLongClick?.invoke(player)
                            wasLongPressed = false
                        }
                    }
                }
                false
            }

            itemView.setOnLongClickListener {
                wasLongPressed = true
                onLongClick?.invoke(player)
                true
            }
        }
    }
}
