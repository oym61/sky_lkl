package com.imagpay.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.imagpay.MessageHandler;
import com.imagpay.SwipeEvent;
import com.imagpay.SwipeHandler;
import com.imagpay.SwipeListener;

public class MagneticCardActivity extends MyActivity {
	private SwipeHandler _handler;
	private MessageHandler _msg;
	private Handler _ui;
	private boolean _testFlag = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_magneticcard);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.title_magneticcard);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		_handler = new SwipeHandler(this);
		// only read magnetic card
		_handler.setReadonly(true);
		_msg = new MessageHandler((TextView) findViewById(R.id.status));
		_ui = new Handler(Looper.myLooper());
		_handler.addSwipeListener(new SwipeListener() {
			@Override
			public void onReadData(SwipeEvent event) {
			}

			@Override
			public void onParseData(SwipeEvent event) {
				if (_testFlag)
					return;
				String result = event.getValue();
				// hex string message
				sendMessage("Final(16)=>% " + result);
				
				String[] tmps = event.getValue().split(" ");
				StringBuffer sbf = new StringBuffer();
				for (String str : tmps) {
					sbf.append((char) Integer.parseInt(str, 16));
					sbf.append(" ");
				}
				// char message: b{card number}^{card holder}^{exp date}{other track1 data}?;{track2 data}
				// or            b{card number}&{card holder}&{exp date}{other track1 data}?;{track2 data}
				final String data = sbf.toString().replaceAll(" ", "");
				int idx = data.indexOf("^");
				// plain text of card data
				if (data.toUpperCase().startsWith("B") && idx > 0 && data.indexOf("?") > 0) {
					sendMessage("Final(10)=>% " + data);
					_ui.post(new Runnable() {
						@Override
						public void run() {
							int idx = data.indexOf("^");
							String cardNo = data.substring(1, idx);
							String cardHolder = "";
							String expDate = "";
							int idx1 = data.indexOf("^", idx + 1);
							if (idx1 > 0 && idx1 < data.length() - 4) {
								cardHolder = data.substring(idx + 1, idx1);
								expDate = data.substring(idx1 + 1, idx1 + 1 + 4);
							}
					
							EditText et = (EditText) findViewById(R.id.cardno);
							et.setText(cardNo);
							et = (EditText) findViewById(R.id.holder);
							et.setText(cardHolder);
							et = (EditText) findViewById(R.id.expdate);
							et.setText(expDate);
						}
					});
				}
				// encryption data of card data
				else if (data.length() > 20 + 5 + 4) {
					_ui.post(new Runnable() {
						@Override
						public void run() {
							EditText et = (EditText) findViewById(R.id.cardno);
							et.setText("****************");
							et = (EditText) findViewById(R.id.holder);
							et.setText("********");
							et = (EditText) findViewById(R.id.expdate);
							et.setText("****");
						}
					});
				} else {
					_ui.post(new Runnable() {
						@Override
						public void run() {
							EditText et = (EditText) findViewById(R.id.cardno);
							et.setText("");
							et = (EditText) findViewById(R.id.holder);
							et.setText("");
							et = (EditText) findViewById(R.id.expdate);
							et.setText("");
						}
					});
				}
			}

			@Override
			public void onDisconnected(SwipeEvent event) {
				sendMessage("Device is disconnected!");
				toggleConnectStatus();
			}

			@Override
			public void onConnected(SwipeEvent event) {
				sendMessage("Device is connected!");
				checkDevice();
			}

			@Override
			public void onStarted(SwipeEvent event) {
				if (_testFlag)
					return;
				sendMessage("Device is started");
				toggleConnectStatus();
			}

			@Override
			public void onStopped(SwipeEvent event) {
				if (_testFlag)
					return;
				sendMessage("Device is stopped");
				toggleConnectStatus();
			}
		});

		Button btn = (Button) findViewById(R.id.btnback);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finishAll();
			}
		});
	}

	private void checkDevice() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (!_handler.isConnected()) {
					toggleConnectStatus();
					return;
				}
				if (_handler.isPowerOn()) {
					toggleConnectStatus();
					return;
				}
				if (_handler.isReadable()) {
					sendMessage("Device is ready");
					_handler.powerOn();
				} else {
					_testFlag = false;
					sendMessage("Please wait! testing parameter now");
					if (_handler.test() && _handler.isReadable()) {
						_testFlag = false;
						sendMessage("Device is ready");
						_handler.powerOn();
					} else {
						_testFlag = false;
						sendMessage("Device is not supported or Please close some audio effects(SRS/DOLBY/BEATS/JAZZ/Classic...) and insert device!");
					}
				}
				toggleConnectStatus();
			}
		}).start();
	}

	private void toggleConnectStatus() {
		_ui.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (_handler.isConnected() && _handler.isPowerOn()
						&& _handler.isReadable()) {
					ImageView iv = (ImageView) findViewById(R.id.connect);
					iv.setVisibility(View.VISIBLE);
					iv = (ImageView) findViewById(R.id.disconnect);
					iv.setVisibility(View.INVISIBLE);
				} else {
					ImageView iv = (ImageView) findViewById(R.id.connect);
					iv.setVisibility(View.INVISIBLE);
					iv = (ImageView) findViewById(R.id.disconnect);
					iv.setVisibility(View.VISIBLE);
				}
			}
		}, 500);
	}

	private void sendMessage(String msg) {
		_msg.sendMessage(msg);
	}

	public void onStart() {
		super.onStart();
		checkDevice();
	}

	public void onStop() {
		_handler.powerOff();
		super.onStop();
	}

	public void onDestroy() {
		_handler.onDestroy();
		super.onDestroy();
	}
}