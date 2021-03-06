package name.tanglei.airplaneswitcher.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import name.tanglei.airplaneswitcher.R;
import name.tanglei.airplaneswitcher.utils.AirplaneModeUtils;
import name.tanglei.airplaneswitcher.utils.OperationLogUtils;
import name.tanglei.airplaneswitcher.utils.Utils;

public class ReceivedAction extends Activity
{
	public static String TAG = ReceivedAction.class.getName();

	public final static String ACTION_TAG = "airmode_action";
	public final static String USERACTION_TAG = "user_force_Action";

	private AlertDialog dialog = null;
	private Button positiveButton = null;
	private int delaycount = 5;
	private ProgressDialog                 progressDialog = null;

	private BroadcastReceiver airmodechanged_receiver;

	//if user force action, do not write alarm
	private boolean is_user_force_action = false;

	private boolean isScreenLocked = false;

	@Override
	protected void onDestroy()
	{
		Log.i(TAG, "onDestroy");


        HashMap<String, String> map = new HashMap<String, String>();
        map.put("is_user_force_action", is_user_force_action ? "true" : "false");
        MobclickAgent.onEvent(this, "airplaneswitch", map);

        try //no api to detect if receiver is registered
        {
            ReceivedAction.this.unregisterReceiver(airmodechanged_receiver);
            Log.i(TAG, "unregisterReceiver(airmodechanged_receiver) sucessfully");
        } catch (Exception e)
        {
            Log.i(TAG, "unregisterReceiver(airmodechanged_receiver) failed " + e.getLocalizedMessage());
        }

		super.onDestroy();
		if(this.isScreenLocked)
		{
			Intent mIntent=new Intent(Intent.ACTION_MAIN);
			mIntent.addCategory(Intent.CATEGORY_HOME);
			this.startActivity(mIntent);
		}
	}

	@Override
	protected void onRestart()
	{
		// TODO Auto-generated method stub
		super.onRestart();
		Log.i(TAG, "onRestart");
	}


    @Override
    protected void onResume()
    {
        super.onResume();
        Log.i(TAG, "onResume");
        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        MobclickAgent.onPause(this);
    }

	@Override
	protected void onStart()
	{
		// TODO Auto-generated method stub
		super.onStart();
		Log.i(TAG, "onStart");
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		Log.i(TAG, "ReceivedAction received the action");
		/*getWindow().addFlags(
		        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
		        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|
		        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		*/
	    super.onCreate(savedInstanceState);
	    Intent intent = this.getIntent();
        boolean action = intent.getBooleanExtra(ACTION_TAG, false);
        is_user_force_action  = intent.getBooleanExtra(USERACTION_TAG, false);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        final String toggle_success_tip = action ? this.getString(R.string.toggle_success_on) : this.getString(R.string.toggle_success_off);
        
    	airmodechanged_receiver = new BroadcastReceiver() {
    	      @Override
    	      public void onReceive(Context context, Intent intent) {
    	    	    ReceivedAction.this.finish();
    	            Log.i(TAG, "Received airplane mode state has changed");
    	            dismissProgressDialog();
    	            
    	            Toast.makeText(ReceivedAction.this, 
    	            		toggle_success_tip, Toast.LENGTH_SHORT).show();
                    OperationLogUtils.addOperation(false, "\t"+toggle_success_tip, true);
    	      }
    	};
    	
    	this.registerReceiver(airmodechanged_receiver, intentFilter);

        if(is_user_force_action)
            OperationLogUtils.addOperation(true, this.getString(action ?
                    R.string.operation_log_attempt_on:
                    R.string.operation_log_attempt_off) + this.getString(R.string.operation_log_attempt_user_force_suf) , false);
        else
            OperationLogUtils.addOperation(true, this.getString(action ?
             R.string.operation_log_attempt_on:
             R.string.operation_log_attempt_off) + this.getString(R.string.operation_log_attempt_auto_suf), false);

        isScreenLocked = Utils.isScreenLocked(this);
        //fix bug,
        if (isScreenLocked)
        {
            Log.i(TAG, "screenLocked no dialog, direct set airplanemode");
            AirplaneModeUtils.setAirplane(ReceivedAction.this,
                    action, true);
            return;
        }
        if(action)
        {
	        String title = this.getString(R.string.confirmAirmodeTitle);
			String content = this.getString(R.string.confirmAirmodeContentHtml);
			
			showAlertDialog(title, content, 
							R.string.confirmAirmodeButtonOK, 
							R.string.confirmAirmodeButtonCancel, 
							oKListener, cancelListener);
        }else
        {
        	setAirPlaneState(false);
        }
	}
	
	
	@Override
	protected void onStop()
	{
        Log.i(TAG, "onStop");

		ReceivedAction.this.finish();
		dismissProgressDialog();
		super.onStop();
	}
	
