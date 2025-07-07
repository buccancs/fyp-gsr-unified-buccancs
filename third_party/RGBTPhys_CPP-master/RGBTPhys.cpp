#if defined(_WIN32)
    #include <Windows.h>
#endif

#include <iostream>
#include <sstream>

#ifndef _WIN32
#include <pthread.h>
#endif
#include <thread>

#ifndef _CommonFlags
#define _CommonFlags
#include "src/CommonFlags.hpp"
#endif

#include "src/ConfigReader.cpp"
#include "src/CaptureRGB.cpp"
#include "src/CaptureThermal.cpp"
#include "src/Utils.cpp"
#include "src/SerialCom.cpp"
#include <string>

using namespace std;

// This function acts as the body of the example
int RunThreadedImageAcquisition(void)
{
    int result = 0;
    unsigned int num_threads = 0;
    unsigned int curr_thread = 0;


    if (CommonFlags::flag_capture_thermal)
    {
        num_threads = num_threads + 1;
        if (CommonFlags::flag_show_thermal)
        {
            num_threads = num_threads + 1;
        }
    }

    if (CommonFlags::flag_capture_rgb)
    {
        num_threads = num_threads + 1;
    }

    //For Serial Port Reading
    if (CommonFlags::capture_phys)
    {
        num_threads = num_threads + 1;
    }
    

    try
    {
        // Create an array of handles
#if defined(_WIN32)
        HANDLE* grabThreads = new HANDLE[num_threads];
#else
        pthread_t* grabThreads = new pthread_t[num_threads];
#endif

#if defined(_WIN32)
        if (CommonFlags::flag_capture_thermal)
        {
            grabThreads[curr_thread] = CreateThread(nullptr, 0, AcquireThermalImages, nullptr, 0, nullptr);
            assert(grabThreads[curr_thread] != nullptr);
            curr_thread = curr_thread + 1;
            if (CommonFlags::flag_show_thermal)
            {
                grabThreads[curr_thread] = CreateThread(nullptr, 0, ShowThermalImages, nullptr, 0, nullptr);
                assert(grabThreads[curr_thread] != nullptr);
                curr_thread = curr_thread + 1;
            }
        }

        if (CommonFlags::flag_capture_rgb)
        {
            grabThreads[curr_thread] = CreateThread(nullptr, 0, AcquireRGBImages, nullptr, 0, nullptr);
            assert(grabThreads[curr_thread] != nullptr);
            curr_thread = curr_thread + 1;
        }

        if (CommonFlags::capture_phys)
        {
            grabThreads[curr_thread] = CreateThread(nullptr, 0, ReadSerialPort, nullptr, 0, nullptr);
            assert(grabThreads[curr_thread] != nullptr);
            curr_thread = curr_thread + 1;
        }


#else
        curr_thread = 0;

        if (CommonFlags::flag_capture_thermal)
        {
            int thermal_acq_err = pthread_create(&(grabThreads[curr_thread]), nullptr, &AcquireThermalImages, nullptr);
            assert(thermal_acq_err == 0);
            curr_thread = curr_thread + 1;
            if (CommonFlags::flag_show_thermal)
            {
                int thermal_show_err = pthread_create(&(grabThreads[curr_thread]), nullptr, &ShowThermalImages, nullptr);
                assert(thermal_show_err == 0);
                curr_thread = curr_thread + 1;
            }
        }

        if (CommonFlags::flag_capture_rgb)
        {
            int rgb_acq_err = pthread_create(&(grabThreads[curr_thread]), nullptr, &AcquireRGBImages, nullptr);
            assert(rgb_acq_err == 0);
            curr_thread = curr_thread + 1;
        }

        int phys_acq_err = pthread_create(&(grabThreads[curr_thread]), nullptr, &ReadSerialPort, nullptr);
        assert(phys_acq_err == 0);
        curr_thread = curr_thread + 1;

#endif

#if defined(_WIN32)
        // Wait for all threads to finish
        WaitForMultipleObjects(
            num_threads, // number of threads to wait for
            grabThreads, // handles for threads to wait for
            TRUE,        // wait for all of the threads
            INFINITE     // wait forever
        );

        // Check thread return code for each camera
        for (unsigned int i = 0; i < num_threads; i++)
        {
            DWORD exitcode;

            BOOL rc = GetExitCodeThread(grabThreads[i], &exitcode);
            if (!rc)
            {
                cout << "Handle error from GetExitCodeThread() returned for camera at index " << i << endl;
                result = -1;
            }
            else if (exitcode != 0)
            {
                cout << "Grab thread for camera at index " << i
                    << " exited with errors."
                    "Please check onscreen print outs for error details"
                    << endl;
                result = -1;
            }
        }

#else
        for (unsigned int i = 0; i < num_threads; i++)
        {
            // Wait for all threads to finish
            void* exitcode;
            int rc = pthread_join(grabThreads[i], &exitcode);
            if (rc != 0)
            {
                cout << "Handle error from pthread_join returned for camera at index " << i << endl;
                result = -1;
            }
            else if ((int)(intptr_t)exitcode != 0) // check thread return code for each camera
            {
                cout << "Grab thread for camera at index " << i
                    << " exited with errors."
                    "Please check onscreen print outs for error details"
                    << endl;
                result = -1;
            }
        }
#endif

        // Clear CameraPtr array and close all handles
        for (unsigned int i = 0; i < num_threads; i++)
        {
            #if defined(_WIN32)
                CloseHandle(grabThreads[i]);
            #endif
        }

        // Delete array pointer
        delete[] grabThreads;
    }
    catch (std::exception const& e)
    {
        cout << "Error: " << e.what() << endl;
        result = -1;
    }

    return result;
}

