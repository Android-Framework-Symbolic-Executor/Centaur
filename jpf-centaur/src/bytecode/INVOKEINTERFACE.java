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
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import migration.Migrate;
import queryer.Query;

public class INVOKEINTERFACE extends gov.nasa.jpf.jvm.bytecode.INVOKEINTERFACE {
	public INVOKEINTERFACE(String clsName, String methodName, String methodSignature) {
	    super(clsName, methodName, methodSignature);
	}
	
	@Override
	public Instruction execute(ThreadInfo th) {
		int objRef = th.getCalleeThis(getArgSize());

	    if ((objRef == -1) || (objRef == 0)) {
	    	lastObj = -1;
	    	return th.createAndThrowException("java.lang.NullPointerException", "Calling '" + mname + "' on null object");
	    }

	    JVMInvokeInstruction newJI = this;
	    
		/*
		 * because we modified cname and mname in the previous path (at the first time) involving "statemachine.sendmessage" calls,
		 * the invocation is automatically replaced by "state.processmessage" in the second path involving "statemachine.sendmessage" call, 
		 * resulting in the error that "processmessage" cannot be found.
		 * thus, in this (second) time, we do not need to find method by getInvokedMethod(th, objRef)
		 * we directly go to our HandleStateMachine
		 */
		String cname = newJI.getInvokedMethodClassName();
		String mname = newJI.getInvokedMethodName();
		if (cname.contains("StateMachine") && mname.contains("processMessage")) {
			newJI = HandleSyncMessage(th);
			System.out.println("newJI inst: " + newJI.toString());
			BytecodeUtils.InstructionOrSuper nextInstr = BytecodeUtils.execute(newJI,  th);			
			if (nextInstr.callSuper) {
	            return super.execute(th);
	        } else {
	            return nextInstr.inst;
	        }
		}
		if (cname.contains("Handler") && mname.contains("handleMessage")) {
			newJI = HandleSyncMessage(th);
			System.out.println("newJI inst: " + newJI.toString());
			BytecodeUtils.InstructionOrSuper nextInstr = BytecodeUtils.execute(newJI,  th);			
			if (nextInstr.callSuper) {
	            return super.execute(th);
	        } else {
	            return nextInstr.inst;
	        }
		}	
        
	    MethodInfo mi = getInvokedMethod(th, objRef); 	   
	    if (mi == null) {
		    ClassInfo ci = th.getClassInfo(objRef);
		    String clsName = (ci != null) ? ci.getName() : "?UNKNOWN?";
		    return th.createAndThrowException("java.lang.NoSuchMethodError", clsName + '.' + mname);
		}
	    

	    /*
	     * for handle: 
	     * Messenger m = ns.getMessenger();
	     * m.send(message);
	     */	    
	    String name = mi.getName();  
	    String clsName = mi.getClassName(); 
	    if (clsName.contains("android.os.Handler$MessengerImpl") && name.equals("send")) {    	
	    	newJI = HandleSyncMessage(th);
	    	System.out.println("newJI inst: " + newJI.toString());
	    	BytecodeUtils.InstructionOrSuper nextInstr = BytecodeUtils.execute(newJI,  th);			
			if (nextInstr.callSuper) {
		        return super.execute(th);
		    } else {
		        return nextInstr.inst;
		    }
	    }
	    
	    
	    /*
	     * set taint sink
	     */
	    boolean isSkeleton = false;
	    if ((mi.toString().contains("java.util.HashSet.contains")) ||
	    		mi.toString().contains("java.util.HashMap.containsKey")) {
	    	StackFrame sf = th.getModifiableTopFrame();
			int numStackSlots = this.getArgSize();
			for (int i = 0; i < numStackSlots; i++) {
				int ref = sf.peek(i);
                if ((ref == -1) || (ref == 0)) continue;
                ElementInfo ei = th.getElementInfo(ref);
                Object argumentAttr = ei.getObjectAttr();
                if ((argumentAttr != null) && (argumentAttr.toString().contains(KeyWords.SkeletonAttr))) {
                	isSkeleton = true;
                }
			}
	    }
	    
	    if (isSkeleton) {
	    	Instruction nextInstr = SkeletonExecute.handleSkeleton(this,  th);    	
	        return nextInstr;
	    } else {
			BytecodeUtils.InstructionOrSuper nextInstr = BytecodeUtils.execute(this,  th);			
			if (nextInstr.callSuper) {
	            return super.execute(th);
	        } else {
	            return nextInstr.inst;
	        }
	    }
    }
	
	
	
