
#include <iostream>
#include <sstream>

#ifndef _WIN32
    #include <pthread.h>
#endif

#include <thread>
#include <opencv2/core.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <chrono>

#ifndef _CommonFlags
    #define _CommonFlags
    #include "CommonFlags.hpp"
#endif

#include <sys/stat.h> // stat
#include <errno.h>    // errno, ENOENT, EEXIST

using namespace cv;
using namespace std;


// This function acquires and saves color images from specified RGB camera.
#if defined(_WIN32)
    DWORD WINAPI AcquireRGBImages(LPVOID lpParam)
    {
#else
    void* AcquireRGBImages(void* arg)
    {
#endif
        VideoCapture cap(CommonFlags::rgb_camera_number + CAP_DSHOW);

        if (!cap.isOpened()) // if not success, exit program
        {
            cout << "Cannot open the video cam" << endl;
            #if defined(_WIN32)
                return -1;
            #else
                return (void *)(-1);
            #endif
        }

        unsigned int acquisition_duration = CommonFlags::acquisition_duration;
        //cap.set(CAP_PROP_SETTINGS, 0);
        //int fourcc = VideoWriter::fourcc('M', 'J', 'P', 'G');
        //int fourcc = VideoWriter::fourcc('Y', 'U', 'Y', 'V');
        //cap.set(CAP_PROP_FOURCC, fourcc);
        //cap.set(CAP_PROP_FRAME_WIDTH, double(CommonFlags::rgb_im_width));
        //cap.set(CAP_PROP_FRAME_HEIGHT, double(CommonFlags::rgb_im_height));
        //cap.set(CAP_PROP_FPS, double(CommonFlags::rgb_fps));
        //cap.set(CAP_PROP_BUFFERSIZE, 3);

        double dWidth = cap.get(CAP_PROP_FRAME_WIDTH);   // get the width of frames of the video
        double dHeight = cap.get(CAP_PROP_FRAME_HEIGHT); // get the height of frames of the video
        cout << "Frame size : " << dWidth << " x " << dHeight << endl;

        bool stop_exp = false;

        if (CommonFlags::flag_show_rgb) {
            namedWindow("RGBVideo", WINDOW_AUTOSIZE); // create a window called "RGBVideo"
            moveWindow("RGBVideo", CommonFlags::thermal_im_width + 100, 0);
        }

        cout << "RGB camera is ready to start acquisition." << endl;
        CommonFlags::rgb_cam_ready = true;

        if (CommonFlags::flag_capture_thermal) //&& CommonFlags::flag_save_rgb)
        {
            if (!(CommonFlags::thermal_cam_ready))
            {
                cout << "RGB camera waiting for thermal camera to get ready" << endl;
                while (1)
                {
                    if (CommonFlags::thermal_cam_ready)
                        break;
                    else
                        this_thread::sleep_for(std::chrono::microseconds(CommonFlags::thread_sleep_interval_acquisition));
                }
            }
            
            cout << "RGB camera waiting for synchronized start..." << endl;

            while(1)
            {
                if (CommonFlags::synchronized_start)
                    break;
                else
                    this_thread::sleep_for(std::chrono::microseconds(CommonFlags::thread_sleep_interval_acquisition));
            }
        }

        else if (CommonFlags::flag_save_rgb)
        {
            cout << "Waiting for manual trigger for synchronized start. Press ENTER to begin acquisition" << endl;
            char a = getchar();
            CommonFlags::synchronized_start = true;
        }

        Mat frame;
        const auto begin = std::chrono::system_clock::now();

        while (1)
        {

            bool bSuccess = cap.read(frame); 

            if (!bSuccess) // if not success, break loop
            {
                cout << "Cannot read a frame from video stream" << endl;
                break;
            }

            if (CommonFlags::flag_save_rgb)
            {
                //vid_write.write(frame);
                ostringstream filename;
                const auto p2 = std::chrono::system_clock::now();
                filename << CommonFlags::data_dir_rgb;
                filename << std::chrono::duration_cast<std::chrono::milliseconds>(p2.time_since_epoch()).count();
                filename << ".bmp";
                imwrite(filename.str().c_str(), frame);
            }

            if (CommonFlags::flag_show_rgb)
            {
                imshow("RGBVideo", frame);
            }

            if (acquisition_duration > 0)
            {
                stop_exp = 1000 * acquisition_duration < std::chrono::duration_cast<std::chrono::milliseconds > (std::chrono::system_clock::now() - begin).count();
            }

            if (CommonFlags::flag_show_rgb)
            {
                if ((waitKey(1) == 27) || CommonFlags::synchronized_stop == true || stop_exp)
                {
                    cout << "Stopping RGB image acquisition ..." << endl;
                    CommonFlags::synchronized_stop = true;
                    break;
                }
            }
            else
            {
                if (CommonFlags::synchronized_stop == true || stop_exp)
                {
                    cout << "Stopping RGB image acquisition ..." << endl;
                    CommonFlags::synchronized_stop = true;
                    break;
                }
            }
            this_thread::sleep_for(std::chrono::microseconds(CommonFlags::thread_sleep_interval_acquisition));   
        }

        // When everything done, release the video capture object
        
        cap.release();
        //if (CommonFlags::flag_save_rgb) {
        //    vid_write.release();
        //}

        // Closes all the frames
        if (CommonFlags::flag_show_rgb) {
            destroyAllWindows();
        }

        #if defined(_WIN32)
            return 0;
        #else
            return (void *)0;
        #endif
        // cout << "Cannot open the video cam" << endl;
    }


