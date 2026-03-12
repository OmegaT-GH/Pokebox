package com.omegat.pokebox.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.omegat.pokebox.R
import com.omegat.pokebox.data.LogMovimiento

class LogAdapter(
    private val context: Context,
    private var logs: List<LogMovimiento>
) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardImage: ImageView = itemView.findViewById(R.id.ivLogCardImage)
        val cardName: TextView = itemView.findViewById(R.id.tvLogCardName)
        val cardSet: TextView = itemView.findViewById(R.id.tvLogCardSet)
        val movement: TextView = itemView.findViewById(R.id.tvLogMovement)
        val timestamp: TextView = itemView.findViewById(R.id.tvLogTimestamp)

        @SuppressLint("SetTextI18n")
        fun bind(log: LogMovimiento) {
            Glide.with(context)
                .load(log.cardImageUrl)
                .placeholder(R.drawable.placeholdercard)
                .error(R.drawable.placeholdercard)
                .fitCenter()
                .into(cardImage)

            cardName.text = log.cardName ?: context.getString(R.string.unknown_card)
            cardSet.text = "${log.setName ?: "N/A"} #${log.cardNumber ?: "?"}"

            val movementText = if (log.cantidadNueva > log.cantidadAnterior) {
                context.getString(R.string.log_increased, log.cantidadAnterior, log.cantidadNueva)
            } else {
                context.getString(R.string.log_decreased, log.cantidadAnterior, log.cantidadNueva)
            }
            movement.text = movementText

            val timeAgo = DateUtils.getRelativeTimeSpanString(
                log.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
            timestamp.text = timeAgo
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newLogs: List<LogMovimiento>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}
