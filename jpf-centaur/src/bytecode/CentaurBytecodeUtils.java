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

import gov.nasa.jpf.Config;

import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.bytecode.SymbolicStringHandler;
import gov.nasa.jpf.symbc.heap.Helper;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.PreCondition;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.vm.AnnotationInfo;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

public class CentaurBytecodeUtils extends gov.nasa.jpf.symbc.bytecode.BytecodeUtils {	
		
    /**
	  * Execute INVOKESPECIAL, INVOKESTATIC, and INVOKEVIRTUAL symbolically.
	  * @param invInst The instance of INVOKESPECIAL, INVOKESTATIC, or INVOKEVIRTUAL
	  * @param ss The VM's system state
	  * @param ks The VM's kernel state
	  * @param th The current thread info
	  * @return an InstructionOrSuper instance saying what to do next.
	  */
	public static InstructionOrSuper execute(JVMInvokeInstruction invInst, ThreadInfo th) {
	    boolean isStatic = (invInst instanceof INVOKESTATIC);
		String bytecodeName = invInst.getMnemonic().toUpperCase();
		String mname = invInst.getInvokedMethodName();
		String cname = invInst.getInvokedMethodClassName();

		MethodInfo mi = invInst.getInvokedMethod(th);
			
		if (mi == null) {
			return new InstructionOrSuper(false,
				th.createAndThrowException("java.lang.NoSuchMethodException", "calling " + cname + "." + mname));
		}

		/* Here we test if the the method should be executed symbolically.
		 * We perform two checks:
		 * 1. Does the invoked method correspond to a method listed in the
		 * symbolic.method property and does the number of parameters match?
		 * 2. Is the method contained in a class that is to be executed symbolically?
		 * If the method is symbolic, initialize the parameter attributes
		 * and the fields if they are specified as symbolic based on annotations
		 *
		 */
		String longName = mi.getFullName();
		String[] argTypes = mi.getArgumentTypeNames();
		//System.out.println(longName);

		int argSize = argTypes.length; // does not contain "this"

		Vector<String> args = new Vector<String>();
		Config conf = th.getVM().getConfig();

		// Start string handling
		/**** This is where we branch off to handle symbolic string variables *******/
		CentaurSymbolicStringHandler a = new CentaurSymbolicStringHandler();
		Instruction handled = a.handleSymbolicStrings(invInst, th);
		if(handled != null){ // go to next instruction as symbolic string operation was done
			System.out.println("Symbolic string analysis");	
			return new InstructionOrSuper(false, handled);
		} else {
			return BytecodeUtils.execute(invInst, th);
		}		
	}
}
