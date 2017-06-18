#include <iostream>
#include <fstream>
#include <map>
#include <vector>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sstream>

using namespace std;

class TransFormulaFolder {
  private:
    string inDir;
    string outDir;
    map<string, int> stringIntMapping;
    vector<string> files;  

  private: 
    void GetFiles ();
    void GetMapping ();
    void TransformFolder ();

  public:
    TransFormulaFolder (map<string, string>& filesInfo);
    void Run();
};

TransFormulaFolder::TransFormulaFolder (map<string, string>& filesInfo) {
    map<string, string>::iterator it = filesInfo.find("-io");
    if (it != filesInfo.end()) inDir = it->second;
    it = filesInfo.find("-oo");
    if (it != filesInfo.end()) outDir = it->second;
}

void TransFormulaFolder::Run() {
    GetFiles ();
    cout<<"file number: "<<files.size()<<endl;
    GetMapping ();
    TransformFolder ();
}

void TransFormulaFolder::GetFiles () {
    string command = "ls " + inDir + " > " + inDir + "allformates.txt";
    system (command.c_str());

    ifstream read;
    string file = inDir + "allformates.txt";
    read.open(file.c_str());
    if (!read.is_open()) {
        cout<<"read allformates.txt fail"<<endl;
        return;
    }
    string line;
    bool t = false;
    while (!read.eof()) {
        t = getline(read, line);
        if (!t) break;
        if ((line.find("path_") != string::npos) && (line.find("_formate") != string::npos)) {
            files.push_back(line);
        }
    }
    read.close();
}

void TransFormulaFolder::GetMapping () { 
    int i = 10000;
    for (vector<string>::iterator it = files.begin(); it != files.end(); ++ it) {
	string file = inDir + (*it);
    	ifstream read;
    	read.open(file.c_str());
    	if (!read.is_open()) {
            cout<<"TransFormula; 11 read f fail:"<<file<<endl;
            return;
    	}
    	string line;
    	bool t = false;
    	int pos1, pos2;
    	while (!read.eof()) {
            t = getline(read, line);
            if (!t) break;
	    if (line.find("equals") != string::npos) {
		char* pch;
        	pch = strtok ((char*)line.c_str(), " ");
        	while (pch != NULL) {
            	    string r = pch;
            	    if (r.find("CONST") != string::npos) {
                    	bool push = true;
                    	for (map<string, int>::iterator it = stringIntMapping.begin(); it != stringIntMapping.end(); ++ it) {
                    	    if ((it->first).compare(r) == 0) {
                            	push = false;
                            	break;
                    	    }
                    	}
                    	if (push) {
                    	    stringIntMapping.insert(pair<string, int>(r, i));
			    ++i;
                    	}
            	    }
                    pch = strtok (NULL, " ()");
		}
	    }
    	}
	read.close();
    }

    ofstream write;
    string wFile = outDir + "stringIntMapping.txt";
    write.open(wFile.c_str());
    if (!write.is_open()) {
        cout<<"TransFormula; 22 write f fail:"<<wFile<<endl;
        return;
    }
    cout<<"stringIntMapping:"<<endl;
    for (map<string, int>::iterator it = stringIntMapping.begin(); it != stringIntMapping.end(); ++ it) {
	cout<<(it->first)<<"    "<<(it->second)<<endl;
	write<<(it->first)<<"    "<<(it->second)<<endl;
    }
    write.close();
 

    string wFile2 = inDir + "stringIntMapping.txt";
    write.open(wFile2.c_str());
    if (!write.is_open()) {
        cout<<"TransFormula; 22 write f fail:"<<wFile2<<endl;
        return;
    }
    for (map<string, int>::iterator it = stringIntMapping.begin(); it != stringIntMapping.end(); ++ it) {
        write<<(it->first)<<"    "<<(it->second)<<endl;
    }
    write.close();
}

void TransFormulaFolder::TransformFolder () {
   for (vector<string>::iterator it = files.begin(); it != files.end(); ++ it) {
        string command = "2-transOutputToFormula/transFormula -f " + inDir + (*it);
        system (command.c_str());
    }

    string command = "mv " + inDir + "path_1*_formate_formula " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_2*_formate_formula " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_3*_formate_formula " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_4*_formate_formula " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_5*_formate_formula " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_6*_formate_formula " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_7*_formate_formula " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_8*_formate_formula " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_9*_formate_formula " + outDir;
    system (command.c_str());
}

int  main (int argc, char** argv) 
{
    if (argc != 5) {
	cout<<"wrong input."<<endl<<"USAGE: ./transFormulaFolder -io indir -oo outdir"<<endl;
	return 0;
    }

    map<string, string> usages;
    usages.insert (pair<string, string> (argv[1], argv[2]));
    usages.insert (pair<string, string> (argv[3], argv[4]));    

    TransFormulaFolder tff (usages);
    tff.Run();
    
    return 0; 
}









