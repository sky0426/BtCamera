package kr.co.rh.apps.btcamera.main;

import kr.co.rh.apps.btcamera.R;
import kr.co.rh.apps.btcamera.bluetooth.data.SendData;
import kr.co.rh.apps.btcamera.bluetooth.service.BluetoothChatService;
import kr.co.rh.apps.btcamera.comm.Constants;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ClientActivity extends Activity {

		private StringBuffer mOutStringBuffer;
		
    	private ImageView mImgMainView;
    	private Bitmap mBitmap;
    	private Button btnSaveImg;
    	
        // Debugging
        private static final String TAG = "BluetoothChat";
        private static final boolean D = true;

        // Name of the connected device
        private String mConnectedDeviceName = null;
        
        // Local Bluetooth adapter
        private BluetoothAdapter mBluetoothAdapter = null;
        
        // Member object for the chat services
        private BluetoothChatService mChatService = null;
    		
    	@Override
    	protected void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		
    		requestWindowFeature(Window.FEATURE_NO_TITLE);
    		
    		setContentView(R.layout.activity_client);
    		
    		mImgMainView = (ImageView) findViewById(R.id.client_img_main);
    		
    		// Get local Bluetooth adapter
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            // If the adapter is null, then Bluetooth is not supported
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            
    	}
    	
    	@Override
    	protected void onStart() {    	
    		super.onStart();
    		
    		// If BT is not on, request that it be enabled.
            // setupChat() will then be called during onActivityResult
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
            } else {
                if (mChatService == null) setupChat();
            }
    	}
    	
    	
    	@Override
    	protected void onResume() {	
    		super.onResume();
    		
    		 // Performing this check in onResume() covers the case in which BT was
            // not enabled during onStart(), so we were paused to enable it...
            // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
            if (mChatService != null) {
                // Only if the state is STATE_NONE, do we know that we haven't started already
                if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                  // Start the Bluetooth chat services
                  mChatService.start();
                }
            }
    	}
    	
    	
    	private void setupChat() {
            Log.d(TAG, "setupChat()");

            // Initialize the BluetoothChatService to perform bluetooth connections
            mChatService = new BluetoothChatService(this, mHandler);
            
            mOutStringBuffer = new StringBuffer("");
            
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, Constants.REQUEST_CONNECT_DEVICE_INSECURE);

        }
    	
    	@Override
    	protected void onDestroy() {    	
    		super.onDestroy();
    		
            // Stop the Bluetooth chat services
            if (mChatService != null) mChatService.stop();
    	}
    	
        // The Handler that gets information back from the BluetoothChatService
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                    case BluetoothChatService.STATE_CONNECTED:
//                        mConversationArrayAdapter.clear();
                        break;
                    case BluetoothChatService.STATE_CONNECTING:
                        break;
                    case BluetoothChatService.STATE_LISTEN:
                    case BluetoothChatService.STATE_NONE:
                        break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
//                    byte[] writeBuf = (byte[]) msg.obj;
//                    // construct a string from the buffer
//                    String writeMessage = new String(writeBuf);
////                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                	SendData data = (SendData) msg.obj;
                	Toast.makeText(getApplicationContext(), data.getTitle(), Toast.LENGTH_SHORT).show();
                	mImgMainView.setImageBitmap(data.getImg());
                    Log.e("ssryu", "size : " + msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                                   + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                   Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        };

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(D) Log.d(TAG, "onActivityResult " + resultCode);
            switch (requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case Constants.REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case Constants.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

        private void connectDevice(Intent data, boolean secure) {
            // Get the device MAC address
            String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            // Get the BluetoothDevice object
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            // Attempt to connect to the device
            mChatService.connect(device, secure);
        }
}