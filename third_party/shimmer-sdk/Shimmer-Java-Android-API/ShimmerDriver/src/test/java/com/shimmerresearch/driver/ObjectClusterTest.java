/*Rev 0.3
 * 
 *  Copyright (c) 2010, Shimmer Research, Ltd.
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:

 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Shimmer Research, Ltd. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Jong Chern Lim
 * @date   October, 2013
 * 
 * Changes since 0.2
 * - SDLog support
 * 
 * Changes since 0.1
 * - Added method to remove a format 
 * 
 */
package com.shimmerresearch.driver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE;
import com.shimmerresearch.grpc.ShimmerGRPC.ObjectCluster2;
import com.shimmerresearch.grpc.ShimmerGRPC.ObjectCluster2.Builder;
import com.shimmerresearch.grpc.ShimmerGRPC.ObjectCluster2.FormatCluster2;
import com.shimmerresearch.grpc.ShimmerGRPC.ObjectCluster2.FormatCluster2.DataCluster2;
import com.shimmerresearch.sensors.SensorShimmerClock;
import com.shimmerresearch.driverUtilities.ChannelDetails;
import com.shimmerresearch.driverUtilities.ChannelDetails.CHANNEL_TYPE;

final public class ObjectClusterTest implements Cloneable,Serializable{
	
	private static final long serialVersionUID = -7601464501144773539L;
	
	// ----------------- JOS: Temp for testing START ----------------- //
	
	//JC: Only temporary for testing this class can be deleted if we decide not to use it in the future
	public ArrayList<SensorData> mSensorDataList = new ArrayList<SensorData>();
	public HashMap<String, HashMap<String, FormatCluster>> mHashMap = new HashMap<>();
	public HashMap<String, FormatCluster[]> mHashMapArray = new HashMap<>();
	public SensorData[] mSensorDataArray = new SensorData[50];
	public int mSensorDataArrayIndex = 0;
	public int dataStructureSelector = 1;	//1 = ArrayList, 2 = HashMap, 3 = HashMapArray, 4 = Arrays, 5 = Multimap
	
	// ----------------- JOS: Temp for testing END ----------------- //

	
	public Multimap<String, FormatCluster> mPropertyCluster = HashMultimap.create();
	//TODO implement below to remove the need for the Guava library?
//	private HashMap<String, HashMap<CHANNEL_TYPE, FormatCluster>> mPropertyClusterProposed = new HashMap<String, HashMap<CHANNEL_TYPE, FormatCluster>>();

	/**
	 * Some times it is necessary to get a list of Channels from the
	 * mPropertyCluster in order of insertion which is not possible with the
	 * Multimap approach. This separate list was created to keep a record
	 * of the order of insertion.
	 */
	private List<String> listOfChannelNames = new ArrayList<String>(); 

	private String mMyName;
	private String mBluetoothAddress;
	
	// ------- Old Array approach - Start -----------
	public byte[] mRawData;
	public double[] mUncalData = new double[50];
	public double[] mCalData = new double[50];
	/** The SensorNames array is an older approach and has been largely replaced
	 * by the mPropertyCluster. Suggest using getChannelNamesByInsertionOrder()
	 * or getChannelNamesFromKeySet() instead. */
	@Deprecated
	public String[] mSensorNames;
	public String[] mUnitCal = new String[50];
	public String[] mUnitUncal = new String[50];
	
	public String[] mSensorNamesCal = new String[50];	//JOS: would 2D arrays be better here?
	public String[] mSensorNamesUncal = new String[50];
	
	//JOS: TEST ARRAYS RESIZING HERE
	public String[] mSensorNamesCalResize;
	public String[] mUnitCalResize;
	public double[] mCalDataResize;
	public double[] mCalDataNew = new double[50];
	public double[] mUncalDataNew = new double[50];
	
	SensorDataArray sensorDataArray = new SensorDataArray(50);
	
