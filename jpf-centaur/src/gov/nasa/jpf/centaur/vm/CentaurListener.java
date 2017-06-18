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

package gov.nasa.jpf.centaur.vm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DynamicElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.DRETURN;
import gov.nasa.jpf.jvm.bytecode.FRETURN;
import gov.nasa.jpf.jvm.bytecode.IRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.LRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.bytecode.INVOKESTATIC;
import gov.nasa.jpf.symbc.concolic.PCAnalyzer;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.numeric.SymbolicConstraintsGeneral;

import gov.nasa.jpf.util.Pair;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import bootstrap.KeyWords;


public class CentaurListener extends gov.nasa.jpf.symbc.SymbolicListener {
   
	public CentaurListener(Config conf, JPF jpf) {
		super(conf, jpf);
		// TODO Auto-generated constructor stub
	}

	 private static String pre_spcString = "";
	 
	@Override
	public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {

		if (!vm.getSystemState().isIgnored()) {
			Instruction insn = executedInstruction;	
			
			/******************************************/
			/******************************************/
			/******************************************/
			/*
			 * when we reach the targeted code, we output the path condition
			 */			
		    String curSource = insn.getSourceOrLocation();		    
		    boolean reach = false;
		    Config conf = vm.getConfig();
		    boolean setTargetedLine = conf.containsKey("targeted.code");
		    if (setTargetedLine) {
		    	String[] targetedCode = conf.getStringArray("targeted.code");
		    	
		    	for (int i = 0; i < targetedCode.length; ++i) {
		    		if (curSource.contains(targetedCode[i])) {
		    			reach = true;
		    			break;
		    		}
		    	}
		    }
		    
		    if (reach) { //targeted code
		    	System.out.println("in jpf-centaur we reach targeted line of code :"+curSource);
                OVHeap heap = (OVHeap) currentThread.getHeap();
		        ChoiceGenerator <?>cg = vm.getChoiceGenerator();
                if (!(cg instanceof PCChoiceGenerator)){
                    ChoiceGenerator <?> prev_cg = cg.getPreviousChoiceGenerator();
                    while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
                        prev_cg = prev_cg.getPreviousChoiceGenerator();
                    }
                    cg = prev_cg;
                }
                if ((cg instanceof PCChoiceGenerator) &&(
                        (PCChoiceGenerator) cg).getCurrentPC() != null){
                    PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();                  
                    //System.out.println(pc.toString());
                    if (!pre_spcString.equals(pc.spc.toString())) {                        
                        System.out.println("reach targeted line of code");
                        //System.out.println("PC is: " + pc.toString());                       
                        System.out.println("PC.spc is: " + pc.spc.toString()); 
                        
                        /*ChoiceGenerator<?> thisHeapCG = currentThread.getVM().getSystemState().getCurrentChoiceGeneratorOfType(HeapChoiceGenerator.class);                        
                        if (thisHeapCG != null) {                       
                            PathCondition pcHeap = ((HeapChoiceGenerator)thisHeapCG).getCurrentPCheap();
                            System.out.println("PC heap is:"+pcHeap);
                        }*/                                                         
                        System.out.println("end print PC");                       
                        pre_spcString = pc.spc.toString();
                    }                              
                }
                //since we reach the targeted code, we do not need to continue the execution
                //we stop the current path here. ????
                vm.getSystemState().setIgnored(true);
		    }
		    /******************************************/
            /******************************************/
            /******************************************/
		    
		    
			ThreadInfo ti = currentThread;

			if (insn instanceof JVMInvokeInstruction) {
				super.instructionExecuted(vm, currentThread, nextInstruction, executedInstruction);
				
			} else if (insn instanceof JVMReturnInstruction){
				MethodInfo mi = insn.getMethodInfo();
				ClassInfo ci = mi.getClassInfo();
				if (null != ci){
					String className = ci.getName();
					String methodName = mi.getName();
					String longName = mi.getLongName();
					int numberOfArgs = mi.getNumberOfArguments();
					
					if (((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
							|| BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null))){
					
						ChoiceGenerator <?>cg = vm.getChoiceGenerator();
						if (!(cg instanceof PCChoiceGenerator)){
							ChoiceGenerator <?> prev_cg = cg.getPreviousChoiceGenerator();
							while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
								prev_cg = prev_cg.getPreviousChoiceGenerator();
							}
							cg = prev_cg;
						}
						if ((cg instanceof PCChoiceGenerator) &&(
								(PCChoiceGenerator) cg).getCurrentPC() != null){
							PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
							if (SymbolicInstructionFactory.concolicMode) { //TODO: cleaner
								SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
								PCAnalyzer pa = new PCAnalyzer();
								pa.solve(pc,solver);
							}
							else {
								pc.solve();
							}
							
							if (!PathCondition.flagSolved) {
							  return;
							}

							String pcString = pc.toString();//pc.stringPC();
							Pair<String,String> pcPair = null;

							String returnString = "";
							Expression result = null;

							if (insn instanceof IRETURN){
								IRETURN ireturn = (IRETURN)insn;
								int returnValue = ireturn.getReturnValue();
								IntegerExpression returnAttr = (IntegerExpression) ireturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}else{ // concrete
									returnString = "Return Value: " + String.valueOf(returnValue);
									result = new IntegerConstant(returnValue);
								}
							}
							else if (insn instanceof LRETURN) {
								LRETURN lreturn = (LRETURN)insn;
								long returnValue = lreturn.getReturnValue();
								IntegerExpression returnAttr = (IntegerExpression) lreturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}else{ // concrete
									returnString = "Return Value: " + String.valueOf(returnValue);
									result = new IntegerConstant((int)returnValue);
								}
							}
							else if (insn instanceof DRETURN) {
								DRETURN dreturn = (DRETURN)insn;
								double returnValue = dreturn.getReturnValue();
								RealExpression returnAttr = (RealExpression) dreturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}else{ // concrete
									returnString = "Return Value: " + String.valueOf(returnValue);
									result = new RealConstant(returnValue);
								}
							}
							else if (insn instanceof FRETURN) {							
								FRETURN freturn = (FRETURN)insn;
								double returnValue = freturn.getReturnValue();
								RealExpression returnAttr = (RealExpression) freturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}else{ 
									returnString = "Return Value: " + String.valueOf(returnValue);
									result = new RealConstant(returnValue);
								}

							}
							else if (insn instanceof ARETURN){
								ARETURN areturn = (ARETURN)insn;
								IntegerExpression returnAttr = (IntegerExpression) areturn.getReturnAttr(ti);
								if (returnAttr != null){
									returnString = "Return Value: " + String.valueOf(returnAttr.solution());
									result = returnAttr;
								}
								else {// concrete
									DynamicElementInfo val = (DynamicElementInfo)areturn.getReturnValue(ti);								    
									if (val != null) {
										// 用element's classInfo来判定．如果是ＨashMap则直接返回它的HashMap.objRef
										ClassInfo cival = val.getClassInfo();
										if (!ClassInfo.isStringClassInfo(cival)) {
											String clsName = val.getClassInfo().toString(); 
											clsName = clsName.substring(15, clsName.length()-1);
											int objRef = val.getObjectRef();
											returnString = "Return Value: " + clsName + "." + objRef;
											String tmp = clsName + "." + objRef;
										    tmp = tmp.substring(tmp.lastIndexOf('.')+1);
										    result = new SymbolicInteger(tmp);
										} else {
											returnString = "Return Value: " + val.asString();						   
										    String tmp = val.asString();
										    tmp = tmp.substring(tmp.lastIndexOf('.')+1);
										    result = new SymbolicInteger(tmp);
										}
									} else {
									    returnString = "Return Value: NULL";
									}							
								}							
							}							
							else {
								returnString = "Return Value: --";
							}
														
							if (SymbolicInstructionFactory.debugMode) {
							    System.out.println("*************Summary***************");
							    System.out.println("PC is:"+pc.toString());
							    System.out.println("PC.spc is:"+pc.spc.toString());                       
							    if(result!=null){
							    	System.out.println("Return is:  "+result);
							    	System.out.println("***********************************");
							    }
							}
						}
					}
				}
			}
		}
	}
}
