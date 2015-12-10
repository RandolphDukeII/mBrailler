package android.randy;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.view.GestureDetector;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Vibrator;

import at.abraxas.amarino.Amarino;
import at.abraxas.amarino.AmarinoIntent;

import static android.view.KeyEvent.*;


@SuppressLint("DefaultLocale")
public class brailleKeyboard extends InputMethodService implements
		GestureDetector.OnGestureListener,
		GestureDetector.OnDoubleTapListener, OnInitListener {

	// inputView of the keyboard
	public View mInputView;
	private int mWidth = 0;
	private int mHeight = 0;
	public FrameLayout OverlayView;

	// connection to the Arduino
	private static final String TAG = "mBrailler";
	private static final String DEVICE_ADDRESS= "00:06:66:4E:9B:F0";
	private BluetoothReceiver mBTReceiver = new BluetoothReceiver();

	//global text case flag for upper case command
	boolean isUpperCase=false;

	//global number toggle flag for the number pad
	boolean isNumberToggle=false;

	//global leftside flag
	boolean isLeft=false;

	//global rightside flag
	boolean isRight=false;

	//close keyboard flag
	boolean closeKeyboard=false;

	//global scroll up flag
	boolean goingUp=false;

	//global scroll down flag
	boolean goingDown=false;

	//global action flag
	boolean scrollingDone=false;

	//full scrolling action and toggle finger released
	boolean isDone=false;

	//Second finger status
	boolean secondFinger=false;

	//menu option
	int leftFinger = 0;
	int rightFinger = 0;

	//Gesture Testing
	private static final String DEBUG_TAG = "Gestures";
	private GestureDetectorCompat mDetector;

	private VelocityTracker mVelocityTracker = null;

	//Vibration Settings
	private Vibrator shake = null;

	//Text Clipboard
	private ClipboardManager clippedText;

	//Edit Text
	private EditText textContents;

	// tts vars
	static public TextToSpeech mTTS = null;
	private boolean mttsloaded = false;

	private Map <Integer, String> byteToKeyboardCharacter = new HashMap<Integer, String>();
	private Map <Integer, String> byteToKeyboardNumber = new HashMap<Integer, String>();


	public brailleKeyboard() {
		//letters for US English
		byteToKeyboardCharacter.put(32, "A");
		byteToKeyboardCharacter.put(48, "B");
		byteToKeyboardCharacter.put(36, "C");
		byteToKeyboardCharacter.put(38, "D");
		byteToKeyboardCharacter.put(34, "E");
		byteToKeyboardCharacter.put(52, "F");
		byteToKeyboardCharacter.put(54, "G");
		byteToKeyboardCharacter.put(50, "H");
		byteToKeyboardCharacter.put(20, "I");
		byteToKeyboardCharacter.put(22, "J");
		byteToKeyboardCharacter.put(40, "K");
		byteToKeyboardCharacter.put(56, "L");
		byteToKeyboardCharacter.put(44, "M");
		byteToKeyboardCharacter.put(46, "N");
		byteToKeyboardCharacter.put(42, "O");
		byteToKeyboardCharacter.put(60, "P");
		byteToKeyboardCharacter.put(62, "Q");
		byteToKeyboardCharacter.put(58, "R");
		byteToKeyboardCharacter.put(28, "S");
		byteToKeyboardCharacter.put(30, "T");
		byteToKeyboardCharacter.put(41, "U");
		byteToKeyboardCharacter.put(57, "V");
		byteToKeyboardCharacter.put(23, "W");
		byteToKeyboardCharacter.put(45, "X");
		byteToKeyboardCharacter.put(47, "Y");
		byteToKeyboardCharacter.put(43, "Z");

		//space button
		byteToKeyboardCharacter.put(64, " ");

		//symbols
		byteToKeyboardCharacter.put(16, ",");
		byteToKeyboardCharacter.put(24, ";");
		byteToKeyboardCharacter.put(18, ":");
		byteToKeyboardCharacter.put(19, ".");
		byteToKeyboardCharacter.put(26, "!");
		byteToKeyboardCharacter.put(25, "?");
		byteToKeyboardCharacter.put(25, "\"");
		byteToKeyboardCharacter.put(11, "\"");
		byteToKeyboardCharacter.put(8, "'");
		byteToKeyboardCharacter.put(9, "-");

		//delete button KEYCODE_DELETE
		byteToKeyboardCharacter.put(128, "Delete");

		//shift button KEYCODE_SHIFT_RIGHT
		byteToKeyboardCharacter.put(63, "Close");

		//shift button KEYCODE_SHIFT_RIGHT
		byteToKeyboardCharacter.put(1, "Shift");

		//number toggle KEYCODE_NUM
		byteToKeyboardCharacter.put(15, "Number Toggle");
		byteToKeyboardNumber.put(15, "Number Toggle");

		//numbers
		byteToKeyboardNumber.put(22, "0");
		byteToKeyboardNumber.put(32, "1");
		byteToKeyboardNumber.put(48, "2");
		byteToKeyboardNumber.put(36, "3");
		byteToKeyboardNumber.put(38, "4");
		byteToKeyboardNumber.put(34, "5");
		byteToKeyboardNumber.put(52, "6");
		byteToKeyboardNumber.put(54, "7");
		byteToKeyboardNumber.put(50, "8");
		byteToKeyboardNumber.put(20, "9");
	}

	public boolean onRightEvent(MotionEvent event) {

		//where I will be implementing the switches based on fingers down

		int index = event.getActionIndex();
		int action = event.getActionMasked();
		int pointerId = event.getPointerId(index);
		int allPointers = event.getPointerCount();

		//put the getX and getY local variables
		float locationOfX = event.getX();
		float locationOfY = event.getY();

		float YAxis = (mWidth/2);

		float XAxis = (mHeight/2);


		//still working out how to get the Action_Up and Action_Pointer_Up to help in recoginizing the pointer locations
		switch (action  & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				Log.d("", "This is an onRightEvent Action_Down.");
				break;

			case MotionEvent.ACTION_POINTER_DOWN:
				Log.d("", "This is an onRightEvent Action_Pointer_Down.");
				secondFinger = true;
				break;

			case MotionEvent.ACTION_MOVE:
				Log.d("", "This is an onRightEvent Action_Move.");

				break;

			case MotionEvent.ACTION_UP:
				Log.d("", "This is an onRightEvent Action_Up.");
				break;

			case MotionEvent.ACTION_POINTER_UP:
				Log.d("", "This is an onRightEvent Action_Pointer_Up.");
				secondFinger = false;
				break;

			case MotionEvent.ACTION_CANCEL:
				// Return a VelocityTracker object back to be re-used by others.
				mVelocityTracker.recycle();
				break;
		}
		return true;
	}

	public boolean onLeftEvent(MotionEvent event) {

		//where I will be implementing the switches based on fingers down

		int index = event.getActionIndex();
		int action = event.getActionMasked();
		int pointerId = event.getPointerId(index);
		int allPointers = event.getPointerCount();

		//put the getX and getY local variables
		float locationOfX = event.getX();
		float locationOfY = event.getY();

		float YAxis = (mWidth/2);

		float XAxis = (mHeight/2);


		//still working out how to get the Action_Up and Action_Pointer_Up to help in recoginizing the pointer locations
		switch (action  & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				Log.d("", "This is an onLeftEvent Action_Down.");
				break;

			case MotionEvent.ACTION_POINTER_DOWN:
				Log.d("", "This is an onLeft Action_Pointer_Down.");
				secondFinger = true;
				break;

			case MotionEvent.ACTION_MOVE:
				Log.d("", "This is an onLeftEvent Action_Move.");

				break;

			case MotionEvent.ACTION_UP:
				Log.d("", "This is an onLeftEvent Action_Up.");
				break;

			case MotionEvent.ACTION_POINTER_UP:
				Log.d("", "This is an onLeftEvent Action_Pointer_Up.");
				secondFinger = false;
				break;

			case MotionEvent.ACTION_CANCEL:
				// Return a VelocityTracker object back to be re-used by others.
				mVelocityTracker.recycle();
				break;
		}
		return true;
	}

	@Override
	public boolean onDown(MotionEvent event) {
		//put the getX and getY local variables
		float locationOfX = event.getX();

		float YAxis = (mWidth/2);

		if (locationOfX > YAxis){
			isRight = true;
		}

		if (locationOfX < YAxis){
			isLeft = true;
		}

		if(isLeft){
			getCurrentInputConnection().setComposingText("", -1);
			Log.d(DEBUG_TAG, "Left onDown < Back Cursor");
			onLeftEvent(event);
			return true;
		}
		else if(isRight){
			getCurrentInputConnection().commitText("", 2);
			Log.d(DEBUG_TAG, "Right onDown > Forward Cursor");
			onRightEvent(event);
			return true;
		}
		return true;
	}


	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2,
						   float velocityX, float velocityY) {
		return true;
	}

	@Override
	public void onLongPress(MotionEvent event) {
		//put the getX and getY local variables
		float locationOfX = event.getX();
		float locationOfY = event.getY();

		float YAxis = (mWidth/2);
		float XAxis = (mHeight/2);

		if(locationOfX < YAxis){
			isLeft = true;
			Log.d(DEBUG_TAG, "Left onLongPress occurred.");
			mInputView.setContentDescription("Left thumb held.");
			mTTS.speak("Scroll with your right thumb for granularity.", TextToSpeech.QUEUE_ADD, null);
			//scrollingDone = true;
		}

		else {
			isRight = true;
			Log.d(DEBUG_TAG, "Right onLongPress occurred.");
			mInputView.setContentDescription("Right thumb held.");

			mTTS.speak("Scroll with your left thumb for editing.", TextToSpeech.QUEUE_ADD, null);
			//scrollingDone = true;
		}
		shake.vibrate(40);
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
							float distanceY) {

		int index = e2.getActionIndex();
		float locationOfXFinish = e2.getX(index);
		int allPointers = e2.getPointerCount();

		float YAxis = (mWidth/2);

		//resets the scrollable side
		if (locationOfXFinish > YAxis){
			isRight = true;
		}

		if (locationOfXFinish < YAxis){
			isLeft = true;
		}

		//determines the direction of the scrolling
		if (distanceY < 0)
		{
			goingDown = true;
		}

		if (distanceY > 0)
		{
			goingUp = true;
		}

		//scrolling only recorded by the ACTION_POINTER movements
		//should include activating the Granularity and Editing when the scrolling finger is completed

		if (allPointers < 2) {
			return false;
		}
		//&& locationOfXStart > YAxis && locationOfXFinish > YAxis

		//Right finger held, left finger swipe
		else if (mDetector.isLongpressEnabled() && isRight) {
							/*
							Log.d(DEBUG_TAG, "Pointer Count: " + allPointers);
							Log.d(DEBUG_TAG, "Scroll away since your right finger is down!");
							Log.d(DEBUG_TAG, "Distance X: "+distanceX+" Distance Y: "+distanceY);
							*/
			onLeftEvent(e2);
			isRight =! isRight;
			scrollingDone = true;

			if (goingDown){
				isLeft = false;
				leftFinger = leftFinger-1;
				//Log.d(DEBUG_TAG, "Left going down "+leftFinger);
				goingDown =! goingDown;
			}

			else if (goingUp){
				isLeft = false;
				leftFinger = leftFinger+1;
				//Log.d(DEBUG_TAG, "Left going up "+leftFinger);
				goingUp =! goingUp;
			}

			if (scrollingDone) {
				textEditingMenu(leftFinger, "");
			}
		}


		//Left finger held, right finger swipe
		else if (mDetector.isLongpressEnabled() && isLeft)
		{

						/*
						Log.d(DEBUG_TAG, "Scroll away since your left finger is down!");
						Log.d(DEBUG_TAG, "Pointer Count: "+allPointers);
						Log.d(DEBUG_TAG, "Distance X: "+distanceX+" Distance Y: "+distanceY);
						*/
			onRightEvent(e2);
			isRight =! isRight;
			scrollingDone = true;

			if (goingDown){
				isRight = false;
				rightFinger = rightFinger-1;
				//Log.d(DEBUG_TAG, "Right going down " + rightFinger);
				goingDown =! goingDown;
			}

			else if (goingUp){
				isRight = false;
				rightFinger = rightFinger+1;
				//Log.d(DEBUG_TAG, "Right going up "+rightFinger);
				goingUp =! goingUp;
			}
		}
		if (scrollingDone) {
			granularityMenu(rightFinger, isDone);
			return true;
		}
		return false;
	}

	@Override
	public void onShowPress(MotionEvent event) {
		Log.d(DEBUG_TAG, "onShowPress occurred.");
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		Log.d(DEBUG_TAG, "onSingleTapUp happened.");
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent event) {
		Log.d(DEBUG_TAG, "onDoubleTap pressed.");
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent event) {
		//put the getX and getY local variables
		float locationOfX = event.getX();
		float locationOfY = event.getY();

		float YAxis = (mWidth/2);
		float XAxis = (mHeight/2);

		if(locationOfX < YAxis){
			isLeft =! isLeft;

			//the contextual menu action for startSelectingText
			//getCurrentInputConnection().performContextMenuAction(16908328);
			//getCurrentInputConnection().setComposingText();
			mTTS.speak("Left onTap", TextToSpeech.QUEUE_ADD, null);
			Log.d(DEBUG_TAG, "Left onDoubleTapEvent occurred.");

		}

		else{
			isRight =! isRight;
			Log.d(DEBUG_TAG, "Right onDoubleTapEvent occurred.");
		}
		shake.vibrate(40);

		return true;
	}


	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {//put the getX and getY local variables
		float locationOfX = event.getX();
		float locationOfY = event.getY();

		float YAxis = (mWidth/2);
		float XAxis = (mHeight / 2);

		if (locationOfX < YAxis) {
			isLeft =! isLeft;
			Log.d(DEBUG_TAG, "Left onSingleTapConfirmed occurred.");

		} else{
			isRight =! isRight;
			Log.d(DEBUG_TAG, "Right onSingleTapConfirmed occurred.");

		}

		return true;
	}

	/*********
	 * TEXT TO SPEECH
	 *********/

	/**
	 * OnInitLister implementation for TTS
	 */

	public void onInit(int status) {
		if(mttsloaded || mTTS == null) return;
		mttsloaded = true;

		if(status == TextToSpeech.SUCCESS)
		{
			mTTS.setLanguage(Locale.getDefault());
		}
		else //ERROR
		{
			//mTTs.playEarcon("error", TextToSpeech.QUEUE_FLUSH, null);
			//Toast.makeText(this, "Error: TTS not avaliable. Check your device settings.", Toast.LENGTH_LONG).show();
		}
	}

	private void ttsSpeak(String message, int queuemode)
	{
		if(mTTS != null)
		{
			mTTS.speak(message, queuemode, null);
		}
	}

	private void ttsStop()
	{
		if(mTTS != null && mTTS.isSpeaking())
		{
			mTTS.stop();
		}
	}

	@Override
	public View onCreateInputView()
	{
		if(!mttsloaded)
		{
			mTTS = new TextToSpeech(this, this); //wait for TTS init
		}

		// inflate the Overlay and sets the touch listener
		mInputView = (View) getLayoutInflater().inflate(R.layout.keyboardui, null);
		updateWindowSize();


		//Believe my touch problems are here
		mDetector = new GestureDetectorCompat(this, this);
		// Set the gesture detector as the double tap
		// listener.
		mDetector.setOnDoubleTapListener(this);


		// set fullscreen overlay
		OverlayView = (FrameLayout)mInputView.findViewById(R.id.overlay);
		OverlayView.setLayoutParams(new FrameLayout.LayoutParams(mWidth, mHeight));

		return mInputView;
	}

	private void updateWindowSize()
	{
		// get window size
		WindowManager wm = (WindowManager) this.getSystemService(brailleKeyboard.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		mWidth=display.getWidth();
		mHeight=display.getHeight();
	}


	@Override
	public void onStartInputView(final EditorInfo info, boolean restarting)
	//sets the width and height of the device
	{
		super.onStartInputView(info, restarting);
		// set fullscreen overlay
		OverlayView.setLayoutParams(new FrameLayout.LayoutParams(mWidth, mHeight));
		mTTS.speak("mBrailler keyboard ready", TextToSpeech.QUEUE_ADD, null);
		mInputView.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View view, MotionEvent motionEvent) {
				int action = motionEvent.getAction();

				mDetector.onTouchEvent(motionEvent);
				if (action == MotionEvent.ACTION_UP) {
					Log.d(DEBUG_TAG, "Finger Raised");
					isDone = true;
				}

				else if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
					isDone = false;
				}
				return mDetector.onTouchEvent(motionEvent);
			}
		});

	}



	@Override
	public void onCreate() {
		super.onCreate();

		// register broadcast receiver
		registerReceiver(mBTReceiver, new IntentFilter(AmarinoIntent.ACTION_RECEIVED));
		registerReceiver(mBTReceiver, new IntentFilter(AmarinoIntent.ACTION_CONNECTED));

		// connects to BT module
		Amarino.connect(getApplicationContext(), DEVICE_ADDRESS);
		shake = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		// clipboard manager
		clippedText = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);

		// text box contents
		//textContents = (EditText)getSystemService(Context.INPUT_METHOD_SERVICE);

	}

	private class BluetoothReceiver extends BroadcastReceiver
	{
		//This is the original code to receive Strings from the pressed bytes of the Arduino

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if(action.equalsIgnoreCase(AmarinoIntent.ACTION_CONNECTED))
			{
				Log.v(TAG, "connected");
			}

			else if(action.equalsIgnoreCase(AmarinoIntent.ACTION_RECEIVED))
			{
				final int dataType = intent.getIntExtra(AmarinoIntent.EXTRA_DATA_TYPE, -1);

				if(dataType == AmarinoIntent.STRING_EXTRA)
				{
					String data = intent.getStringExtra(AmarinoIntent.EXTRA_DATA);

					if(data != null)
					{
						// TODO whatever you want
						Log.v(TAG, "data received: " + data);
						sendKeyChar(Integer.valueOf(data));

					}
				}
			}
		}
	}

	public String textEditingMenu(int data, String doAction) {

		//cut, copy, paste, cancel (which will loop through the options until the scrolling finger released)
		//will pass the appropriate function on to be carried out in performTextEditing

		int function=Math.abs(data);
		doAction = "";

		if (data > 0 && data <= 40)
		{
			if (function % 40 == 1) {
				doAction="Cancel";
				Log.d(DEBUG_TAG, "cancel");
			}
			else if (function % 30 == 1) {
				doAction="Paste";
				Log.d(DEBUG_TAG, "Paste");
			}
			else if (function % 20 == 1) {
				doAction="Copy";
				Log.d(DEBUG_TAG, "Copy");
			}
			else if (function % 10 == 1) {
				doAction="Cut";
				Log.d(DEBUG_TAG, "Cut");
			}
		}

		else if (data <=0 && data >= -40)
		{
			function= 40-function;

			if (function % 40 == 1) {
				doAction="Cancel";
				Log.d(DEBUG_TAG, "Cancel");
			}
			else if (function % 30 == 1) {
				doAction="Paste";
				Log.d(DEBUG_TAG, "Paste");
			}
			else if (function % 20 == 1) {
				doAction="Copy";
				Log.d(DEBUG_TAG, "Copy");
			}
			else if (function % 10 == 1) {
				doAction="Cut";
				Log.d(DEBUG_TAG, "Cut");
			}
		}

		else if (data > 40)
		{
			function = function-40;

			if (function % 40 == 1) {
				doAction="Cancel";
				Log.d(DEBUG_TAG, "cancel");
			}
			else if (function % 30 == 1) {
				doAction="Paste";
				Log.d(DEBUG_TAG, "Paste");
			}
			else if (function % 20 == 1) {
				doAction="Copy";
				Log.d(DEBUG_TAG, "Copy");
			}
			else if (function % 10 == 1) {
				doAction="Cut";
				Log.d(DEBUG_TAG, "Cut");
			}
		}

		else if (data < -40)
		{
			function = Math.abs(function)-40;

			if (function % 40 == 1) {
				doAction="Cancel";
				Log.d(DEBUG_TAG, "cancel");
			}
			else if (function % 30 == 1) {
				doAction="Paste";
				Log.d(DEBUG_TAG, "Paste");
			}
			else if (function % 20 == 1) {
				doAction="Copy";
				Log.d(DEBUG_TAG, "Copy");
			}
			else if (function % 10 == 1) {
				doAction="Cut";
				Log.d(DEBUG_TAG, "Cut");
			}
		}
		return doAction;
	}

	public void performTextEditing(String function,boolean isDone)
	{
		int start=0;
		int finish=0;
		String doAction=textEditingMenu(0,function);

		if (isDone) {
		switch (doAction) {
			case "Cut":
				Log.d(DEBUG_TAG, "Cut Done");
				cutText(start, finish);
				break;

			case "Copy":
				Log.d(DEBUG_TAG, "Copy Done");
				copyText(start, finish);
				break;

			case "Paste":
				Log.d(DEBUG_TAG, "Paste Done");
				pasteText(start, finish);
				break;

			case "Cancel":
				break;

			default:
				break;
		}
	}

	}

	public void cutText(int start, int finish){
		shake.vibrate(40);
		mTTS.speak("Cut", TextToSpeech.QUEUE_ADD, null);
		Log.d(DEBUG_TAG, "Cut Text!!!!!!!!!!!!");
	}

	public void copyText(int start, int finish) {
		shake.vibrate(40);
		ClipData clip = ClipData.newPlainText("simple text", "Hello, World!");
		clippedText.setPrimaryClip(clip);

		Log.d(DEBUG_TAG, String.valueOf(clip));
		mTTS.speak("Copy", TextToSpeech.QUEUE_ADD, null);
		Log.d(DEBUG_TAG, "Copy Text!!!!!!!!!!!");
	}

	public void pasteText(int start, int finish) {
		shake.vibrate(40);
		// Checks to see if the clip item contains an Intent, by testing to see if getIntent() returns null
		Intent pasteIntent = clippedText.getPrimaryClip().getItemAt(0).getIntent();

		if (pasteIntent != null) {


			mTTS.speak("Paste", TextToSpeech.QUEUE_ADD, null);
			// handle the Intent
			Log.d(DEBUG_TAG, "Pasted");

		} else {

			mTTS.speak("Nothing to paste", TextToSpeech.QUEUE_ADD, null);
			// ignore the clipboard, or issue an error if your application was expecting an Intent to be
			// on the clipboard
			Log.d(DEBUG_TAG, "not pasted");
		}
	}

	public int granularityMenu(int data, boolean isDone){

		//letter, word, sentence select granularity (which will loop through the options until the scrolling finger is released)

		int function=Math.abs(data);
		String doAction="";

		if (data > 0 && data <= 40)
		{
			if (function % 40 == 1) {
				doAction="Cancel";
				Log.d(DEBUG_TAG, "cancel");
			}
			else if (function % 30 == 1) {
				doAction="Sentence";
				Log.d(DEBUG_TAG, "sentence");
			}
			else if (function % 20 == 1) {
				doAction="Word";
				Log.d(DEBUG_TAG, "word");
			}
			else if (function % 10 == 1) {
				doAction="Letter";
				Log.d(DEBUG_TAG, "letter");
			}
		}

		else if (data <=0 && data >= -40)
		{
			function= 40-function;

			if (function % 40 == 1 || function == 0) {
				doAction="Cancel";
				Log.d(DEBUG_TAG, "cancel");
			}
			else if (function % 30 == 1) {
				doAction="Sentence";
				Log.d(DEBUG_TAG, "sentence");
			}
			else if (function % 20 == 1) {
				doAction="Word";
				Log.d(DEBUG_TAG, "word");
			}
			else if (function % 10 == 1) {
				doAction="Letter";
				Log.d(DEBUG_TAG, "letter");
			}
		}

		else if (data > 40)
		{
			function = function-40;

			if (function % 40 == 1 || function == 0) {
				doAction="Cancel";
				Log.d(DEBUG_TAG, "cancel");
			}
			else if (function % 30 == 1) {
				doAction="Sentence";
				Log.d(DEBUG_TAG, "sentence");
			}
			else if (function % 20 == 1) {
				doAction="Word";
				Log.d(DEBUG_TAG, "word");
			}
			else if (function % 10 == 1) {
				doAction="Letter";
				Log.d(DEBUG_TAG, "letter");
			}
		}

		else if (data < -40)
		{
			function = Math.abs(function)-40;

			if (function % 40 == 1 || function == 0) {
				doAction="Cancel";
				Log.d(DEBUG_TAG, "cancel");
			}
			else if (function % 30 == 1) {
				doAction="Sentence";
				Log.d(DEBUG_TAG, "sentence");
			}
			else if (function % 20 == 1) {
				doAction="Word";
				Log.d(DEBUG_TAG, "word");
			}
			else if (function % 10 == 1) {
				doAction="Letter";
				Log.d(DEBUG_TAG, "letter");
			}
		}

		if (isDone) {
			switch (doAction) {
				case "Letter":
					letterGranularity();
					Log.d(DEBUG_TAG, "Letter Done");
					break;

				case "Word":
					wordGranularity();
					Log.d(DEBUG_TAG, "Word Done");
					break;

				case "Sentence":
					sentenceGranularity();
					Log.d(DEBUG_TAG, "Sentence Done");
					break;

				case "Cancel":
					break;

				default:
					break;
			}
		}
			return function;

	}

	public void letterGranularity(){
		shake.vibrate(40);
		//mTTS.speak("By Letter", TextToSpeech.QUEUE_ADD, null);
		//Log.d(DEBUG_TAG, "By Letter!!!!!!!!!!!");
	}

	public void wordGranularity(){
		shake.vibrate(40);
		//mTTS.speak("By word", TextToSpeech.QUEUE_ADD, null);
		//Log.d(DEBUG_TAG, "By Word!!!!!!!!!!!!!");
	}

	public void sentenceGranularity(){
		shake.vibrate(40);
		//mTTS.speak("By sentence", TextToSpeech.QUEUE_ADD, null);
		//Log.d(DEBUG_TAG, "By sentence!!!!!!!!!!");
	}

	@SuppressLint("DefaultLocale")
	public void sendKeyChar(int data)
	{
		//this should be where we reference the tables before committing that to the string
		String c = byteToKeyboardCharacter.containsKey(data) ? byteToKeyboardCharacter.get(data) : "?";
		String n = byteToKeyboardNumber.containsKey(data) ? byteToKeyboardNumber.get(data) : "?";

		if(c.equals("Delete"))
		{
			getCurrentInputConnection().deleteSurroundingText(1, 0);
		}
		else if(c.equals("Shift"))
		{
			isUpperCase =! isUpperCase;
		}
		else if(c.equals("Number Toggle"))
		{
			isNumberToggle =! isNumberToggle;
		}
		else if(c.equals("Close")){
			closeKeyboard =! closeKeyboard;
		}
		//prints the key value pair based on the letter HashMap
		else if (isUpperCase&&!isNumberToggle)
		{
			getCurrentInputConnection().commitText(c.toUpperCase(), 1);
			isUpperCase =! isUpperCase;
		}
		else if (!isUpperCase&&!isNumberToggle)
		{
			getCurrentInputConnection().commitText(c.toLowerCase(), 1);
		}

		else if (isNumberToggle)
		{
			getCurrentInputConnection().commitText(n, 1);
		}

		else if(closeKeyboard)
		{
			getCurrentInputConnection().performContextMenuAction(KEYCODE_BACK);
		}
	}

	@Override
	public void onDestroy(){
		// disconnects to BT module
		Amarino.disconnect(getApplicationContext(), DEVICE_ADDRESS);

		// un-registers broadcast receiver
		unregisterReceiver(mBTReceiver);

		super.onDestroy();


	}
}
