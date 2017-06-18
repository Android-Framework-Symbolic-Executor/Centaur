import android.os.RemoteException;
import com.android.server.NsdService;


public class NSsetEnabled {

  private static NsdService ns; 
  
  public static void main(String[] args) throws RemoteException {

      ns.setEnabled(true); 

  }
}
