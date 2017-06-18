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
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class IFGE extends gov.nasa.jpf.symbc.bytecode.IFGE {
	public IFGE(int targetPosition){
	    super(targetPosition);
	  }
	@Override
	public Instruction execute (ThreadInfo ti) {

		StackFrame sf = ti.getModifiableTopFrame();
		IntegerExpression sym_v = (IntegerExpression) sf.getOperandAttr();

		if ((sym_v != null) && sym_v.toString().substring(0, KeyWords.SkeletonAttr.length()).equals(KeyWords.SkeletonAttr)) {
			sf.setOperandAttr(null);
		}

		return super.execute(ti);
		
	}
}
