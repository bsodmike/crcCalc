package com.crcmike;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.InputStream;
import java.io.File;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.util.Log;
import android.view.Gravity;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.content.Context;
import android.view.View;


public class crcMike extends Activity {
    private static final char[] HEX_CHARS = {'0', '1', '2', '3',
        									 '4', '5', '6', '7',
        									 '8', '9', 'a', 'b',
        									 'c', 'd', 'e', 'f',};	
	private static final String TAG = "crcMike";	
	Button buttonCalc;
	RadioButton radioC, radioF;
	EditText editText;

	// Create an anonymous implementation of OnClickListener
	private OnClickListener mCalcListener = new OnClickListener(){
		public void onClick(View v){			
			long id = 0;
		    byte[] buf = new byte[65536];
		    int num_read;

		    // the external storage is assumed as connected here!
			String FILENAME = "test.log"; //hard coded filename!			
			File path = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DOWNLOADS);
			File file = new File(path,FILENAME);

			// do something when the button is clicked
			try{
				if (editText.getText().length() == 0) {
					Context context = getApplicationContext();
					CharSequence text = "Please enter a valid number";
					int duration = Toast.LENGTH_LONG;
					Toast.makeText(context, text, duration).show();
					return;
				}
				
				float inputValue = Float.parseFloat(editText.getText().toString());
				if (radioC.isChecked()) {
					editText.setText(String
							.valueOf(fToC(inputValue)));
				} else {
					editText.setText(String
							.valueOf(cToF(inputValue)));
				}		
			
				if (radioF.isChecked()) {
					radioF.setChecked(false);
					radioC.setChecked(true);
				} else {
					radioF.setChecked(true);
					radioC.setChecked(false);
				}				

				/*
				 *  testing the MD5 hash computation.
				 */
				java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
				InputStream in = new FileInputStream(file);
				//InputStream in = new StringBufferInputStream("dasdadad");			
				while ((num_read = in.read(buf)) != -1) {
					  digest.update(buf, 0, num_read);
				}
				Context context = getApplicationContext();
				Toast.makeText(context, asHex(digest.digest()), Toast.LENGTH_LONG).show();
				in.close();
				
			}catch (IOException e){
				Context context = getApplicationContext();
				CharSequence text = "Error reading from " + file;
				int duration = Toast.LENGTH_LONG;
				Toast.makeText(context, text, duration).show();
		        Log.d(TAG, "IOException");				
			}catch (Exception e){
				Context context = getApplicationContext();
				CharSequence text = e.toString() + "ID = " + id;
				int duration = Toast.LENGTH_LONG;
				Toast.makeText(context, text, duration).show();
		        Log.d(TAG, "Exception");				
			}
		}
	};  
	
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
        buttonCalc.setOnClickListener(mCalcListener);        
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