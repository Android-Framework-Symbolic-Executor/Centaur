import android.os.RemoteException;
import com.android.server.telecom.TelecomServiceImpl;

public class TSIisInCall {

  private static TelecomServiceImpl tsi; 
  
  public static void main(String[] args) throws RemoteException {

      tsi.isInCall(); 

  }
}
