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

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.VM;

public class SystemClassLoader extends gov.nasa.jpf.vm.SystemClassLoaderInfo {

	public SystemClassLoader(VM vm, int appId) {
		super(vm, appId);
		// TODO Auto-generated constructor stub
	}

	public ClassInfo getClassClassInfo() {
		    return classClassInfo;
	}	

	@Override
	protected void initializeSystemClassPath(VM vm, int appId) {
		// TODO Auto-generated method stub
		
	}
}
