#pragma once

#include <Windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <sstream>
#include <string>
#include <string.h>
#include <fstream>
#include <chrono>

#ifndef _WIN32
#include <pthread.h>
#endif

#include <thread>


using namespace std;

class SimpleSerial
{

private:
	HANDLE io_handler_;
	COMSTAT status_;
	DWORD errors_;

public:
	SimpleSerial(char* com_port, DWORD COM_BAUD_RATE);
	void ReadSerialPort();

	bool CloseSerialPort();
	~SimpleSerial();
	bool connected_;
};
