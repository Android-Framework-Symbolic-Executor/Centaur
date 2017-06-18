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
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class IALOAD extends gov.nasa.jpf.jvm.bytecode.IALOAD {
	@Override
	  public Instruction execute (ThreadInfo ti) {
		
		  if (peekIndexAttr(ti)==null || !(peekIndexAttr(ti) instanceof IntegerExpression))
			  return super.execute(ti);
		  
		  StackFrame frame = ti.getModifiableTopFrame();
		  arrayRef = frame.peek(1); // ..,arrayRef,idx
		  
		  if (arrayRef == MJIEnv.NULL) {
		      return ti.createAndThrowException("java.lang.NullPointerException");
		  }

		  //throw new RuntimeException("Arrays: symbolic index not handled");
		  //handle: symbolic index
		  Expression sym = null;
		  
		  String symName = arrayRef + "_indexof_(" + peekIndexAttr(ti).toString() + ")";
          System.out.println("symName: " + symName);
          
		  sym = new SymbolicInteger(symName); 

		  frame.pop(2); // now we can pop index and array reference	  

          frame.pushRef(0);
          frame.setOperandAttr(sym);

          return getNext(ti);	  
		    
	  }
}
