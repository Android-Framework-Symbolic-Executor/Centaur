#include <iostream>
#include <fstream>
#include <map>
#include <vector>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sstream>
#include <string>

using namespace std;

class TransFormula {
  private:
    string inFile;
    string outFile;
    map<string, string> symVars;  
    map<string, int> stringIntMapping;

  private: 
    void GetSymVars ();
    void GetStringIntMapping ();
    void Transform ();

  public:
    TransFormula (map<string, string>& filesInfo);
    void Run();
};

TransFormula::TransFormula (map<string, string>& filesInfo) {
    map<string, string>::iterator it = filesInfo.find("-f");
    if (it != filesInfo.end()) inFile = it->second;
    outFile = inFile + "_formula";
}

void TransFormula::Run() {
    GetStringIntMapping ();
    GetSymVars ();
    Transform ();
}

void TransFormula::GetStringIntMapping () {
    string mappingFile = inFile.substr(0, inFile.find_last_of("/") + 1) + "stringIntMapping.txt";

    ifstream read;
    read.open(mappingFile.c_str());
    if (!read.is_open()) {
        cout<<"TransFormula; 33 read mapping file fail:"<<mappingFile<<endl;
        return;
    }
    string line;
    bool t = false;
    int pos1, pos2;
    while (!read.eof()) {
	string stringSym;
	string intSym;
        t = getline(read, line);
	cout<<"getstringintmapping: "<<line<<endl;
        if (!t) break;
	if (line.length() <= 4) continue;
	char* pch;
        pch = strtok ((char*)line.c_str(), " ");
        while (pch != NULL) {
            string r = pch;
            if (r.find("CONST") != string::npos) {
		stringSym = r;
            } else intSym = r;
            pch = strtok (NULL, " ");
        }
	int isym = atoi(intSym.c_str());	
	stringIntMapping.insert(pair<string, int>(stringSym, isym));
    }    
    read.close();

    cout<<"stringIntMapping:"<<endl;
    for (map<string, int>::iterator it = stringIntMapping.begin(); it != stringIntMapping.end(); ++ it) {
	cout<<(it->first)<<"    "<<(it->second)<<endl;
    }
}

void TransFormula::GetSymVars () {
    ifstream read;
    read.open(inFile.c_str());
    if (!read.is_open()) {
        cout<<"TransFormula; aa read f fail:"<<inFile<<endl;
        return;
    }

    string line;
    bool t = false;
    int pos1, pos2;
    while (!read.eof()) {
        t = getline(read, line);
        if (!t) break;
	cout<<endl<<"line: "<<line<<endl;
	bool bitvectorFlg = false;
	bool constString = false;
	if ((line.find("&") != string::npos) || ((line.find("|")) != string::npos)) bitvectorFlg = true;
	if ((line.find("equals") != string::npos) || (line.find("contains") != string::npos)) constString = true;

        char* pch;
  	pch = strtok ((char*)line.c_str(), " ()");
  	while (pch != NULL) {
	    string r = pch;
	    cout<<"pch: "<<r<<endl;
	    //cout<<"line: "<<line<<endl;
	    if (((r.find("SYM") != string::npos)) || (constString && (r.find("CONST") != string::npos))) {
		bool push = true;
		for (map<string, string>::iterator it = symVars.begin(); it != symVars.end(); ++ it) {
		    if ((it->first).compare(r) == 0) {
			push = false;
			break;
		    }
		}
		if (push) {
		    if (bitvectorFlg) symVars.insert(pair<string, string>(r, "(_ BitVec 32)"));
		    else symVars.insert(pair<string, string>(r, "Int"));
		    //cout<<"push this pch: "<<pch<<endl;
		}
	    }
    	    pch = strtok (NULL, " ()");
   	}
    }

    read.close();
   
    cout<<"symVars: "<<endl;
    for (map<string, string>::iterator it = symVars.begin(); it != symVars.end(); ++ it) cout<<(it->first)<<"  "<<(it->second)<<endl;
    
}

