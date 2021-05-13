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
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.mm2d.codereader.R
import net.mm2d.codereader.databinding.ItemResultBinding
import net.mm2d.codereader.result.ScanResultAdapter.ViewHolder

class ScanResultAdapter(
    context: Context,
    private val onItemClickListener: (ScanResult) -> Unit
) : Adapter<ViewHolder>(), Observer<List<ScanResult>> {
    private var results: List<ScanResult> = emptyList()
    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemResultBinding.inflate(layoutInflater, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.apply(result)
        holder.itemView.setOnClickListener {
            onItemClickListener(result)
        }
    }

    override fun getItemCount(): Int = results.size

    override fun onChanged(list: List<ScanResult>?) {
        list ?: return
        val diff = DiffUtil.calculateDiff(DiffCallback(results, list))
        results = list
        diff.dispatchUpdatesTo(this)
    }

    class DiffCallback(
        private val oldList: List<ScanResult>,
        private val newList: List<ScanResult>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
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
