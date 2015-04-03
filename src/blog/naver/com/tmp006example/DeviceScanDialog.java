package blog.naver.com.tmp006example;

import java.util.ArrayList;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceScanDialog extends Dialog implements OnClickListener{

	private static final String TAG = "DeviceScanDialog";
	private static final String ADDRESS_DATA = "Address";
	
	private static final int HANDLER_CONNECT_ADDRESS = 1;
	
	private Button mCancel;
	private ListView mDeviceList;
	private BluetoothAdapter mBluetoothAdapter;
	private Handler mHandler;
	private LeDeviceListAdapter mLeDeviceListAdapter;
	
	public DeviceScanDialog(Context context, BluetoothAdapter adapter, Handler handler) {
		super(context);
		
		if( adapter != null){
			mBluetoothAdapter = adapter;
		}
		else{
			Log.e(TAG, "BluetoothAdapter is null");
			return;
		}
		
		if(handler != null){
			mHandler = handler;
		}
		else{
			Log.e(TAG, "Handler is null");
			return;
		}
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dev_list);
		
		scanLeDevice(true);
		
		mCancel = (Button)findViewById(R.id.btn_scan_cancel);
		mCancel.setOnClickListener(this);
		mDeviceList = (ListView)findViewById(R.id.deviceList);
		mDeviceList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
				Message msg = new Message();
				Bundle b = new Bundle();
				
				b.putString(ADDRESS_DATA, device.getAddress());
				msg.setData(b);
				msg.what = HANDLER_CONNECT_ADDRESS;
				
				mHandler.sendMessage(msg);
				scanLeDevice(false);
				dismiss();
			}
			
		});
		
		mLeDeviceListAdapter = new LeDeviceListAdapter();
		mDeviceList.setAdapter(mLeDeviceListAdapter);
		mLeDeviceListAdapter.clear();
	}

	@Override
	public void onClick(View v) {
		
		scanLeDevice(false);
		dismiss();
	}

	private void scanLeDevice(final boolean enable){
		
		if(enable){
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		}
		else{
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}
	
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			
			mLeDeviceListAdapter.addDevice(device);
			mLeDeviceListAdapter.notifyDataSetChanged();
		}
	};
	
	private class LeDeviceListAdapter extends BaseAdapter {

		private ArrayList<BluetoothDevice> mLeDevices;
		private LayoutInflater mInflater;
		
		public LeDeviceListAdapter() {
			super();
			mLeDevices = new ArrayList<BluetoothDevice>();
			mInflater = DeviceScanDialog.this.getLayoutInflater();
		}
		
		public void addDevice(BluetoothDevice device){
			
			if(!mLeDevices.contains(device)){
				mLeDevices.add(device);
			}
		}
		
		public BluetoothDevice getDevice(int position){
			return mLeDevices.get(position);
		}
		
		public void clear(){
			mLeDevices.clear();
		}
		
		@Override
		public int getCount() {
			return mLeDevices.size();
		}

		@Override
		public Object getItem(int position) {
			return mLeDevices.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.dev_format, null);
				viewHolder = new ViewHolder();
				viewHolder.deviceAddress = (TextView) convertView.findViewById(R.id.device_address);
				viewHolder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			BluetoothDevice device = mLeDevices.get(position);
			final String deviceName = device.getName();
			if (deviceName != null && deviceName.length() > 0)
				viewHolder.deviceName.setText(deviceName);
			else
				viewHolder.deviceName.setText("unknown device");
			viewHolder.deviceAddress.setText(device.getAddress());

			return convertView;
		}
		
	}
	
	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}
}
