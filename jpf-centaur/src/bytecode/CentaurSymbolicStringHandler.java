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

import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class CentaurSymbolicStringHandler extends gov.nasa.jpf.symbc.bytecode.SymbolicStringHandler {	

	public Instruction handleSymbolicStrings(JVMInvokeInstruction invInst, ThreadInfo th) {
		boolean needToHandle = isMethodStringSymbolic(invInst, th);
		if (needToHandle) {
			// do the string manipulations
			String mname = invInst.getInvokedMethodName();
			String shortName = mname.substring(0, mname.indexOf("("));
			if (shortName.equals("compareTo")) {
				ChoiceGenerator<?> cg;
				if (!th.isFirstStepInsn()) { // first time around
					cg = new PCChoiceGenerator(2);
					th.getVM().setNextChoiceGenerator(cg);
					return invInst;
				} else {
					handleObjectCompareTo(invInst, th);
					return invInst.getNext(th);
				}
			} else {
				return super.handleSymbolicStrings(invInst, th);
			}
		} else {
			return null;
		}
	}	
	
	
	public void handleObjectCompareTo(JVMInvokeInstruction invInst,  ThreadInfo th) {
		StackFrame sf = th.getModifiableTopFrame();
		Expression sym_v1 = (Expression) sf.getOperandAttr(0);
		Expression sym_v2 = (Expression) sf.getOperandAttr(1);

		if (sym_v1 != null) {
			// System.out.println("*" + sym_v1.toString());
			if (!(sym_v1 instanceof StringExpression)) {
				System.err.println("ERROR: expressiontype not handled: ObjectEquals");
				return;
			}
		}
		if (sym_v2 != null) {
			// System.out.println("***" + sym_v2.toString());
			if (!(sym_v2 instanceof StringExpression)) {
				System.err.println("ERROR: expressiontype not handled: ObjectEquals");
				return;
			}
		}

		handleBooleanStringInstructionsCompareTo(invInst,  th, StringComparator.EQUALS);
	}
	
	private void handleBooleanStringInstructionsCompareTo(JVMInvokeInstruction invInst,  ThreadInfo th, StringComparator comp) {
		StackFrame sf = th.getModifiableTopFrame();
		StringExpression sym_v1 = (StringExpression) sf.getOperandAttr(0);
		StringExpression sym_v2 = (StringExpression) sf.getOperandAttr(1);

		if ((sym_v1 == null) & (sym_v2 == null)) {
			System.err.println("ERROR: symbolic string method must have one symbolic operand: HandleStartsWith");
		} else {
			ChoiceGenerator<?> cg;
			boolean conditionValue;

			cg = th.getVM().getChoiceGenerator();
			assert (cg instanceof PCChoiceGenerator) : "expected PCChoiceGenerator, got: " + cg;
			conditionValue = (Integer) cg.getNextChoice() == 0 ? false : true;

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
				if (sym_v1 != null) {
					if (sym_v2 != null) { // both are symbolic values
						pc.spc._addDet(comp, sym_v1, sym_v2);
					} else {
						ElementInfo e2 = th.getElementInfo(s2);
						String val = e2.asString();
						pc.spc._addDet(comp, sym_v1, val);
					}
				} else {
					ElementInfo e1 = th.getElementInfo(s1);
					String val = e1.asString();
					pc.spc._addDet(comp, val, sym_v2);
				}
				if (!pc.simplify()) {// not satisfiable
					th.getVM().getSystemState().setIgnored(true);
				} else {
					// pc.solve();
					((PCChoiceGenerator) cg).setCurrentPC(pc);
					// System.out.println(((PCChoiceGenerator) cg).getCurrentPC());
				}
			} else {
				if (sym_v1 != null) {
					if (sym_v2 != null) { // both are symbolic values
						pc.spc._addDet(comp.not(), sym_v1, sym_v2);
					} else {
						ElementInfo e2 = th.getElementInfo(s2);
						String val = e2.asString();
						pc.spc._addDet(comp.not(), sym_v1, val);

					}
				} else {
					ElementInfo e1 = th.getElementInfo(s1);
					String val = e1.asString();
					pc.spc._addDet(comp.not(), val, sym_v2);
				}
				if (!pc.simplify()) {// not satisfiable
					th.getVM().getSystemState().setIgnored(true);
				} else {
					((PCChoiceGenerator) cg).setCurrentPC(pc);
				}
			}
			sf.push(conditionValue ? 0 : -1, true);
		}
	}
}
