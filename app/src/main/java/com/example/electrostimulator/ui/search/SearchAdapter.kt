package com.example.electrostimulator.ui.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.electrostimulator.R
import com.example.electrostimulator.databinding.ItemDeviceBinding

class SearchAdapter : RecyclerView.Adapter<SearchAdapter.DeviceViewHolder>() {
    private val items = mutableListOf<BtDeviceInfo>()
    private var callback: Callback? = null

    @SuppressLint("NotifyDataSetChanged")
    fun update(items: List<BtDeviceInfo>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    fun addCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BtDeviceInfo) {
            binding.container.setOnClickListener {
                callback?.onItemClick(item)
            }
            binding.apply {
                textName.text =
                    item.deviceName ?: textName.context.getString(R.string.unnamed_device)
                textAddress.text = item.deviceAddress
            }
        }
    }

    interface Callback {
        fun onItemClick(device: BtDeviceInfo)
    }
}
