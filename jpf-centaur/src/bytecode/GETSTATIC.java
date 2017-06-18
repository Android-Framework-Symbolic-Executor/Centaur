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

import migration.Migrate;
import queryer.Query;
import bootstrap.KeyWords;
import gov.nasa.jpf.centaur.vm.CentaurClassInfo;
import gov.nasa.jpf.centaur.vm.HelperArray;
import gov.nasa.jpf.centaur.vm.OVHeap;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.heap.HeapNode;
import gov.nasa.jpf.symbc.heap.Helper;
import gov.nasa.jpf.symbc.heap.SymbolicInputHeap;
import gov.nasa.jpf.vm.BooleanFieldInfo;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DoubleFieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.FloatFieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.IntegerFieldInfo;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.LongFieldInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.string.SymbolicStringBuilder;


public class GETSTATIC extends gov.nasa.jpf.jvm.bytecode.GETSTATIC {
	public GETSTATIC(String fieldName, String clsName, String fieldDescriptor){
	    super(fieldName, clsName, fieldDescriptor);
	  }

	//private int numNewRefs = 0; // # of new reference objects to account for polymorphism -- work of Neha Rungta -- needs to be updated
	 
	boolean abstractClass = false;


	@Override
	public Instruction execute (ThreadInfo ti) {
		System.out.println("jpf-centaur, getstatic");
		ChoiceGenerator<?> prevHeapCG = null;
		HeapNode[] prevSymRefs = null;
		int numSymRefs = 0;
		
		Config conf = ti.getVM().getConfig();
		String[] lazy = conf.getStringArray("symbolic.lazy");
		if (lazy == null || !lazy[0].equalsIgnoreCase("true"))
			return super.execute(ti);

	    ClassInfo ciField;
	    FieldInfo fieldInfo;
	    
	    try {
	      fieldInfo = getFieldInfo(); 
	    } catch(LoadOnJPFRequired lre) {
	      return ti.getPC();
	    }
	 
	    if (fieldInfo == null) {
	      return ti.createAndThrowException("java.lang.NoSuchFieldError",
	          (className + '.' + fname));
	    }
		
		ciField = fieldInfo.getClassInfo();  
		
		String clsName = ciField.toString();  
	    clsName = clsName.substring(15, clsName.length()-1);
	    if (Query.classObjRefMap.containsKey(clsName)) { 
	    	//if ciField belongs to heap CentaurClassInfo...
	    	if (!mi.isClinit(ciField) && CentaurClassInfo.initializeClass(ciField, ti)) {
	    		return ti.getPC();
	    	}
	    } else {
	    	 if (!mi.isClinit(ciField) && ciField.initializeClass(ti)) {
	   	      // note - this returns the next insn in the topmost clinit that just got pushed
	   	      return ti.getPC();
	   	    }
	    }

	    ElementInfo ei = ciField.getModifiableStaticElementInfo();   
	    
	    if (ei == null){
	      throw new JPFException("attempt to access field: " + fname + " of uninitialized class: " + ciField.getName());
	    }
	    
        //only for bootstrapclass object
        //copy all the field reference ids for the eiFieldOwner whose class name equals bootStrappedClsName
        String fieldClsName = fieldInfo.getType(); // com.android.server.LocationManagerService              
        String[] bootStrappedClsName = conf.getStringArray("bootstrap.class");
        if (bootStrappedClsName == null) {
        	throw new JPFException("no bootstrap class is specified.");
        }
        
        if (fieldClsName.equals(bootStrappedClsName[0])) {
        	Query.QueryInitialize(ti);       	
            KeyWords.bootStrap = true;         
            ei.setObjectAttr(KeyWords.migrationAttr);
            System.out.println("bootStrappedClsName");
            int numField = ei.getNumberOfFields();
            for (int i = 0; i < numField; ++ i) {
                FieldInfo f = ei.getFieldInfo(i);             
                String type = f.getSignature();  
                String fName = f.getName();
                System.out.println("fieldinfo classinfo: " + type); 
               
                if ((type.equals("I"))||(type.equals("C")) || (type.equals("B")) 
                        || (type.equals("F")) || (type.equals("S")) || (type.equals("Z")) 
                        || (type.equals("J")) || (type.equals("D"))) {
                    System.out.println("cannot!!!!, bootstrapped static field should be class instance!!!");
                }
                else {       
                    int fRef = Query.QueryBootStrappedStaticFieldValue(type);      
                    int size = f.getStorageSize(); 
                    if (size == 1) {
                        ei.set1SlotField(f, fRef);   
                        //ei.setFieldAttr(f, KeyWords.migrationAttr);
                    } else {
                        ei.set2SlotField(f, fRef);
                        //ei.setFieldAttr(f, KeyWords.migrationAttr);
                    }
                }
            }
        }
             
	    Object migrateAttr = ei.getObjectAttr();	  
	    if (migrateAttr == null)
	          return  SymbcExecuteFirstCheckArray(ti);
	    
	    //--- check if this causes a class load by a user defined classloader
	    try {
	      fieldInfo = getFieldInfo();  
	    } catch (LoadOnJPFRequired lre) {
	      return ti.getPC();
	    }
	    
	    if (fieldInfo == null) {
	      return ti.createAndThrowException("java.lang.NoSuchFieldError",
	              (className + '.' + fname));
	    }

	    //--- check if this has to trigger class initialization
	    //Check whether the class contains in the heap file of android framework
	    String clsName2 = ciField.toString();  
	    clsName2 = clsName2.substring(15, clsName2.length()-1);
	    if (Query.classObjRefMap.containsKey(clsName2)) { 
	    	if (!mi.isClinit(ciField) && CentaurClassInfo.initializeClass(ciField, ti)) {
	    		return ti.getPC();
	    	}
	    } else {
	    	if (!mi.isClinit(ciField) && ciField.initializeClass(ti)) {
	  	      // note - this returns the next insn in the topmost clinit that just got pushed
	  	      return ti.getPC();
	  	    }
	    }
	    
	    //--- check if this breaks the transition
	    Scheduler scheduler = ti.getScheduler();
	    if (scheduler.canHaveSharedClassCG( ti, this, ei, fieldInfo)){
	    	ei = scheduler.updateClassSharedness(ti, ei, fieldInfo);
	      if (scheduler.setsSharedClassCG( ti, this, ei, fieldInfo)){
	        return this; // re-execute
	      }
	    }

	    StackFrame frame = ti.getModifiableTopFrame();
	    
	    //determine whether we need to migrate this field
	    //if this field is primitive type: do not need to migrate, we already copied its value
	    //for string: we still need to migrate; string is also reference type
	    OVHeap heap = (OVHeap)ti.getHeap();	      
		
		String fieldOwnerClsName = fieldInfo.getClassInfo().toString(); 
	    fieldOwnerClsName = fieldOwnerClsName.substring(15, fieldOwnerClsName.length()-1);
	    String fieldName = fieldInfo.getName(); // ci
	    System.out.println("GestStatic (jpf-symbc): " + fieldOwnerClsName + "." + fieldName);
	    System.out.println("Owner migrateAttr: " + migrateAttr);
	    String type = fieldInfo.getSignature();
	    System.out.println("fieldinfo classinfo: " + type); 
	    
	    int fieldRef = 0;
	    if (size == 1) {
	        fieldRef = ei.get1SlotField(fieldInfo);    
	    } else {
	        fieldRef = (int) ei.get2SlotField(fieldInfo);
	    }   
	    
	    if ((fieldRef != 0) && (!type.equals("I")) && (!type.equals("C")) && (!type.equals("B")) 
	            && (!type.equals("F")) && (!type.equals("S")) && (!type.equals("Z")) 
	            && (!type.equals("J")) && (!type.equals("D")) /*&& (!type.equals("Ljava.lang.String"))*/) {
	        //this field is reference: class/array/string
	        //now, we allocate a heap node for the new instance field object
	        //find the real type from snapshot
	        
	        //check whether the instance field has been initialized
	        boolean hasInitialized = false; 
	        
	        hasInitialized = heap.CheckMigrated(fieldRef);
	        System.out.println("test migration. hasInitialized? " + hasInitialized);
	    
	        ElementInfo eii = ti.getModifiableElementInfo(fieldRef);
	        
	        //only contain "migrated", we will migrate it
	        if (((eii == null) || !hasInitialized)  && KeyWords.bootStrap && (migrateAttr != "") 
	                && migrateAttr.toString().contains(KeyWords.migrationAttr)) {           
	           
	        	int[] reff = new int[1];
	        	reff[0] = fieldRef;
	            String fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff);
	            fieldRef = reff[0];
	            int symRef = Migrate.MigrateField(fieldClass, ti, fieldRef, migrateAttr.toString());
	        
	            int size = fi.getStorageSize(); 
	            if (size == 1) {
	                ei.set1SlotField(fi, symRef);   
	            } else {
	                ei.set2SlotField(fi, symRef);
	            }
	              
	            ElementInfo eiField = (ElementInfo) ti.getModifiableElementInfo(symRef); 
	            eiField.setObjectAttr(migrateAttr);
	              
	            heap.AddMigratedField(fieldRef, symRef);  // java.util.HashMap . table (????)
	            //need to set and remove in heap.elementInfos??
	        }  
	        
	        /*
	           * two references point to the same object
	           * if the first reference point to the object is migrated
	           * the second reference with the same value will be contained in con2Sym.
	           * we do not need to migrate this object. but instead we need to directly set 
	           * lastValue = con2Sym.getValue(reference)
	           * as well as set the eiOwner.field reference value = lastValue. 
	           */         
	          if (hasInitialized) {
	        	  boolean isConRef = heap.IsConRef(fieldRef);
	        	  if (isConRef) {
	        		  int symRef = Integer.parseInt(heap.GetSymRef(fieldRef));
	        		  if (fi.getStorageSize() == 1) { // 1 slotter
	        		      ei.set1SlotField(fi, symRef);             		        
	        		  } else {  // 2 slotter
	        		      ei.set2SlotField(fi, symRef);
	        		  }
	        	  }
	          }
	    }

