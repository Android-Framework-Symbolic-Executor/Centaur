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

import bootstrap.KeyWords;
import gov.nasa.jpf.centaur.vm.OVHeap;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.heap.SymbolicInputHeap;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.string.SymbolicStringBuilder;
import gov.nasa.jpf.vm.ArrayFields;
import gov.nasa.jpf.vm.ArrayIndexOutOfBoundsExecutiveException;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import migration.Migrate;
import queryer.Query;

public class NEW extends gov.nasa.jpf.symbc.bytecode.NEW {

	public NEW(String clsName) {
		super(clsName);
		// TODO Auto-generated constructor stub
	}

	@Override
	  public Instruction execute (ThreadInfo ti) {
		  Heap heap = ti.getHeap();
		    ClassInfo ci;

		    // resolve the referenced class
		    ClassInfo cls = ti.getTopFrameMethodInfo().getClassInfo();
		    try {
		      ci = cls.resolveReferencedClass(cname);  
		    } catch(LoadOnJPFRequired lre) {
		      return ti.getPC();
		    }		    
		    
		    //if classInfo is Exception related class, we skip the following analysis on this path.
		    //ClassInfo[name=java.lang.SecurityException]; cname=java.lang.SecurityException 
		    if (cname.contains("Exception")) {
		    	VM vm = ti.getVM();
		    	vm.getSystemState().setIgnored(true);
		    	return getNext(ti);
		    } else {
		    	return super.execute(ti);
		    }
		   
	}
	
}