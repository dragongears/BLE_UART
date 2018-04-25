package com.dragongears.bleuart;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

public class PeripheralActivity extends Activity implements BleWrapperUiCallbacks {	
    public static final String EXTRAS_DEVICE_NAME    = "BLE_DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "BLE_DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_RSSI    = "BLE_DEVICE_RSSI";

	SharedPreferences preferences;

	ArrayList<String> items;
	ArrayAdapter<String> itemsAdapter;
	ListView lvItems;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceRSSI;

    private BleWrapper mBleWrapper;
    
    private TextView mDeviceNameView;
    private TextView mDeviceAddressView;
    private TextView mDeviceRssiView;
    private TextView mDeviceStatus;

	// UI elements
	private EditText input;
	private Button send;

	StringBuilder stringBuilder;
	String message;
	int len;
	int pos;



	// Handler for mouse click on the send button.
	public void sendClick(View view) {
		message = input.getText().toString();

		// We can only send 20 bytes per packet, so break longer messages
		// up into 20 byte payloads
		len = message.length();
		pos = 0;

		sendChunk();

		// Add message to list if it's not already there
		if (message.length() > 0 && !items.contains(message)) {
			itemsAdapter.insert(message, 0);
		}

	}

	public void sendChunk() {
		if (len != 0) {
			stringBuilder.setLength(0);
			if (len>=20) {
				stringBuilder.append(message.toCharArray(), pos, 20 );
				len-=20;
				pos+=20;
			}
			else {
				stringBuilder.append(message.toCharArray(), pos, len);
				len = 0;
			}
			mBleWrapper.send(stringBuilder.toString());
		}
	}

	public void uiDeviceConnected(final BluetoothGatt gatt,
			                      final BluetoothDevice device)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceStatus.setText("connected");

				invalidateOptionsMenu();

				send = (Button)findViewById(R.id.send);
				send.setClickable(true);
				send.setEnabled(true);
			}
    	});
    }
    
    public void uiDeviceDisconnected(final BluetoothGatt gatt,
			                         final BluetoothDevice device)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceStatus.setText("disconnected");

				invalidateOptionsMenu();

				send = (Button)findViewById(R.id.send);
				send.setClickable(true);
				send.setEnabled(true);
			}
    	});    	
    }
    
    public void uiNewRssiAvailable(final BluetoothGatt gatt,
    							   final BluetoothDevice device,
    							   final int rssi)
    {
    	runOnUiThread(new Runnable() {
	    	@Override
			public void run() {
				mDeviceRSSI = rssi + " db";
				mDeviceRssiView.setText(mDeviceRSSI);
			}
		});    	
    }
    
    public void uiAvailableServices(final BluetoothGatt gatt,
    						        final BluetoothDevice device,
    							    final List<BluetoothGattService> services)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
			}
    	});
    }

    public void uiCharacteristicForService(final BluetoothGatt gatt,
    				 					   final BluetoothDevice device,
    									   final BluetoothGattService service,
    									   final List<BluetoothGattCharacteristic> chars)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
			}
    	});
    }

    public void uiNewValueForCharacteristic(final BluetoothGatt gatt,
											final BluetoothDevice device,
											final BluetoothGattService service,
											final BluetoothGattCharacteristic characteristic,
											final String strValue,
											final int intValue,
											final byte[] rawValue,
											final String timestamp)
    {
    }

	public void uiSuccessfulWrite(final BluetoothGatt gatt,
            					  final BluetoothDevice device,
            					  final BluetoothGattService service,
            					  final BluetoothGattCharacteristic ch,
            					  final String description)
	{
		if (len != 0) {
			sendChunk();
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Writing to " + description + " was finished successfully!", Toast.LENGTH_LONG).show();
			}
		});
	}
	
	public void uiFailedWrite(final BluetoothGatt gatt,
							  final BluetoothDevice device,
							  final BluetoothGattService service,
							  final BluetoothGattCharacteristic ch,
							  final String description)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Writing to " + description + " FAILED!", Toast.LENGTH_LONG).show();
			}
		});	
	}

	
	public void uiGotNotification(final BluetoothGatt gatt,
								  final BluetoothDevice device,
								  final BluetoothGattService service,
								  final BluetoothGattCharacteristic ch)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// at this moment we only need to send this "signal" do characteristic's details view
			}
		});
	}

	@Override
	public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {
		// no need to handle that in this Activity (here, we are not scanning)
	}  	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		setContentView(R.layout.activity_peripheral);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Grab references to UI elements.
