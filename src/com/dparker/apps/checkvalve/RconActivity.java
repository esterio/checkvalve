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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import com.dparker.apps.checkvalve.R;
import com.github.koraktor.steamcondenser.exceptions.RCONBanException;
import com.github.koraktor.steamcondenser.exceptions.RCONNoAuthException;
import com.github.koraktor.steamcondenser.servers.GoldSrcServer;
import com.github.koraktor.steamcondenser.servers.SourceServer;

public class RconActivity extends Activity {
    private final static String TAG = RconActivity.class.getSimpleName();
    private Animation fade_in;
    private Animation fade_out;

    private ProgressDialog p;
    private ServerQuery q;

    private TextView rcon_console;
    private TextView sending;
    private AutoCompleteTextView field_command;
    private Button send_button;

    private String password;
    private String command;
    private String[] unsafeCommands;
    private String server;
    private int port;
    private int timeout;
    private boolean rconIsAuthenticated;

    private SourceServer s;
    private GoldSrcServer g;

    private OnClickListener sendButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Send" button was clicked
             */

            command = field_command.getText().toString().trim();

            if( command.length() == 0 )
                UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_empty_rcon_command);
            else
                sendCommand(false);
        }
    };

    private OnKeyListener enterKeyListener = new OnKeyListener() {
        public boolean onKey( View v, int k, KeyEvent e ) {
            /*
             * "Enter" or "Done" key was pressed
             */

            if( (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (e.getAction() == KeyEvent.ACTION_UP) ) {
                command = field_command.getText().toString().trim();

                if( command.length() == 0 )
                    UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_empty_rcon_command);
                else
                    sendCommand(false);

                return true;
            }

            return false;
        }
    };

    // Handler for the server query thread
    private Handler progressHandler = new Handler() {
        public void handleMessage( Message msg ) {
            switch( msg.what ) {
                case -1:
                    Log.d(TAG, "progressHandler [" + msg.toString() + "]");
                    Log.d(TAG, "Message object string = " + msg.obj.toString());
                    Log.d(TAG, "Message object class = " + msg.obj.getClass().toString());
                case Values.ENGINE_GOLDSRC:
                    Log.i(TAG, "Server engine is GoldSrc.");
                    g = (GoldSrcServer)msg.obj;
                    break;
                case Values.ENGINE_SOURCE:
                    Log.i(TAG, "Server engine is Source.");
                    s = (SourceServer)msg.obj;
                    break;
                default:
                    Log.w(TAG, "Unhandled value from engine query: " + msg.what);
                    UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_rcon_general_error);
                    break;
            }

            p.dismiss();

            if( ! rconIsAuthenticated ) {
                if( password.length() == 0 )
                    getPassword();
                else
                    rconAuthenticate();
            }
        }
    };

    // Handler for the "Sending" pop-up thread
    private Handler popUpHandler = new Handler() {
        public void handleMessage( Message msg ) {
            runFadeOutAnimation(RconActivity.this, sending);

            switch( msg.what ) {
                case 0:
                    String response = (String)msg.obj;
                    rcon_console.append("> " + command + "\n\n");
                    rcon_console.append(response + "\n\n");
                    scrollToBottomOfText();
                    break;
                case 1:
                    if( msg.obj.getClass() == RCONNoAuthException.class ) {
                        showBadPasswordMessage();
                    }
                    else if( msg.obj.getClass() == RCONBanException.class ) {
                        UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_rcon_ban_exception);
                    }
                    else if( msg.obj.getClass() == TimeoutException.class ) {
                        UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_rcon_disconnected);
                    }
                    else {
                        UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_rcon_general_error);
                    }
                    break;
            }

            command = "";
        }
    };

    // Handler for the RCON authentication thread
    private Handler rconAuthHandler = new Handler() {
        @Override
        public void handleMessage( Message msg ) {
            runFadeOutAnimation(RconActivity.this, sending);

            switch( msg.what ) {
                case -1:
                    // An error occurred getting the engine type (most likely a socket timeout)
                    break;
                case 0:
                    rconIsAuthenticated = true;
                    if( g!= null )
                        g = (GoldSrcServer)msg.obj;
                    else
                        s = (SourceServer)msg.obj;
                    break;
                case 1:
                    Log.d(TAG, "rconAuthHandler [" + msg.toString() + "]");
                    Log.d(TAG, "Message object string = " + msg.obj.toString());
                    Log.d(TAG, "Message object class = " + msg.obj.getClass().toString());

                    if( msg.obj.getClass() == RCONNoAuthException.class ) {
                        // Failed authentication
                        UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_no_rcon_auth);
                        getPassword();
                    }
                    else if( msg.obj.getClass() == RCONBanException.class ) {
                        // RCONNoAuthException
                        UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_rcon_ban_exception);
                    }
                    else if( msg.obj.getClass() == TimeoutException.class ) {
                        // TimeoutException (happens if RCON password was already sent)
                        UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_rcon_timeout_exception);
                    }
                    else {
                        // Any other exception
                        UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_rcon_general_error);
                    }

                    break;
            }

            p.dismiss();
        }
    };

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        
        if( android.os.Build.VERSION.SDK_INT < 11 ) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        else if( android.os.Build.VERSION.SDK_INT >= 14 ) {
            if( ViewConfiguration.get(this).hasPermanentMenuKey() )
                requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        
        this.setContentView(R.layout.rcon);
        this.setResult(1);

        Intent thisIntent = getIntent();

        server = thisIntent.getStringExtra(Values.EXTRA_SERVER);
        port = thisIntent.getIntExtra(Values.EXTRA_PORT, 27015);
        timeout = thisIntent.getIntExtra(Values.EXTRA_TIMEOUT, 2);
        password = thisIntent.getStringExtra(Values.EXTRA_PASSWORD);
        rconIsAuthenticated = false;

        fade_in = AnimationUtils.loadAnimation(RconActivity.this, R.anim.fade_in);
        fade_out = AnimationUtils.loadAnimation(RconActivity.this, R.anim.fade_out);

        rcon_console = (TextView)findViewById(R.id.rcon_console);
        sending = (TextView)findViewById(R.id.sending);
        send_button = (Button)findViewById(R.id.send_button);

        send_button.setOnClickListener(sendButtonListener);
        rcon_console.setMovementMethod(ScrollingMovementMethod.getInstance());
        rcon_console.setHorizontallyScrolling(true);

        sending.setVisibility(-1);

        Resources res = getResources();
        String[] commandList = res.getStringArray(R.array.all_commands);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.autocomplete_textview_custom, commandList);

        field_command = (AutoCompleteTextView)findViewById(R.id.field_command);
        field_command.setAdapter(adapter);
        field_command.setOnKeyListener(enterKeyListener);

        // Hack to disable auto-complete if desired by the user
        if( CheckValve.settings.getBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS) == true )
            field_command.setThreshold(1);
        else
            field_command.setThreshold(1000);

        if( CheckValve.settings.getBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND) == true )
            unsafeCommands = res.getStringArray(R.array.unsafe_commands);
        else
            unsafeCommands = null;

        getServerType();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rcon_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.back:
                finish();
                return true;
            case R.id.clear_console:
                rcon_console.setText("");
                rcon_console.scrollTo(0, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        return;
    }

    public void onActivityResult( int request, int result, Intent data ) {
        if( request == Values.ACTIVITY_RCON_PASSWORD_DIALOG ) {
            if( result == 0 ) finish();

            if( result == 1 ) {
                password = data.getStringExtra(Values.EXTRA_PASSWORD);
                rconAuthenticate();
            }
        }

        if( request == Values.ACTIVITY_CONFIRM_UNSAFE_COMMAND && result == 1 ) sendCommand(true);
    }

    public void sendCommand( boolean force ) {
        if( unsafeCommands != null ) {
            if( !force ) {
                // Get the bare command without any arguments
                String bareCommand = (command.indexOf(" ") != -1)?command.substring(0, command.indexOf(" ")):command;
    
                if( Arrays.asList(unsafeCommands).contains(bareCommand) ) {
                    // Show a warning and force user acknowledgment
                    confirmUnsafeCommand();
                    return;
                }
            }
        }

        field_command.setText("");

        runFadeInAnimation(RconActivity.this, sending);

        if( g != null )
            new Thread(new RconQuery(command, g, popUpHandler)).start();
        else
            new Thread(new RconQuery(command, s, popUpHandler)).start();

        new Thread(q).start();
    }

    public void getServerType() {
        // Show the progress dialog
        p = ProgressDialog.show(this, "", RconActivity.this.getText(R.string.status_connecting), true, false);

        // Run the server queries in a new thread
        new Thread(new EngineQuery(server, port, timeout, progressHandler)).start();
    }

    public void scrollToBottomOfText() {
        /*
         * I can't take credit for this. This is based on a little trick I found at:
         * http://groups.google.com/group/android-developers/browse_thread/thread/8752156cca1e3742
         */
        int lineCount = rcon_console.getLineCount();
        int lineHeight = rcon_console.getLineHeight();
        int viewHeight = rcon_console.getHeight();

        int difference = (lineCount * lineHeight) - viewHeight;

        if( difference < 1 )
            return;

        rcon_console.scrollTo(0, difference);
    }

    public void rconAuthenticate() {
        p = ProgressDialog.show(this, "", RconActivity.this.getText(R.string.status_rcon_verifying_password), true, false);

        if( g != null )
            new Thread(new RconAuth(password, g, rconAuthHandler)).start();
        else
            new Thread(new RconAuth(password, s, rconAuthHandler)).start();
    }

    public void runFadeInAnimation( Context c, View v ) {
        v.startAnimation(fade_in);
    }

    public void runFadeOutAnimation( Context c, View v ) {
        v.startAnimation(fade_out);
    }

    public void getPassword() {
        Intent rconPasswordIntent = new Intent();
        rconPasswordIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.RconPasswordActivity");
        startActivityForResult(rconPasswordIntent, Values.ACTIVITY_RCON_PASSWORD_DIALOG);
    }

    public void showBadPasswordMessage() {
        UserVisibleMessage.showMessage(RconActivity.this, R.string.msg_no_rcon_auth);
        getPassword();
    }

    @SuppressLint("InlinedApi")
    public void confirmUnsafeCommand() {
        AlertDialog.Builder alertDialogBuilder;

        if( android.os.Build.VERSION.SDK_INT >= 11 )
            alertDialogBuilder = new AlertDialog.Builder(RconActivity.this, AlertDialog.THEME_HOLO_DARK);
        else
            alertDialogBuilder = new AlertDialog.Builder(RconActivity.this);

        alertDialogBuilder.setTitle(R.string.title_confirm_unsafe_command);
        alertDialogBuilder.setMessage(R.string.msg_send_unsafe_command);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton(R.string.button_send, new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int id ) {
                /*
                 *  "Send" button was clicked
                 */
                sendCommand(true);
            }
        });

        alertDialogBuilder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int id ) {
                /*
                 * "Cancel" button was clicked
                 */
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