void TransFormula::Transform () {
    ifstream read;
    read.open(inFile.c_str());
    if (!read.is_open()) {
	cout<<"TransFormula; read f fail:"<<inFile<<endl;
	return;
    }

    ofstream write;
    write.open(outFile.c_str());
    if (!write.is_open()) {
        cout<<"TransFormula; write f fail:"<<outFile<<endl;
        return;
    }
   
    //first, write the header
    cout<<"write: ;; activate model generation"<<endl;

    write<<";; activate model generation"<<endl<<"(set-option :produce-models true)"<<endl<<endl;
    for (map<string, string>::iterator it = symVars.begin(); it != symVars.end(); ++ it) {
	write<<"(declare-fun "<<(it->first)<<" () "<<(it->second)<<")"<<endl;
    }
    write<<endl;

    //second, write the assert
    for (map<string, int>::iterator it = stringIntMapping.begin(); it != stringIntMapping.end(); ++ it) {
	string constring = it->first;
	bool find = false;
	for (map<string, string>::iterator its = symVars.begin(); its != symVars.end(); ++ its) {
	    if ((its->first).compare(it->first) == 0) {
		find = true;
		break;
            }
	}
	if (find) write<<"(assert (= " << (it->first) << " " << (it->second) <<"))"<<endl;
    }		
    write<<endl;

    //now, write the body
    string line, fLine, leftLine, rightLine;
    bool t = false;
    int pos1, pos2;
    bool notFlg = false;
    vector<string> operands;
    vector<string> leftOperands;
    vector<string> rightOperands;
    vector<string> leftOperators;  // & | / etc.
    vector<string> rightOperators;
    bool bitvectorFlg = false;
    while (!read.eof()) {
	operands.clear();
	leftOperands.clear();
	leftOperators.clear();
	rightOperators.clear();
	rightOperands.clear();
	notFlg = false;
	bitvectorFlg = false;
	t = getline(read, line);
	if (!t) break;
	if (line.length() < 2) continue;

	cout<<"line: "<<line<<endl;

	if ((line.find("&") != string::npos) || ((line.find("|")) != string::npos)) bitvectorFlg = true;
	if ((pos1 = line.find("notequals")) != string::npos) line = line.replace(pos1, 9, "!=");
	else if ((pos1 = line.find("equals")) != string::npos) line = line.replace(pos1, 6, "==");

 	if ((pos1 = line.find("notstartswith")) != string::npos) line = line.replace(pos1, 13, "!=");
        else if ((pos1 = line.find("startswith")) != string::npos) line = line.replace(pos1, 10, "==");

        if ((pos1 = line.find("notcontains")) != string::npos) line = line.replace(pos1, 11, "!=");
        else if ((pos1 = line.find("contains")) != string::npos) line = line.replace(pos1, 8, "==");

	if ((pos1 = line.find("!="))!= string::npos) {
	    leftLine = line.substr(0, pos1-1); 
	    rightLine = line.substr(pos1+3);
	    //cout<<"leftLine: "<<leftLine<<endl;
	    //cout<<"rightLine: "<<rightLine<<endl;
	    fLine = "(assert (not (=";
	    notFlg = true;
	}
	else if ((pos1 = line.find("==")) != string::npos){
	    leftLine = line.substr(0, pos1-1);
	    rightLine = line.substr(pos1+3);
	    //cout<<"leftLine: "<<leftLine<<endl;
	    //cout<<"rightLine: "<<rightLine<<endl;
	    fLine = "(assert (=";
	}
	else if ((pos1 = line.find(">=")) != string::npos){
            leftLine = line.substr(0, pos1-1);
            rightLine = line.substr(pos1+3);
            //cout<<"leftLine: "<<leftLine<<endl;
            //cout<<"rightLine: "<<rightLine<<endl;
            fLine = "(assert (>=";
        }
        else if ((pos1 = line.find(">")) != string::npos){
            leftLine = line.substr(0, pos1-1);
            rightLine = line.substr(pos1+2);
            //cout<<"leftLine: "<<leftLine<<endl;
            //cout<<"rightLine: "<<rightLine<<endl;
            fLine = "(assert (>";
        }
        else if ((pos1 = line.find("<=")) != string::npos){
            leftLine = line.substr(0, pos1-1);
            rightLine = line.substr(pos1+3);
            //cout<<"leftLine: "<<leftLine<<endl;
            //cout<<"rightLine: "<<rightLine<<endl;
            fLine = "(assert (<=";
        }
        else if ((pos1 = line.find("<")) != string::npos){
            leftLine = line.substr(0, pos1-1);
            rightLine = line.substr(pos1+2);
            //cout<<"leftLine: "<<leftLine<<endl;
            //cout<<"rightLine: "<<rightLine<<endl;
            fLine = "(assert (<";
        }

        char* pch;
        pch = strtok ((char*)leftLine.c_str(), " ()");
	int i = 0;
        while (pch != NULL) {
            string r = pch;
	    if (i % 2 == 0) {/*cout<<"push into left operands: "<<pch<<endl;*/ leftOperands.push_back(pch);}
	    else {/*cout<<"push into left operators: "<<pch<<endl;*/ leftOperators.push_back(pch);}
	    pch = strtok (NULL, " ()");	
	    ++ i;
        }
	        
        char* pch2;
        pch2 = strtok ((char*)rightLine.c_str(), " ()");
        int j = 0;
        while (pch2 != NULL) {
            string r = pch2;
            if (j % 2 == 0) {/*cout<<"push into right operands: "<<pch<<endl;*/ rightOperands.push_back(pch2);}
            else {/*cout<<"push into right operators: "<<pch<<endl;*/ rightOperators.push_back(pch2);}
            pch2 = strtok (NULL, " ()"); 
            ++ j;
        }

        if (leftOperands.size() == 1) {
	    if (!bitvectorFlg) {
		int poss;
		bool isNum = false;
		if ((poss = (*leftOperands.begin()).find("CONST_")) != string::npos) {
		    string value = (*leftOperands.begin()).substr(poss+6);
		    std::string::const_iterator its = value.begin();
    		    while (its != value.end() && std::isdigit(*its)) ++its;
    		    isNum = !value.empty() && its == value.end();
		} else {
		    isNum = false;
		}
		if (isNum) {
		    string value = (*leftOperands.begin()).substr(poss+6);
		    string newvalue = value;
		    if (value.substr(0,1).compare("-") == 0) {
			newvalue = "(- " + value.substr(1) + ")"; 
		    }	
		    //cout<<"aaaa: "<<newvalue<<endl;    
		    fLine += " " + newvalue;
		    //fLine += " " + (*leftOperands.begin()).substr(poss+6);
		} else { fLine += " " + (*leftOperands.begin()); cout<<"here!!!"<<endl; cout<<fLine<<endl; }
	    }
	    else {
		int poss;
            	string op;
            	if (((poss = (*leftOperands.begin()).find("CONST_")) != string::npos) && ((*leftOperands.begin()).find_first_of("0123456789") != std::string::npos)) {
                    int num = atoi((*leftOperands.begin()).substr(6).c_str());
                    unsigned unum = (unsigned)num;
                    stringstream ss;
                    ss<<std::hex<<unum;
                    int size = ss.str().length();
                    string addzero(8-size, '0');
                    op = "#x" + addzero + ss.str();
                } else if ((*leftOperands.begin()).find("SYM") != string::npos) {
                    op = *leftOperands.begin();
                }
                fLine += " " + op;
	    }
	    for (vector<string>::reverse_iterator rit = rightOperators.rbegin(); rit != rightOperators.rend(); ++ rit) {
            	if ((*rit).compare("&") == 0)
                    fLine += " (bvand";
            	else if ((*rit).compare("|") == 0)
                    fLine += " (bvor";
            	else if ((*rit).compare("/") == 0)
                    fLine += " (/";
            }
	    operands = rightOperands;
	} else {
	    if (!bitvectorFlg) {
	        int poss;
                if (((poss = (*rightOperands.begin()).find("CONST_")) != string::npos) && ((*rightOperands.begin()).find_first_of("0123456789") != std::string::npos)) {
		    string value = (*rightOperands.begin()).substr(poss+6); 
                    string newvalue = value;
                    if (value.substr(0,1).compare("-") == 0) {
                        newvalue = "(- " + value.substr(1) + ")";
                    }
		    //cout<<"bbbb: "<<newvalue<<endl;
                    fLine += " " + newvalue;
                    //fLine += " " + (*rightOperands.begin()).substr(poss+6);
                } else fLine += " " + (*rightOperands.begin()); 
	    } else {
		int poss;
            	string op;
            	if (((poss = (*rightOperands.begin()).find("CONST_")) != string::npos) && ((*rightOperands.begin()).find_first_of("0123456789") != std::string::npos)) {
                    int num = atoi((*rightOperands.begin()).substr(6).c_str());
                    unsigned unum = (unsigned)num;
                    stringstream ss;
                    ss<<std::hex<<unum;
                    int size = ss.str().length();
                    string addzero(8-size, '0');
                    op = "#x" + addzero + ss.str();
             	} else if ((*rightOperands.begin()).find("SYM") != string::npos) {
                    op = *rightOperands.begin();
            	}
                fLine += " " + op;
	    }
	    for (vector<string>::reverse_iterator rit = leftOperators.rbegin(); rit != leftOperators.rend(); ++ rit) {
            	if ((*rit).compare("&") == 0)
                    fLine += " (bvand";
            	else if ((*rit).compare("|") == 0)
                    fLine += " (bvor";
            	else if ((*rit).compare("/") == 0)
                    fLine += " (/";
        	}
	    operands = leftOperands;
	}

	if (operands.size() >= 2) { 
	    vector<string>::iterator it = operands.begin();
	    fLine += " " + (*it);
	    ++ it;
    	    for (; it != operands.end(); ++ it) {
		if (!bitvectorFlg) {
		    int poss;
                    if (((poss = (*it).find("CONST_")) != string::npos) && ((*it).find_first_of("0123456789") != std::string::npos)) {
			string value = (*it).substr(poss+6) + ")"; 
                    	string newvalue = value;
                    	if (value.substr(0,1).compare("-") == 0) {
                            newvalue = "(- " + value.substr(1) + ")";
                    	}
			//cout<<"cccc: "<<newvalue<<endl;
                    	fLine += " " + newvalue;
                        //fLine += " " + (*it).substr(poss+6) + ")";
                    } else fLine += " " + (*it) + ")";        
	    	} else {
		    int poss;
	    	    string op;
	    	    if (((poss = (*it).find("CONST_")) != string::npos) && ((*it).find_first_of("0123456789") != std::string::npos)) {
		        int num = atoi((*it).substr(6).c_str());
		        unsigned unum = (unsigned)num;
		    	stringstream ss;
		    	ss<<std::hex<<unum;
		    	//op = "(_bv" + ss.str() + " 32)";
		    	int size = ss.str().length();
		    	string addzero(8-size, '0');
		    	op = "#x" + addzero + ss.str();
	    	    } else if ((*it).find("SYM") != string::npos) {
		    	op = *it;
	            }
    	    	    fLine += " " + op + ")";
		}
	    }
	    fLine += ")";
	}
	else {
	    if (!bitvectorFlg) {
		int poss;
                if (((poss = (*operands.begin()).find("CONST_")) != string::npos) && ((*operands.begin()).find_first_of("0123456789") != std::string::npos)) {
		    string value = (*operands.begin()).substr(poss+6) + ")";
                    string newvalue = value;
                    if (value.substr(0,1).compare("-") == 0) {
                        newvalue = "(- " + value.substr(1) + ")";
                    }
		    //cout<<"dddd: "<<newvalue<<endl;
                    fLine += " " + newvalue;
                    //fLine += " " + (*operands.begin()).substr(poss+6) + ")";
	    	} else fLine += " " + (*operands.begin()) + ")";
	    } else {
		int poss;
	    	string op;
	    	if (((poss = (*operands.begin()).find("CONST_")) != string::npos) && ((*operands.begin()).find_first_of("0123456789") != std::string::npos)) {
                    int num = atoi((*operands.begin()).substr(6).c_str());
                    unsigned unum = (unsigned)num;
                    stringstream ss;
                    ss<<std::hex<<unum;
                    //op = "(_bv" + ss.str() + " 32)";
                    int size = ss.str().length();
                    string addzero(8-size, '0');
                    op = "#x" + addzero + ss.str();
                } else if ((*operands.begin()).find("SYM") != string::npos) {
                    op = *operands.begin();
                }
                fLine += " " + op + ")";
	    }
	}
    
	if (notFlg) fLine += "))";
	else fLine += ")";

	write<<fLine<<endl;	
    }	

    //finally, write the tail
    write<<endl<<"(check-sat)"<<endl<<"(get-model)"<<endl<<"(exit)"<<endl;
    
    read.close();
    write.close();
}


int  main (int argc, char** argv) 
{
    if (argc != 3) {
	cout<<"wrong input."<<endl<<"USAGE: ./transFormula -f filename"<<endl;
	return 0;
    }

    map<string, string> usages;
    usages.insert (pair<string, string> (argv[1], argv[2]));
    
    TransFormula tf (usages);
    tf.Run();
    
    return 0; 
}
