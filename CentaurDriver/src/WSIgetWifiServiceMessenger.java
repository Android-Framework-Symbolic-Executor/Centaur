import android.os.RemoteException;
import com.android.server.wifi.WifiServiceImpl;


public class WSIgetWifiServiceMessenger {

	private static WifiServiceImpl wsi;
  
    public static void main(String[] args) throws RemoteException {

	  wsi.getWifiServiceMessenger(); 
	  
  }
}
