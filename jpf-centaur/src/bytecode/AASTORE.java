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

import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class AASTORE extends gov.nasa.jpf.jvm.bytecode.AASTORE {

	 @Override
	  public Instruction execute (ThreadInfo ti) {
		 
		 if (peekIndexAttr(ti)==null || !(peekIndexAttr(ti) instanceof IntegerExpression))
			  return super.execute(ti);
		  int arrayref = peekArrayRef(ti); // need to be polymorphic, could be LongArrayStore
		      
		  if (arrayref == MJIEnv.NULL) {
		        return ti.createAndThrowException("java.lang.NullPointerException");
		  } 
		  
		  //throw new RuntimeException("Arrays: symbolic index not handled");
		  
		  //we do not handle this situation; we ignore it.  
		  StackFrame frame = ti.getModifiableTopFrame();		  
		  frame.pop(3);
		  
		  return getNext(ti);  
			
	 }

}
