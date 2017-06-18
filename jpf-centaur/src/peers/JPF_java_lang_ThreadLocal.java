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
import gov.nasa.jpf.jvm.bytecode.CHECKCAST;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.NativePeer;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

public class JPF_java_lang_ThreadLocal extends NativePeer {

	@MJI
	public int get____Ljava_lang_Object_2 (MJIEnv env, int objRef){
	  
		StackFrame frame = env.getModifiableCallerStackFrame();
		Instruction instr = frame.getPC().getNext();  //checkcast, 
		
		String type = "";

		if (instr.toString().equals("checkcast")) { 
			CHECKCAST cinstr = (CHECKCAST) instr;
			type = cinstr.getTypeName();
		}
	
		if (type.contains("Identity")) {  //ThreadLocal<Identity>; com.android.server.am.ActivityManagerService$Identity
			System.out.println("JPF_java_lang_ThreadLocal_get (Identity) -- MJI"); 
			return MJIEnv.NULL;    
		} else if (type.contains("String")) {
			System.out.println("JPF_java_lang_ThreadLocal_get (String) -- MJI: return android");              
			String result = "android";  //this is based on our experience. need to check.
			return env.newString(result);
		} 
		else {
			System.err.println("In JPF_java_lang_ThreadLocal -- MJI: other type: " + type);              
			System.exit(0);
			return MJIEnv.NULL;
		}
	}
	
	@MJI
	public void set__Ljava_lang_Object_2 (MJIEnv env, int objRef, int arg0){
		System.out.println("JPF_java_lang_ThreadLocal_set -- MJI"); 		
	}
}


