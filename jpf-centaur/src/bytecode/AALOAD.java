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
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.heap.SymbolicInputHeap;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.vm.ArrayFields;
import gov.nasa.jpf.vm.ArrayIndexOutOfBoundsExecutiveException;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import migration.Migrate;
import queryer.Query;

public class AALOAD extends gov.nasa.jpf.symbc.bytecode.AALOAD {

	
  @Override
  public Instruction execute (ThreadInfo ti) {
	
	  StackFrame frame = ti.getModifiableTopFrame();
	  index = frame.peek();
	  arrayRef = frame.peek(1);

	  if (arrayRef == MJIEnv.NULL) {
	      return ti.createAndThrowException("java.lang.NullPointerException");
	  }
	  ElementInfo eiArray = ti.getElementInfo(arrayRef);
	  Object attr = eiArray.getObjectAttr();
	  
	  if (attr == null) {
		  if (peekIndexAttr(ti)==null || !(peekIndexAttr(ti) instanceof IntegerExpression))
			  return super.execute(ti);
		  
		  // index is symbolic, we return null. need to be revised.		  
		  if (arrayRef == MJIEnv.NULL) {
		      return ti.createAndThrowException("java.lang.NullPointerException");
		  }
		  frame.pop(2);		  
		  frame.pushRef(MJIEnv.NULL);
	      return getNext(ti);	
	  }
	  
	  //first, migrate all elements of eiArray
	  OVHeap heap = (OVHeap)ti.getHeap();
	  int elementRef = eiArray.getReferenceElement(index);
	  
	  StackFrame sf = ti.getModifiableTopFrame();
	  Object indexAttr = peekIndexAttr(ti);
	  boolean isSkeleton = false;
	  if ((indexAttr != null) && indexAttr.toString().equals(KeyWords.SkeletonAttr)) {
		  isSkeleton = true;	  
	  } 
	  
	  String elementAttr = "";
	  if (isSkeleton) {
		  if (!attr.toString().contains(KeyWords.SkeletonAttr))
				elementAttr = attr + "***" + KeyWords.SkeletonAttr;
		  else elementAttr = attr.toString();
	  } else {
		  elementAttr = attr.toString();
	  }
	  
	  String elementType = "";
	  if ((elementRef != 0) && (attr != "") && (attr.toString().contains(KeyWords.migrationAttr))) {    
		  //check whether the instance field has been migrated
		  boolean hasInitialized = false;
		  hasInitialized = heap.CheckMigrated(elementRef);
	      System.out.println("test migration. hasInitialized? " + hasInitialized);
	      
	      ElementInfo eii = ti.getModifiableElementInfo(elementRef); 
          if (eii == null)
        	  System.out.println("eii is null; ref: " + elementRef);
          else 
        	  System.out.println("eii is NOT null; ref: " + elementRef);
                   
		  //contain "migrated", we will migrate it
	      if ((eii == null) || !hasInitialized) { 
	    	  int[] reff = new int[1];
        	  reff[0] = elementRef;
	    	  elementType = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff);	
	    	  elementRef = reff[0];
    		  if (elementType.equals("0")) {
    			  //now, we need to remigrate the array, in order to find the concrete ref of the element.
    			  /*
    			   * the reason for remigration is: heap.con2Sym 
    			   * for an array A(1000)[a1(1001), a2(1002), a3(1003)]; 1000 -- 1003 are symbolic reference.
    			   * when backtracking, in con2Sym, one case: 1002 and 1003 are removed, but 1000 and 1001 are kept.
    			   * Then A(1000)[a1(1001), a2(1002), a3(1003)] is still the same. 
    			   * As 1000 is kept in con2Sym, when getField of A, A will not be migrated, and all the elelments of A are the same (references: 10001 -- 1003)
    			   * However, the object pointed by 1002 are remomved from the heap, resulting in NULL element here.
    			   * The reason is that each element has its own reference in con2Sym to show whether it has been migrated.
    			   * However, the array and its elements should be consistent.
    			   * Namely, as long as one element of the array is removed from the heap due to backtracking, this array and all its elements should be remigrated. 
    			   * But, this is difficult to achieve currently. And this solution here is a little trick.
    			   */
    			  int conarrayRef = heap.FindConcreteRef(arrayRef);
    			  String elementconRef = (String) Query.QueryArrayElementValueUsingMyObjRef(conarrayRef, index); //java.lang.String
    			  boolean isFrozen = false;
    	          if (eiArray.isFrozen()) {
    	        	  isFrozen = true;
    	          }
    	          eiArray.defreeze();  
    	          eiArray.setReferenceElement(index, Integer.parseInt(elementconRef));  	           
    	          if (isFrozen) {
    	        	  eiArray.freeze();
    	          }  			  			  
    		  } 
    		  elementRef = eiArray.getReferenceElement(index);
    		  int[] reff2 = new int[1];
        	  reff2[0] = elementRef;
	    	  elementType = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff2); //从heap file找 elementType. used in the second part
	    	  elementRef = reff2[0];
	    	  int symRef = Migrate.MigrateField(elementType, ti, elementRef, elementAttr.toString());		              
	          
	          boolean isFrozen = false;
	          if (eiArray.isFrozen()) {
	        	  isFrozen = true;
	          }
	          eiArray.defreeze();  
	          eiArray.setReferenceElement(index, symRef);	           
	          if (isFrozen) {
	        	  eiArray.freeze();
	          }
	          
	          ElementInfo eiElement = ti.getModifiableElementInfo(symRef); 
	          eiElement.setObjectAttr(elementAttr);		              

	          heap.AddMigratedField(elementRef, symRef);  // java.util.HashMap . table (????)
	      }  
	  }
  
	  //now, determine the return value: whether skeleton or not?	 
	  frame.pop(2); // now we can pop index and array reference
	  elementRef = eiArray.getReferenceElement(index);
	  
	  if (isSkeleton) {	  
		  //this is aaload, only consider the type is object or string
		  if (elementType.contains("java.lang.String")) {
			  System.out.println("into this one: "+ elementType);
			  Expression sym = new StringSymbolic(arrayRef + "_indexof_" 
                                  + index + KeyWords.SkeletonExpressionSuffix);                
			  frame.pushRef(elementRef);
			  frame.setOperandAttr(sym);  
		  } 
		  else {
				/*
				 * (1) If the reference is reference (except for string), we need to set its attribute both
				 *    SkeletonApp.SkeletonExpressionSuffix and its original attr (maybe "migrated")
				 * (2) If the reference's attribute contains "migrated", all its fields will be migrated 
				 *    in getStatic or getField
				 * (3) If the reference's attribute contains "skeleton", when calling getStatic or getField, 
				 *    (a) the requested field is string or primitive type, we create a symbolic expression for 
				 *    it and return the expression; (b) the requested field is not string or primitive, 
				 *    instead, it is reference, we still migrate it and set all its fields attribute as both 
				 *    "migrated" and "skeleton"
				 */
				frame.pushRef(elementRef);
			}
	  }	else {
		  frame.pushRef(elementRef);
	  }	
	  
	  return getNext(ti);	  
  }
}
