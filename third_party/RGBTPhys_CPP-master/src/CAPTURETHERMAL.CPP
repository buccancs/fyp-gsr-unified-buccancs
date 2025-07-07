#include <chrono>
#include <thread>

#include "Spinnaker.h"
#include "SpinGenApi/SpinnakerGenApi.h"
// #include "SpinVideo.h"

#include <opencv2/core.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "AutoForceIP.cpp"
#include "SetupFlirCam.cpp"

#ifndef _CommonFlags
#define _CommonFlags
#include "CommonFlags.hpp"
#endif

using namespace Spinnaker;
using namespace Spinnaker::GenApi;
using namespace Spinnaker::GenICam;
using namespace std;
using namespace cv;

// This function acquires and saves thermal images from FLIR AX5 camera.
#if defined(_WIN32)
DWORD WINAPI AcquireThermalImages(LPVOID lpParam)
{
#else
void *AcquireThermalImages(void *arg)
{
#endif
    try
    {

        // Force IP address to be setup automatically.
        /*
        if (CommonFlags::configure_thermal_camera)
        {
            AutoForceIP();
        }*/

        // Retrieve singleton reference to system object
        SystemPtr system = System::GetInstance();

        // Print out current library version
        const LibraryVersion spinnakerLibraryVersion = system->GetLibraryVersion();
        cout << "Spinnaker library version: " << spinnakerLibraryVersion.major << "." << spinnakerLibraryVersion.minor
             << "." << spinnakerLibraryVersion.type << "." << spinnakerLibraryVersion.build << endl
             << endl;

        // Retrieve list of cameras from the system
        CameraList camList = system->GetCameras();

        unsigned int numCameras = camList.GetSize();

        cout << "Number of cameras detected: " << numCameras << endl
             << endl;

        // Finish if there are no cameras
        if (numCameras == 0)
        {
            // Clear camera list before releasing system
            camList.Clear();

            // Release system
            system->ReleaseInstance();

            cout << "No thermal camera found!" << endl;
            return -1;
        }

        CameraPtr pCam = nullptr;
        pCam = camList.GetByIndex(0);


        // Retrieve TL device nodemap
        INodeMap &nodeMapTLDevice = pCam->GetTLDeviceNodeMap();

        // Retrieve device serial number for filename
        CStringPtr ptrStringSerial = pCam->GetTLDeviceNodeMap().GetNode("DeviceSerialNumber");

        std::string serialNumber = "";

        if (IsAvailable(ptrStringSerial) && IsReadable(ptrStringSerial))
        {
            serialNumber = ptrStringSerial->GetValue();
        }

        cout << endl
             << "[" << serialNumber << "] "
             << "*** IMAGE ACQUISITION THREAD STARTING"
             << " ***" << endl
             << endl;

        // Initialize camera
        pCam->Init();

        // Print device information
        PrintDeviceInfo(nodeMapTLDevice, serialNumber);

        bool stop_exp = false;
        ConfigureUserSet1(pCam, CommonFlags::configure_thermal_camera);

        CommonFlags::thermal_cam_ready = true;
        cout << "Thermal camera is ready to start acquisition..." << endl;

        if (CommonFlags::flag_capture_rgb) // && CommonFlags::flag_save_thermal)
        {
            if (!(CommonFlags::rgb_cam_ready))
            {
                cout << "Thermal camera waiting for RGB camera to get ready" << endl;
                while (1)
                {
                    if (CommonFlags::rgb_cam_ready)
                        break;
                    else
                        this_thread::sleep_for(std::chrono::microseconds(CommonFlags::thread_sleep_interval_acquisition));
                }
            }
            cout << "Thermal camera waiting for manual trigger for synchronized start. Press ENTER to begin acquisition" << endl;
            char a = getchar();
            CommonFlags::synchronized_start = true;
        }
        else if (CommonFlags::flag_save_thermal)
        {
            cout << "Waiting for manual trigger for synchronized start. Press ENTER to begin acquisition" << endl;
            char a = getchar();
            CommonFlags::synchronized_start = true;
        }

        // Begin acquiring images
        pCam->BeginAcquisition();

        cout << "[" << serialNumber << "] "
             << "Started acquiring images..." << endl;
        const auto begin = std::chrono::system_clock::now();

        while (1)
        {
            try
            {
                // Retrieve next received image and ensure image completion
                ImagePtr pResultImage = pCam->GetNextImage(1000);

                if (pResultImage->IsIncomplete())
                {
                    cout << "[" << serialNumber << "] "
                         << "Image incomplete with image status " << pResultImage->GetImageStatus() << "..." << endl
                         << endl;
                    CommonFlags::thermalFrameAvailableforShow = false;
                }
                else
                {
                    if (CommonFlags::flag_save_thermal)
                    {
                        ostringstream filename;
                        const auto p1 = std::chrono::system_clock::now();

                        filename << CommonFlags::data_dir_thermal;
                        filename << std::chrono::duration_cast<std::chrono::milliseconds>(p1.time_since_epoch()).count();
                        filename << ".raw";

                        pResultImage->Save(filename.str().c_str());
                    }
                    if (CommonFlags::flag_show_thermal && !(CommonFlags::thermalFrameAvailableforShow))
                    {
                        CommonFlags::pResultImageShow->DeepCopy(pResultImage);
                        CommonFlags::thermalFrameAvailableforShow = true;
                    }
                }

                // Release image
                pResultImage->Release();

                // cout << endl;
            }
            catch (Spinnaker::Exception &e)
            {
                cout << "[" << serialNumber << "] "
                     << "Error: " << e.what() << endl;
            }

            if (CommonFlags::acquisition_duration > 0)
            {
                stop_exp = 1000 * CommonFlags::acquisition_duration < std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now() - begin).count();
            }

            if (CommonFlags::synchronized_stop == true || stop_exp) // wait for 'esc' key press for 30ms. If 'esc' key is pressed, break loop
            {
                cout << "Stopping thermal image acquisition ..." << endl;
                CommonFlags::synchronized_stop = true;
                break;
            }
            else
            {
                this_thread::sleep_for(std::chrono::microseconds(CommonFlags::thread_sleep_interval_acquisition));
            }
        }

        // End acquisition
        pCam->EndAcquisition();

        ResetCameraUserSetToDefault(pCam);

        // Deinitialize camera
        pCam->DeInit();

        pCam = nullptr;

        // Clear camera list before releasing system
        camList.Clear();

        // Release system
        system->ReleaseInstance();

#if defined(_WIN32)
        return 0;
#else
        return (void *)0;
#endif
    }
    catch (Spinnaker::Exception &e)
    {
        cout << "Error: " << e.what() << endl;
#if defined(_WIN32)
        return 1;
#else
        return (void *)1;
#endif
    }
}



