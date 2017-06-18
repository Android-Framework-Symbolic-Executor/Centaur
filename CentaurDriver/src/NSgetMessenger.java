import android.os.RemoteException;
import com.android.server.NsdService;


public class NSgetMessenger {

  private static NsdService ns; 
  
  public static void main(String[] args) throws RemoteException {

      ns.getMessenger(); 

  }
}
