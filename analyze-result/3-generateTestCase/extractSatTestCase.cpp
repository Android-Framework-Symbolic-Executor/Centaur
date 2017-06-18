#include <iostream>
#include <fstream>
#include <map>
#include <vector>
#include <stdlib.h>
#include <sstream>

using namespace std;

class ExtractSatTestCase {
  private:
    string file;

  private:
    void Extract ();

  public:
    ExtractSatTestCase (map<string, string>& filesInfo);
    void Run ();
};

ExtractSatTestCase::ExtractSatTestCase (map<string, string>& filesInfo) {
    map<string, string>::iterator it = filesInfo.find("-f");
    if (it != filesInfo.end()) file = it->second;
}

void ExtractSatTestCase::Run() {
    Extract ();
}

void ExtractSatTestCase::Extract () {
    ifstream read;
    read.open(file.c_str());
    if (!read.is_open()) {
        cout<<"ExtractSatTestCase; read file fail: "<<file<<endl;
        return;
    }

    ofstream write;
    string outfile = file + "_final";
    write.open(outfile.c_str());
    if (!write.is_open()) {
        cout<<"ExtractSatTestCase; write file fail: "<<outfile<<endl;
        return;
    }

    string line;
    bool t = false;
    string formulaLine;
    int i = 1;
    while (!read.eof()) {
        t = getline(read, line);
        if (!t) break;

	if (line.find("begin a new formula") != string::npos) {
	    getline(read, line);   // "command: *****"
	    formulaLine += line + "\n";
	    getline(read, line);   // "unsat" or "sat"
	    if (line.substr(0, 3).find("sat") != string::npos) {
		while (!read.eof()) {
		    getline(read, line);
		    if (line.find("end this formula") != string::npos) break;
		    else formulaLine += line + "\n";
		}
		stringstream ss;
		ss << i;
		write<<"i="<<ss.str()<<endl;
		write<<formulaLine<<endl;
		formulaLine = "";
		++ i;
	    } else {
		formulaLine = "";
		getline(read, line);   //"end this formula"
		continue;
	    }  
	}
    }
    read.close();
}


int  main (int argc, char** argv)
{
    if (argc != 3) {
        cout<<"wrong input."<<endl<<"USAGE: ./extractSatTestCase -f file"<<endl;
        return 0;
    }

    map<string, string> usages;
    usages.insert (pair<string, string> (argv[1], argv[2]));

    ExtractSatTestCase etc (usages);
    etc.Run();

    return 0;
}



