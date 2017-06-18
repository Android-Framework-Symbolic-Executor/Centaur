#include <iostream>
#include <fstream>
#include <map>
#include <vector>

//this is to remove repeating test cases.

using namespace std;

class RemoveRepeatTestCase {
  private:
    string inFile;
    string outFile;
    vector<string> testCases;
  private:
    void Remove ();
    void CreateNewFile ();

  public:
    RemoveRepeatTestCase (map<string, string>& filesInfo);
    void Run();
};

RemoveRepeatTestCase::RemoveRepeatTestCase (map<string, string>& filesInfo) {
    map<string, string>::iterator it = filesInfo.find("-f");
    if (it != filesInfo.end()) inFile = it->second;
    outFile = inFile + "_remove";
}

void RemoveRepeatTestCase::Run () {
    Remove ();
    CreateNewFile ();
}

void RemoveRepeatTestCase::Remove () {
    ifstream read;
    read.open(inFile.c_str());
    if (!read.is_open()) {
        cout<<"RemoveRepeatTestCase; read f fail:"<<inFile<<endl;
        return;
    }

    string line;
    bool t = false;
    int pos1, pos2, pos3;
    
    while (!read.eof()) {
        t = getline(read, line);
        if (!t) break;

	if (line.find("command:") != string::npos) {
	    string testCase = "";
	    while (!read.eof()) {
		t = getline(read,line);
		if (!t) break;
		if (line.substr(0,2).compare("i=") == 0) {
		    cout<<"another test case"<<endl;
		    break;
		} else {
		    testCase += line + "\n";
		}
	    }
	    cout<<"testCase: "<<testCase<<endl;

	    bool contain = false;
	    for (vector<string>::iterator it = testCases.begin(); it != testCases.end(); ++ it) {
		if ((*it).compare(testCase) == 0) {
		    contain = true;
		    cout<<"this test case is repeating!!!"<<endl;
		    break;
	        }
	    }
	    if (!contain) {
		cout<<"push back new test case"<<endl;
		testCases.push_back(testCase);
	    }
	}
    }

    read.close();

    cout<<"testCases.size:"<<testCases.size()<<endl;
    for (vector<string>::iterator it = testCases.begin(); it != testCases.end(); ++ it) {
    	cout<<endl<<*it;       
    }
}


void RemoveRepeatTestCase::CreateNewFile () {
    ofstream write;
    write.open(outFile.c_str());
    if (!write.is_open()) {
        cout<<"TransFormula; write f fail:"<<outFile<<endl;
        return;
    }

    int i = 1;
    for (vector<string>::iterator it = testCases.begin(); it != testCases.end(); ++ it) {
        write<<"i="<<i<<endl<<*it<<endl;
	++i;
    }

    write.close();
}


int  main (int argc, char** argv)
{
    if (argc != 3) {
        cout<<"wrong input."<<endl<<"USAGE: ./removeRepeatTestCase -f filename"<<endl;
        return 0;
    }

    map<string, string> usages;
    usages.insert (pair<string, string> (argv[1], argv[2]));

    RemoveRepeatTestCase rmtc (usages);
    rmtc.Run();

    return 0;
}
