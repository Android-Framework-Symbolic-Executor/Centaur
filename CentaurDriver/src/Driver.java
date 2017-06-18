import android.net.nsd.NsdManager;
import android.os.Binder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.android.internal.util.AsyncChannel;
import com.android.server.LocationManagerService;
import com.android.server.NsdService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.wifi.WifiServiceImpl;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wm.WindowManagerService;

import android.os.SystemProperties;
import android.util.Slog;

import libcore.io.Libcore;

import java.util.Vector;

public class Driver {

  //private static LocationManagerService lms; 
  //private static WindowManagerService ams; 
  //private static WifiServiceImpl wsi;
  
  private static NsdService ns;
  
  private static ActivityStackSupervisor ass;
  
  public static void main(String[] args) throws RemoteException {
	  //ass.startActivityUncheckedLocked(null, null, null, null, 0, false, null, null);  // note: need to modify startActivityUncheckedLocked to be "public"
	  //ass.onDisplayAdded(0);
	  
      //wsi.getWifiServiceMessenger();
	  
      ns.getMessenger();
	  
      //ams.closeSystemDialogs(null);
      //lms.getProviders(null,false); 
      //lms.getAllProviders();
      //.getLastLocation(null, "com.novoda.demos.activitylaunchmode.mal"); 
      //lms.getAllProviders();
      //Debug1.printPC("\nMyClassWithFields.myMethod1 Path Condition: ");
      //System.out.println("");
  }
}
