#include "Spinnaker.h"
#include "SpinGenApi/SpinnakerGenApi.h"

using namespace Spinnaker;
using namespace Spinnaker::GenApi;
using namespace Spinnaker::GenICam;
using namespace std;

#ifndef _CommonFlags
#define _CommonFlags
#include "CommonFlags.hpp"
#endif



// This function prints the device information of the camera from the transport
// layer; please see NodeMapInfo example for more in-depth comments on printing
// device information from the nodemap.
int PrintDeviceInfo(INodeMap& nodeMap, std::string camSerial)
{
    int result = 0;

    cout << "[" << camSerial << "] Printing device information ..." << endl << endl;

    FeatureList_t features;
    CCategoryPtr category = nodeMap.GetNode("DeviceInformation");
    if (IsAvailable(category) && IsReadable(category))
    {
        category->GetFeatures(features);

        FeatureList_t::const_iterator it;
        for (it = features.begin(); it != features.end(); ++it)
        {
            CNodePtr pfeatureNode = *it;
            CValuePtr pValue = (CValuePtr)pfeatureNode;
            cout << "[" << camSerial << "] " << pfeatureNode->GetName() << " : "
                << (IsReadable(pValue) ? pValue->ToString() : "Node not readable") << endl;
        }
    }
    else
    {
        cout << "[" << camSerial << "] "
            << "Device control information not available." << endl;
    }

    cout << endl;

    return result;
}



