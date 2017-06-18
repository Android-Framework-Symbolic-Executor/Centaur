#include <iostream>
#include <fstream>
#include <map>
#include <vector>

//this is to modify the path_* to path_*_formate.
//the first line in path_* is the path condition related to string.
//we will make a special way to deal with it. 

using namespace std;

class ModifyFormate {
  private:
    string inFile;
    string outFile;
  
  private: 
    void Modify ();

  public:
    ModifyFormate (map<string, string>& filesInfo);
    void Run();
};

ModifyFormate::ModifyFormate (map<string, string>& filesInfo) {
    map<string, string>::iterator it = filesInfo.find("-f");
    if (it != filesInfo.end()) inFile = it->second;
    outFile = inFile + "_formate";
}

void ModifyFormate::Run() {
    Modify();
}

void ModifyFormate::Modify () {
    ifstream read;
    read.open(inFile.c_str());
    if (!read.is_open()) {
	cout<<"ModifyFormate; read f fail:"<<inFile<<endl;
	return;
    }

    ofstream write;
    write.open(outFile.c_str());
    if (!write.is_open()) {
        cout<<"ModifyFormate; write f fail:"<<outFile<<endl;
        return;
    }

    string line;
    bool t = false;
    int pos1, pos2, pos3;
    
    getline(read, line);
    if ((pos1 = line.find("constraint # =")) != string::npos) {
    // if contains, then this line is the string path condition.
	string spcLine = line.substr(0, pos1-1);
	while ((pos1 = spcLine.find("(")) != string::npos) {
	    pos3 = spcLine.find(")");
	    string spc = spcLine.substr(pos1+1, pos3-pos1-1);
	    //cout<<"    spc: "<<spc<<endl;
	    while ((pos1 = spc.find("[")) != string::npos) {
            	pos2 = spc.find("]");
            	spc.replace(pos1, pos2-pos1+1, "");
            }
	    //cout<<"    final spc for writing: "<<spc<<endl;
	    write<<spc<<endl;
	    if ((spcLine.length() >= pos3 + 4) && (spcLine.substr(pos3 + 2).find("(") != string::npos)) {
		spcLine = spcLine.substr(pos3 + 4);
		//cout<<endl<<"  prun spcLine: "<<spcLine<<endl;
	    }
            else break;
	}
    } else {
	cout<<"the first line is not spc"<<endl;
	while ((pos1 = line.find("[")) != string::npos) {
            pos2 = line.find("]");
            line.replace(pos1, pos2-pos1+1, "");
        }
        if ((line.length() > 4) && (line.substr(line.length()-2).find("&") != string::npos))
            write<<line.substr(0, line.length()-2)<<endl;
        else write<<line<<endl;
    }


    //the following line is the numeric path condition.
    while (!read.eof()) {
	t = getline(read, line);
	if (!t) break;

	//cout<<"line: "<<endl;
	while ((pos1 = line.find("[")) != string::npos) {
	    pos2 = line.find("]");
	    //cout<<"  pos1: "<<pos1<<"  pos2: "<<pos2<<endl;
	    line.replace(pos1, pos2-pos1+1, "");
	    //cout<<"  line: "<<line<<endl;
	}
	//cout<<"line length:"<<line.length()<<endl;
	if ((line.length() > 4) && (line.substr(line.length()-2).find("&") != string::npos)) 
	    write<<line.substr(0, line.length()-2)<<endl;
	else write<<line<<endl;
    }
    
    read.close();
    write.close();
}



int  main (int argc, char** argv) 
{
    if (argc != 3) {
	cout<<"wrong input."<<endl<<"USAGE: ./modifyFormate -f filename"<<endl;
	return 0;
    }

    map<string, string> usages;
    usages.insert (pair<string, string> (argv[1], argv[2]));
    
    ModifyFormate mf (usages);
    mf.Run();
    
    return 0; 
}









