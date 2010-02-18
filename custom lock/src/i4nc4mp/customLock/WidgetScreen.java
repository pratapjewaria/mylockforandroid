package i4nc4mp.customLock;

import java.util.ArrayList;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;


public class WidgetScreen extends Activity {
//for testing we have set this up as a Launcher/Main icon
//for implementation we will make it subclass LockActivity
	
	
private AppWidgetManager mAppWidgetManager;
private AppWidgetHost mAppWidgetHost;

//private LayoutInflater mInflater;

static final int APPWIDGET_HOST_ID = 2037;
//this int identifies you. The Launcher's ID is 1024.
//If you were to implement different hosts that need to be distinguished
//then the ID is a shortcut for passing what you're doing to the correct one 


private static final int REQUEST_CREATE_APPWIDGET = 5;
private static final int REQUEST_PICK_APPWIDGET = 9;

private AppWidgetHostView widgets[] = new AppWidgetHostView[16];
//the views will be repopulated at oncreate
private int[] widgetId = new int[16];
//the id is how we get back to the persistent state of what widget was assigned by the user
//the id points to the object on the appwidgetmanager which remembers the user picked widget
private int widgCount = 0;
//we also need to know how many the actual user has added so we can reference them in the array

//the id array and the value of widgCount will be persisted in the state bundle.
//when re-created, another method which does the completeAdd for each widgetId we have makes all the views again.


//the mediator service needs to actually maintain the created widget references since the activity
//is destroyed and recreated. this way they don't need to be spawned every time



@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    
    requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

  
    updateLayout();
    
    //mInflater = getLayoutInflater();
    
    mAppWidgetManager = AppWidgetManager.getInstance(this);

    mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);
    mAppWidgetHost.startListening();
    
    if (savedInstanceState == null || savedInstanceState.isEmpty()) {
    	Log.v("no state","first start, no views to repopulate");
    	return;
    }
    
    //obtain how many widgetIds existed in the saved state
    int idCount = savedInstanceState.getInt("idcount");
            
    Log.v("repopulate","Need to restore this many widget views: " + idCount);
    
  //Set our own widgetId array so we have the Ids
    widgetId = savedInstanceState.getIntArray("idlist");
    
    //do a RepopulateWidgetView for each one we have
    for (int i = 0; i != idCount; i++) {
    	RepopulateWidgetView(widgetId[i]);
    }
    //once that is done, our own widgCount will equal the IdCount we saved
    //so it is ready for user to add a new one, also

}

//from LockActivity
private void updateLayout() {
    LayoutInflater inflater = LayoutInflater.from(this);

    setContentView(inflateView(inflater));
}

//override is for when we subclass LockActivity for purpose pass the blank slate layout
//@Override
protected View inflateView(LayoutInflater inflater) {
	
  
	return inflater.inflate(R.layout.mylockscreen, null);
}

@Override
protected void onSaveInstanceState (Bundle outState) {
//this should work. the problem is i need to test it
	//when i answer a call i choose to kill the activity
	//how i cause this state data to be persisted?
	
	
	//I believe I will just have to store all of this to prefs file instead
	
	
	super.onSaveInstanceState(outState);

	//put count int
	outState.putInt("idcount", widgCount);
	//put Ids array
	outState.putIntArray("idlist", widgetId);
	
	//these are used to literally make new views for all the widgets.
	//the system widget manager actually keeps these IDs persistent unless we tell it to delete one. 
}

@Override
public void onBackPressed() {
	moveTaskToBack(true);
	//this makes sure we don't get killed unless system forces it
}


public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, 1, 0, "Add Widget");
    return true;
}

/* Handles item selections */
public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case 1:
        doWidgetPick();
        return true;
    }
    return false;
}

//here's where it gets really fun. we're going to launch the widget pick-list intent 

//there is a bug with this intent/process where they have made it dependent on defining a custom extra widget. 
//The "search" widget is not coded as an AppWidgetProvider for some reason 
//they insert it into the list when pick intent is called, and if you don't insert a custom item 
//you get a null pointer exception when trying to start 
protected void doWidgetPick() {
	int appWidgetId = WidgetScreen.this.mAppWidgetHost.allocateAppWidgetId();

    Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
    pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
    
    /* custom extra that has to be there or else NPE will happen due to android bug   */
  //this is pulled from the Launcher source, I just changed a few things as it's just a dummy entry at this point 
    ArrayList<AppWidgetProviderInfo> customInfo =
            new ArrayList<AppWidgetProviderInfo>();
    AppWidgetProviderInfo info = new AppWidgetProviderInfo();
    info.provider = new ComponentName(getPackageName(), "XXX.YYY");
    info.label = "i love android";
    info.icon = R.drawable.icon;
    customInfo.add(info);
    pickIntent.putParcelableArrayListExtra(
            AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
    ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
    Bundle b = new Bundle();
    b.putString("custom_widget", "search_widget");
    customExtras.add(b);
    pickIntent.putParcelableArrayListExtra(
            AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    /* that's a lot of lines that are there for no function at all */
        
    
    // start the pick activity
    startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    //because we've defined ourselves as a singleTask activity, it will allow this intent to be part of the task

}


@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.v("result",requestCode + ", " + resultCode + ", " + data);
  //when the user completes the pick process, both these cases come back, according to log. 
  //looks like this is happening because Pick does a check to see if a config needs to be launched first 
  //if not it just sends the create intent 
    
    
    if (resultCode == RESULT_OK) {
        switch (requestCode) {
        	case REQUEST_PICK_APPWIDGET:
        		addAppWidget(data);
        		break;
        	case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(data);
                break;
        }
    }
    else if ((requestCode == REQUEST_PICK_APPWIDGET ||
            requestCode == REQUEST_CREATE_APPWIDGET) && resultCode == RESULT_CANCELED &&
            data != null) {
        // Clean up the appWidgetId if we canceled
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId != -1) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        }
    }
}


