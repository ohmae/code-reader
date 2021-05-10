/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.result

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.mm2d.codereader.R
import net.mm2d.codereader.databinding.ItemResultBinding
import net.mm2d.codereader.result.ScanResultAdapter.ViewHolder

class ScanResultAdapter(
    context: Context,
    private val listener: (ScanResult) -> Unit
) : Adapter<ViewHolder>() {
    private val results: MutableList<ScanResult> = mutableListOf()
    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemResultBinding.inflate(layoutInflater, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.apply(result)
        holder.itemView.setOnClickListener {
            listener(result)
        }
    }

    override fun getItemCount(): Int = results.size

    fun add(result: ScanResult): Boolean {
        if (results.contains(result)) return false
        results.add(result)
        notifyItemInserted(results.size - 1)
        return true
    }

    class ViewHolder(
        private val binding: ItemResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun apply(result: ScanResult) {
            val context = binding.root.context
            binding.resultValue.text = result.value
            binding.resultType.text = context.getString(R.string.type, result.type)
            binding.resultFormat.text = context.getString(R.string.format, result.format)
        }
    }
}
