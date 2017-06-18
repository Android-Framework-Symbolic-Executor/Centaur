import android.os.RemoteException;
import com.android.server.wm.WindowManagerService;


public class WMScloseSystemDialogs {

  private static WindowManagerService wms; 
  
  public static void main(String[] args) throws RemoteException {

      wms.closeSystemDialogs(null); 

  }
}
