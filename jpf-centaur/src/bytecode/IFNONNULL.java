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

import gov.nasa.jpf.symbc.bytecode.util.IFInstrSymbHelper;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class IFNONNULL extends gov.nasa.jpf.symbc.bytecode.IFNONNULL {
	public IFNONNULL (int targetPc) {
	    super(targetPc);
	}
	
	@Override
	public Instruction execute (ThreadInfo ti) {

		StackFrame sf = ti.getModifiableTopFrame();
		Expression sym_v = (Expression) sf.getOperandAttr();
		if(sym_v == null) { 
			StackFrame frame = ti.getModifiableTopFrame();
		    conditionValue = popConditionValue(frame);
		    if (conditionValue) {
		      return getTarget();
		    } else {
		      return getNext(ti);
		    }
		}
		else {		   
		    SymbolicInteger si = new SymbolicInteger(sym_v.toString()); 		    
		    Instruction nxtInstr = IFInstrSymbHelper.getNextInstructionAndSetPCChoice(ti, 
                    this, si, Comparator.NE,  Comparator.EQ);

		    if(nxtInstr==getTarget()) {
		        conditionValue=true;
		    } else {
		        conditionValue=false;
		    }
		    return nxtInstr;

			
		}
	}
	
	@Override
	public boolean popConditionValue (StackFrame frame) {
	    int i = frame.pop();
	    return (i != MJIEnv.NULL && i != -1);
	}
}

