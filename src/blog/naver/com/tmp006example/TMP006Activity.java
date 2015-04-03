package blog.naver.com.tmp006example;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
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
	
	private static final int HANDLER_CONNECT_ADDRESS = 1;
	private static final int HANDLER_READ_VOBJ_DATA = 2;
	private static final int HANDLER_READ_TAMB_DATA = 3;
	private static final int HANDLER_ASK_VOBJ = 4;
	private static final int HANDLER_ASK_TAMB = 5;
	
	private static final int POWER_OFF = 0;
	private static final int POWER_ON = 1;
	private static final int UNKNOWN_STATE = 2;
	
	private static final int INDEX_SCAN = 0;
	private static final int INDEX_MEASUREMENT = 1;
	private static final int INDEX_POWER_STATE = 2;
	
	private static final double TMP006_B0 = -0.0000294;
	private static final double TMP006_B1 = -0.00000057;
	private static final double TMP006_B2 = 0.00000000463;
	private static final double TMP006_C2 = 13.4;
	private static final double TMP006_TREF = 298.15;
	private static final double TMP006_A2 = -0.00001678;
	private static final double TMP006_A1 = 0.00175;
	private static final double TMP006_S0 = 6.4;
	
	private TextView mTextVOBJ;
	private TextView mTextTAMB;
	private TextView mTextTemperature;
	
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothLeService mBluetoothLeService;
	private BluetoothGattCharacteristic mGattCharacteristic;
	
	private String mDeviceAddress;
	
	private int PowerState = UNKNOWN_STATE;
	private boolean connected = false;
	private boolean askFlag = false;
	
	private short valueTamb;
	private short valueVOBJ;
	
	private Menu mMenu;
	
	private double calculateTemperature(){
		double Tdie = valueTamb * 0.03125;
		Tdie += 273.15; // T Die temperature
		
		double Vobj = valueVOBJ * 156.25;
		Vobj /= 1000;
		Vobj /= 1000;
		Vobj /= 1000; // nV -> V
		
		double tdie_tref = Tdie - TMP006_TREF;
		double S = (1 + TMP006_A1*tdie_tref + 
                TMP006_A2*tdie_tref*tdie_tref);
		S *= TMP006_S0;
		S /= 10000000;
		S /= 10000000;

		double Vos = TMP006_B0 + TMP006_B1*tdie_tref + 
           TMP006_B2*tdie_tref*tdie_tref;

		double fVobj = (Vobj - Vos) + TMP006_C2*(Vobj-Vos)*(Vobj-Vos);

		double Tobj = Math.sqrt(Math.sqrt(Tdie * Tdie * Tdie * Tdie + fVobj/S));

		Tobj -= 273.15; // Kelvin -> *C
		
		return Tobj;
	}
	private Handler mHandler = new Handler(){
		
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			switch(msg.what){
			case HANDLER_CONNECT_ADDRESS:
				Bundle address_bundle = msg.getData();
				mDeviceAddress = address_bundle.getString(ADDRESS_DATA);
				
				Log.d(TAG, "Address : " + mDeviceAddress);
				
				if( mDeviceAddress != null ){
					if(mBluetoothLeService.connect(mDeviceAddress)){
						Toast.makeText(getApplication(), "connecting : " + mDeviceAddress, Toast.LENGTH_SHORT).show();
					}
					else{
						Toast.makeText(getApplication(), "cannot connect : " + mDeviceAddress, Toast.LENGTH_SHORT).show();
					}
				}
				break;
			case HANDLER_READ_VOBJ_DATA:
				mGattCharacteristic = mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(1);
				mBluetoothLeService.readCharacteristic(mGattCharacteristic);
				break;
			case HANDLER_READ_TAMB_DATA:
				mGattCharacteristic = mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(2);
				mBluetoothLeService.readCharacteristic(mGattCharacteristic);
				break;
			case HANDLER_ASK_TAMB:
				break;
			case HANDLER_ASK_VOBJ:
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
		
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
			Toast.makeText(getApplication(), "Bluetooth LE is not supported", Toast.LENGTH_SHORT).show();
			finish();
		}
		
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		
		if(mBluetoothAdapter == null){
			Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT).show();
			finish();
		}
		
		setup();
		
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		
		if(startService(gattServiceIntent) != null){
			bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
			
		}
		else{
			Log.w(TAG, "bind service is null");
		}
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
			
			if(!connected){
				DeviceScanDialog dialog = getNewScanDialog();
				dialog.show();
			}
			else{
				mBluetoothLeService.disconnect();
			}
			return true;
		}
		else if (id == R.id.action_measurement){
			if ( askFlag ){
				item.setTitle("측정 시작");
				Toast.makeText(getApplication(), "측정 종료", Toast.LENGTH_SHORT).show();
				askFlag = false;
				mHandler.removeMessages(HANDLER_READ_VOBJ_DATA);
				mHandler.removeMessages(HANDLER_READ_TAMB_DATA);
			}
			else{
				item.setTitle("측정 종료");
				Toast.makeText(getApplication(), "측정 시작", Toast.LENGTH_LONG).show();
				mGattCharacteristic = mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(1);

				mHandler.sendEmptyMessage(HANDLER_READ_VOBJ_DATA);
				askFlag = true;
			}
			return true;
		}
		else if( id == R.id.action_power_state){
			
			mGattCharacteristic = mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(0);
			
			if(PowerState == POWER_OFF){
				mBluetoothLeService.writeCharacteristic(mGattCharacteristic, (byte)0x01);
				
				PowerState = POWER_ON;
				
				item.setTitle(R.string.action_state_power_on);
			}
			else if (PowerState == POWER_ON){
				mBluetoothLeService.writeCharacteristic(mGattCharacteristic, (byte)0x00);
				
				PowerState = POWER_OFF;
				
				item.setTitle(R.string.action_state_power_off);
			}
			else{
				mBluetoothLeService.readCharacteristic(mGattCharacteristic);
			}
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
			
			if(!mBluetoothLeService.initialize()){
				Log.e(TAG, "unable to initialize Bluetoot LE service");
				finish();
			}
		}
	};
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			final String action = intent.getAction();
			
			if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
				
				connected = true;
				
				MenuItem item = mMenu.getItem(INDEX_POWER_STATE);
				item.setTitle(R.string.action_state_unknown);
				item.setVisible(true);
				
				Toast.makeText(getApplication(), "connected", Toast.LENGTH_SHORT).show();
			}
			else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
				PowerState = UNKNOWN_STATE;
				connected = false;
				askFlag = false;
				MenuItem item = mMenu.getItem(INDEX_SCAN);
				item.setTitle(R.string.action_scanning);
				item = mMenu.getItem(INDEX_MEASUREMENT);
				item.setTitle("측정 시작");
				item.setVisible(false);
				item = mMenu.getItem(INDEX_POWER_STATE);
				item.setVisible(false);
			}
			else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
				
			}
			else if(BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)){
				
				Bundle b = intent.getExtras();
				SensorValueParcelableData data = b.getParcelable(BluetoothLeService.ACTION_DATA_AVAILABLE);
				byte[] origin_value = data.value;
				
				if(data.UUID.equals(UUID_POWER_ENABLER)){
					MenuItem item = mMenu.getItem(INDEX_POWER_STATE);
					
					if(origin_value[0] == (byte)0x00){
						PowerState = POWER_OFF;
						Toast.makeText(getApplication(), "sensor power disabled", Toast.LENGTH_SHORT).show();
						
						item.setTitle(R.string.action_state_power_off);
						item.setVisible(false);
						item.setVisible(true);
					}
					else{
						PowerState = POWER_ON;
						Toast.makeText(getApplication(), "sensor power enabled", Toast.LENGTH_SHORT).show();
						item.setTitle(R.string.action_state_power_on);
						item.setVisible(false);
						item.setVisible(true);
					}
					
					item = mMenu.getItem(INDEX_MEASUREMENT);
					item.setVisible(true);
					Log.d(TAG, "RECEIVED POWER STATE");
				}
				else if(data.UUID.equals(UUID_TAMB_READ)){
					
					valueTamb = (short) ((((short)origin_value[1] << 8) & 0xff00) | ((short)origin_value[0] & 0x00ff));
					
					mTextTAMB.setText(Integer.toHexString((int)(valueTamb & 0xffff)));
					mHandler.sendEmptyMessageDelayed(HANDLER_READ_VOBJ_DATA, 1000);
				}
				else if(data.UUID.equals(UUID_VOBJ_READ)){
					valueVOBJ = (short) ((((short)origin_value[1] << 8) & 0xff00) | ((short)origin_value[0] & 0x00ff));
					
					mTextVOBJ.setText(Integer.toHexString((int)(valueVOBJ & 0xffff)));
					
					double temp_data = calculateTemperature();
					
					mTextTemperature.setText(String.valueOf(temp_data));
					mHandler.sendEmptyMessageDelayed(HANDLER_READ_TAMB_DATA, 1000);
				}
			}
		}
	};
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if( !mBluetoothAdapter.isEnabled()){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
		registerReceiver(mGattUpdateReceiver, makeGattupdateIntentFilter());
	};
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if( requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED){
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy() {
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		stopService(gattServiceIntent);
		unbindService(mServiceConnection);
		super.onDestroy();
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if(keyCode == KeyEvent.KEYCODE_BACK){
			AlertDialog.Builder builder = new AlertDialog.Builder(TMP006Activity.this);
			builder.setTitle("종료하시겠습니까?");
			builder.setPositiveButton("종료", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					System.exit(0);
				}
			});
			builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			
			AlertDialog dialog = builder.create();
			
			dialog.show();
			
			return true;
		}
		return super.onKeyDown(keyCode, event);
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
