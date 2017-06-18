import android.os.RemoteException;
import com.android.server.LocationManagerService;


public class LMSgetProviders {

  private static LocationManagerService lms; 
  
  public static void main(String[] args) throws RemoteException {

      lms.getProviders(null,false); 

  }
}
