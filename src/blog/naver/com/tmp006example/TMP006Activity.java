package blog.naver.com.tmp006example;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class TMP006Activity extends Activity {

	private static final String TAG = "TMP006";
	
	private static final String UUID_POWER_ENABLER = "000084a1-0000-1000-8000-00805f9b34fb";
	private static final String UUID_VOBJ_READ = "000084a2-0000-1000-8000-00805f9b34fb";
	private static final String UUID_TAMB_READ = "000084a3-0000-1000-8000-00805f9b34fb";
	
	private static final int REQUEST_ENABLE_BT = 1;
	private static final String ADDRESS_DATA = "Address";
	private static final String READ_VOBJ_DATA = "VOBJ";
	private static final String READ_TAMB_DATA = "TAMB";
	
	private static final int HANDLER_CONNECT_ADDRESS = 1;
	private static final int HANDLER_READ_VOBJ_DATA = 2;
	private static final int HANDLER_READ_TAMB_DATA = 3;
	private static final int HANDLER_ASK_VOBJ = 4;
	private static final int HANDLER_ASK_TAMB = 5;
	private static final int HANDLER_ASK_POWER_STATE = 6;
	
	private static final int POWER_OFF = 0;
	private static final int POWER_ON = 1;
	private static final int UNKNOWN_STATE = 2;
	
	private static final int INDEX_SCAN = 0;
	private static final int INDEX_MEASUREMENT = 1;
	private static final int INDEX_POWER_STATE = 2;
	
	private TextView mTextVOBJ;
	private TextView mTextTAMB;
	private TextView mTextTemperature;
	
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothLeService mBluetoothLeService;
	private BluetoothGattCharacteristic mGattCharacteristic;
	
	private String mDeviceAddress;
	
	private int PowerState = UNKNOWN_STATE;
	private Menu mMenu;
	
	private Handler mHandler = new Handler(){
		
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			switch(msg.what){
			case HANDLER_CONNECT_ADDRESS:
				Bundle address_bundle = msg.getData();
				mDeviceAddress = address_bundle.getString(ADDRESS_DATA);
				
				if( mDeviceAddress != null ){
					if(mBluetoothLeService.connect(ADDRESS_DATA)){
						Toast.makeText(getApplication(), "connecting : " + mDeviceAddress, Toast.LENGTH_SHORT).show();
					}
					else{
						Toast.makeText(getApplication(), "cannot connect : " + mDeviceAddress, Toast.LENGTH_SHORT).show();
					}
				}
				break;
			case HANDLER_READ_VOBJ_DATA:
				break;
			case HANDLER_READ_TAMB_DATA:
				break;
			case HANDLER_ASK_TAMB:
				break;
			case HANDLER_ASK_VOBJ:
				break;
			case HANDLER_ASK_POWER_STATE:
				break;
			default:
				break;
			}
		};
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tmp006);
		
		setup();
	}

	private void setup(){
		
		mTextVOBJ = (TextView)findViewById(R.id.value_vobj);
		mTextTAMB = (TextView)findViewById(R.id.value_tamb);
		mTextTemperature = (TextView)findViewById(R.id.value_temperature);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.tmp006, menu);
		mMenu = menu;
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_scan) {
			DeviceScanDialog dialog = getNewScanDialog();
			
			dialog.show();
			
			return true;
		}
		else if (id == R.id.action_measurement){
			Toast.makeText(getApplication(), "measure", Toast.LENGTH_SHORT).show();
			return true;
		}
		else if( id == R.id.action_power_state){
			Toast.makeText(getApplication(), "power_state", Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private DeviceScanDialog getNewScanDialog(){
		
		DeviceScanDialog dialog = new DeviceScanDialog(this, mBluetoothAdapter, mHandler);
		
		return dialog;
	}
	
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBluetoothLeService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			
			if(mBluetoothLeService.initialize()){
				Log.e(TAG, "unable to initialize BluetootLE service");
				finish();
			}
		}
	};
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			final String action = intent.getAction();
			
			if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
				MenuItem item = mMenu.getItem(INDEX_SCAN);
				
				item.setTitle(R.string.action_disconnect);
				
				item = mMenu.getItem(INDEX_MEASUREMENT);
				item.setVisible(true);
				item = mMenu.getItem(INDEX_POWER_STATE);
				item.setTitle(R.string.action_state_unknown);
				item.setVisible(true);
			}
			else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
				PowerState = UNKNOWN_STATE;
				
				MenuItem item = mMenu.getItem(INDEX_SCAN);
				item.setTitle(R.string.action_scanning);
				item = mMenu.getItem(INDEX_MEASUREMENT);
				item.setVisible(false);
				item = mMenu.getItem(INDEX_POWER_STATE);
				item.setVisible(false);
			}
			else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
				
			}
			else if(BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)){
				
				Bundle b = intent.getExtras();
				String uuid = b.getString(BluetoothLeService.ACTION_DATA_AVAILABLE);
				
				if(uuid != null){
					if(uuid.equals(UUID_POWER_ENABLER)){
						
						boolean power_state = b.getBoolean(BluetoothLeService.ACTION_DATA_AVAILABLE);
						MenuItem item = mMenu.getItem(INDEX_POWER_STATE);
						if(power_state){
							item.setTitle(R.string.action_state_power_on);
						}
						else{
							item.setTitle(R.string.action_state_power_off);
						}
					}
					else if(uuid.equals(UUID_TAMB_READ)){
						short tamb = b.getShort(BluetoothLeService.ACTION_DATA_AVAILABLE);
						
						mTextTAMB.setText(Integer.toHexString(tamb & 0xffff));
					}
					else if(uuid.equals(UUID_VOBJ_READ)){
						short vobj = b.getShort(BluetoothLeService.ACTION_DATA_AVAILABLE);
						
						mTextVOBJ.setText(Integer.toHexString(vobj & 0xffff));
					}
					else{
						Log.w(TAG, "unknown uuid : " + uuid);
					}
				}
				else{
					Log.e(TAG, "UUID is null");
				}
			}
		}
	};
	
	private void sendMessage(int message_type){
		
		Bundle b = new Bundle();
		Message msg = new Message();
		
		msg.what = message_type;
		
		switch(message_type){
		case HANDLER_READ_TAMB_DATA:
			break;
		case HANDLER_READ_VOBJ_DATA:
			break;
		case HANDLER_ASK_TAMB:
			break;
		case HANDLER_ASK_VOBJ:
			break;
		case HANDLER_ASK_POWER_STATE:
			break;
		}
	}
	
	private static IntentFilter makeGattupdateIntentFilter(){
		final IntentFilter intentFilter = new IntentFilter();
		
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}
}
