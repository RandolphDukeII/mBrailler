package android.randy;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.speech.tts.TextToSpeech;
import android.app.usage.UsageEvents.Event;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.text.Spannable;
import android.view.GestureDetector;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import at.abraxas.amarino.Amarino;
import at.abraxas.amarino.AmarinoIntent;



@SuppressLint("DefaultLocale")
public class brailleKeyboard extends InputMethodService implements
		GestureDetector.OnGestureListener,
		GestureDetector.OnDoubleTapListener
{
	
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

	//Gesture Testing
	private static final String DEBUG_TAG = "Gestures";
	private GestureDetectorCompat mDetector;

	//Finger position testing
	private static final String DEBUG = "Velocity";
	private VelocityTracker mVelocityTracker = null;


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

		public boolean onTouchEvent(MotionEvent event) {

			//where I will be implementing the switches based on fingers down

			int index = event.getActionIndex();
			int action = event.getActionMasked();
			int pointerId = event.getPointerId(index);

			//put the getX and getY local variables
			float locationOfX = event.getX();
			float locationOfY = event.getY();

			float centerOfScreen = (mWidth/2);

			float xAxis = (mHeight/2);


			mDetector = new GestureDetectorCompat(this, this);
			// Set the gesture detector as the double tap
			// listener.
			mDetector.setOnDoubleTapListener(this);



			switch(action) {
				case MotionEvent.ACTION_DOWN:
					if(mVelocityTracker == null) {
						// Retrieve a new VelocityTracker object to watch the velocity of a motion.
						mVelocityTracker = VelocityTracker.obtain();
					}
					else {
						// Reset the velocity tracker back to its initial state.
						mVelocityTracker.clear();
					}
					// Add a user's movement to the tracker.
					mVelocityTracker.addMovement(event);
					Log.d("", "Action Down Exists at x= "+event.getX()+" and y= "+event.getY());
					break;

				case MotionEvent.ACTION_POINTER_DOWN:
					mVelocityTracker.addMovement(event);

						if (centerOfScreen > locationOfX) {
							Log.d(DEBUG_TAG, "Left Side");
						}
						else {
							Log.d(DEBUG_TAG, "Right Side");
						}
					Log.d("", "There are "+event.getPointerCount()+" fingers down.");
					Log.d("", "Action_Pointer_Down Exists at x= "+event.getX()+"and y= "+event.getY());
					break;

				case MotionEvent.ACTION_MOVE:
					mVelocityTracker.addMovement(event);
					// When you want to determine the velocity, call
					// computeCurrentVelocity(). Then call getXVelocity()
					// and getYVelocity() to retrieve the velocity for each pointer ID.
					mVelocityTracker.computeCurrentVelocity(1000);
					// Log velocity of pixels per second
					// Best practice to use VelocityTrackerCompat where possible.
					Log.d("", "X velocity: " +
							VelocityTrackerCompat.getXVelocity(mVelocityTracker,
									pointerId));
					Log.d("", "Y velocity: " +
							VelocityTrackerCompat.getYVelocity(mVelocityTracker,
									pointerId));
					break;

				case MotionEvent.ACTION_UP:
					break;

				case MotionEvent.ACTION_POINTER_UP:
					break;

				case MotionEvent.ACTION_CANCEL:
					// Return a VelocityTracker object back to be re-used by others.
					mVelocityTracker.recycle();
					break;

				default :
					onDoubleTap(event);
					break;


			}
			return true;
		}

		public boolean gestureAction (MotionEvent startOfEvent, MotionEvent endOfEvent, float x1, float y1, float x2, float y2){
		// where I want to put the table of gestures to act upon


			//switch(){
				return true;
			//}
		}


	@Override
		public boolean onDown(MotionEvent event) {
		return true;
	}


		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,
		float velocityX, float velocityY) {
		return true;
	}

		@Override
		public void onLongPress(MotionEvent event) {
	}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
		float distanceY) {
		return true;
	}

		@Override
		public void onShowPress(MotionEvent event) {
		Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
	}

		@Override
		public boolean onSingleTapUp(MotionEvent event) {
		return true;
	}

		@Override
		public boolean onDoubleTap(MotionEvent event) {
				Log.d(DEBUG_TAG,"onDoubleTap: " + event.toString());
		return true;
	}

		@Override
		public boolean onDoubleTapEvent(MotionEvent event) {

			float locationOfX = event.getRawX();
			float centerOfScreen = (mWidth/2);

			if (centerOfScreen > locationOfX)
			{
				//double tap on the left causes cursor to move left

				//having difficulty identifying the proper method here using KEYCODE_SOFT_LEFT

				//textView.setImeOptions(EditorInfo.IME_ACTION_DONE);
				Log.d(DEBUG_TAG, "Left onDoubleTapEvent: " + event.toString());
			}
			else
			{

				//double tap on the right causes cursor to move right

				//having difficulty identifying the proper method here using KEYCODE_SOFT_RIGHT
				CharSequence textBeforeCursor;
				textBeforeCursor = getCurrentInputConnection().getTextBeforeCursor(-1,0);
				Log.d(DEBUG_TAG,"Right onDoubleTapEvent: " + event.toString());
			}

		return true;
	}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent event) {
			float locationOfX = event.getRawX();
			float centerOfScreen = (mWidth/2);

			if (centerOfScreen > locationOfX)
			{
				Log.d(DEBUG_TAG,"Left onSingleTapConfirmed: " + event.toString());
			}
			else
			{
				Log.d(DEBUG_TAG,"Right onSingleTapConfirmed: " + event.toString());
			}
		return true;
	}
		

	
	@Override
	public View onCreateInputView()
    {
        // inflate the Overlay and sets the touch listener
        mInputView = (View) getLayoutInflater().inflate(R.layout.keyboardui, null);

        updateWindowSize();


		mDetector = new GestureDetectorCompat(this,this);
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

	/**
     * Get pointer index
    private int getIndex(MotionEvent event)
   {

        int idx = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        return idx;
    }
	 */
    
    /**
     * This is called when the user starts editing a field.
     * We can use this to reset our state.
     */

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting)
    //sets the width and height of the device
    {
        super.onStartInputView(info, restarting);
		// set fullscreen overlay
        OverlayView.setLayoutParams(new FrameLayout.LayoutParams(mWidth, mHeight));

		mInputView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {

				//splits the screen by the y-axis
				float locationOfX = motionEvent.getX();
				float centerOfScreen = (mWidth / 2);

				//need to adjust the logic here to acknowledge multitouch events here

					onTouchEvent(motionEvent);

				return true;
			}
		});

    }


    @Override
	public void onCreate()
    {
    	super.onCreate();

		// register broadcast receiver
		registerReceiver(mBTReceiver, new IntentFilter(AmarinoIntent.ACTION_RECEIVED));
		registerReceiver(mBTReceiver, new IntentFilter(AmarinoIntent.ACTION_CONNECTED));
    	
    	// connects to BT module
		Amarino.connect(getApplicationContext(), DEVICE_ADDRESS);
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
				//need to adjust to byte data so that I can map byte data to characters
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

	public void touchAction ()
	{

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
