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

import queryer.Query;
import bootstrap.KeyWords;
import gov.nasa.jpf.centaur.vm.CentaurClassInfo;
import gov.nasa.jpf.centaur.vm.OVHeap;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import peers.JPF_android_os_Binder;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ReferenceFieldInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


public class INVOKESTATIC extends gov.nasa.jpf.symbc.bytecode.INVOKESTATIC {
	public INVOKESTATIC(String clsName, String methodName, String methodSignature) {
	    super(clsName, methodName, methodSignature);
	  }
	@Override
	public Instruction execute(ThreadInfo th) {
		ClassInfo clsInfo = getClassInfo();
	    if (clsInfo == null){
	      return th.createAndThrowException("java.lang.NoClassDefFoundError", cname);
	    }

	    MethodInfo callee = getInvokedMethod(th);  //name: getCallingUid; ci: ClassInfo[name=BI]; MethodInfo[BI.getCallingUid()I]
	    
	    if (callee == null) {
	       return th.createAndThrowException("java.lang.NoSuchMethodException!!",
	                                        cname + '.' + mname);
	    }
	    
	    //Check whether the class contains in the heap file of android framework
	    String clsName = clsInfo.toString();  
	    clsName = clsName.substring(15, clsName.length()-1);
	    if (Query.classObjRefMap.containsKey(clsName)) { 
	    	if (CentaurClassInfo.initializeClass(clsInfo, th)) {
	    		return th.getPC();
	    	}
	    } else {
	    	return super.execute(th);
	    }
	    	       
	    //note that for invokestatic, we do not need to sf.pop(); (for invokevirtual we need to)

        return super.execute(th);
       
    }
	
	 @Override
	  public MethodInfo getInvokedMethod (ThreadInfo ti){
	    if (invokedMethod == null) {
	      ClassInfo clsInfo = getClassInfo(); 
	      if (clsInfo != null){ 
	          
	        /*
	         * for handling arraycopy([II[III)V (in android framework)
	         */
	        String clsName = clsInfo.toString();  // ClassInfo[name=testCIs]       
	        clsName = clsName.substring(15, clsName.length()-1);
	        if (clsName.equals("java.lang.System") && mname.equals("arraycopy([II[III)V")) {
	            mname = "arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V";
	        } 
	 
	        MethodInfo callee = clsInfo.getMethod(mname, true);
	        if (callee != null){
	          ClassInfo ciCallee = callee.getClassInfo(); // might be a superclass of ci, i.e. not what is referenced in the insn

	          if (!ciCallee.isRegistered()){
	            // if it wasn't registered yet, classLoaded listeners didn't have a chance yet to modify it..
	            ciCallee.registerClass(ti);
	            // .. and might replace/remove MethodInfos
	            callee = clsInfo.getMethod(mname, true);
	          }
	          invokedMethod = callee;
	        }
	      }    
	    }
	    return invokedMethod;
	  }
	 
}

