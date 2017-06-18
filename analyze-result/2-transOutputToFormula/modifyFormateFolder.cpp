#include <iostream>
#include <fstream>
#include <map>
#include <vector>
#include <stdlib.h>

using namespace std;

class ModifyFormateFolder {
  private:
    string inDir;
    string outDir;
    vector<string> files;
  
  private: 
    void GetFiles ();
    void ModifyFolder ();

  public:
    ModifyFormateFolder (map<string, string>& filesInfo);
    void Run();
};

ModifyFormateFolder::ModifyFormateFolder (map<string, string>& filesInfo) {
    map<string, string>::iterator it = filesInfo.find("-io");
    if (it != filesInfo.end()) inDir = it->second;
    it = filesInfo.find("-oo");
    if (it != filesInfo.end()) outDir = it->second;
}

void ModifyFormateFolder::Run() {
    GetFiles ();
    ModifyFolder ();
}

void ModifyFormateFolder::GetFiles () {
    string command = "ls " + inDir + " > " + inDir + "allpaths.txt";
    system (command.c_str());

    ifstream read;
    string file = inDir + "allpaths.txt";
    read.open(file.c_str());
    if (!read.is_open()) {
	cout<<"read allpaths.txt fail"<<endl;
	return;
    }
    string line;
    bool t = false;
    while (!read.eof()) {
	t = getline(read, line);
	if (!t) break;
	if (line.find("path_") != string::npos) {
	    files.push_back(line);
    	}
    }
    read.close();
}

void ModifyFormateFolder::ModifyFolder () {
    for (vector<string>::iterator it = files.begin(); it != files.end(); ++ it) {
	string command = "2-transOutputToFormula/modifyFormate -f " + inDir + (*it);
	system (command.c_str());
    }

    string command = "mv " + inDir + "path_1*_formate " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_2*_formate " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_3*_formate " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_4*_formate " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_5*_formate " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_6*_formate " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_7*_formate " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_8*_formate " + outDir;
    system (command.c_str());
    command = "mv " + inDir + "path_9*_formate " + outDir;
    system (command.c_str());

}


int  main (int argc, char** argv) 
{
    if (argc != 5) {
	cout<<"wrong input."<<endl<<"USAGE: ./modifyFormateFolder -io indir -oo outdir"<<endl;
	return 0;
    }

    map<string, string> usages;
    usages.insert (pair<string, string> (argv[1], argv[2]));
    usages.insert (pair<string, string> (argv[3], argv[4]));    

    ModifyFormateFolder mff (usages);
    mff.Run();
    
    return 0; 
}









