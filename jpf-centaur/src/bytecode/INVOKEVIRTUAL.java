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
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils.InstructionOrSuper;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.string.SymbolicStringBuilder;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import migration.Migrate;
import queryer.Query;


public class INVOKEVIRTUAL extends gov.nasa.jpf.jvm.bytecode.INVOKEVIRTUAL {
	public INVOKEVIRTUAL(String clsName, String methodName, String methodSignature) {
	    super(clsName, methodName, methodSignature);
	}
	
	@Override
	public Instruction execute(ThreadInfo th) {
		int objRef = th.getCalleeThis(getArgSize());

		if ((objRef == -1) || (objRef == MJIEnv.NULL)) {
		      lastObj = -1;
		      return th.createAndThrowException("java.lang.NullPointerException", "Calling '" + mname + "' on null object");
		}
		
		boolean isSkeleton = false;
		
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
			ElementInfo ei = th.getModifiableElementInfo(objRef); 
			if (ei.getClassInfo().toString().contains("AsyncChannel")) {
				newJI = HandleAsyncMessage(th);
				System.out.println("newJI inst: " + newJI.toString());				
			} else {			
				newJI = HandleSyncMessage(th);
				System.out.println("newJI inst: " + newJI.toString());
			}
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
    		String cls = (ci != null) ? ci.getName() : "?UNKNOWN?";
    		return th.createAndThrowException("java.lang.NoSuchMethodError", cls + '.' + mname);
    	}
    	
