package android.randy;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class keyboardPreferences extends PreferenceActivity {
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        addPreferencesFromResource(R.xml.preferences);
    }


    
}