// This function configures newly discovered cameras with desired settings, saves
// them to User Set 1 and sets this as the default. This way cameras will not need
// to be reconfigured if they are disconnected during the example. Note that this may
// overwrite current settings for User Set 1; please see ImageFormatControl example
// for more in-depth comments on camera configuration.
bool ConfigureUserSet1(CameraPtr pCam, bool setParameters)
{
    bool result = true;

    try
    {
        // Get the camera node map
        INodeMap& nodeMap = pCam->GetNodeMap();
        INodeMap& sNodeMap = pCam->GetTLStreamNodeMap();

        // Get User Set 1 from the User Set Selector
        CEnumerationPtr ptrUserSetSelector = nodeMap.GetNode("UserSetSelector");
        if (!(IsAvailable(ptrUserSetSelector)) || !(IsWritable(ptrUserSetSelector)))
        {
            cout << "unable to set user set selector to user set 1 (node retrieval). aborting..." << endl << endl;
            return false;
        }

        CEnumEntryPtr ptrUserSet1 = ptrUserSetSelector->GetEntryByName("UserSet1");
        if (!IsReadable(ptrUserSet1))
        {
            cout << "Unable to get User Set Selector to User Set 1 (enum entry retrieval). Aborting..." << endl << endl;
            return false;
        }
        const int64_t userSet1 = ptrUserSet1->GetValue();

        // Set User Set Selector to User Set 1
        ptrUserSetSelector->SetIntValue(userSet1);

        if (setParameters)
        {
            /******************************************************************************************************/
            // Set acquisition mode to continuous
            CEnumerationPtr ptrAcquisitionMode = nodeMap.GetNode("AcquisitionMode");
            if (!IsReadable(ptrAcquisitionMode))
            {
                cout << "Unable to get acquisition mode to continuous (node retrieval). Aborting..." << endl << endl;
                return false;
            }

            CEnumEntryPtr ptrAcquisitionModeContinuous = ptrAcquisitionMode->GetEntryByName(CommonFlags::thermal_AcquisitionMode);
            if (!IsReadable(ptrAcquisitionModeContinuous))
            {
                cout << "Unable to set acquisition mode to continuous (enum entry retrieval). Aborting..." << endl << endl;
                return false;
            }

            const int64_t acquisitionModeContinuous = ptrAcquisitionModeContinuous->GetValue();

            if (!IsWritable(ptrAcquisitionMode))
            {
                cout << "Unable to set acquisition mode to continuous (node retrieval). Aborting..." << endl << endl;
                return false;
            }

            ptrAcquisitionMode->SetIntValue(acquisitionModeContinuous);

            /******************************************************************************************************/

            // Set NUCMode to manual
            // Retrieve the enumeration node from the nodemap
            CEnumerationPtr NUCMode_node = nodeMap.GetNode("NUCMode");
            if (IsAvailable(NUCMode_node) && IsWritable(NUCMode_node))
            {
                // Retrieve the desired entry node from the enumeration node
                CEnumEntryPtr NUCMode_Target = NUCMode_node->GetEntryByName(CommonFlags::thermal_NUCMode);
                if (IsAvailable(NUCMode_Target) && IsReadable(NUCMode_Target))
                {
                    // Retrieve the integer value from the entry node
                    int64_t NUCMode = NUCMode_Target->GetValue();

                    // Set integer as new value for enumeration node
                    NUCMode_node->SetIntValue(NUCMode);

                    cout << "NUCMode set to " << NUCMode_node->GetCurrentEntry()->GetSymbolic() << "..." << endl;
                }
                else
                {
                    cout << "NUCMode not set to " << CommonFlags::thermal_NUCMode << " ..." << endl;
                }
            }
            else
            {
                cout << "NUCMode not available..." << endl;
            }


            /******************************************************************************************************/

            // Set bufferhandling mode to newest_only
            CEnumerationPtr ptrStreamBufferMode = sNodeMap.GetNode("StreamBufferHandlingMode");
            if (IsAvailable(ptrStreamBufferMode) && IsWritable(ptrStreamBufferMode))
            {
                // Retrieve the desired entry node from the enumeration node
                CEnumEntryPtr ptrStreamBufferModeNewestOnly = ptrStreamBufferMode->GetEntryByName(CommonFlags::thermal_StreamBufferHandlingMode); // "NewestOnly", "OldestFirst"
                if (IsAvailable(ptrStreamBufferModeNewestOnly) && IsReadable(ptrStreamBufferModeNewestOnly))
                {
                    // Retrieve the integer value from the entry node
                    int64_t streamingBufferNewest = ptrStreamBufferModeNewestOnly->GetValue();

                    // Set integer as new value for enumeration node
                    ptrStreamBufferMode->SetIntValue(streamingBufferNewest);

                    cout << "StreamBufferHandling Mode set to " << ptrStreamBufferMode->GetCurrentEntry()->GetSymbolic() << "..." << endl;
                }
                else
                {
                    cout << "StreamBufferHandling Mode " << CommonFlags::thermal_StreamBufferHandlingMode << " not available..." << endl;
                }
            }
            else
            {
                cout << "StreamBufferHandling Mode not available..." << endl;
            }

            /******************************************************************************************************/

            // Apply mono  pixel format
            // Retrieve the enumeration node from the nodemap
            CEnumerationPtr ptrPixelFormat = nodeMap.GetNode("PixelFormat");
            if (IsAvailable(ptrPixelFormat) && IsWritable(ptrPixelFormat))
            {
                // Retrieve the desired entry node from the enumeration node
                CEnumEntryPtr ptrPixelFormatMono = ptrPixelFormat->GetEntryByName(CommonFlags::thermal_PixelFormat);
                if (IsAvailable(ptrPixelFormatMono) && IsReadable(ptrPixelFormatMono))
                {
                    // Retrieve the integer value from the entry node
                    int64_t pixelFormatMono = ptrPixelFormatMono->GetValue();

                    // Set integer as new value for enumeration node
                    ptrPixelFormat->SetIntValue(pixelFormatMono);

                    cout << "Pixel format set to " << ptrPixelFormat->GetCurrentEntry()->GetSymbolic() << "..." << endl;
                }
                else
                {
                    cout << "Pixel format " << CommonFlags::thermal_PixelFormat << " not available..." << endl;
                }
            }
            else
            {
                cout << "Pixel format not available..." << endl;
            }

            /******************************************************************************************************/
            // Turn ON Linear Temperature Mode
            // Retrieve the enumeration node from the nodemap
            CEnumerationPtr temperatureLinearMode_node = nodeMap.GetNode("TemperatureLinearMode");
            if (IsAvailable(temperatureLinearMode_node) && IsWritable(temperatureLinearMode_node))
            {
                // Retrieve the desired entry node from the enumeration node
                CEnumEntryPtr temperatureLinearMode_entry_ON = temperatureLinearMode_node->GetEntryByName(CommonFlags::thermal_TemperatureLinearMode);
                if (IsAvailable(temperatureLinearMode_entry_ON) && IsReadable(temperatureLinearMode_entry_ON))
                {
                    // Retrieve the integer value from the entry node
                    int64_t temperatureLinearMode = temperatureLinearMode_entry_ON->GetValue();

                    // Set integer as new value for enumeration node
                    temperatureLinearMode_node->SetIntValue(temperatureLinearMode);

                    cout << "Linear temperature mode set to " << temperatureLinearMode_node->GetCurrentEntry()->GetSymbolic() << "..." << endl;
                }
                else
                {
                    cout << "Linear temperature mode not set to: " << CommonFlags::thermal_TemperatureLinearMode << endl;
                }
            }
            else
            {
                cout << "Linear temperature mode not available..." << endl;
            }

            /******************************************************************************************************/

            // Set Temperature Resolution to High
            // Retrieve the enumeration node from the nodemap
            CEnumerationPtr temperatureResolution_node = nodeMap.GetNode("TemperatureLinearResolution");
            if (IsAvailable(temperatureResolution_node) && IsWritable(temperatureResolution_node))
            {
                // Retrieve the desired entry node from the enumeration node
                CEnumEntryPtr temperatureResolution_entry_HIGH = temperatureResolution_node->GetEntryByName(CommonFlags::thermal_TemperatureLinearResolution);
                if (IsAvailable(temperatureResolution_entry_HIGH) && IsReadable(temperatureResolution_entry_HIGH))
                {
                    // Retrieve the integer value from the entry node
                    int64_t temperatureResolution = temperatureResolution_entry_HIGH->GetValue();

                    // Set integer as new value for enumeration node
                    temperatureResolution_node->SetIntValue(temperatureResolution);

                    cout << "Temperature resolution set to " << temperatureResolution_node->GetCurrentEntry()->GetSymbolic() << "..." << endl;
                }
                else
                {
                    cout << "Temperature resolution not set to " << CommonFlags::thermal_TemperatureLinearResolution << " ..." << endl;
                }
            }
            else
            {
                cout << "Temperature resolution not available..." << endl;
            }

            /******************************************************************************************************/
            // Set CMOSBitDepth to bit14bit
            // Retrieve the enumeration node from the nodemap
            CEnumerationPtr CMOSBitDepth_node = nodeMap.GetNode("CMOSBitDepth");
            if (IsAvailable(CMOSBitDepth_node) && IsWritable(CMOSBitDepth_node))
            {
                // Retrieve the desired entry node from the enumeration node
                CEnumEntryPtr CMOSBitDepth_entry_14 = CMOSBitDepth_node->GetEntryByName(CommonFlags::thermal_CMOSBitDepth);
                if (IsAvailable(CMOSBitDepth_entry_14) && IsReadable(CMOSBitDepth_entry_14))
                {
                    // Retrieve the integer value from the entry node
                    int64_t CMOSBitDepth = CMOSBitDepth_entry_14->GetValue();

                    // Set integer as new value for enumeration node
                    CMOSBitDepth_node->SetIntValue(CMOSBitDepth);

                    cout << "CMOSBitDepth set to " << CMOSBitDepth_node->GetCurrentEntry()->GetSymbolic() << "..." << endl;
                }
                else
                {
                    cout << "CMOSBitDepth not set to " << CommonFlags::thermal_CMOSBitDepth << " ..." << endl;
                }
            }
            else
            {
                cout << "CMOSBitDepth not available..." << endl;
            }

            /******************************************************************************************************/

            // Execute User Set Save to save User Set 1
            CCommandPtr ptrUserSetSave = nodeMap.GetNode("UserSetSave");
            if (!ptrUserSetSave.IsValid())
            {
                cout << "Unable to save Settings to User Set 1. Aborting..." << endl << endl;
                return false;
            }
            ptrUserSetSave->Execute();

            /*
            // Set User Set Default to User Set 1
            // This ensures the camera will re-enumerate using User Set 1, instead of the default user set.
            CEnumerationPtr ptrUserSetDefault = nodeMap.GetNode("UserSetDefault");
            if (!IsWritable(ptrUserSetDefault))
            {
                cout << "Unable to set User Set Default to User Set 1 (node retrieval). Aborting..." << endl << endl;
                return false;
            }

            ptrUserSetDefault->SetIntValue(userSet1);
            */

            /******************************************************************************************************/
        }
        /*
        else
        {
            // Execute User Set Load to load UserSet1
            CCommandPtr ptrUserSetLoad = nodeMap.GetNode("UserSetLoad");
            if (!ptrUserSetLoad.IsValid())
            {
                cout << "Unable to load Settings from UserSet1. Aborting..." << endl << endl;
                return false;
            }

            ptrUserSetLoad->Execute();
        }
        */

        // Execute NUCAction
        CCommandPtr ptrNUCAction = nodeMap.GetNode("NUCAction");
        if (ptrNUCAction.IsValid())
        {
            ptrNUCAction->Execute();
        }
        else
        {
            cout << "Unable to perform NUCAction..." << endl << endl;
        }
        

    }
    catch (std::exception& e)
    {
        cout << "Error configuring user set 1: " << e.what() << endl;
        result = false;
    }

    return result;
}