	/*
     * for handle: messenger.send(message)
     */
	private JVMInvokeInstruction HandleSyncMessage (ThreadInfo th) {
		System.out.println("the method: android.os.Handler$MessengerImpl; we redirect the method");               
		
		JVMInvokeInstruction newJI = null;
		int objRef = th.getCalleeThis(getArgSize());
		OVHeap heap = (OVHeap)th.getHeap();
		
		//we need to find the object whose type is com.android.internal.util.StateMachine$SmHandler and has a field with the reference value of objRef
		ElementInfo ei = heap.FindOwnerElement("mMessenger", objRef);  //mSmHandler --> search field name: mStateStack
		
		boolean isStateMachine = false;
		
		if (ei.getClassInfo().toString().contains("com.android.internal.util.StateMachine")) {
			isStateMachine = true;
		}
		
		if (isStateMachine) {
			//state machine handler
			int ssRef = ei.getReferenceField("mStateStack"); 
			int topIndex = ei.getIntField("mStateStackTopIndex");
		
			//mStateStack has not created and migrated. we need to create and migrate it first                             
			int[] reff = new int[1];
      	    reff[0] = ssRef;
			String fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff); 
			ssRef = reff[0];
			if (!fieldClass.equals("0")) {
				int symRef = Migrate.MigrateField(fieldClass, th, ssRef, ei.getObjectAttr().toString());
				ei.setReferenceField("mStateStack", symRef);
				ElementInfo eiField = (ElementInfo) th.getModifiableElementInfo(symRef); 
				eiField.setObjectAttr(ei.getObjectAttr().toString());              	  
				heap.AddMigratedField(ssRef, symRef);  
			}
                
			//3. process on mStateStack, which is an array. 
			ssRef = ei.getReferenceField("mStateStack"); 	
			ElementInfo eissArray = th.getModifiableElementInfo(ssRef);  
			int curStateInfoRef = eissArray.getReferenceElement(topIndex);
			
			//the element within mStateStack has not created and migrated. we need to create and migrate it first                             

      	    reff[0] = curStateInfoRef;		
			fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff);  
			curStateInfoRef = reff[0];
			if (!fieldClass.equals("0")) {
				int symRef = Migrate.MigrateField(fieldClass, th, curStateInfoRef, ei.getObjectAttr().toString());
				eissArray.setReferenceElement(topIndex, symRef);
				ElementInfo eiField = (ElementInfo) th.getModifiableElementInfo(symRef); 
				eiField.setObjectAttr(ei.getObjectAttr().toString());              	  
				heap.AddMigratedField(curStateInfoRef, symRef);  
			}
            
			//4. find "state" field in curStateInfo object
			curStateInfoRef = eissArray.getReferenceElement(topIndex);
			ElementInfo eiStateInfo = th.getModifiableElementInfo(curStateInfoRef);  
			int curStateRef = eiStateInfo.getReferenceField("state"); 
               
			//we also need to create and migrate the object corresponding to curStateRef; 
			//otherwise, when getinvokedmethod is executed, nullpointer (classinfo is null) will incur. 
			//now: sstype: com.android.server.NsdService$NsdStateMachine$EnabledState
			reff[0] = curStateRef;
			fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff); 
			curStateRef = reff[0];
			System.out.println("current state: " + fieldClass); 
			if (!fieldClass.equals("0")) {
				int symRef = Migrate.MigrateField(fieldClass, th, curStateRef, ei.getObjectAttr().toString());
				eiStateInfo.setReferenceField("state", symRef);
				ElementInfo eiField = (ElementInfo) th.getModifiableElementInfo(symRef); 
				eiField.setObjectAttr(ei.getObjectAttr().toString());              	  
				heap.AddMigratedField(curStateRef, symRef);  
			}
            	
			//next: modify classinfo and methodinfo, to let sendmessage direct to processmessage of the state
			//now, we modify "this" (instruction): slots, and create a new INVOKEVIRTUAL instruction.  
			curStateRef = eiStateInfo.getReferenceField("state");
			if(fieldClass.equals("0")) {
				int conRef = heap.FindConcreteRef(curStateRef);
				reff[0] = conRef;
				fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff); 
				conRef = reff[0];
			}
			setLastObj(curStateRef);	

			ClassInfo cci = th.getClassInfo(curStateRef);
			MethodInfo newMethod = cci.getMethod("processMessage(Landroid/os/Message;)Z", true);
			invokedMethod = newMethod;
			setInvokedMethod(fieldClass, "processMessage", "(Landroid/os/Message;)Z");    
			
			newJI = new INVOKEVIRTUAL(fieldClass, "processMessage", "(Landroid/os/Message;)Z");
                
			int[] st = th.getModifiableTopFrame().getSlots(); 
			int top = th.getModifiableTopFrame().getTopPos();
			st[top-1] = curStateRef;      
		}
		// handler 
		else {
			int handlerRef = ei.getObjectRef();		
			setLastObj(handlerRef);	
			
			String fieldClass = ei.getClassInfo().toString();
			fieldClass = fieldClass.substring(15, fieldClass.length()-1);
			ClassInfo cci = th.getClassInfo(handlerRef);
			MethodInfo newMethod = cci.getMethod("handleMessage(Landroid/os/Message;)V", true);
			invokedMethod = newMethod;
			setInvokedMethod(fieldClass, "handleMessage", "(Landroid/os/Message;)V");  
			
			newJI = new INVOKEVIRTUAL(fieldClass, "handleMessage", "(Landroid/os/Message;)V");
                
			int[] st = th.getModifiableTopFrame().getSlots(); 
			int top = th.getModifiableTopFrame().getTopPos();
			st[top-1] = handlerRef; 
			
			
		}
		
	    return newJI;
	}

	
	private void setLastObj(int objRef) {
		if (objRef != MJIEnv.NULL) {
	        lastObj = objRef;	        
	      } else {
	        lastObj = MJIEnv.NULL;	        
	      }		
	}
}
