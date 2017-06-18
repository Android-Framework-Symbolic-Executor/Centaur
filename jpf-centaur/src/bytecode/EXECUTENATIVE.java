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

package bytecode;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.NativeMethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;

public class EXECUTENATIVE extends gov.nasa.jpf.jvm.bytecode.EXECUTENATIVE {

	public EXECUTENATIVE (NativeMethodInfo mi){
		super(mi);
	}
	
	@Override
	  public Instruction execute (ThreadInfo ti) {
		if (executedMethod.toString().contains("android.os.Binder")) {
			return executedMethod.executeNative(ti);
		}
		
		int size = executedMethod.getReturnSize(); 
		//String type = executedMethod.getReturnType(); 

		if ((size == 0) && executedMethod.toString().contains("android")) {
			return getNext(ti);
		} else {
			// we don't have to enter/leave or push/pop a frame, that's all done
			// in NativeMethodInfo.execute()
			// !! don't re-enter if this is reexecuted !!
			return executedMethod.executeNative(ti); 
		}
	}
}
