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

import bootstrap.KeyWords;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;
import gov.nasa.jpf.vm.NativeStackFrame;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class JPF_java_util_BitSet extends NativePeer {
	@MJI
	public int cardinality____I (MJIEnv env, int objRef) {
		ThreadInfo ti = env.getThreadInfo();		
		ElementInfo ei = ti.getModifiableElementInfo(objRef);
		FieldInfo fi = ei.getFieldInfo(2);
		Object sym = ei.getFieldAttr(fi);

		if (sym != null) {
			String symName = sym.toString() + "_numOfTrueBits";
			IntegerExpression new_sym = new SymbolicInteger(symName);
			env.setReturnAttribute(new_sym);
			return 0;
		} else {			
			int size = ei.getIntField("longCount");
			int bitRef = ei.getReferenceField("bits");
			ElementInfo eiBit = ti.getModifiableElementInfo(bitRef);			
			if (eiBit == null) return 0;			
			int ret = 0;
			for (int i = 0; i < size; ++ i) {
				long bit = eiBit.getLongElement(i);
				ret += BitCount(bit);
			}			
			return ret;
		}
	}
	
	@MJI
	public boolean get__I__Z(MJIEnv env, int objRef, int index) {
        if (index < 0) { // TODO: until we have an inlining JIT.
        	throw new IndexOutOfBoundsException("index < 0: " + index);
        }
        int arrayIndex = index / 64;
        
        ThreadInfo ti = env.getThreadInfo();
        ElementInfo ei = ti.getModifiableElementInfo(objRef);
        FieldInfo fi = ei.getFieldInfo(2);
		Object sym = ei.getFieldAttr(fi);
		
		if (sym != null) {
			String symName = sym.toString() + "_indexof_"+index;
			IntegerExpression new_sym = new SymbolicInteger(symName);
			env.setReturnAttribute(new_sym);
			return false;
		} else {
			int longCount = ei.getIntField("longCount");
			int bitRef = ei.getReferenceField("bits");
			ElementInfo eiBit = ti.getModifiableElementInfo(bitRef);	
			if (arrayIndex >= longCount) {
				return false;
			}
			long bit = eiBit.getLongElement(arrayIndex);
			return (bit & (1L << index)) != 0;
		}
    }
	
	
	
	public static int BitCount(long v) {
        // Combines techniques from several sources
        v -=  (v >>> 1) & 0x5555555555555555L;
        v = (v & 0x3333333333333333L) + ((v >>> 2) & 0x3333333333333333L);
        int i =  ((int)(v >>> 32)) + (int) v;
        i = (i & 0x0F0F0F0F) + ((i >>> 4) & 0x0F0F0F0F);
        i += i >>> 8;
        i += i >>> 16;
        return i  & 0x0000007F;
    }
}

