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

public class JPF_com_android_server_wifi_WifiStateMachine extends NativePeer {
	
	@MJI
	public static void logStateAndMessage__Landroid_os_Message_2Ljava_lang_String_2__V (MJIEnv env, int objref,
			int arg0, int arg1) {
		System.out.println("JPF_com_android_server_wifi_WifiStateMachine_logStateAndMessage -- MJI");
	}
	
	@MJI
	public static void logStateAndMessage__Landroid_os_Message_2Ljava_lang_String_2 (MJIEnv env, int objref,
			int arg0, int arg1) {
		System.out.println("JPF_com_android_server_wifi_WifiStateMachine_logStateAndMessage -- MJI");
	}
}