import android.os.RemoteException;
import com.android.server.telecom.TelecomServiceImpl;

public class TSIgetCallState {

  private static TelecomServiceImpl tsi; 
  
  public static void main(String[] args) throws RemoteException {

      tsi.getCallState(); 

  }
}