	// ------- Old Array approach - End -----------
	
	/** mObjectClusterBuilder needs to be uninitialized to avoid crash when connecting on Android */
	private Builder mObjectClusterBuilder; 
	
	private int indexKeeper = 0;
	
	public byte[] mSystemTimeStamp = new byte[8];
	private double mTimeStampMilliSecs;
	public boolean mIsValidObjectCluster = true;
	
	public boolean useList = false;
	
	public int mPacketIdValue = 0;
	
	public enum OBJECTCLUSTER_TYPE{
		ARRAYS,
		FORMAT_CLUSTER,
		PROTOBUF
	}
	public static List<OBJECTCLUSTER_TYPE> mListOfOCTypesEnabled = Arrays.asList(
			OBJECTCLUSTER_TYPE.ARRAYS,
			OBJECTCLUSTER_TYPE.FORMAT_CLUSTER,
			OBJECTCLUSTER_TYPE.PROTOBUF);

	//TODO remove this variable? unused in PC applications
	public BT_STATE mState;

	
	public ObjectClusterTest(){
	}
	
	public ObjectClusterTest(String myName){
		mMyName = myName;
	}

	public ObjectClusterTest(String myName, String myBlueAdd){
		this(myName);
		mBluetoothAddress=myBlueAdd;
	}

	//TODO remove this constructor? unused in PC applications
	public ObjectClusterTest(String myName, String myBlueAdd, BT_STATE state){
		this(myName, myBlueAdd);
		mState = state;
	}
	
	public ObjectClusterTest(ObjectCluster2 ojc2){
		ojc2.getDataMap().get("");
		for (String channelName:ojc2.getDataMap().keySet()){
			FormatCluster2 fc=ojc2.getDataMap().get(channelName);
			for (String formatName:fc.getFormatMap().keySet()){
				DataCluster2 data = fc.getFormatMap().get(formatName);
				addDataToMap(channelName,formatName,data.getUnit(),data.getData(),data.getDataArrayList());
			}
		}
		mBluetoothAddress = ojc2.getBluetoothAddress();
		mMyName = ojc2.getName();
	}
	
	public String getShimmerName(){
		return mMyName;
	}
	
	public void setShimmerName(String name){
		mMyName = name;
	}
	
	public String getMacAddress(){
		return mBluetoothAddress;
	}
	
	public void setMacAddress(String macAddress){
		mBluetoothAddress = macAddress;
	}
	
	/**
	 * Takes in a collection of Format Clusters and returns the Format Cluster specified by the string format
	 * @param collectionFormatCluster
	 * @param format 
	 * @return FormatCluster
	 */
	public static FormatCluster returnFormatCluster(Collection<FormatCluster> collectionFormatCluster, String format){
		FormatCluster returnFormatCluster = null;

		Iterator<FormatCluster> iFormatCluster = collectionFormatCluster.iterator();
		while(iFormatCluster.hasNext()){
			FormatCluster formatCluster = iFormatCluster.next();
			if (formatCluster.mFormat.equals(format)){
				returnFormatCluster = formatCluster;
			}
		}
		return returnFormatCluster;
	}

	public double getFormatClusterValueDefaultFormat(ChannelDetails channelDetails){
		return getFormatClusterValue(channelDetails.mObjectClusterName, channelDetails.mListOfChannelTypes.get(0).toString());
	}

	public double getFormatClusterValue(ChannelDetails channelDetails, CHANNEL_TYPE channelType){
		return getFormatClusterValue(channelDetails.mObjectClusterName, channelType.toString());
	}
	
//	public double getFormatClusterValue(ChannelDetails channelDetails, String format){
//		return getFormatClusterValue(channelDetails.mObjectClusterName, format);
//	}

	public double getFormatClusterValue(String channelName, String format){
		FormatCluster formatCluster = getLastFormatCluster(channelName, format);
		if(formatCluster!=null){
			return formatCluster.mData;
		}
		return Double.NaN;
	}
	
