# Centaur
Symbolic executor for Android Framework code

Paper: "System Service Call-oriented Symbolic Execution of Android Framework with Applications to Vulnerability Discovery and Exploit Generation"

Lannan Luo*, Qiang Zeng*, Chen Cao, Kai Chen, Jian Liu, Limin Liu, Neng Gao, Min Yang, Xinyu Xing, and Peng Liu. (*Co-first authors).
In Proceedings of the 15th ACM International Conference on Mobile Systems, Applications, and Services (MobiSys), Niagara Falls, NY, June 19th-23rd, 2017.


1. First-phase Preparation (preparing the environments that our tool jpf-centaur relies on): 

	1.1 download Eclipse

	1.2 install jpf-core (http://babelfish.arc.nasa.gov/trac/jpf/wiki/projects/jpf-core), and jpf-symbc (http://babelfish.arc.nasa.gov/trac/jpf/wiki/projects/jpf-symbc). Make sure they can run successfully.

	1.3 download Android source code (https://source.android.com/source/); compile the framework and SDK


2. Second-phase Preparation (preparing our tool jpf-centaur):

	2.1 download Centaur

	2.2 import jpf-centaur into Eclipse 

 	2.3 change the visibility of gov.nasa.jpf.symbc.bytecode.BytecodeUtils.InstructionOrSuper.InstructionOrSuper(boolean callSuper, Instruction inst) to "public" (under "jpf-symbc"). We should find a better way to do this.

 	2.4 modify the path of “jpf-centaur” in the file jpf.properties (see the example in the repo) 
	
	2.5 create a new folder named “for-android-libcore” under the folder Centaur

	2.6 put the following subfolders/files of the compiled Android code (in step 1.3) (how to organize the files please see the example “for-android-libcore” folder in the repo) under the folder of “for-android-libcore”: 
		a) ./out/target/common/obj/JAVA_LIBRARIES/core-libart_intermediates/classes/android/*; 
		b) ./out/target/common/obj/JAVA_LIBRARIES/core-libart_intermediates/classes/java/io/*; 
		c) ./out/target/common/obj/JAVA_LIBRARIES/core-libart_intermediates/classes/java/util/*;  
		d) ./out/target/common/obj/JAVA_LIBRARIES/core-libart_intermediates/classes/java/lang/String.class
		e) ./out/target/common/obj/JAVA_LIBRARIES/core-libart_intermediates/classes/java/lang/String$1.class; 
		f) ./out/target/common/obj/JAVA_LIBRARIES/core-libart_intermediates/classes/libcore/*

	2.7 run your app on an emulator, and dump the heap memory snapshot. Assume it is named systemservice-android.txt 

	2.8 use hprof (https://github.com/eaftan/hprof-parser) to parse systemservice-android.txt and get:  parsed-systemservice-android.txt (we have the example snapshot files in the support-files.tar.gz that our experiments were tested on)

	2.9 import CentaurDriver into Eclipse

	2.10 run RPCserver (an Android app) on the emulator. Remember to configure the build path of RPCserver

 	2.11 configure the TCP port (see the "socket-communication" file under the folder of document). Do not forget to redirect tcp port (using redir add tcp:5050:8080)

	2.12 under the src folder, there are several examples corresponding to our experiments. And the corresponding evaluation results are under the folder experiment-results


3. the folder “analysis-result” contains the executables that can analyze the result produced by jpf-centaure and generate the concrete values.