// Example entry point; please see Enumeration example for more in-depth
// comments on preparing and cleaning up the system.
int main(int argc, char* argv[])
{
    if (argc < 4)
    { // This prohram expects one argument: the program name and a path to the configuration file
        std::cerr << "Usage: " << argv[0] << " <CONFIG_FILEPATH> " << " <Base_Save_Path> " << " <Participant ID> " << std::endl;
        return 1;
    }
    #ifndef _WIN32
        system("sudo sysctl -p");
    #endif
    
    // Print application build information
    cout << "Application build date: " << __DATE__ << " " << __TIME__ << endl << endl;

    ConfigReader* p = ConfigReader::getInstance(); // Create object of the class ConfigReader
    p->parseFile(argv[1]); // parse the configuration file
    p->dumpFileValues(); // Dump map on the console after parsing it

    CommonFlags::base_save_path = argv[2];
    CommonFlags::participant_id = argv[3];

    cout << "Using config file: " << argv[1] << endl;
    cout << "Base save path: " << CommonFlags::base_save_path << endl;
    cout << "Participant ID: " << CommonFlags::participant_id << endl;

    cout << endl << "Press Enter to continue..." << endl;
    char a = getchar();

    int thread_sleep_interval_acquisition = 0;
    int thread_sleep_interval_save = 0;
    int thread_sleep_interval_display = 0;
    int acquisition_duration = 0;
    std::string exp_condition = "";

    std::string capture_thermal = "";
    std::string configure_thermal_camera = "";
    std::string save_thermal = "";
    std::string show_thermal = "";
    int thermal_fps = 0;
    int thermal_im_width = 0;
    int thermal_im_height = 0;
    std::string thermal_AcquisitionMode = "";
    std::string thermal_StreamBufferHandlingMode = "";
    std::string thermal_PixelFormat = "";
    std::string thermal_TemperatureLinearMode = "";
    std::string thermal_TemperatureLinearResolution = "";
    std::string thermal_CMOSBitDepth = "";

    std::string capture_rgb = "";
    int rgb_camera_number = 0;
    std::string save_rgb = "";
    std::string show_rgb = "";
    int rgb_im_width = 0;
    int rgb_im_height = 0;
    int rgb_fps = 0;

    std::string capture_phys = "";

    p->getValue("thread_sleep_interval_acquisition", thread_sleep_interval_acquisition);
    CommonFlags::thread_sleep_interval_acquisition = thread_sleep_interval_acquisition;

    p->getValue("thread_sleep_interval_save", thread_sleep_interval_save);
    CommonFlags::thread_sleep_interval_save = thread_sleep_interval_save;

    p->getValue("thread_sleep_interval_display", thread_sleep_interval_display);
    CommonFlags::thread_sleep_interval_display = thread_sleep_interval_display;

    p->getValue("acquisition_duration", acquisition_duration);
    CommonFlags::acquisition_duration = acquisition_duration;

    p->getValue("exp_condition", exp_condition);
    CommonFlags::exp_condition = exp_condition;

    p->getValue("configure_thermal_camera", configure_thermal_camera);
    if (configure_thermal_camera.compare("true") == 0)
    {
        CommonFlags::configure_thermal_camera = true;
    }

    p->getValue("capture_thermal", capture_thermal);
    if (capture_thermal.compare("true") == 0)
    {
        CommonFlags::flag_capture_thermal = true;
    }

    p->getValue("thermal_fps", thermal_fps);
    CommonFlags::thermal_fps = thermal_fps;

    p->getValue("thermal_im_width", thermal_im_width);
    CommonFlags::thermal_im_width = thermal_im_width;

    p->getValue("thermal_im_height", thermal_im_height);
    CommonFlags::thermal_im_height = thermal_im_height;

    p->getValue("thermal_AcquisitionMode", thermal_AcquisitionMode);
    CommonFlags::thermal_AcquisitionMode = gcstring(thermal_AcquisitionMode.c_str());

    p->getValue("thermal_StreamBufferHandlingMode", thermal_StreamBufferHandlingMode);
    CommonFlags::thermal_StreamBufferHandlingMode = gcstring(thermal_StreamBufferHandlingMode.c_str());

    p->getValue("thermal_PixelFormat", thermal_PixelFormat);
    CommonFlags::thermal_PixelFormat = gcstring(thermal_PixelFormat.c_str());

    p->getValue("thermal_TemperatureLinearMode", thermal_TemperatureLinearMode);
    CommonFlags::thermal_TemperatureLinearMode = gcstring(thermal_TemperatureLinearMode.c_str());

    p->getValue("thermal_TemperatureLinearResolution", thermal_TemperatureLinearResolution);
    CommonFlags::thermal_TemperatureLinearResolution = gcstring(thermal_TemperatureLinearResolution.c_str());

    p->getValue("thermal_CMOSBitDepth", thermal_CMOSBitDepth);
    CommonFlags::thermal_CMOSBitDepth = gcstring(thermal_CMOSBitDepth.c_str());

    p->getValue("show_thermal", show_thermal);
    if (show_thermal.compare("true") == 0)
    {
        CommonFlags::flag_show_thermal = true;
    }

    p->getValue("save_thermal", save_thermal);
    if (save_thermal.compare("true") == 0)
    {
        CommonFlags::flag_save_thermal = true;
    }

    p->getValue("capture_rgb", capture_rgb);
    if (capture_rgb.compare("true") == 0)
    {
        CommonFlags::flag_capture_rgb = true;
    }

    p->getValue("rgb_im_width", rgb_im_width);
    CommonFlags::rgb_im_width = rgb_im_width;

    p->getValue("rgb_im_height", rgb_im_height);
    CommonFlags::rgb_im_height = rgb_im_height;

    p->getValue("rgb_camera_number", rgb_camera_number);
    CommonFlags::rgb_camera_number = rgb_camera_number;

    p->getValue("show_rgb", show_rgb);
    if (show_rgb.compare("true") == 0)
    {
        CommonFlags::flag_show_rgb = true;
    }

    p->getValue("save_rgb", save_rgb);
    if (save_rgb.compare("true") == 0)
    {
        CommonFlags::flag_save_rgb = true;
    }

    p->getValue("capture_phys", capture_phys);
    if (capture_phys.compare("true") == 0)
    {
        CommonFlags::capture_phys = true;
        p->getValue("com_port", CommonFlags::serialPort);
        p->getValue("baud_rate", CommonFlags::baudrate);
        p->getValue("phys_channels", CommonFlags::phys_channels);
    }

    if (CommonFlags::flag_save_thermal || CommonFlags::flag_save_rgb)
    {
        if (!(isDirExist(CommonFlags::base_save_path + '/' + CommonFlags::participant_id)))
        {
            cout << "Creating directory for participant " << CommonFlags::participant_id << " at: " << CommonFlags::base_save_path << "..." << endl;
            try
            {
                makePath(CommonFlags::base_save_path + '/' + CommonFlags::participant_id);
            }
            catch (std::exception const& e)
            {
                cout << "Unable to create directory to save images... " << e.what() << endl;
                return -1;
            }
        }
        // else
        // {
        //     cout << "Participant ID already exists: " << CommonFlags::participant_id << ". Please check participant ID" << endl;
        //     return -1;
        // }

        if (CommonFlags::flag_save_thermal)
        {
            CommonFlags::data_dir_thermal = CommonFlags::base_save_path + '/' + CommonFlags::participant_id + "/" + CommonFlags::exp_condition + "_" + CommonFlags::data_dir_thermal;
            if (!(isDirExist(CommonFlags::data_dir_thermal)))
            {
                cout << "Creating directory for saving thermal images at: " << CommonFlags::data_dir_thermal << "..." << endl;
                try
                {
                    makePath(CommonFlags::data_dir_thermal);
                }
                catch (std::exception const& e)
                {
                    cout << "Unable to create directory to save thermal images... " << e.what() << endl;
                    return -1;
                }
            }
            else
            {
                cout << "Thermal data for participant ID: " << CommonFlags::participant_id << "and experiment condition" << CommonFlags::exp_condition << " already exists. Please check participant ID" << endl;
                return -1;
            }
        }

        if (CommonFlags::flag_save_rgb)
        {
            CommonFlags::data_dir_rgb = CommonFlags::base_save_path + '/' + CommonFlags::participant_id + "/" + CommonFlags::exp_condition + "_" + CommonFlags::data_dir_rgb;
            if (!(isDirExist(CommonFlags::data_dir_rgb)))
            {
                cout << "Creating directory for saving rgb images at: " << CommonFlags::data_dir_rgb << "..." << endl;
                try
                {
                    makePath(CommonFlags::data_dir_rgb);
                }
                catch (std::exception const& e)
                {
                    cout << "Unable to create directory to save rgb images... " << e.what() << endl;
                    return -1;
                }
            }
            else
            {
                cout << "RGB data for participant ID: " << CommonFlags::participant_id << "and experiment condition" << CommonFlags::exp_condition << " already exists. Please check participant ID" << endl;
                return -1;
            }
        }
        if (CommonFlags::capture_phys)
        {
            CommonFlags::data_dir_phys = CommonFlags::base_save_path + '/' + CommonFlags::participant_id + "/" + CommonFlags::exp_condition + "_" + CommonFlags::data_dir_phys;
            if (!(isDirExist(CommonFlags::data_dir_phys)))
            {
                cout << "Creating directory for saving physiological data at: " << CommonFlags::data_dir_phys << "..." << endl;
                try
                {
                    makePath(CommonFlags::data_dir_phys);
                }
                catch (std::exception const& e)
                {
                    cout << "Unable to create directory to save physiological data... " << e.what() << endl;
                    return -1;
                }
            }
            else
            {
                cout << "Physiological data for participant ID: " << CommonFlags::participant_id << "and experiment condition" << CommonFlags::exp_condition << " already exists. Please check participant ID" << endl;
                return -1;
            }

        }

    }

    cout << "configure_thermal_camera: " << CommonFlags::configure_thermal_camera << endl;
    cout << "flag_capture_thermal: " << CommonFlags::flag_capture_thermal << endl;
    cout << "flag_capture_rgb: " << CommonFlags::flag_capture_rgb << endl;
    cout << "flag_save_thermal: " << CommonFlags::flag_save_thermal << endl;
    cout << "flag_save_rgb: " << CommonFlags::flag_save_rgb << endl;
    cout << "flag_show_thermal: " << CommonFlags::flag_show_thermal << endl;
    cout << "flag_show_rgb: " << CommonFlags::flag_show_rgb << endl;
    cout << "rgb_camera_number: " << CommonFlags::rgb_camera_number << endl;

    int result = 0;

    result = RunThreadedImageAcquisition();

    cout << "Acquisition complete..." << endl << endl;
    cout << endl << "Done! Press Enter to exit..." << endl;
    char aa = getchar();

    return result;
}