	    if (size == 1) {
	        int ival = ei.get1SlotField(fieldInfo);    // use fieldInfo to find the field's objRef
	        lastValue = ival;
	        if (fieldInfo.isReference()) {
	            frame.pushRef(ival);
	        } else {
	            frame.push(ival);
	        }	      	      
	    } else {
	        long lval = ei.get2SlotField(fieldInfo);
	        lastValue = lval;	      
	        frame.pushLong(lval);
	    }  
	    
	    
	    /************************************/
        /************************************/
        /*
         * if fieldAttr contains "skeleton", and the field is primitive type or string, 
         * we set it as symbol
         */
        if ((migrateAttr != null) && (migrateAttr.toString().contains(KeyWords.SkeletonAttr))) {
        	System.out.println("fieldRef: " + fieldRef);

        	System.out.println("fieldInfo.getName: " + fi.getName());
            System.out.println("fieldInfo.getFullName: " + fieldInfo.getFullName());
            
            //need to modify: should use field-object's symbolic reference (i.e., lastValue) to represent this expression
            //e.g., a->c, b->c, both a and b CONTAINS the same fields. if we use a or b's reference, 
            //then the expression will be different, however, the expression should be the same 
            //as the return variables are the same: c.
            //String symName = fieldInfo.getFullName() + SkeletonApp.SkeletonExpressionSuffix;
            String symName = fi.getName() + "." + KeyWords.SkeletonExpressionSuffix;         
            System.out.println("symName: " + symName);
            
            Expression sym = null;
            
            if (fieldInfo instanceof IntegerFieldInfo || fieldInfo instanceof LongFieldInfo) {
                sym = new SymbolicInteger(symName); 
                
            } else if (fieldInfo instanceof FloatFieldInfo || fieldInfo instanceof DoubleFieldInfo) {
                sym = new SymbolicReal(symName); 
                
            } else if (fieldInfo.getType().equals("java.lang.String")) {                
                sym = new StringSymbolic(symName);
                
            } else if (fieldInfo instanceof BooleanFieldInfo) {
                sym = new SymbolicInteger(symName, 0, 1);
            }   
            
            if (sym != null) { // when sym == null --> fieldInfo is reference
                if (size == 1) {
                    frame.setOperandAttr(sym);
                } else {
                    frame.setLongOperandAttr(sym);
                }           
                ei.setFieldAttr(fieldInfo, sym);
            }
        }
	    return getNext(ti);
	  }	
	
	
	
	public Instruction SymbcExecuteFirstCheckArray (ThreadInfo ti) {
		
		ChoiceGenerator<?> prevHeapCG = null;
		HeapNode[] prevSymRefs = null;
		int numSymRefs = 0;
		
		Config conf = ti.getVM().getConfig();
		String[] lazy = conf.getStringArray("symbolic.lazy");
		if (lazy == null || !lazy[0].equalsIgnoreCase("true"))
			return super.execute(ti);

	    ClassInfo ciField;
	    FieldInfo fieldInfo;
	    
	    try {
	      fieldInfo = getFieldInfo();
	    } catch(LoadOnJPFRequired lre) {
	      return ti.getPC();
	    }
	    
	    if (fieldInfo == null) {
	      return ti.createAndThrowException("java.lang.NoSuchFieldError",
	          (className + '.' + fname));
	    }
		
		ciField = fieldInfo.getClassInfo();
	    
	    if (!mi.isClinit(ciField) && ciField.initializeClass(ti)) {
	      // note - this returns the next insn in the topmost clinit that just got pushed
	      return ti.getPC();
	    }

	    ElementInfo ei = ciField.getModifiableStaticElementInfo();
	   
	    if (ei == null){
	      throw new JPFException("attempt to access field: " + fname + " of uninitialized class: " + ciField.getName());
	    }

		Object attr = ei.getFieldAttr(fi);

		if (!(fi.isReference() && attr != null))
			return super.execute(ti);

		if(attr instanceof StringExpression || attr instanceof SymbolicStringBuilder)
				return super.execute(ti); // Strings are handled specially

		if (SymbolicInstructionFactory.debugMode)
			  System.out.println("lazy initialization");
		
		int currentChoice;
		ChoiceGenerator<?> heapCG;

		ClassInfo typeClassInfo = fi.getTypeClassInfo(); // use this instead of fullType

		if (!ti.isFirstStepInsn()) {
			prevSymRefs = null;
			numSymRefs = 0;
			
			prevHeapCG = ti.getVM().getSystemState().getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);

			if (prevHeapCG != null) {
				// collect candidates for lazy initialization
				SymbolicInputHeap symInputHeap =
					((HeapChoiceGenerator)prevHeapCG).getCurrentSymInputHeap();
				prevSymRefs = symInputHeap.getNodesOfType(typeClassInfo);
				numSymRefs = prevSymRefs.length;					  
			}			
			// TODO: fix subtypes
            heapCG = new HeapChoiceGenerator(numSymRefs+2);  //+null,new
			ti.getVM().getSystemState().setNextChoiceGenerator(heapCG);
			return this;
		} else {  // this is what really returns results
			heapCG = ti.getVM().getSystemState().getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);
			assert (heapCG !=null && heapCG instanceof HeapChoiceGenerator) :
				  "expected HeapChoiceGenerator, got: " + heapCG;
			currentChoice = ((HeapChoiceGenerator)heapCG).getNextChoice();
		}
		
		PathCondition pcHeap; //this pc contains only the constraints on the heap
		SymbolicInputHeap symInputHeap;

		// pcHeap is updated with the pcHeap stored in the choice generator above
		// get the pcHeap from the previous choice generator of the same type
		
		prevHeapCG = heapCG.getPreviousChoiceGeneratorOfType(HeapChoiceGenerator.class);

		if (prevHeapCG == null){
			pcHeap = new PathCondition();
			symInputHeap = new SymbolicInputHeap();
		}
		else {
			pcHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentPCheap();
			symInputHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentSymInputHeap();
		}
		assert pcHeap != null;
		assert symInputHeap != null;

		prevSymRefs = symInputHeap.getNodesOfType(typeClassInfo);
        numSymRefs = prevSymRefs.length;		

		int daIndex = 0; //index into JPF's dynamic area
		if (currentChoice < numSymRefs) { // lazy initialization
			HeapNode candidateNode = prevSymRefs[currentChoice];
			// here we should update pcHeap with the constraint attr == candidateNode.sym_v
			pcHeap._addDet(Comparator.EQ, (SymbolicInteger) attr, candidateNode.getSymbolic());
	        daIndex = candidateNode.getIndex();
		}
		else if (currentChoice == numSymRefs) { //existing (null)
			pcHeap._addDet(Comparator.EQ, (SymbolicInteger) attr, new IntegerConstant(-1));
			daIndex = MJIEnv.NULL;
		} else if (currentChoice == (numSymRefs + 1) && !abstractClass) {		
			// creates a new object with all fields symbolic and adds the object to SymbolicHeap
	    	boolean isArray = HelperArray.isArrayCreated(typeClassInfo);
	    	if (isArray) {
	    		daIndex = HelperArray.addNewHeapNodeForArray (typeClassInfo, ti, attr, pcHeap,
				  		symInputHeap, numSymRefs, prevSymRefs, ei.isShared());
	    	} else {
	    		daIndex = Helper.addNewHeapNode(typeClassInfo, ti, attr, pcHeap,
				  		symInputHeap, numSymRefs, prevSymRefs, ei.isShared());
	    	}
		} else {
			//TODO: fix
			System.err.println("subtyping not handled");
		}

		ei.setReferenceField(fi,daIndex );
		ei.setFieldAttr(fi, null);//Helper.SymbolicNull); // was null
		StackFrame frame = ti.getModifiableTopFrame();
		frame.pushRef(daIndex);
		((HeapChoiceGenerator)heapCG).setCurrentPCheap(pcHeap);
		((HeapChoiceGenerator)heapCG).setCurrentSymInputHeap(symInputHeap);
		if (SymbolicInstructionFactory.debugMode)
			System.out.println("GETSTATIC pcHeap: " + pcHeap);
		return getNext(ti);
	}
}