private void completeAddAppWidget(Intent data){
	//actually creates the view for the widget

    Bundle extras = data.getExtras();
    int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

    AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
    

    /* Launcher would calculate the grid spans needed to fit this widget
     * It would also do a check operation to abort if the cell user picked wasn't acceptable
     * given the size of the widget they chose
     */
    
    //What we'll do is log the info about the widget for help in letting user reposition it
    
    int width = appWidgetInfo.minWidth;
    int height = appWidgetInfo.minHeight;
    
/*
next we need to make a record of where we are adding this widget
what the launcher is doing is spawning a helper object where it saves details about the widget
it saves the number of cells wide and tall the widget is
it adds the spawned object to the array list for widgetinfos
the array list is a member of LauncherInfo helper object
the model seems to retain the references to everything that's been placed on the Launcher
*/
    //we can get a reference to our main view here, and then add a relative layout to it.
    //I can probably directly reference the relative layout I want and then add widgets filling in from the top
    //just need to figure out how to determine if the widget being selected is too long to fit on existing row
    //to decide whether to place it on right of last widget or on the bottom
    RelativeLayout parent= (RelativeLayout) findViewById(R.id.mylockscreen); 
    
    //Log.v("getting parent ref","the ID of the parent is " + parent.getId());

    //AppWidgetHostView newWidget = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);

    //we need to store this widget in an array. the views can be recreated but we need to have a persistent ref
    //FIXME currently we aren't persistent, need to learn how to make activity save persistent state

    widgets[widgCount] = attachWidget(mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo), width, height);
    
    
    parent.addView(widgets[widgCount]); 
    
    
    Log.v("widget was added","the manager ID of the widget is " + appWidgetId);
    
    
    widgetId[widgCount] = appWidgetId;
    widgCount++;
    
    
    
    
    
        //launcher is doing something to pass this view to their the workspace or the celllayout
        
        //so every single widget that gets created is one instance of the AppWidgetHostView.
        //the viewgroup we would have to maintain holds all the appwidgethostviews/
}


//the created new widget is passed raw with the data about its size, then we figure out how to position it
private AppWidgetHostView attachWidget(AppWidgetHostView widget, int w, int h){ 
         
    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams 
    (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); 
    if (widgCount == 0) params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    //first widget goes at the top of the relative view widget area
    else params.addRule(RelativeLayout.RIGHT_OF, widgets[widgCount-1].getId());
     
    widget.setLayoutParams(params); 
    
    widget.setId(100+widgCount);
    return widget; 
    }

private void RepopulateWidgetView(int Id) {
	
	AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(Id);
    

    
    int width = appWidgetInfo.minWidth;
    int height = appWidgetInfo.minHeight;
    
    RelativeLayout parent= (RelativeLayout) findViewById(R.id.mylockscreen); 

    widgets[widgCount] = attachWidget(mAppWidgetHost.createView(this, Id, appWidgetInfo), width, height);
    
    
    parent.addView(widgets[widgCount]);
    widgCount++;
}

public AppWidgetHost getAppWidgetHost() {
    return mAppWidgetHost;
}




void addAppWidget(Intent data) {
    // TODO: catch bad widget exception when sent
    int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

    String customWidget = data.getStringExtra("custom_widget");
    if ("search_widget".equals(customWidget)) {//user picked the extra
        // We don't need this any more, since this isn't a real app widget.
        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        //scold user for disobedience
    } else {
        AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        if (appWidget.configure != null) {
            // Launch over to configure widget, if needed
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidget.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise just add it
            onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_OK, data);
        }
    }
}

/**
 * Re-listen when widgets are reset.
 */
private void onAppWidgetReset() {
    mAppWidgetHost.startListening();
}

@Override
public void onDestroy() {
    //mDestroyed = true;

    super.onDestroy();

    try {
        mAppWidgetHost.stopListening();
    } catch (NullPointerException ex) {
        Log.w("lockscreen destroy", "problem while stopping AppWidgetHost during Lockscreen destruction", ex);
    }
}

}