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

public class JPF_android_os_BinderProxy extends NativePeer {

    @MJI
    public boolean transact__ILandroid_os_Parcel_2Landroid_os_Parcel_2I__Z (MJIEnv env, int rcls,
    		int code, int data, int reply, int flags) {
    	System.out.println("JPF_android_os_BinderProxy_transact -- MJI");
        return true;
    }
}
