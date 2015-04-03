package blog.naver.com.tmp006example;

import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class BluetoothLeService extends Service {

private static final String TAG = "BluetoothLeService";
	
	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;
	
	private static final String UUID_POWER_ENABLER = "000084a1-0000-1000-8000-00805f9b34fb";
	private static final String UUID_VOBJ_READ = "000084a2-0000-1000-8000-00805f9b34fb";
	private static final String UUID_TAMB_READ = "000084a3-0000-1000-8000-00805f9b34fb";
	
	public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
	
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;
	
	// Implements callback methods for GATT events that the app cares about. For
		// example,
		// connection change and services discovered.
		private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status,
					int newState) {
				String intentAction;
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					intentAction = ACTION_GATT_CONNECTED;
					mConnectionState = STATE_CONNECTED;
					broadcastUpdate(intentAction);
					Log.i(TAG, "Connected to GATT server.");
					// Attempts to discover services after successful connection.
					Log.i(TAG, "Attempting to start service discovery:"
							+ mBluetoothGatt.discoverServices());

				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					intentAction = ACTION_GATT_DISCONNECTED;
					mConnectionState = STATE_DISCONNECTED;
					Log.i(TAG, "Disconnected from GATT server.");
					broadcastUpdate(intentAction);
				}
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status) {
				if (status == BluetoothGatt.GATT_SUCCESS) {
					broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
				} else {
					Log.w(TAG, "onServicesDiscovered received: " + status);
				}
			}

			@Override
			public void onCharacteristicRead(BluetoothGatt gatt,
					BluetoothGattCharacteristic characteristic, int status) {
				if (status == BluetoothGatt.GATT_SUCCESS) {
					broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
				}
				else{
					Log.w(TAG, Integer.toHexString(status));
				}
			}

			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt,
					BluetoothGattCharacteristic characteristic) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		};

		private void broadcastUpdate(final String action) {
			final Intent intent = new Intent(action);
			sendBroadcast(intent);
		}

		private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
			final Intent intent = new Intent(action);
			// For all other profiles, writes the data formatted in HEX.
			final byte[] data = characteristic.getValue();
			final String UUID = characteristic.getUuid().toString();
			
			if(UUID.equals(UUID_POWER_ENABLER)){
				boolean state = false;
				
				if(data[0] == 0){
					state = false;
				}
				else{
					state = true;
				}
				
				intent.putExtra(ACTION_DATA_AVAILABLE, state);
				intent.putExtra(ACTION_DATA_AVAILABLE, UUID);
			}
			else{
				short short_data = (short) (data[1] << 8 | data[0]);
				intent.putExtra(ACTION_DATA_AVAILABLE, short_data);
				intent.putExtra(ACTION_DATA_AVAILABLE, UUID);
			}
			
			sendBroadcast(intent);
		}

		public class LocalBinder extends Binder {
			BluetoothLeService getService() {
				return BluetoothLeService.this;
			}
		}

		@Override
		public IBinder onBind(Intent intent) {
			return mBinder;
		}

		@Override
		public boolean onUnbind(Intent intent) {
			// After using a given device, you should make sure that
			// BluetoothGatt.close() is called
			// such that resources are cleaned up properly. In this particular
			// example, close() is
			// invoked when the UI is disconnected from the Service.
			close();
			return super.onUnbind(intent);
		}

		private final IBinder mBinder = new LocalBinder();

		/**
		 * Initializes a reference to the local Bluetooth adapter.
		 *
		 * @return Return true if the initialization is successful.
		 */
		public boolean initialize() {
			// For API level 18 and above, get a reference to BluetoothAdapter
			// through
			// BluetoothManager.
			if (mBluetoothManager == null) {
				mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
				if (mBluetoothManager == null) {
					Log.e(TAG, "Unable to initialize BluetoothManager.");
					return false;
				}
			}

			mBluetoothAdapter = mBluetoothManager.getAdapter();
			if (mBluetoothAdapter == null) {
				Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
				return false;
			}

			return true;
		}

		/**
		 * Connects to the GATT server hosted on the Bluetooth LE device.
		 *
		 * @param address
		 *            The device address of the destination device.
		 *
		 * @return Return true if the connection is initiated successfully. The
		 *         connection result is reported asynchronously through the
		 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
		 *         callback.
		 */
		public boolean connect(final String address) {
			if (mBluetoothAdapter == null || address == null) {
				Log.w(TAG,
						"BluetoothAdapter not initialized or unspecified address.");
				return false;
			}

			// Previously connected device. Try to reconnect.
			if (mBluetoothDeviceAddress != null
					&& address.equals(mBluetoothDeviceAddress)
					&& mBluetoothGatt != null) {
				Log.d(TAG,
						"Trying to use an existing mBluetoothGatt for connection.");
				if (mBluetoothGatt.connect()) {
					mConnectionState = STATE_CONNECTING;
					return true;
				} else {
					return false;
				}
			}

			final BluetoothDevice device = mBluetoothAdapter
					.getRemoteDevice(address);
			if (device == null) {
				Log.w(TAG, "Device not found.  Unable to connect.");
				return false;
			}
			// We want to directly connect to the device, so we are setting the
			// autoConnect
			// parameter to false.
			mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
			Log.d(TAG, "Trying to create a new connection.");
			mBluetoothDeviceAddress = address;
			mConnectionState = STATE_CONNECTING;
			return true;
		}

		/**
		 * Disconnects an existing connection or cancel a pending connection. The
		 * disconnection result is reported asynchronously through the
		 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
		 * callback.
		 */
		public void disconnect() {
			if (mBluetoothAdapter == null || mBluetoothGatt == null) {
				Log.w(TAG, "BluetoothAdapter not initialized");
				return;
			}
			mBluetoothGatt.disconnect();
		}

		/**
		 * After using a given BLE device, the app must call this method to ensure
		 * resources are released properly.
		 */
		public void close() {
			if (mBluetoothGatt == null) {
				return;
			}
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}

		/**
		 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
		 * result is reported asynchronously through the
		 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
		 * callback.
		 *
		 * @param characteristic
		 *            The characteristic to read from.
		 */
		public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
			if (mBluetoothAdapter == null || mBluetoothGatt == null) {
				Log.w(TAG, "BluetoothAdapter not initialized");
				return;
			}
			
			if(mConnectionState != STATE_CONNECTED){
				Log.w(TAG, "mBluetoothGatt is not connected");
				return;
			}
			mBluetoothGatt.readCharacteristic(characteristic);
		}

		/**
		 * Enables or disables notification on a give characteristic.
		 *
		 * @param characteristic
		 *            Characteristic to act on.
		 * @param enabled
		 *            If true, enable notification. False otherwise.
		 */
		public void setCharacteristicNotification(
				BluetoothGattCharacteristic characteristic, boolean enabled) {
			if (mBluetoothAdapter == null || mBluetoothGatt == null) {
				Log.w(TAG, "BluetoothAdapter not initialized");
				return;
			}
			
			if(mConnectionState != STATE_CONNECTED){
				Log.w(TAG, "mBluetoothGatt is not connected");
				return;
			}
			mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
		}

		public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte data){
			boolean status = false;
			
			if ( mBluetoothGatt == null){
				Log.e(TAG, "lost connection");
				return false;
			}
			
			if ( characteristic == null){
				Log.e(TAG, "have no characteristic");
				return false;
			}
			
			if(mConnectionState != STATE_CONNECTED){
				Log.w(TAG, "mBluetoothGatt is not connected");
				return false;
			}
			byte[] value = new byte[1];
			value[0] = (byte)data;
			characteristic.setValue(value);
			status = mBluetoothGatt.writeCharacteristic(characteristic);
			
			return status;
		}
		
		public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data){
			boolean status = false;
			
			if ( mBluetoothGatt == null){
				Log.e(TAG, "lost connection");
				return false;
			}
			
			if ( characteristic == null){
				Log.e(TAG, "have no characteristic");
				return false;
			}
			
			if(mConnectionState != STATE_CONNECTED){
				Log.w(TAG, "mBluetoothGatt is not connected");
				return false;
			}
			characteristic.setValue(data);
			status = mBluetoothGatt.writeCharacteristic(characteristic);
			
			return status;
		}
		
		public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String data){
			
			boolean status = false;
			
			if ( mBluetoothGatt == null){
				Log.e(TAG, "lost connection");
				return false;
			}
			
			if (characteristic == null){
				Log.e(TAG, "have no characteristic");
				return false;
			}
			
			if(mConnectionState != STATE_CONNECTED){
				Log.w(TAG, "mBluetoothGatt is not connected");
				return false;
			}
			
			characteristic.setValue(data);
			
			status = mBluetoothGatt.writeCharacteristic(characteristic);
			
			return status;
		}
		
		public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, int data){
			
			boolean status = false;
			
			if ( mBluetoothGatt == null){
				Log.e(TAG, "lost connection");
				return false;
			}
			
			if (characteristic == null){
				Log.e(TAG, "have no characteristic");
				return false;
			}
			
			if(mConnectionState != STATE_CONNECTED){
				Log.w(TAG, "mBluetoothGatt is not connected");
				return false;
			}
			
			byte[] value = new byte[4];
			
			value[0] = (byte)(((data) & 0xFF000000) >> 24);
			value[1] = (byte)(((data) & 0x00FF0000) >> 16);
			value[2] = (byte)(((data) & 0x0000FF00) >>  8);
			value[3] = (byte)(((data) & 0x000000FF) >>  0);
			
			final StringBuilder stringBuilder = new StringBuilder(value.length);
	        for(byte byteChar : value)
	            stringBuilder.append(String.format("%02X ", byteChar));
	        
	        Log.d(TAG, "parsed value : " + stringBuilder.toString());
			characteristic.setValue(value);
			
			status = mBluetoothGatt.writeCharacteristic(characteristic);
			
			return status;
		}
		/**
		 * Retrieves a list of supported GATT services on the connected device. This
		 * should be invoked only after {@code BluetoothGatt#discoverServices()}
		 * completes successfully.
		 *
		 * @return A {@code List} of supported services.
		 */
		public List<BluetoothGattService> getSupportedGattServices() {
			if (mBluetoothGatt == null)
				return null;

			if(mConnectionState != STATE_CONNECTED){
				Log.w(TAG, "mBluetoothGatt is not connected");
				return null;
			}
			
			return mBluetoothGatt.getServices();
		}
}