//		messages = (TextView) findViewById(R.id.messages);
		input = (EditText) findViewById(R.id.input);

		input.setText(preferences.getString("pref_thought_text", "Hello!"));
		input.setSelection(input.getText().length());

		// Disable the send button until we're connected.
		send = (Button)findViewById(R.id.send);
		send.setClickable(false);
		send.setEnabled(false);

		lvItems = (ListView) findViewById(R.id.listView);
		items = new ArrayList<>();
		itemsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
		lvItems.setAdapter(itemsAdapter);

		try {
			String defaultValue = getResources().getString(R.string.list_default);
			JSONArray jsonArray2 = new JSONArray(preferences.getString("pref_thought_array", defaultValue));
			for (int i = 0; i < jsonArray2.length(); i++) {
				items.add(jsonArray2.getString(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				input.setText(((TextView) view).getText().toString());
			}
		});

		lvItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long rowId) {
				removeItemFromList(position);
				return true;
			}
		});

		lvItems.setEmptyView(findViewById(R.id.emptyElement));

		connectViewsVariables();
		
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceRSSI = intent.getIntExtra(EXTRAS_DEVICE_RSSI, 0) + " db";
        mDeviceNameView.setText(mDeviceName);
        mDeviceAddressView.setText(mDeviceAddress);
        mDeviceRssiView.setText(mDeviceRSSI);
        getActionBar().setTitle(mDeviceName);

		len = 0;
		pos = 0;
		stringBuilder = new StringBuilder();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(mBleWrapper == null) mBleWrapper = new BleWrapper(this, this);
		
		if(!mBleWrapper.initialize()) {
			finish();
		}
		
		// start automatically connecting to the device
    	mDeviceStatus.setText("connecting ...");
    	mBleWrapper.connect(mDeviceAddress);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		mBleWrapper.stopMonitoringRssiValue();
		mBleWrapper.disconnect();
		mBleWrapper.close();

		SharedPreferences.Editor preferencesEditor = preferences.edit();

		input = (EditText) findViewById(R.id.input);

		preferencesEditor.putString("pref_thought_text", input.getText().toString());

		JSONArray jsArray = new JSONArray(items);
		preferencesEditor.putString("pref_thought_array", jsArray.toString());

		preferencesEditor.apply();
	}

	@Override
	protected void onDestroy () {
		super.onDestroy();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.peripheral, menu);
		if (mBleWrapper.isConnected()) {
	        menu.findItem(R.id.device_connect).setVisible(false);
	        menu.findItem(R.id.device_disconnect).setVisible(true);
	    } else {
	        menu.findItem(R.id.device_connect).setVisible(true);
	        menu.findItem(R.id.device_disconnect).setVisible(false);
	    }		
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.device_connect:
            	mDeviceStatus.setText("connecting ...");
            	mBleWrapper.connect(mDeviceAddress);
                return true;
            case R.id.device_disconnect:
            	mBleWrapper.disconnect();
                return true;
            case android.R.id.home:
            	mBleWrapper.disconnect();
            	mBleWrapper.close();
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }	

    
    private void connectViewsVariables() {
    	mDeviceNameView = (TextView) findViewById(R.id.peripheral_name);
		mDeviceAddressView = (TextView) findViewById(R.id.peripheral_address);
		mDeviceRssiView = (TextView) findViewById(R.id.peripheral_rssi);
		mDeviceStatus = (TextView) findViewById(R.id.peripheral_status);
    }


	// method to remove list item
	protected void removeItemFromList(int position) {
		final int deletePosition = position;

		AlertDialog.Builder alert = new AlertDialog.Builder(PeripheralActivity.this);

		alert.setIcon(R.drawable.ic_launcher);
		alert.setTitle("Remove message");
		alert.setMessage("Do you want to remove this message from the list?");
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				items.remove(deletePosition);
				itemsAdapter.notifyDataSetChanged();
				itemsAdapter.notifyDataSetInvalidated();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// just close
			}
		});


		alert.show();

	}

}
