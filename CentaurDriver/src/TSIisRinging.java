import android.os.RemoteException;
import com.android.server.telecom.TelecomServiceImpl;

public class TSIisRinging {

  private static TelecomServiceImpl tsi; 
  
  public static void main(String[] args) throws RemoteException {

      tsi.isRinging(); 

  }
}