// This function resets the cameras default User Set to the Default User Set.
// Note that User Set 1 will retain the settings set during ConfigureUserSet1.
void ResetCameraUserSetToDefault(CameraPtr pCam)
{
    try
    {
        // Get the camera node map
        INodeMap& nodeMap = pCam->GetNodeMap();

        // Execute NUCAction
        CCommandPtr ptrNUCAction = nodeMap.GetNode("NUCAction");
        if (ptrNUCAction.IsValid())
        {
            ptrNUCAction->Execute();
        }
        else
        {
            cout << "Unable to perform NUCAction..." << endl << endl;
        }

        // Get User Set Default from User Set Selector
        CEnumerationPtr ptrUserSetSelector = nodeMap.GetNode("UserSetSelector");
        if (!IsReadable(ptrUserSetSelector) ||
            !IsWritable(ptrUserSetSelector))
        {
            cout << "Unable to set User Set Selector to Default (node retrieval). Aborting..." << endl << endl;
            return;
        }

        CEnumEntryPtr ptrUserSetDefaultEntry = ptrUserSetSelector->GetEntryByName("Default");
        if (!IsReadable(ptrUserSetDefaultEntry))
        {
            cout << "Unable to get User Set Selector to Default (enum entry retrieval). Aborting..." << endl << endl;
            return;
        }

        const int64_t userSetDefault = ptrUserSetDefaultEntry->GetValue();

        // Set User Set Selector back to User Set Default
        ptrUserSetSelector->SetIntValue(userSetDefault);

        /*
        // Set User Set Default to User Set Default
        CEnumerationPtr ptrUserSetDefault = nodeMap.GetNode("UserSetDefault");
        if (!IsWritable(ptrUserSetDefault))
        {
            cout << "Unable to set User Set Default to User Set 1 (node retrieval). Aborting..." << endl << endl;
            return;
        }

        ptrUserSetDefault->SetIntValue(userSetDefault);

        // Execute User Set Load to load User Set Default
        CCommandPtr ptrUserSetLoad = nodeMap.GetNode("UserSetLoad");
        if (!ptrUserSetLoad.IsValid())
        {
            cout << "Unable to load Settings from User Set Default. Aborting..." << endl << endl;
            return;
        }
        ptrUserSetLoad->Execute();
        */

    }
    catch (std::exception& e)
    {
        cout << "Error resetting camera to use default user set: " << e.what() << endl;
    }
}