	    String name = mi.getName();  
	    String clsName = mi.getClassName(); 
	    String[] argTypes = mi.getArgumentTypeNames();
		int argSize = argTypes.length; 
	    if (clsName.contains("android.os.Handler") && name.equals("sendMessage")) {    	
	    	newJI = HandleSyncMessage(th);
	    	System.out.println("newJI inst: " + newJI.toString());
	    	BytecodeUtils.InstructionOrSuper nextInstr = BytecodeUtils.execute(newJI,  th);			
			if (nextInstr.callSuper) {
		        return super.execute(th);
		    } else {
		        return nextInstr.inst;
		    }
	    }
	    if (clsName.contains("com.android.internal.util.AsyncChannel") 
	    		&& name.equals("sendMessageSynchronously") && (argSize == 1)
	    		&& argTypes[0].contains("android.os.Message")) {    	
	    	newJI = HandleAsyncMessage(th);
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
    	if ((mi.toString().contains("java.util.HashSet.contains")) ||
	    		mi.toString().contains("java.util.HashMap.containsKey")) {
	    	StackFrame sf = th.getModifiableTopFrame();
	    	int numStackSlots = this.getArgSize();
	    	for (int i = 0; i < numStackSlots; i++) {
	    		int ref = sf.peek(i);
	    		if ((ref == -1) || (ref == 0)) continue;
	    		ElementInfo ei = th.getElementInfo(ref);
	    		Object argumentAttr = ei.getObjectAttr();
	    		if ((argumentAttr != null) && argumentAttr.toString().contains(KeyWords.SkeletonAttr)) {
	    			isSkeleton = true;
	    		}
	    	}
	    }

	    if (isSkeleton) {
	    	Instruction nextInstr = SkeletonExecute.handleSkeleton(this,  th);    	
	        return nextInstr;
	    } else {
			BytecodeUtils.InstructionOrSuper nextInstr = CentaurBytecodeUtils.execute(newJI,  th);			
			if (nextInstr.callSuper) {
	            return super.execute(th);
	        } else {
	            return nextInstr.inst;
	        }
	    }
    }
	
	
	/*
     * for handle: send message
     * MethodInfo[android.os.Handler$MessengerImpl.send(Landroid/os/Message;)V]
     */
	private JVMInvokeInstruction HandleSyncMessage (ThreadInfo th) {
		System.out.println("the method: android.os.Handler.sendMessage; we redirect the method");               
		
		JVMInvokeInstruction newJI = null;
		int objRef = th.getCalleeThis(getArgSize());
		ElementInfo ei = th.getModifiableElementInfo(objRef);   //mSmHandler --> search field name: mStateStack
	    			
		OVHeap heap = (OVHeap)th.getHeap();
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

	
	/*
     * for handle: 
     * com.android.internal.util.AsyncChannel.sendMessageSynchronously(Landroid/os/Message;)Landroid/os/Message
     */
	private JVMInvokeInstruction HandleAsyncMessage (ThreadInfo th) {
		System.out.println("the method: AsyncChannel.sendMessageSynchronously; we redirect the method");               
		
		JVMInvokeInstruction newJI = null;
		int objRef = th.getCalleeThis(getArgSize());
		ElementInfo ei = th.getModifiableElementInfo(objRef);   //mWifiStateMachineChannel

		OVHeap heap = (OVHeap)th.getHeap();
		boolean isStateMachine = true;
		
		if (isStateMachine) {
			//first we need to find mSmHandler within mWifiStateMachine
			//here, we use another reference relationship to find mTarget.this, 
			//which also points to the object pointed by mWifiStateMachine.mSmHandler 
			int dstref = ei.getReferenceField("mDstMessenger"); 
			//mDstMessenger has not created and migrated. we need to create and migrate it first  
			int[] reff = new int[1];
			reff[0] = dstref;
			String fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff);                
			dstref = reff[0];
			if (!fieldClass.equals("0")) {
				int symRef = Migrate.MigrateField(fieldClass, th, dstref, ei.getObjectAttr().toString());
				ei.setReferenceField("mDstMessenger", symRef);
				ElementInfo eiField = (ElementInfo) th.getModifiableElementInfo(symRef); 
				eiField.setObjectAttr(ei.getObjectAttr().toString());              	  
				heap.AddMigratedField(dstref, symRef);  
			}
			
			//find mDstMessenger.mTarget 
			dstref = ei.getReferenceField("mDstMessenger");  
			ElementInfo eidst = th.getModifiableElementInfo(dstref); 
			int targetref = eidst.getReferenceField("mTarget");  
			reff[0] = targetref;
			fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff); 
			targetref = reff[0];
			if (!fieldClass.equals("0")) {
				int symRef = Migrate.MigrateField(fieldClass, th, targetref, ei.getObjectAttr().toString());
				eidst.setReferenceField("mTarget", symRef);
				ElementInfo eiField = (ElementInfo) th.getModifiableElementInfo(symRef); 
				eiField.setObjectAttr(ei.getObjectAttr().toString());              	  
				heap.AddMigratedField(targetref, symRef);  
			}
			
			//find mTarget.this$0, which points to the object pointed by mWifiStateMachine.mSmHandler 
			targetref = eidst.getReferenceField("mTarget"); 
			ElementInfo eitarget = th.getModifiableElementInfo(targetref); 
			int thisref = eitarget.getReferenceField("this$0");                            
			reff[0] = thisref;
			fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff);
			thisref = reff[0];
			if (!fieldClass.equals("0")) {
				int symRef = Migrate.MigrateField(fieldClass, th, thisref, ei.getObjectAttr().toString());
				eitarget.setReferenceField("this$0", symRef);
				ElementInfo eiField = (ElementInfo) th.getModifiableElementInfo(symRef); 
				eiField.setObjectAttr(ei.getObjectAttr().toString());              	  
				heap.AddMigratedField(thisref, symRef);  
			}
			
			//now, we find mSmHandler
			thisref = eitarget.getReferenceField("this$0"); 
			ei = th.getModifiableElementInfo(thisref); 
			int ssRef = ei.getReferenceField("mStateStack"); 
			int topIndex = ei.getIntField("mStateStackTopIndex");
		
			//mStateStack has not created and migrated. we need to create and migrate it first 
			reff[0] = ssRef;
			fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff);  
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
               
			//force it go to ConnectModeState
			curStateRef = 316492704;
            
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
			setInvokedMethod(fieldClass, "processMessage", "(Landroid/os/Message;)Z");       
			newJI = new INVOKEVIRTUAL(fieldClass, "processMessage", "(Landroid/os/Message;)Z");
                
			int[] st = th.getModifiableTopFrame().getSlots(); 
			int top = th.getModifiableTopFrame().getTopPos();			
			st[top-1] = curStateRef;      
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
