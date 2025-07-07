#ifndef _CommonFlags
  #define _CommonFlags
#endif

#include <string>
#include <atomic>
#include <opencv2/core.hpp>
#include "Spinnaker.h"
#include "SpinGenApi/SpinnakerGenApi.h"

using namespace Spinnaker;
using namespace Spinnaker::GenApi;
using namespace Spinnaker::GenICam;
using namespace std;
using namespace cv;

// CommonFlags.h
class CommonFlags
{
  public:
    static atomic<bool> thermalFrameAvailableforShow;
    static atomic<bool> configure_thermal_camera;
    static atomic<bool> flag_capture_thermal;
    static atomic<bool> flag_capture_rgb;
    static atomic<bool> flag_save_thermal;
    static atomic<bool> flag_save_rgb;
    static atomic<bool> flag_show_thermal;
    static atomic<bool> flag_show_rgb;

    static atomic<bool> thermal_cam_ready;
    static atomic<bool> rgb_cam_ready;
    static atomic<bool> synchronized_start;
    static atomic<bool> synchronized_stop;
    static std::string rgb_camera_gst_pipeline;
    static unsigned int rgb_camera_number;
    static unsigned int rgb_fps;
    static unsigned int rgb_im_width;
    static unsigned int rgb_im_height;

    static atomic<bool> capture_phys;
    static std::string serialPort;
    static std::string phys_channels;
    static int baudrate;

    static unsigned int thread_sleep_interval_acquisition;
    static unsigned int thread_sleep_interval_save;
    static unsigned int thread_sleep_interval_display;
    static unsigned int acquisition_duration;

    static unsigned int thermal_fps;
    static unsigned int thermal_im_width;
    static unsigned int thermal_im_height;
    static gcstring thermal_AcquisitionMode;
    static gcstring thermal_StreamBufferHandlingMode;
    static gcstring thermal_PixelFormat;
    static gcstring thermal_TemperatureLinearMode;
    static gcstring thermal_TemperatureLinearResolution;
    static gcstring thermal_CMOSBitDepth;
    static gcstring thermal_NUCMode;

    static std::string base_save_path;
    static std::string participant_id;
    static std::string exp_condition;
    static std::string data_dir_thermal;
    static std::string data_dir_rgb;
    static std::string data_dir_phys;
    static ImagePtr pResultImageShow;
};

atomic<bool> CommonFlags::configure_thermal_camera(false);
atomic<bool> CommonFlags::thermalFrameAvailableforShow(false);
atomic<bool> CommonFlags::flag_capture_thermal(false);
atomic<bool> CommonFlags::flag_capture_rgb(false);
atomic<bool> CommonFlags::flag_save_thermal(false);
atomic<bool> CommonFlags::flag_save_rgb(false);
atomic<bool> CommonFlags::flag_show_thermal(false);
atomic<bool> CommonFlags::flag_show_rgb(false);
atomic<bool> CommonFlags::thermal_cam_ready(false);
atomic<bool> CommonFlags::rgb_cam_ready(false);
atomic<bool> CommonFlags::synchronized_start(false);
atomic<bool> CommonFlags::synchronized_stop(false);

unsigned int CommonFlags::rgb_camera_number = 0;
unsigned int CommonFlags::rgb_fps = 60;
unsigned int CommonFlags::rgb_im_width = 1280;
unsigned int CommonFlags::rgb_im_height = 720;
std::string CommonFlags::data_dir_rgb = "rgb/";

unsigned int CommonFlags::thermal_fps = 30;
unsigned int CommonFlags::thermal_im_width = 640;
unsigned int CommonFlags::thermal_im_height = 512;
gcstring CommonFlags::thermal_AcquisitionMode = gcstring("Continuous");
gcstring CommonFlags::thermal_StreamBufferHandlingMode = gcstring("OldestFirst");
gcstring CommonFlags::thermal_PixelFormat = gcstring("Mono14");
gcstring CommonFlags::thermal_TemperatureLinearMode = gcstring("On");
gcstring CommonFlags::thermal_TemperatureLinearResolution = gcstring("High");
gcstring CommonFlags::thermal_CMOSBitDepth = gcstring("bit14bit");
gcstring CommonFlags::thermal_NUCMode = gcstring("Manual");
std::string CommonFlags::data_dir_thermal = "thermal/";
ImagePtr CommonFlags::pResultImageShow = Image::Create();

atomic<bool> CommonFlags::capture_phys(false);
std::string CommonFlags::serialPort = "";
std::string CommonFlags::phys_channels = "";
int CommonFlags::baudrate = 0;
std::string CommonFlags::data_dir_phys = "phys/";

unsigned int CommonFlags::thread_sleep_interval_acquisition = 500;
unsigned int CommonFlags::thread_sleep_interval_save = 500;
unsigned int CommonFlags::thread_sleep_interval_display = 500;
unsigned int CommonFlags::acquisition_duration = 60;
std::string CommonFlags::base_save_path = "./";
std::string CommonFlags::participant_id = "pxx";
std::string CommonFlags::exp_condition = "default";