/*

config params:
    thread sleep
    fps
    rgb camera number
    path to save data
    rgb focus value
    rgb exposure value
    participant id
    acquisition duration

---------------------
feature:
    Reset configuration of thermal camera before and after acquisition
    NUC - manual or execute on start before acquisition, and set interval exceeding 5 minutes
    Synchronize the start of RGB acquisition - with flag
    in save only mode with no imshow, present first frame
    acquisition duration
    thread sleep e.g. 1 ms
    check for space availability
    multiple config files passed as command line argument
        RGBT
        RGBOnly
        ThermalOnly (for focus adjustment)
        RGBOnlyShow (for focus adjustment)
        ThermalOnlyShow

*/




/*
// RGBTPhys.cpp : This file contains the 'main' function. Program execution begins and ends there.
//

#include <iostream>

int main()
{
    std::cout << "Hello World!\n";
}

// Run program: Ctrl + F5 or Debug > Start Without Debugging menu
// Debug program: F5 or Debug > Start Debugging menu

// Tips for Getting Started: 
//   1. Use the Solution Explorer window to add/manage files
//   2. Use the Team Explorer window to connect to source control
//   3. Use the Output window to see build output and other messages
//   4. Use the Error List window to view errors
//   5. Go to Project > Add New Item to create new code files, or Project > Add Existing Item to add existing code files to the project
//   6. In the future, to open this project again, go to File > Open > Project and select the .sln file
*/