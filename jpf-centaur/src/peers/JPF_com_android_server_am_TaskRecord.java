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

public class JPF_com_android_server_am_TaskRecord extends NativePeer {
	
	@MJI
	public void performClearTaskLocked____V (MJIEnv env, int objref) {
		System.out.println("JPF_com_android_server_am_TaskRecord_performClearTaskLocked -- MJI");
	}
	
	@MJI
	public int performClearTaskLocked__Lcom_android_server_am_ActivityRecord_2I__Lcom_android_server_am_ActivityRecord_2 (MJIEnv env, int objref,
			int arg0, int arg1) {
		System.out.println("JPF_com_android_server_am_TaskRecord_performClearTaskLocked -- MJI");
		return MJIEnv.NULL;
	}
	
	@MJI
	public void setIntent__Lcom_android_server_am_ActivityRecord_2__V (MJIEnv env, int objref, int arg0) {
		System.out.println("JPF_com_android_server_am_TaskRecord_setIntent -- MJI");
	}
}
