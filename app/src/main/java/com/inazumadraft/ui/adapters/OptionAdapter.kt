package com.inazumadraft.ui.adapters

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
    private val onLongClick: ((Player) -> Unit)? = null
) : RecyclerView.Adapter<OptionAdapter.OptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
     val p= players[position]
     val d= holder.itemView.resources.displayMetrics.density
        holder.itemView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (220*d).toInt()
        )
        holder.bind(p)
    }

    override fun getItemCount(): Int = players.size

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val txtPlayerNickname: TextView = itemView.findViewById(R.id.txtPlayerNickname)
        private val imgPlayer: ImageView = itemView.findViewById(R.id.imgPlayer)
        private val imgElement: ImageView = itemView.findViewById(R.id.imgElement)

        fun bind(player: Player) {
            // 👇 nickname visible durante el draft
            txtPlayerNickname.text = player.nickname
            imgPlayer.setImageResource(player.image)
            imgElement.setImageResource(player.element)

            itemView.setOnClickListener {
                onClick(player)
            }

            itemView.setOnLongClickListener {
                onLongClick?.invoke(player)
                true
            }

            itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    }
                }
                false
            }
        }
    }
}
