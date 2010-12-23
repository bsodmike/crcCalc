/* Author: 	Michael de Silva
 * Date:	23nd December 2010
 * Email:	michael@mwdesilva.com
 * Blog:	bsodmike.com
 *
 * This started out as a self-learning test on creating a simple
 * temperature conversion calculator.  
 * 
 * I ended up incorporating a
 * MD5 hash computation of a file stored on the SD card, and this
 * checksum is presented as a Toast alert.
 * 
 * The hashing part of the code has now been moved into a child
 * thread so as not to disrupt the main (UI) thread.
 * http://codinghard.wordpress.com/2009/05/16/android-thread-messaging/
 */

package com.crcmike;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class crcMike extends Activity implements OnClickListener {
    private static final char[] HEX_CHARS = {'0', '1', '2', '3',
        									 '4', '5', '6', '7',
        									 '8', '9', 'a', 'b',
        									 'c', 'd', 'e', 'f',};	
    private Handler mMainHandler, mChildHandler;
    private static final String TAG = "crcMike";	
	Button buttonCalc;
	RadioButton radioC, radioF;
	EditText editText;

    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Log.d(TAG, "starting...");
        
        // find views by id
		editText = (EditText)findViewById(R.id.editText);
        radioC = (RadioButton)findViewById(R.id.radioC);
        radioF = (RadioButton)findViewById(R.id.radioF);
        buttonCalc = (Button)findViewById(R.id.buttonCalc);
        
        // add listener
        buttonCalc.setOnClickListener(this);    

        editText.setText("32");
        
        /*
         * Create the main handler on the main thread so it is bound to the main
         * thread's message queue.
         */
        mMainHandler = new Handler() {

            public void handleMessage(Message msg) {

                Log.i(TAG, "Got an incoming message from the child thread - "  + (String)msg.obj);

                /*
                 * Handle the message coming from the child thread.
                 */
                //mTextView.setText(mTextView.getText() + (String)msg.obj + "\n");
                Toast.makeText(getApplicationContext(), (String)msg.obj + "\n", Toast.LENGTH_LONG).show();
            }
        };

        /*
         * Start the child thread.
         */
        new ChildThread().start();

        Log.i(TAG, "Main handler is bound to - " + mMainHandler.getLooper().getThread().getName());
        
    }
	
	public void onClick(View v){			

	    switch(v.getId()){
	    
	    case R.id.buttonCalc:
	
	        /*
	         * We cannot guarantee that the mChildHandler is created
	         * in the child thread by the time the user clicks the button.
	         */
	        if (mChildHandler != null) {

	            /*
	             * Send a message to the child thread.
	             */
	            Message msg = mChildHandler.obtainMessage();
	            msg.obj = mMainHandler.getLooper().getThread().getName() + " says Hello";
	            mChildHandler.sendMessage(msg);
	            Log.i(TAG, "Send a message to the child thread - " + (String)msg.obj);
	        }	

	
			// do something when the button is clicked

//			if (editText.getText().length() == 0) {
//				Toast.makeText(getApplicationContext(), "Please enter a valid temperature!", Toast.LENGTH_LONG).show();
//				return;
//			}
			
			float inputValue = Float.parseFloat(editText.getText().toString());
			if (radioC.isChecked()) {
				editText.setText(String
						.valueOf(fToC(inputValue)));
			} else {
				editText.setText(String
						.valueOf(cToF(inputValue)));
			}		
			
			// switch the selected buttons
			if (radioF.isChecked()) {
				radioF.setChecked(false);
				radioC.setChecked(true);
			} else {
				radioF.setChecked(true);
				radioC.setChecked(false);
			}				

			// need to add List interface?
			
		default:		
			break;
	    }
	}

    @Override
    protected void onDestroy() {

        Log.i(TAG, "Stop looping the child thread's message queue");

        /*
         * Remember to stop the looper
         */
        mChildHandler.getLooper().quit();

        super.onDestroy();
    }

    class ChildThread extends Thread {

        private static final String INNER_TAG = "ChildThread";

        public void run() {

            this.setName("child");

            /*
             * You have to prepare the looper before creating the handler.
             */
            Looper.prepare();

            /*
             * Create the child handler on the child thread so it is bound to the
             * child thread's message queue.
             */
            mChildHandler = new Handler() {

                public void handleMessage(Message msg) {
            		long id = 0;
            	    byte[] buf = new byte[65536];
            	    int num_read;

					Message toMain = mMainHandler.obtainMessage();
					
        		    // the external storage is assumed as connected here!
        			String FILENAME = "test.log"; //hard coded filename!			
        			File path = Environment.getExternalStoragePublicDirectory(
        					Environment.DIRECTORY_DOWNLOADS);
        			File file = new File(path,FILENAME);
            	    
                    Log.i(INNER_TAG, "Got an incoming message from the main thread - " + (String)msg.obj);

                    try{
						/*
						 *  testing the MD5 hash computation.
						 */
						java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
						InputStream in = new FileInputStream(file);
						//InputStream in = new StringBufferInputStream("dasdadad");			
						while ((num_read = in.read(buf)) != -1) {
							  digest.update(buf, 0, num_read);
						}
						//Toast.makeText(getApplicationContext(), asHex(digest.digest()), Toast.LENGTH_LONG).show();
						toMain.obj = this.getLooper().getThread().getName() + ": " + asHex(digest.digest());
						in.close();
						
					}catch (IOException e){
						//Toast.makeText(getApplicationContext(), "Error reading from " + file, Toast.LENGTH_LONG).show();
						toMain.obj = this.getLooper().getThread().getName() + "Error reading from " + file.toString();
					
						Log.d(INNER_TAG, "IOException");				
					}catch (Exception e){
						//Toast.makeText(getApplicationContext(), e.toString() + " / ID = " + id, Toast.LENGTH_LONG).show();
						toMain.obj = this.getLooper().getThread().getName() + "Exception";
		    
						Log.d(INNER_TAG, "Exception");				
					}

					/*
					 * Send the processing result back to the main thread.
					 */
					//Message toMain = mMainHandler.obtainMessage();
					//toMain.obj = "This is " + this.getLooper().getThread().getName() +
					//    ".  Did you send me \"" + (String)msg.obj + "\"?";
					mMainHandler.sendMessage(toMain);
					Log.i(INNER_TAG, "Send a message to the main thread - " + (String)toMain.obj);
                }
            };

            Log.i(INNER_TAG, "Child handler is bound to - " + mChildHandler.getLooper().getThread().getName());

            /*
             * Start looping the message queue of this thread.
             */
            Looper.loop();
        }
    }

	// Converts to celcius
	private float fToC(float fahrenheit) {
		return ((fahrenheit - 32) * 5 / 9);
	}

	// Converts to fahrenheit
	private float cToF(float celsius) {
		return ((celsius * 9) / 5) + 32;
	}

    /*
     * Turns array of bytes into string representing each byte as
     * unsigned hex number.
     * 
     * @param hash Array of bytes to convert to hex-string
     * @return Generated hex string
     */
    public static String asHex (byte hash[]) {
        char buf[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }	
	
}