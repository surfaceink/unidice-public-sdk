package com.unidice.scanandcontrolexample.scanning

import android.bluetooth.le.ScanResult
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unidice.scanandcontrolexample.databinding.UnidiceScanCardBinding
import com.unidice.sdk.api.UnidiceScanResult

class ScanResultsAdapter(private val listener: (UnidiceScanResult) -> Unit) : RecyclerView.Adapter<ScanItemViewBinder>() {

    private val scanResults = mutableListOf<UnidiceScanResult>()

    fun update(newItem: UnidiceScanResult) {
        val newItems = mutableListOf<UnidiceScanResult>()
        newItems.add(newItem)
        update(newItems)
    }

    fun update(newItems: List<UnidiceScanResult>) {
        if (newItems.isEmpty()) {
            scanResults.clear()
            notifyDataSetChanged()
            return
        }
        val newItem = newItems.first()
        val indexQuery = scanResults.indexOfFirst { it.macAddress == newItem.macAddress}
        if (indexQuery != -1) { // A scan result already exists with the same address
            scanResults[indexQuery] = newItem
            notifyItemChanged(indexQuery)
        } else {
            if (newItem.name != null/* && newItem.device.name.contains("Unidice")*/) {
                scanResults.add(newItem)
                notifyItemInserted(scanResults.size - 1)
            } else {
                Log.i("scanresultsadapter", "Not listing this item.  not a Unidice")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanItemViewBinder {
        val binding = UnidiceScanCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanItemViewBinder(binding)
    }

    override fun onBindViewHolder(binder: ScanItemViewBinder, position: Int) =
        binder.bind(scanResults[position], listener)

    override fun getItemCount(): Int = scanResults.size

    override fun getItemId(position: Int): Long = scanResults[position].macAddress.hashCode().toLong()
}

class ScanItemViewBinder(
    private val binding: UnidiceScanCardBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        scanResult: UnidiceScanResult,
        listener: (UnidiceScanResult) -> Unit
    ) = with(binding) {
        deviceName.text = scanResult.name ?: "<unknown>"
        macAddress.text = scanResult.macAddress
        rssi.text = "${scanResult.rssi} dBm"
        root.setOnClickListener { listener.invoke(scanResult) }
    }
}

private val ScanResult.id: Long
    get() {
        require(device.address.isNotBlank())
        return java.lang.Long.parseLong(device.address.replace(":", ""), 16)
    }