	public FormatCluster getLastFormatCluster(String channelName, String format){
		Collection<FormatCluster> formatClusterCollection = getCollectionOfFormatClusters(channelName);
		if(formatClusterCollection != null){
			FormatCluster formatCluster = ObjectCluster.returnFormatCluster(formatClusterCollection, format);
			if(formatCluster!=null){
				return formatCluster;
			}
		}
		return null;
	}

	/**
	 * Users should note that a property has to be removed before it is replaced
	 * @param propertyname Property name you want to delete
	 * @param formatname Format you want to delete
	 */
	public void removePropertyFormat(String propertyname, String formatname){
		Collection<FormatCluster> colFormats = mPropertyCluster.get(propertyname); 
		// first retrieve all the possible formats for the current sensor device
		FormatCluster formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(colFormats,formatname)); // retrieve format;
		mPropertyCluster.remove(propertyname, formatCluster);
	}
	
	/**Serializes the object cluster into an array of bytes
	 * @return byte[] an array of bytes
	 * @see java.io.Serializable
	 */
	public byte[] serialize() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
			return baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public List<String[]> generateArrayOfChannelsSorted(){
		List<String[]> listofSignals = new ArrayList<String[]>();
		int size=0;
		for (String fckey : getChannelNamesFromKeySet() ) {
			size++;
		}
		
		//arrange the properties
		String[] properties = new String[size];
		int y=0;
		for (String fckey : getChannelNamesFromKeySet() ) {
			properties[y]=fckey;
			y++;
		}
		
		Arrays.sort(properties);
		
		// now need to try arrange the formats
		int index=0;
		String property;
		for (int k=0;k<size;k++){
			property = properties[k];
			Collection<FormatCluster> ofFormatstemp = getCollectionOfFormatClusters(property);
			// the iterator does not have the same order
			int tempSize=0;
			for (FormatCluster fctemp:ofFormatstemp){
				tempSize++;
			}
			
			String[] formats = new String[tempSize];
			String[] units = new String[tempSize];
			int p=0;
			//sort the formats
			for (FormatCluster fctemp:ofFormatstemp){
				formats[p]=fctemp.mFormat;
				p++;
			
			}
			
			Arrays.sort(formats);
			for (int u=0;u<formats.length;u++){
				for (FormatCluster fctemp:ofFormatstemp){
					if (fctemp.mFormat.equals(formats[u])){
						units[u]=fctemp.mUnits;
					}
				}
			}
			
			for (int u=0;u<formats.length;u++){
				String[] channel = {mMyName,property,formats[u],units[u]};
				listofSignals.add(channel);
				//System.out.println(":::" + address + property + fc.mFormat);		
				System.out.println("Index" + index); 
				
			}
			
		
		}
		return listofSignals;
	}
	
	public List<String[]> generateArrayOfChannels(){
		//First retrieve all the unique keys from the objectClusterLog
		Multimap<String, FormatCluster> m = mPropertyCluster;

		int size = m.size();
		System.out.print(size);
		mSensorNames=new String[size];
		String[] sensorFormats=new String[size];
		String[] sensorUnits=new String[size];
		String[] sensorIsUsingDefaultCal=new String[size];
		int i=0;
		int p=0;
		for(String key : m.keys()) {
			//first check that there are no repeat entries

			if(compareStringArray(mSensorNames, key) == true) {
				for(FormatCluster formatCluster : m.get(key)) {
					sensorFormats[p]=formatCluster.mFormat;
					sensorUnits[p]=formatCluster.mUnits;
					sensorIsUsingDefaultCal[p]=(formatCluster.mIsUsingDefaultCalibration? "*":"");
					//Log.d("Shimmer",key + " " + mSensorFormats[p] + " " + mSensorUnits[p]);
					p++;
				}

			}	

			mSensorNames[i]=key;
			i++;				 
		}
		return getListofEnabledSensorSignalsandFormats(mMyName, mSensorNames, sensorFormats, sensorUnits, sensorIsUsingDefaultCal);
	}
	
	private static List<String[]> getListofEnabledSensorSignalsandFormats(String myName, String[] sensorNames, String[] sensorFormats, String[] sensorUnits, String[] sensorIsUsingDefaultCal){
		List<String[]> listofSignals = new ArrayList<String[]>();
		for (int i=0;i<sensorNames.length;i++){
			String[] channel = new String[]{myName,sensorNames[i],sensorFormats[i],sensorUnits[i],sensorIsUsingDefaultCal[i]};
			listofSignals.add(channel);
		}
		
		return listofSignals;
	}
	
	private boolean compareStringArray(String[] stringArray, String string){
		boolean uniqueString=true;
		int size = stringArray.length;
		for (int i=0;i<size;i++){
			if (stringArray[i]==string){
				uniqueString=false;
			}	
					
		}
		return uniqueString;
	}
	
	public void createArrayData(int length){
		if(mListOfOCTypesEnabled.contains(OBJECTCLUSTER_TYPE.ARRAYS)){
			mUncalData = new double[length];
			mCalData = new double[length];
			mSensorNames = new String[length];
			mUnitCal = new String[length];
			mUnitUncal = new String[length];
		}
	}

	public void addData(ChannelDetails channelDetails, double uncalData, double calData) {
		addData(channelDetails, uncalData, calData, false);
	}

	public void addData(ChannelDetails channelDetails, double uncalData, double calData, boolean usingDefaultParameters) {
		addData(channelDetails, uncalData, calData, indexKeeper, usingDefaultParameters);
	}

	public void addData(ChannelDetails channelDetails, double uncalData, double calData, int index, boolean usingDefaultParameters) {
		if(channelDetails.mListOfChannelTypes.contains(CHANNEL_TYPE.UNCAL)){
			addUncalData(channelDetails, uncalData, index);
		}
		if(channelDetails.mListOfChannelTypes.contains(CHANNEL_TYPE.CAL)){
			addCalData(channelDetails, calData, index, usingDefaultParameters);
		}
		//TODO decide whether to include the below here
//		incrementIndexKeeper();
	}

	public void addCalData(ChannelDetails channelDetails, double calData) {
		addCalData(channelDetails, calData, indexKeeper);
	}

	public void addCalData(ChannelDetails channelDetails, double calData, int index) {
		addCalData(channelDetails, calData, index, false);
	}

	public void addCalData(ChannelDetails channelDetails, double calData, int index, boolean usingDefaultParameters) {
		addData(channelDetails.mObjectClusterName, CHANNEL_TYPE.CAL, channelDetails.mDefaultCalUnits, calData, index, usingDefaultParameters);
	}

	public void addUncalData(ChannelDetails channelDetails, double uncalData) {
		addUncalData(channelDetails, uncalData, indexKeeper);
	}

	public void addUncalData(ChannelDetails channelDetails, double uncalData, int index) {
		addData(channelDetails.mObjectClusterName, CHANNEL_TYPE.UNCAL, channelDetails.mDefaultUncalUnit, uncalData, index);
	}
	
	public void addData(String objectClusterName, CHANNEL_TYPE channelType, String units, double data) {
		addData(objectClusterName, channelType, units, data, indexKeeper);
	}

	public void addData(String objectClusterName, CHANNEL_TYPE channelType, String units, double data, int index) {
		addData(objectClusterName, channelType, units, data, index, false);
	}
	
	public void addData(String objectClusterName, CHANNEL_TYPE channelType, String units, double data, int index, boolean isUsingDefaultCalib) {
		if(mListOfOCTypesEnabled.contains(OBJECTCLUSTER_TYPE.ARRAYS)){
			if(channelType==CHANNEL_TYPE.CAL){
				mCalData[index] = data;
				mUnitCal[index] = units;
			}
			else if(channelType==CHANNEL_TYPE.UNCAL){
				mUncalData[index] = data;
				mUnitUncal[index] = units;
			}
			//TODO below not really needed, just put in to match some legacy code but can be removed. 
			else if(channelType==CHANNEL_TYPE.DERIVED){
				mCalData[index] = data;
				mUnitCal[index] = units;
				mUncalData[index] = data;
				mUnitUncal[index] = units;
			}
			mSensorNames[index] = objectClusterName;
			
			//TODO implement below here and remove everywhere else in the code
//			incrementIndexKeeper();
		}
		
		if(mListOfOCTypesEnabled.contains(OBJECTCLUSTER_TYPE.FORMAT_CLUSTER)){
			addDataToMap(objectClusterName, channelType.toString(), units, data, isUsingDefaultCalib);
		}
		
		if(mListOfOCTypesEnabled.contains(OBJECTCLUSTER_TYPE.PROTOBUF)){
			//TODO
		}
	}

	public void incrementIndexKeeper(){
		if(mListOfOCTypesEnabled.contains(OBJECTCLUSTER_TYPE.ARRAYS)){
			if(indexKeeper<mCalData.length){
				indexKeeper++;
			}
		}
	}

	public int getIndexKeeper() {
		return indexKeeper;
	}

	public void setIndexKeeper(int indexKeeper) {
		this.indexKeeper = indexKeeper;
	}
	
	public void addCalDataToMap(ChannelDetails channelDetails, double data){
		addDataToMap(channelDetails.mObjectClusterName, CHANNEL_TYPE.CAL.toString(), channelDetails.mDefaultCalUnits, data);
	}

	public void addUncalDataToMap(ChannelDetails channelDetails, double data){
		addDataToMap(channelDetails.mObjectClusterName, CHANNEL_TYPE.UNCAL.toString(), channelDetails.mDefaultCalUnits, data);
	}

	public void addDataToMap(String channelName, String channelType, String units, double data){
//		if(useList) {
//			mSensorDataList.add(new SensorData(channelName, channelType, units, data, false));
//		} else {
//			addDataToMap(channelName, channelType, units, data, false);
//		}
		
//		if(dataStructureSelector == 1) {	//JOS: Commented out to put each data structure in its own set method
//			mSensorDataList.add(new SensorData(channelName, channelType, units, data, false));
//		} else if(dataStructureSelector == 2) {
//			HashMap<String, FormatCluster> formatMap = new HashMap<>(3);
//			formatMap.put(channelType, new FormatCluster(channelType, units, data, false));
//			mHashMap.put(channelName, formatMap);
//		} else if(dataStructureSelector == 3) {
//			FormatCluster[] formatClusterArray = new FormatCluster[2];
//			formatClusterArray[0] = new FormatCluster(channelType, units, data, false);
//			mHashMapArray.put(channelName, formatClusterArray);
//		} else if(dataStructureSelector == 4) {
//			mSensorDataArray[mSensorDataArrayIndex] = new SensorData(channelName, channelType, units, data, false);
//			mSensorDataArrayIndex++;
//		} else if(dataStructureSelector == 5) {
//			addDataToMap(channelName, channelType, units, data, false);
//		}
		
		addDataToMap(channelName, channelType, units, data, false);
	}

	public void addDataToMap(String channelName, String channelType, String units, double data, boolean isUsingDefaultCalib){
//		if(useList) {
//			mSensorDataList.add(new SensorData(channelName, channelType, units, data, isUsingDefaultCalib));
//		} else {
//			mPropertyCluster.put(channelName,new FormatCluster(channelType, units, data, isUsingDefaultCalib));
//			addChannelNameToList(channelName);
//		}
		
//		if(dataStructureSelector == 1) {	//ArrayList			//JOS: Commented out to put each data structure in its own set method
//			mSensorDataList.add(new SensorData(channelName, channelType, units, data, isUsingDefaultCalib));
//		} else if(dataStructureSelector == 2) {	//HashMap
//			HashMap<String, FormatCluster> formatMap = new HashMap<>(3);
//			formatMap.put(channelName, new FormatCluster(channelType, units, data, isUsingDefaultCalib));
//			mHashMap.put(channelName, formatMap);
//		} else if(dataStructureSelector == 3) {	//HashMap with Array
//			FormatCluster[] formatClusterArray = new FormatCluster[2];
//			formatClusterArray[0] = new FormatCluster(channelType, units, data, isUsingDefaultCalib);
//			mHashMapArray.put(channelName, formatClusterArray);
//		} else if(dataStructureSelector == 4) {	//Arrays
//			mSensorDataArray[mSensorDataArrayIndex] = new SensorData(channelName, channelType, units, data, isUsingDefaultCalib);
//			mSensorDataArrayIndex++;
//		} else if(dataStructureSelector == 5) {	//Multimap
//			mPropertyCluster.put(channelName, new FormatCluster(channelType, units, data, isUsingDefaultCalib));
////			addChannelNameToList(channelName);		//TODO JOS: Is this necessary?
//		}

		
//		if(channelType.equals(CHANNEL_TYPE.CAL.toString())) {
//			mSensorNamesCal[calArrayIndex] = channelName;
//			mUnitCal[calArrayIndex] = units;
//			mCalDataNew[calArrayIndex] = data;
//			calArrayIndex++;
//		} else {
//			mSensorNamesUncal[uncalArrayIndex] = channelName;
//			mUnitUncal[uncalArrayIndex] = units;
//			mUncalDataNew[uncalArrayIndex] = data;
//			uncalArrayIndex++;
//		}

		
		
		mPropertyCluster.put(channelName, new FormatCluster(channelType, units, data, isUsingDefaultCalib));
//		addChannelNameToList(channelName);		//TODO JOS: Is this necessary?
	}
	
	public void addDataToArrayList(String channelName, String channelType, String units, double data) {
		mSensorDataList.add(new SensorData(channelName, channelType, units, data, false));
	}
	
	public void addDataToNestedHashMap(String channelName, String channelType, String units, double data) {
		HashMap<String, FormatCluster> formatMap = new HashMap<>(3);
		formatMap.put(channelType, new FormatCluster(channelType, units, data, false));
		mHashMap.put(channelName, formatMap);
	}
	
	public void addDataToHashMapArray(String channelName, String channelType, String units, double data) {
		FormatCluster[] formatClusterArray = new FormatCluster[2];
		formatClusterArray[0] = new FormatCluster(channelType, units, data, false);
		mHashMapArray.put(channelName, formatClusterArray);
	}
	
	public void addDataToSensorDataArray(String channelName, String channelType, String units, double data) {
		mSensorDataArray[mSensorDataArrayIndex] = new SensorData(channelName, channelType, units, data, false);
		mSensorDataArrayIndex++;
	}
	
	public int calArrayIndex = 0;
	public int calArrayIndexResize = 0;
	public int uncalArrayIndex = 0;	
	
	public void addDataToArrays(String channelName, String channelType, String units, double data) {
		if(channelType.equals(CHANNEL_TYPE.CAL.toString())) {
			mSensorNamesCal[calArrayIndex] = channelName;
			mUnitCal[calArrayIndex] = units;
			mCalData[calArrayIndex] = data;
			calArrayIndex++;
		} else {
			mSensorNamesUncal[uncalArrayIndex] = channelName;
			mUnitUncal[uncalArrayIndex] = units;
			mUncalData[uncalArrayIndex] = data;
			uncalArrayIndex++;
		}
	}
	
	public void addDataToArraysWithResize(String channelName, String channelType, String units, double data) {
		if(channelType.equals(CHANNEL_TYPE.CAL.toString())) {
			mSensorNamesCalResize[calArrayIndexResize] = channelName;
			mUnitCalResize[calArrayIndexResize] = units;
			mCalDataResize[calArrayIndexResize] = data;
			calArrayIndexResize++;
		} else {
			//TODO JOS: UNCAL ARRAYS HERE
		}
	}
	
	public void addDataToNewArrays(String channelName, String channelType, String units, double data) {
		if(channelType.equals(CHANNEL_TYPE.CAL.toString())) {
			sensorDataArray.mSensorNames[sensorDataArray.mCalArraysIndex] = channelName;	//TODO JOS: This will need to be updated according to updated SensorDataArray for production ObjectCluster class
			sensorDataArray.mCalUnits[sensorDataArray.mCalArraysIndex] = units;
			sensorDataArray.mCalData[sensorDataArray.mCalArraysIndex] = data;
			sensorDataArray.mIsUsingDefaultCalibrationParams[sensorDataArray.mCalArraysIndex] = false;
			sensorDataArray.mCalArraysIndex++;
		} else if(channelType.equals(CHANNEL_TYPE.UNCAL.toString())) {
			sensorDataArray.mSensorNames[sensorDataArray.mUncalArraysIndex] = channelName;
			sensorDataArray.mUncalUnits[sensorDataArray.mUncalArraysIndex] = units;
			sensorDataArray.mUncalData[sensorDataArray.mUncalArraysIndex] = data;
			sensorDataArray.mUncalArraysIndex++;
		}
	}
	
	public void updateArraySensorDataIndex() {
		for(int i=0; i<mSensorDataArray.length; i++) {
			if(mSensorDataArray[i] == null) {
				mSensorDataArrayIndex = i;
				return;
			}
		}
	}	

	@Deprecated
	public void addDataToMap(String channelName, String channelType, String units, List<Double> data){
		mPropertyCluster.put(channelName,new FormatCluster(channelType, units, data));
		addChannelNameToList(channelName);
	}
	
	@Deprecated
	public void addDataToMap(String channelName,String channelType, String units, double data, List<Double> dataArray){
		mPropertyCluster.put(channelName,new FormatCluster(channelType, units, data, dataArray));
		addChannelNameToList(channelName);
	}
	
	private void addChannelNameToList(String channelName) {
		if(!listOfChannelNames.contains(channelName)){
			listOfChannelNames.add(channelName);
		}
	}
	
	@Deprecated
	public void removeAll(String channelName){
		mPropertyCluster.removeAll(channelName);
		listOfChannelNames = new ArrayList<String>();
	}
	
	public Collection<FormatCluster> getCollectionOfFormatClusters(String channelName){
		return mPropertyCluster.get(channelName);
	}

	public Set<String> getChannelNamesFromKeySet(){
		return mPropertyCluster.keySet();
	}

	public List<String> getChannelNamesByInsertionOrder(){
		return listOfChannelNames;
	}

	public Multimap<String, FormatCluster> getPropertyCluster(){
		return mPropertyCluster;
	}
	
	public ObjectCluster2 buildProtoBufMsg(){
		mObjectClusterBuilder = ObjectCluster2.newBuilder();
		for (String channelName:mPropertyCluster.keys()){
			Collection<FormatCluster> fcs = mPropertyCluster.get(channelName);
			FormatCluster2.Builder fcb = FormatCluster2.newBuilder();
			for(FormatCluster fc:fcs){
				DataCluster2.Builder dcb = DataCluster2.newBuilder();
				if (fc.mData!=Double.NaN){
					dcb.setData(fc.mData);	
				}
				if (fc.mDataObject!=null && fc.mDataObject.size()>0){
					dcb.addAllDataArray(fc.mDataObject);
				}
				dcb.setUnit(fc.mUnits);
				fcb.getMutableFormatMap().put(fc.mFormat, dcb.build());
			}
			mObjectClusterBuilder.getMutableDataMap().put(channelName, fcb.build());
		}
		if(mBluetoothAddress!=null)
			mObjectClusterBuilder.setBluetoothAddress(mBluetoothAddress);
		if(mMyName!=null)
			mObjectClusterBuilder.setName(mMyName);
		mObjectClusterBuilder.setCalibratedTimeStamp(mTimeStampMilliSecs);
		ByteBuffer bb = ByteBuffer.allocate(8);
    	bb.put(mSystemTimeStamp);
    	bb.flip();
    	long systemTimeStamp = bb.getLong();
		mObjectClusterBuilder.setSystemTime(systemTimeStamp);
		return mObjectClusterBuilder.build();
	}
	
	public double getTimestampMilliSecs() {
		return mTimeStampMilliSecs;
	}

	public void setTimeStampMilliSecs(double timeStampMilliSecs) {
		this.mTimeStampMilliSecs = timeStampMilliSecs;
	}
	
	/**
	 * @return the mListOfOCTypesEnabled
	 */
	public static List<OBJECTCLUSTER_TYPE> getListOfOCTypesEnabled() {
		return mListOfOCTypesEnabled;
	}

	/**
	 * @param listOfOCTypesEnabled the mListOfOCTypesEnabled to set
	 */
	public static void setListOfOCTypesEnabled(List<OBJECTCLUSTER_TYPE> listOfOCTypesEnabled) {
		ObjectClusterTest.mListOfOCTypesEnabled = listOfOCTypesEnabled;
	}

	public void consolePrintChannelsAndDataSingleLine() {
		System.out.println("ShimmerName:" + mMyName);
		System.out.println("Channels in ObjectCluster:");
		String channelsCal = "Cal:\t";
		String channelsUncal = "Uncal:\t";
		for(String channel:getChannelNamesByInsertionOrder()){
			channelsCal += channel + "=" + getFormatClusterValue(channel, CHANNEL_TYPE.CAL.toString()) + "\t";
			channelsUncal += channel + "=" + getFormatClusterValue(channel, CHANNEL_TYPE.UNCAL.toString()) + "\t";
		}
		System.out.println(channelsCal);
		System.out.println(channelsUncal);
		System.out.println("");
	}

	public void consolePrintChannelsAndDataGrouped() {
		System.out.println("Channels in ObjectCluster:");
		for(String channel:getChannelNamesByInsertionOrder()){
			System.out.println("\t" + channel + ":\t(" + getFormatClusterValue(channel, CHANNEL_TYPE.UNCAL.toString()) + "," + getFormatClusterValue(channel, CHANNEL_TYPE.CAL.toString()) + ")");
		}
		System.out.println("");
	}

	public static ObjectCluster[] generateRandomObjectClusterArray(String deviceName, String signalName, int numSamples, int minValue, int maxValue) {
		Random rand = new Random();
		
		double[] dataArray = new double[numSamples];
		for(int i=0;i<numSamples;i++){
			dataArray[i] = rand.nextInt(maxValue);
		}

//		double[] dataArray = rand.doubles(numSamples, minValue, maxValue);
		
		double timestamp = 0.00001;
		ObjectCluster[] ojcArray = new ObjectCluster[numSamples];
		for(int i=0;i<numSamples;i++){
			ObjectCluster ojc = new ObjectCluster(deviceName);
			ojc.createArrayData(1);
			ojc.addData(signalName, CHANNEL_TYPE.CAL, "", dataArray[i]);
			ojc.addCalData(SensorShimmerClock.channelSystemTimestampPlot, timestamp);
			timestamp+=1;
			ojcArray[i] = ojc;
		}
		
		return ojcArray;
	}

	public ObjectCluster deepClone() {
//		System.out.println("Cloning:" + mUniqueID);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			return (ObjectCluster) ois.readObject();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

}
