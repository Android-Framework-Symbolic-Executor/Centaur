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
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


public class SkeletonExecute {

	public static Instruction handleSkeleton (JVMInvokeInstruction invInst, ThreadInfo th) {
		String mname = invInst.getInvokedMethodName();
    	String shortName = mname.substring(0, mname.indexOf("("));
    	if (shortName.equals ("contains")) {
			ChoiceGenerator<?> cg;
			if (!th.isFirstStepInsn()) { // first time around
				cg = new PCChoiceGenerator(2);
				th.getVM().setNextChoiceGenerator(cg);
				return invInst;
			} else {
				handleContains(invInst, th);
				return invInst.getNext(th);
			}
    	} else {
			System.err.println("ERROR: skeleton method not handled: " + shortName);
			return null;
		}
	}
	
	
	private static void handleContains(JVMInvokeInstruction invInst, ThreadInfo th) {
	    StringComparator comp = StringComparator.CONTAINS;	    
	    StackFrame sf = th.getModifiableTopFrame();

        ChoiceGenerator<?> cg;
        boolean conditionValue;

        cg = th.getVM().getChoiceGenerator();
        assert (cg instanceof PCChoiceGenerator) : "expected PCChoiceGenerator, got: " + cg;
        conditionValue = (Integer) cg.getNextChoice() == 0 ? false : true;

        // System.out.println("conditionValue: " + conditionValue);
        int s1 = sf.pop();
        int s2 = sf.pop();
        PathCondition pc;

        // pc is updated with the pc stored in the choice generator above
        // get the path condition from the
        // previous choice generator of the same type

        ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
        while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
            prev_cg = prev_cg.getPreviousChoiceGenerator();
        }

        if (prev_cg == null) {
            pc = new PathCondition();
        } else {
            pc = ((PCChoiceGenerator) prev_cg).getCurrentPC();
        }

        assert pc != null;

        if (conditionValue) {
            ElementInfo e1 = th.getElementInfo(s1);
            String val = e1.asString();
            StringExpression sym_v2 = new StringSymbolic("HashSet.node_" + s2
                    + KeyWords.SkeletonExpressionSuffix);   
            pc.spc._addDet(comp, val, sym_v2);
            System.out.println("pc.spc: " + pc.spc);

            if (!pc.simplify()) {// not satisfiable
                th.getVM().getSystemState().setIgnored(true);
            } else {
                ((PCChoiceGenerator) cg).setCurrentPC(pc);
            }
        } else {                
            ElementInfo e1 = th.getElementInfo(s1);
            String val = e1.asString();
            StringExpression sym_v2 = new StringSymbolic("HashSet.node_" + s2
                    + KeyWords.SkeletonExpressionSuffix);   
            pc.spc._addDet(comp.not(), val, sym_v2);
            System.out.println("pc.spc: " + pc.spc.toString());

            if (!pc.simplify()) {// not satisfiable
                th.getVM().getSystemState().setIgnored(true);
            } else {
                ((PCChoiceGenerator) cg).setCurrentPC(pc);
            }
        }
        sf.push(conditionValue ? 1 : 0, true);      
    }
}
