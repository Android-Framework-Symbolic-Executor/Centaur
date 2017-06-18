import android.os.RemoteException;
import com.android.server.am.ActivityManagerService;


public class AMScloseSystemDialogs {

  private static ActivityManagerService ams; 
  
  public static void main(String[] args) throws RemoteException {

      ams.closeSystemDialogs(null); 

  }
}
