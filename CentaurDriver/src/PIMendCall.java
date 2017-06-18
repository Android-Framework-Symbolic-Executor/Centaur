import android.os.RemoteException;
import com.android.phone.PhoneInterfaceManager;


public class PIMendCall {

  private static PhoneInterfaceManager pim; 
  
  public static void main(String[] args) throws RemoteException {

      pim.endCall(); 

  }
}

