import android.os.RemoteException;
import com.android.server.am.ActivityStackSupervisor;


public class ASSstartActivityUncheckedLocked {

  private static ActivityStackSupervisor ass; 
  
  public static void main(String[] args) throws RemoteException {

	  ass.startActivityUncheckedLocked(null, null, null, null, 0, false, null, null);  // note: need to modify startActivityUncheckedLocked to be "public"
		
  }
}
