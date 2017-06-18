#include <iostream>
#include <fstream>
#include <map>
#include <vector>
#include <sstream>

using namespace std;

class ExtractDividePath {
  private:
    string inFile;
    string outDir;

  private:
    void ExtractDivide ();

  public:
    ExtractDividePath (map<string, string>& filesInfo);
    void Run();
};

ExtractDividePath::ExtractDividePath (map<string, string>& filesInfo) {
    map<string, string>::iterator it = filesInfo.find("-f");
    if (it != filesInfo.end()) inFile = it->second;
    it = filesInfo.find("-o");
    if (it != filesInfo.end()) outDir = it->second;
}

void ExtractDividePath::Run() {
    ExtractDivide ();
}

void ExtractDividePath::ExtractDivide () {
    ifstream read;
    read.open(inFile.c_str());
    if (!read.is_open()) {
        cout<<"ExtractDiviePaths; read file fail: "<<inFile<<endl;
        return;
    }

    bool t = false;
    string line;
    //string begflg = "got a PC choice generator";
    string begflg = "pc.spc:";
    string endflg1 = "numeric PC: ";
    string endflg2 = "## Warning: empty path condition";
    bool writeflg = false;
    int i = 0;
    ofstream write;
    while (!read.eof()) {
        t = getline(read, line);
        if (!t) break;
        if (line.substr(0,7).find(begflg) != string::npos) {
	    cout<<"fine begflg: "<<line<<endl;
	    writeflg = true;
	    ++i;
	    stringstream ss;
	    ss<<i;
	    string writefile = outDir + "path_" + ss.str();
	    
   	    write.open(writefile.c_str());
    	    if (!write.is_open()) {
        	cout<<"ExtractDividePath; when write, open file fail: "<<writefile<<endl;
        	return;
	    }
	    continue;
        }
     	if ((line.find(endflg1) != string::npos) || (line.find(endflg2) != string::npos)) {
            writeflg = false;
	    write.close();
        }
	if (writeflg) {
	    write<<line<<endl;
	}
    }
    read.close();

    return;
}



int  main (int argc, char** argv)
{
    if (argc != 5) {
        cout<<"wrong input."<<endl<<"USAGE: ./extractDividePath -f filename -o outdir"<<endl;
        return 0;
    }

    map<string, string> usages;
    usages.insert (pair<string, string> (argv[1], argv[2]));
    usages.insert (pair<string, string> (argv[3], argv[4]));

    ExtractDividePath edp (usages);
    edp.Run();

    return 0;
}

