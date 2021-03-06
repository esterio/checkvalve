/*
 * Copyright 2010-2015 by David A. Parker <parker.david.a@gmail.com>
 * 
 * This file is part of CheckValve, an HLDS/SRCDS query app for Android.
 * 
 * CheckValve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 * 
 * CheckValve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the CheckValve source code.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.dparker.apps.checkvalve;

import java.io.File;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import com.dparker.apps.checkvalve.R;

public class ShowNoteActivity extends Activity {
    private static final String TAG = ShowNoteActivity.class.getSimpleName();
    
    private String filename;
    
    final OnClickListener dismissButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * Dismiss button was clicked
             */
            v.setBackgroundColor(ShowNoteActivity.this.getResources().getColor(R.color.steam_blue));
            finish();
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        this.setResult(0);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.shownote);
        this.findViewById(R.id.dismiss_button).setOnClickListener(dismissButtonListener);

        Intent thisIntent = this.getIntent();        
        filename = thisIntent.getStringExtra(Values.EXTRA_FILE_NAME);
        int noteId = thisIntent.getIntExtra(Values.EXTRA_NOTE_ID, 0);
        
        if( noteId == 0 )
            finish();
        
        ((TextView)findViewById(R.id.note_text)).setText(noteId);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    
    public void DoNotShowCheckboxHandler( View view ) {
        boolean checked = ((CheckBox)view).isChecked();
        
        try {
            File f = new File(ShowNoteActivity.this.getFilesDir(), filename);

            if( checked ) {
                if( f.createNewFile() )
                    Log.i(TAG, "Created file " + f.getCanonicalPath());
                else
                    Log.e(TAG, "Failed to create file " + f.getCanonicalPath());
            }
            else {
                if( f.delete() )
                    Log.i(TAG, "Deleted file " + f.getCanonicalPath());
                else
                    Log.e(TAG, "Failed to delete file " + f.getCanonicalPath());
            }
        }
        catch( Exception e ) {
            Log.w(TAG, "DoNotShowCheckboxHandler(): Caught an exception:");
            Log.w(TAG, e.toString());

            StackTraceElement[] ste = e.getStackTrace();

            for( StackTraceElement x : ste )
                Log.w(TAG, "    " + x.toString());
            
            finish();
        }
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        return;
    }
}