// This function saves acquired thermal images.
#if defined(_WIN32)
DWORD WINAPI ShowThermalImages(LPVOID lpParam)
{
#else
void *ShowThermalImages(void *arg)
{
#endif
    ImageProcessor processor;
    processor.SetColorProcessing(Spinnaker::SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR);
    namedWindow("ThermalVideo", WINDOW_AUTOSIZE);
    moveWindow("ThermalVideo", 0, 0);

    while (1)
    {
        if (CommonFlags::thermalFrameAvailableforShow)
        {
            CommonFlags::pResultImageShow = processor.Convert(CommonFlags::pResultImageShow, PixelFormat_Mono16);

            int XPadding = int(CommonFlags::pResultImageShow->GetXPadding());
            int YPadding = int(CommonFlags::pResultImageShow->GetYPadding());
            int rowsize = int(CommonFlags::pResultImageShow->GetWidth());
            int colsize = int(CommonFlags::pResultImageShow->GetHeight());

            // image data contains padding. When allocating Mat container size, you need to account for the X,Y image data padding.
            Mat cvimg = cv::Mat(colsize + YPadding, rowsize + XPadding, CV_16UC1, CommonFlags::pResultImageShow->GetData(), CommonFlags::pResultImageShow->GetStride());
            cvimg.convertTo(cvimg, CV_32F);
            cv::normalize(cvimg, cvimg, 0, 1, cv::NORM_MINMAX);
            cvimg = cvimg * 255;
            cvimg.convertTo(cvimg, CV_8UC1);
            applyColorMap(cvimg, cvimg, COLORMAP_MAGMA);
            imshow("ThermalVideo", cvimg);

            CommonFlags::thermalFrameAvailableforShow = false;
        }
        else
        {
            this_thread::sleep_for(std::chrono::microseconds(CommonFlags::thread_sleep_interval_display));
        }

        if ((waitKey(1) == 27) || (CommonFlags::synchronized_stop == true))
        {
            CommonFlags::synchronized_stop = true;
            cout << "Stopping thermal image streaming ..." << endl;
            break;
        }
    }

#if defined(_WIN32)
    return 0;
#else
    return (void *)0;
#endif
}
