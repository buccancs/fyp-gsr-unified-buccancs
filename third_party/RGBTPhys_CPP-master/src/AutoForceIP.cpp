#include "Spinnaker.h"
#include "SpinGenApi/SpinnakerGenApi.h"
#include <iostream>

using namespace Spinnaker;
using namespace Spinnaker::GenApi;
using namespace Spinnaker::GenICam;
using namespace std;

void AutoForceIP()
{
    cout << "---> Setting all GigE cameras discovered to an IP configuration" << endl
         << "---> that will allow it to work with Spinnaker..." << endl
         << endl;

    //
    // Retrieve singleton reference to system object
    //
    // *** NOTES ***
    // Everything originates with the system object. It is important to notice
    // that it has a singleton implementation, so it is impossible to have
    // multiple system objects at the same time. Users can only get a smart
    // pointer (SystemPtr) to the system instance.
    //
    // *** LATER ***
    // The system object should be cleared prior to program completion. If not
    // released explicitly, it will be released automatically when all SystemPtr
    // objects that point to the system go out of scope.
    SystemPtr pSystem = System::GetInstance();

    //
    // Retrieve list of interfaces from the system
    //
    // *** NOTES ***
    // Interface lists are retrieved from the system object.
    //
    // *** LATER ***
    // Interface lists must be cleared manually. This must be done prior to
    // releasing the system and while the interface list is still in scope.
    //
    InterfaceList interfaceList = pSystem->GetInterfaces();

    // Iterate through available interfaces and attempt to execute force IP
    for (unsigned int i = 0; i < interfaceList.GetSize(); i++)
    {
        InterfacePtr pInterface = interfaceList.GetByIndex(i);

        //
        // Retrieve TL nodemap from interface
        //
        // *** NOTES ***
        // Each interface has a nodemap that can be retrieved in order to
        // access information about the interface itself, any devices
        // connected, or addressing information if applicable.
        //
        INodeMap &nodeMapInterface = pInterface->GetTLNodeMap();

        CEnumerationPtr ptrInterfaceType = nodeMapInterface.GetNode("InterfaceType");
        if (!IsAvailable(ptrInterfaceType) || !IsReadable(ptrInterfaceType))
        {
            cout << "Unable to read InterfaceType for interface at index " << i << endl;
            continue;
        }

        if (ptrInterfaceType->GetIntValue() != InterfaceType_GigEVision)
        {
            // Only force IP on GEV interface
            continue;
        }

        //
        // Print interface display name
        //
        // *** NOTES ***
        // Grabbing node information requires first retrieving the node and
        // then retrieving its information. There are two things to keep in
        // mind. First, a node is distinguished by type, which is related
        // to its value's data type. Second, nodes should be checked for
        // availability and readability/writability prior to making an
        // attempt to read from or write to the node.
        //
        CStringPtr ptrInterfaceDisplayName = nodeMapInterface.GetNode("InterfaceDisplayName");
        if (IsAvailable(ptrInterfaceDisplayName) && IsReadable(ptrInterfaceDisplayName))
        {
            gcstring interfaceDisplayName = ptrInterfaceDisplayName->GetValue();
            cout << "*** " << interfaceDisplayName << " ***" << endl;
        }
        else
        {
            cout << "*** "
                 << "Unknown Interface (Display name not readable)"
                 << " ***" << endl;
        }

        CCommandPtr ptrAutoForceIP = nodeMapInterface.GetNode("GevDeviceAutoForceIP");
        if (IsAvailable(ptrAutoForceIP) && IsWritable(ptrAutoForceIP))
        {
            if (!IsWritable(pInterface->TLInterface.DeviceSelector.GetAccessMode()))
            {
                cout << "Unable to write to the DeviceSelector node while forcing IP" << endl;
            }
            else
            {
                const int cameraCount = pInterface->GetCameras().GetSize();
                for (int i = 0; i < cameraCount; i++)
                {
                    pInterface->TLInterface.DeviceSelector.SetValue(i);
                    pInterface->TLInterface.GevDeviceAutoForceIP.Execute();
                    cout << "AutoForceIP executed for camera at index " << i << endl;
                }
            }
        }
        else
        {
            cout << "Warning : Force IP node not available for this interface" << endl;
        }

        cout << endl;
    }

    interfaceList.Clear();
    pSystem->ReleaseInstance();

    cout << "---> Auto-configuration complete" << endl
         << endl;
}