	private DialogInterface.OnClickListener oKListener = new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			Log.d(TAG, "user confirm true");
			setAirPlaneState(true);
		}
	};
	
	private DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			//if(!is_user_force_action)
			//	Utils.delayOnSchedule(ReceivedAction.this, System.currentTimeMillis(), 24 * 60 * 60 * 1000);
			ReceivedAction.this.finish();
			Log.d(TAG, "user confirm false, is_user_force_action = " + is_user_force_action);
			dialog.dismiss();
			Toast.makeText(ReceivedAction.this, ReceivedAction.this.getString(R.string.cancelAirmodeToast), Toast.LENGTH_SHORT).show();
            OperationLogUtils.addOperation(false, "\t"+ReceivedAction.this.getString(R.string.operation_log_attempt_user_cancel), true);
		}
	};

	
	public  void showAlertDialog(String title,
			String htmlContent, int positive_id, int negative_id, DialogInterface.OnClickListener okListener, 
			DialogInterface.OnClickListener cancelListener)
	{
		if(dialog == null)
		{
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
					this);
			alertDialogBuilder.setTitle(title);
			alertDialogBuilder.setIcon(R.drawable.ic_action_warning);
			//alertDialogBuilder.setMessage(content);
			
			TextView txtView = new TextView(this);
			txtView.setTextSize(20f);
			Spanned text = Html.fromHtml(htmlContent);
			txtView.setText(text);
			alertDialogBuilder.setView(txtView);
		   
			alertDialogBuilder.setPositiveButton(
					this.getString(positive_id), okListener);
			alertDialogBuilder.setNegativeButton(
					this.getString(negative_id), cancelListener);
			dialog = alertDialogBuilder.create();
		}
		dialog.show();
		positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
		countDownTimeHandler.sendEmptyMessageDelayed(0, 100);
	}
	
	//http://ifoggy.iteye.com/blog/1874499
	//or else This Handler class should be static or leaks might occur
	static class MsgHandler extends Handler 
	{  
	    private WeakReference<ReceivedAction> mActivity;  
	  
	    MsgHandler(ReceivedAction activity) {  
	        mActivity = new WeakReference<ReceivedAction>(activity);  
	    }  
	  
	    @Override  
	    public void handleMessage(Message msg) {  
	    	ReceivedAction activity = mActivity.get();  
	        if (activity != null) {  
	            activity.handleMessage(msg);  
	        }  
	    }  
	}  
	  
	private Handler countDownTimeHandler = new MsgHandler(this);
	
	public void handleMessage(Message msg)
	{
		if(delaycount >= 0)
		{
			if(dialog != null)
			{
				String txt = this.getString(R.string.confirmAirmodeButtonOK) + " (" + delaycount + " s )";
				positiveButton.setText(txt);
				countDownTimeHandler.sendEmptyMessageDelayed(0, 1000);
			}
			delaycount --;
		}else
		{
			
			if(dialog.isShowing())//user does not action
			{
				dialog.dismiss();
				this.setAirPlaneState(true);
			}
		}
	}
	
	
	public void setAirPlaneState(boolean state)
	{
		final boolean air_action = state;
		try
		{
			String tip = "";
			if(state)
				tip = this.getString(R.string.airplanemode_on_tip);
			else 
				tip = this.getString(R.string.airplanemode_off_tip);

			showProgressDialog(this.getString(R.string.airplanemode_action_title), tip);
			
			new Thread()
			{
				public void run()
				{
					try
					{
						Thread.sleep(1*1000);
					} catch (InterruptedException e)
					{
						e.printStackTrace();
					}
                    Log.i("tanglei", "set airplane mode, time:" + System.currentTimeMillis());
					AirplaneModeUtils.setAirplane(ReceivedAction.this,
                            air_action);
				}
			}.start();
			
		} catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
			Toast.makeText(this, this.getString(R.string.airplanemode_error), Toast.LENGTH_SHORT).show();
			dismissProgressDialog();
		} 
	}
	
	 public void showProgressDialog(String title, String content) 
	 {
		 if(progressDialog == null)
	        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(title);
        progressDialog.setMessage(content);
        progressDialog.setCancelable(false);
        progressDialog.show();
        Log.i(TAG, "show progress dialog");
	 }
	 
	 public void dismissProgressDialog() 
	 {
		 if(progressDialog == null)
			 return;
		 if(!progressDialog.isShowing())
			 return;
		 progressDialog.dismiss();
		 Log.i(TAG, "dismiss progress dialog");
	 }
	
}
