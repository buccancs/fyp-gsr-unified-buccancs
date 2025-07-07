#include "SerialCom.h"

#ifndef _CommonFlags
#define _CommonFlags
#include "CommonFlags.hpp"
#endif


SimpleSerial::SimpleSerial(char* com_port, DWORD COM_BAUD_RATE)
{
	connected_ = false;

	io_handler_ = CreateFileA(static_cast<LPCSTR>(com_port),
		GENERIC_READ | GENERIC_WRITE,
		0,
		NULL,
		OPEN_EXISTING,
		FILE_ATTRIBUTE_NORMAL,
		NULL);

	if (io_handler_ == INVALID_HANDLE_VALUE) {

		if (GetLastError() == ERROR_FILE_NOT_FOUND)
			printf("Warning: Handle was not attached. Reason: %s not available\n", com_port);
	}
	else {

		DCB dcbSerialParams = { 0 };

		if (!GetCommState(io_handler_, &dcbSerialParams)) {

			printf("Warning: Failed to get current serial params");
		}

		else {
			dcbSerialParams.BaudRate = COM_BAUD_RATE;
			dcbSerialParams.ByteSize = 8;
			dcbSerialParams.StopBits = ONESTOPBIT;
			dcbSerialParams.Parity = NOPARITY;
			dcbSerialParams.fDtrControl = DTR_CONTROL_ENABLE;

			if (!SetCommState(io_handler_, &dcbSerialParams))
				printf("Warning: could not set serial port params\n");
			else {
				connected_ = true;
				PurgeComm(io_handler_, PURGE_RXCLEAR | PURGE_TXCLEAR);
			}
		}
	}
}


void SimpleSerial::ReadSerialPort() {
	DWORD bytes_read;
	char inc_msg[2];
	char string_out[200];
	unsigned int CharCount = 0;
	int acquisition_duration = CommonFlags::acquisition_duration;

	time_t start_time = time(nullptr);

	ClearCommError(io_handler_, &errors_, &status_);

	ostringstream filename;
	const auto p2 = std::chrono::system_clock::now();
	filename << CommonFlags::data_dir_phys;
	filename << std::chrono::duration_cast<std::chrono::milliseconds>(p2.time_since_epoch()).count();
	filename << ".csv";
	std::ofstream fileHandle;
	fileHandle.open(filename.str(), std::ofstream::out | std::ofstream::app);
	fileHandle << CommonFlags::phys_channels << endl;

	bool stop_exp = false;
	cout << "Serial reading waiting for synchronized start..." << endl;

	//while ((time(nullptr) - start_time) < acquisition_duration) {
	// keep reading serial port to keep buffer clean before synchronized start
	while (1){
		if (CommonFlags::synchronized_start) {
			cout << "Serial recording started ..." << endl;
			break;
		}
		else {
			if (ReadFile(io_handler_, &inc_msg, 1, &bytes_read, NULL)) {
				if ((inc_msg[0] != '\n') && (inc_msg[0] != '\r')) {
					string_out[CharCount] = inc_msg[0];
					CharCount++;
				}
				else if (inc_msg[0] == '\r') {
					string_out[CharCount] = '\0';
					CharCount = 0;
					string_out[0] = '\0';
				}
			}
			else {
				cout << "\nWarning: Failed to receive data ..." << endl;
			}
			//this_thread::sleep_for(std::chrono::microseconds(CommonFlags::thread_sleep_interval_acquisition));
		}
	}
	const auto begin = std::chrono::system_clock::now();

	while (1) {
		if (ReadFile(io_handler_, &inc_msg, 1, &bytes_read, NULL)) {
			if ((inc_msg[0] != '\n') && (inc_msg[0] != '\r')) {
				string_out[CharCount] = inc_msg[0];
				CharCount++;
			}
			else if (inc_msg[0] == '\r') {
				string_out[CharCount] = '\0';
				fileHandle << string_out << ',' << endl;
				CharCount = 0;
				string_out[0] = '\0';
			}
		}
		else
		{
			cout << "\nWarning: Failed to receive data ..." << endl;
		}
		if (acquisition_duration > 0)
		{
			stop_exp = 1000 * acquisition_duration < std::chrono::duration_cast<std::chrono::milliseconds> (std::chrono::system_clock::now() - begin).count();
		}

		if (CommonFlags::synchronized_stop == true || stop_exp)
		{
			cout << "Stopping serial data acquisition ..." << endl;
			CommonFlags::synchronized_stop = true;
			break;
		}
	}
	fileHandle.close();
}


bool SimpleSerial::CloseSerialPort()
{
	if (connected_) {
		connected_ = false;
		CloseHandle(io_handler_);
		return true;
	}
	else
		return false;
}

SimpleSerial::~SimpleSerial()
{
	if (connected_) {
		connected_ = false;
		CloseHandle(io_handler_);
	}
}


#if defined(_WIN32)
DWORD WINAPI ReadSerialPort(LPVOID lpParam)
{
#else
void* ReadSerialPort(void* arg)
{
#endif
	char serialPort[] = "COM4";
	SimpleSerial* serObj = new SimpleSerial(serialPort, 2000000);

	serObj->ReadSerialPort();

	#if defined(_WIN32)
		return 0;
	#else
		return (void*)0;
	#endif
}