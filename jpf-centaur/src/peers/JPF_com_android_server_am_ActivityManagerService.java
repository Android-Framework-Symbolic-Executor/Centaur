/***************************************************************************
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ***************************************************************************/

package peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;

public class JPF_com_android_server_am_ActivityManagerService extends NativePeer {
	
	@MJI
	public static int broadcastIntent__Landroid_app_IApplicationThread_2Landroid_content_Intent_2Ljava_lang_String_2Landroid_content_IIntentReceiver_2ILjava_lang_String_2Landroid_os_Bundle_2Ljava_lang_String_2IZZI__I (MJIEnv env, int robj, 
			int arg0, int arg1, int arg2, int arg3, 
			int arg4, int arg5, int arg6, int arg7, 
			int arg8, boolean arg9, boolean arg10, int arg11) {
		System.out.println("JPF_com_android_server_am_ActivityManagerService_broadcastIntent -- MJI");
    	return 0; //0 == ActivityManager.BROADCAST_SUCCESS;
	}
	
    @MJI
    public static int broadcastIntentLocked__Lcom_android_server_am_ProcessRecord_2Ljava_lang_String_2Landroid_content_Intent_2Ljava_lang_String_2Landroid_content_IIntentReceiver_2ILjava_lang_String_2Landroid_os_Bundle_2Ljava_lang_String_2IZZIII__I (MJIEnv env, int robj, 
    		int arg0, int arg1, int arg2, int arg3, 
    		int arg4, int arg5, int arg6, int arg7, 
    		int arg8, int arg9, boolean arg10, boolean arg11, 
    		int arg12, int arg13, int arg14) {
    	System.out.println("JPF_com_android_server_am_ActivityManagerService_broadcastIntentLocked -- MJI");
    	return 0; //0 == ActivityManager.BROADCAST_SUCCESS;
    }
}