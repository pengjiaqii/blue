package com.example.bluet.ble;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.example.bluet.R;
import com.example.bluetooth.ble.bean.SearchResult;

import java.util.List;

public class DeviceListAdapter extends BaseQuickAdapter<SearchResult, BaseViewHolder> {


    public DeviceListAdapter(@LayoutRes int layoutResId, @Nullable List<SearchResult> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, SearchResult item) {
        helper.setText(R.id.name, item.getName());
        helper.setText(R.id.mac, item.getAddress());
        helper.setText(R.id.rssi, String.format("Rssi: %d", item.getRssi()));
    }


}
