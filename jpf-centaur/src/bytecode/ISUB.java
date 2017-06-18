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
import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class ISUB extends gov.nasa.jpf.symbc.bytecode.ISUB {

	@Override
	public Instruction execute (ThreadInfo th) {

		StackFrame sf = th.getModifiableTopFrame();

	    IntegerExpression sym_v1 = (IntegerExpression) sf.getOperandAttr(0);
	    IntegerExpression sym_v2 = (IntegerExpression) sf.getOperandAttr(1);
	 
	    // if the index is "skeleton", we do special analysis
        // we compute the correct result, and then also assign "skeleton" attr for the result 
	    if ((sym_v2 != null) && sym_v2.toString().substring(0, KeyWords.SkeletonAttr.length()).equals(KeyWords.SkeletonAttr)) {
            int v1 = sf.pop();
            int v2 = sf.pop();
            sf.push(v2 - v1);
            IntegerExpression sym = new SymbolicInteger(KeyWords.SkeletonAttr);
            sf.setOperandAttr(sym);           
            return getNext(th);
        }
	    else {
        	return super.execute( th);
        }
	}
}
