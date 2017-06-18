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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.NativeMethodInfo;


public class CentaurInstructionFactory extends gov.nasa.jpf.symbc.SymbolicInstructionFactory {

	  public CentaurInstructionFactory(Config conf) {
		super(conf);
		// TODO Auto-generated constructor stub
	}	
	  
	  //public Instruction areturn() {
	  //      return (filter.isPassing(ci) ? new ARETURN(): super.areturn());
	  //    }

      //public Instruction ireturn() {
      //      return (filter.isPassing(ci) ? new IRETURN(): super.ireturn());
      //    }


	  /******************************/
	  /* in gov.nasa.jpf.symbc.SymbolicInstructionFactory:
	   * protected ClassInfo ci;
	   * public ClassInfoFilter filter;
	   */
	  public Instruction executenative(NativeMethodInfo mi){
		  return (filter.isPassing(ci) ?  new EXECUTENATIVE(mi): super.executenative(mi));
	  }

	  
	  public Instruction getfield(String fieldName, String clsName, String fieldDescriptor){
		  return (filter.isPassing(ci) ?  new GETFIELD(fieldName, clsName, fieldDescriptor): super.getfield(fieldName, clsName, fieldDescriptor));
	  }

	  public Instruction getstatic(String fieldName, String clsName, String fieldDescriptor){
		  return (filter.isPassing(ci) ?  new GETSTATIC(fieldName, clsName, fieldDescriptor): super.getstatic(fieldName, clsName, fieldDescriptor));
	  }

	  public Instruction invokestatic(String clsName, String methodName, String methodSignature) {
		    return (filter.isPassing(ci) ? new INVOKESTATIC(clsName, methodName, methodSignature): super.invokestatic(clsName, methodName, methodSignature));
	  }
	  
	  public Instruction invokevirtual(String clsName, String methodName, String methodSignature) {
		    return (filter.isPassing(ci) ? new INVOKEVIRTUAL(clsName, methodName, methodSignature): super.invokevirtual(clsName, methodName, methodSignature));
	  }
	  
	  public Instruction invokeinterface(String clsName, String methodName, String methodSignature) {
		    return (filter.isPassing(ci) ? new INVOKEINTERFACE(clsName, methodName, methodSignature): super.invokeinterface(clsName, methodName, methodSignature));
	  }
	  
	  public Instruction irem() {
		    return (filter.isPassing(ci) ? new IREM(): super.irem());
	  }
	  
	  public Instruction isub() {
		    return (filter.isPassing(ci) ? new ISUB() : super.isub());
	  }
	  
	  public Instruction ifeq(int targetPc) {
		    return (filter.isPassing(ci) ? new IFEQ(targetPc): super.ifeq(targetPc));
	  }

	  public Instruction ifne(int targetPc) {
		    return (filter.isPassing(ci) ? new IFNE(targetPc): super.ifne(targetPc));
	  }

	  public Instruction ifle(int targetPc) {
		    return (filter.isPassing(ci) ? new IFLE(targetPc) : super.ifle(targetPc));
	  }

	  public Instruction iflt(int targetPc) {
		    return (filter.isPassing(ci) ? new IFLT(targetPc) : super.iflt(targetPc));
	  }

	  public Instruction ifge(int targetPc) {
		    return (filter.isPassing(ci) ? new IFGE(targetPc): super.ifge(targetPc));
	  }

	  public Instruction ifgt(int targetPc) {
		    return (filter.isPassing(ci) ? new IFGT(targetPc): super.ifgt(targetPc));
	  }
		  
	  public Instruction if_icmpeq(int targetPc) {
		    return (filter.isPassing(ci) ? new IF_ICMPEQ(targetPc): super.if_icmpeq(targetPc));
	  }
	  
	  public Instruction if_icmpne(int targetPc) {
		    return (filter.isPassing(ci) ? new IF_ICMPNE(targetPc): super.if_icmpne(targetPc));
	  }

	  public Instruction if_icmpge(int targetPc) {
		    return (filter.isPassing(ci) ? new IF_ICMPGE(targetPc): super.if_icmpge(targetPc));
	  }

	  public Instruction if_icmpgt(int targetPc) {
		    return (filter.isPassing(ci) ? new IF_ICMPGT(targetPc): super.if_icmpgt(targetPc));
	  }

	  public Instruction if_icmple(int targetPc) {
		    return (filter.isPassing(ci) ? new IF_ICMPLE(targetPc): super.if_icmple(targetPc));
	  }

	  public Instruction if_icmplt(int targetPc) {
		    return (filter.isPassing(ci) ? new IF_ICMPLT(targetPc): super.if_icmplt(targetPc));
	  }
	  
	  public Instruction aaload() {
          return (filter.isPassing(ci) ? new AALOAD() : super.aaload());
      }
	  
	  public Instruction iaload() {
          return (filter.isPassing(ci) ? new IALOAD() : super.iaload());
      }

	  public Instruction aastore() {
          return (filter.isPassing(ci) ? new AASTORE() : super.aastore());
      }
	  
	  public Instruction idiv() {
		    return (filter.isPassing(ci) ? new IDIV(): super.idiv());
	  }

	  public Instruction ifnonnull(int targetPc) {
		  return  (filter.isPassing(ci) ?  new IFNONNULL(targetPc): super.ifnonnull(targetPc));
	  }

	  public Instruction ifnull(int targetPc) {
		  return  (filter.isPassing(ci) ?  new IFNULL(targetPc): super.ifnull(targetPc));
	  }

	  public Instruction new_(String clsName){
		  return (filter.isPassing(ci) ?  new NEW(clsName): super.new_(clsName) );
	  }
	  
	  /*public Instruction checkcast(String clsName) {
		    return (filter.isPassing(ci) ? new CHECKCAST(clsName): super.checkcast(clsName));
	  }	*/  
}
