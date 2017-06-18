import android.os.RemoteException;
import com.android.server.LocationManagerService;


public class LMSgetAllProviders {

  private static LocationManagerService lms; 
  
  public static void main(String[] args) throws RemoteException {

      lms.getAllProviders(); 

  }
}
