#include <iostream>
#include <fstream>
#include <map>
#include <vector>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sstream>

using namespace std;

class MathSatFolder {
  private:
    string inDir;
    string pathMath;
    vector<string> files;

  private:
    void GetFiles ();
    void MathSat ();

  public:
    MathSatFolder (map<string, string>& filesInfo);
    void Run();
};

MathSatFolder::MathSatFolder (map<string, string>& filesInfo) {
    map<string, string>::iterator it = filesInfo.find("-io");
    if (it != filesInfo.end()) inDir = it->second;
    
    it = filesInfo.find("-pmath");
    if (it != filesInfo.end()) pathMath = it->second;
}

void MathSatFolder::Run() {
    GetFiles ();
    MathSat ();
}

void MathSatFolder::GetFiles () {
    string command = "ls " + inDir + " > " + inDir + "allformulas.txt";
    system (command.c_str());

    ifstream read;
    string file = inDir + "allformulas.txt";
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
        if ((line.find("path_") != string::npos) && (line.find("_formate_formula") != string::npos)) {
            files.push_back(line);
        }
    }
    read.close();
}

void MathSatFolder::MathSat () {
   for (vector<string>::iterator it = files.begin(); it != files.end(); ++ it) {
        //string command = "/home/lanlan/program/android_tool/mathsat-5.3.10-linux-x86_64/bin/mathsat -input=smt2 < " + inDir + (*it);
        string command = pathMath + " -input=smt2 < " + inDir + (*it);
	cout<<endl<<"begin a new formula"<<endl<<"command: "<<command<<endl;
        system (command.c_str());
	cout<<"end this formula"<<endl;
    }
}


int  main (int argc, char** argv)
{
    //pmath = /home/lanlan/program/android_tool/mathsat-5.3.10-linux-x86_64/bin/mathsat
    if (argc != 5) {
        cout<<"wrong input."<<endl<<"USAGE: ./mathSatFolder -io -pmath"<<endl;
        return 0;
    }

    map<string, string> usages;
    usages.insert (pair<string, string> (argv[1], argv[2]));
    usages.insert (pair<string, string> (argv[3], argv[4]));

    MathSatFolder msf (usages);
    msf.Run();

    return 0;
}





