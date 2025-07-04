package com.shimmerresearch.algorithms.orientation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.shimmerresearch.algorithms.AlgorithmDetails;
import com.shimmerresearch.algorithms.AlgorithmDetails.SENSOR_CHECK_METHOD;
import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.ShimmerDevice;
import com.shimmerresearch.driver.Configuration.CHANNEL_UNITS;
import com.shimmerresearch.driver.Configuration.Shimmer3.DerivedSensorsBitMask;
import com.shimmerresearch.driverUtilities.ChannelDetails;
import com.shimmerresearch.driverUtilities.SensorGroupingDetails;
import com.shimmerresearch.driverUtilities.ChannelDetails.CHANNEL_TYPE;

public class OrientationModule6DOF extends OrientationModule{

	private static final long serialVersionUID = 278045647130089746L;

	//--------- Algorithm specific variables start --------------	
	public static class AlgorithmName{
		public static final String ORIENTATION_6DOF_LN = "LN_Acc_6DoF";
		public static final String ORIENTATION_6DOF_WR = "WR_Acc_6DoF";
	}

	public class GuiLabelConfig{
		public static final String ACCELEROMETER = "Accelerometer";
		public static final String QUATERNION_OUTPUT_6DOF = "Quaternion_6DOF";
		public static final String EULER_OUTPUT_6DOF = "Euler_6DOF";
	}

	protected static final String WR = "_WR";
	protected static final String LN = "_LN";

	public static class ObjectClusterSensorName{
		public static final String QUAT_MADGE_6DOF_W = "Quat_Madge_6DOF_W"; 
		public static final String QUAT_MADGE_6DOF_W_LN = QUAT_MADGE_6DOF_W + LN; 
		public static final String QUAT_MADGE_6DOF_W_WR = QUAT_MADGE_6DOF_W + WR; 
		public static final String QUAT_MADGE_6DOF_X = "Quat_Madge_6DOF_X"; 
		public static final String QUAT_MADGE_6DOF_X_LN = QUAT_MADGE_6DOF_X + LN; 
		public static final String QUAT_MADGE_6DOF_X_WR = QUAT_MADGE_6DOF_X + WR; 
		public static final String QUAT_MADGE_6DOF_Y = "Quat_Madge_6DOF_Y"; 
		public static final String QUAT_MADGE_6DOF_Y_LN = QUAT_MADGE_6DOF_Y + LN; 
		public static final String QUAT_MADGE_6DOF_Y_WR = QUAT_MADGE_6DOF_Y + WR; 
		public static final String QUAT_MADGE_6DOF_Z = "Quat_Madge_6DOF_Z"; 
		public static final String QUAT_MADGE_6DOF_Z_LN = QUAT_MADGE_6DOF_Z + LN; 
		public static final String QUAT_MADGE_6DOF_Z_WR = QUAT_MADGE_6DOF_Z + WR; 

		public static final String AXIS_ANGLE_6DOF_A = "Axis_Angle_6DOF_A"; 
		public static final String AXIS_ANGLE_6DOF_A_LN = AXIS_ANGLE_6DOF_A + LN; 
		public static final String AXIS_ANGLE_6DOF_A_WR = AXIS_ANGLE_6DOF_A + WR; 
		public static final String AXIS_ANGLE_6DOF_X = "Axis_Angle_6DOF_X"; 
		public static final String AXIS_ANGLE_6DOF_X_LN = AXIS_ANGLE_6DOF_X + LN; 
		public static final String AXIS_ANGLE_6DOF_X_WR = AXIS_ANGLE_6DOF_X + WR; 
		public static final String AXIS_ANGLE_6DOF_Y = "Axis_Angle_6DOF_Y"; 
		public static final String AXIS_ANGLE_6DOF_Y_LN = AXIS_ANGLE_6DOF_Y + LN; 
		public static final String AXIS_ANGLE_6DOF_Y_WR = AXIS_ANGLE_6DOF_Y + WR; 
		public static final String AXIS_ANGLE_6DOF_Z = "Axis_Angle_6DOF_Z"; 
		public static final String AXIS_ANGLE_6DOF_Z_LN = AXIS_ANGLE_6DOF_Z + LN; 
		public static final String AXIS_ANGLE_6DOF_Z_WR = AXIS_ANGLE_6DOF_Z + WR; 
		
		public static final String EULER_6DOF_YAW = "Euler_6DOF_Yaw"; 
		public static final String EULER_6DOF_YAW_LN = EULER_6DOF_YAW + LN; 
		public static final String EULER_6DOF_YAW_WR = EULER_6DOF_YAW + WR; 
		public static final String EULER_6DOF_PITCH = "Euler_6DOF_Pitch"; 
		public static final String EULER_6DOF_PITCH_LN = EULER_6DOF_PITCH + LN; 
		public static final String EULER_6DOF_PITCH_WR = EULER_6DOF_PITCH + WR; 
		public static final String EULER_6DOF_ROLL = "Euler_6DOF_Roll"; 
		public static final String EULER_6DOF_ROLL_LN = EULER_6DOF_ROLL + LN; 
		public static final String EULER_6DOF_ROLL_WR = EULER_6DOF_ROLL + WR; 


//		@Deprecated //need to describe axis angle 9DOF vs 6DOF
//		public static final String AXIS_ANGLE_A = "Axis_Angle_A";//Configuration.Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_A; 
//		@Deprecated //need to describe axis angle 9DOF vs 6DOF
//		public static final String AXIS_ANGLE_X = "Axis_Angle_X";//Configuration.Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_X; 
//		@Deprecated //need to describe axis angle 9DOF vs 6DOF
//		public static final String AXIS_ANGLE_Y = "Axis_Angle_Y";//Configuration.Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Y; 
//		@Deprecated //need to describe axis angle 9DOF vs 6DOF
//		public static final String AXIS_ANGLE_Z = "Axis_Angle_Z";//Configuration.Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Z; 
	}
	
	public static class DatabaseChannelHandles{
		public static final String QUARTENION_W_6DOF = "QUAT_MADGE_6DOF_W";
		public static final String QUARTENION_X_6DOF = "QUAT_MADGE_6DOF_X";
		public static final String QUARTENION_Y_6DOF = "QUAT_MADGE_6DOF_Y";
		public static final String QUARTENION_Z_6DOF = "QUAT_MADGE_6DOF_Z";
		
		public static final String EULER_6DOF_YAW = "EULER_6DOF_YAW";
		public static final String EULER_6DOF_PITCH = "EULER_6DOF_PITCH";
		public static final String EULER_6DOF_ROLL = "EULER_6DOF_ROLL";
	}
	//--------- Algorithm specific variables end --------------	


	//--------- Channel maps start --------------
	//Euler
	public static final ChannelDetails channel_Euler_Yaw_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.EULER_6DOF_YAW_LN, //ObjectClusterName
			ObjectClusterSensorName.EULER_6DOF_YAW_LN, //GUI friendly text to display
			ObjectClusterSensorName.EULER_6DOF_YAW_LN, //database name
			CHANNEL_UNITS.DEGREES,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channel_Euler_Pitch_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.EULER_6DOF_PITCH_LN, //ObjectClusterName
			ObjectClusterSensorName.EULER_6DOF_PITCH_LN, //GUI friendly text to display
			ObjectClusterSensorName.EULER_6DOF_PITCH_LN, //database name
			CHANNEL_UNITS.DEGREES,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channel_Euler_Roll_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.EULER_6DOF_ROLL_LN, //ObjectClusterName
			ObjectClusterSensorName.EULER_6DOF_ROLL_LN, //GUI friendly text to display
			ObjectClusterSensorName.EULER_6DOF_ROLL_LN, //database name
			CHANNEL_UNITS.DEGREES,
			Arrays.asList(CHANNEL_TYPE.CAL));
	
	public static final ChannelDetails channelAngleA_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_A_LN, //ObjectClusterName
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_A_LN, //GUI friendly text to display
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_A_LN, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelAngleX_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_X_LN, //ObjectClusterName
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_X_LN, //GUI friendly text to display
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_X_LN, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelAngleY_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Y_LN, //ObjectClusterName
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Y_LN, //GUI friendly text to display
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Y_LN, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelAngleZ_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Z_LN, //ObjectClusterName
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Z_LN, //GUI friendly text to display
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Z_LN, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelQuatW_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.QUAT_MADGE_6DOF_W_LN,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_W_LN,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_W_LN, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelQuatX_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.QUAT_MADGE_6DOF_X_LN,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_X_LN,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_X_LN, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelQuatY_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Y_LN,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Y_LN,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Y_LN, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelQuatZ_6DOF_LN = new ChannelDetails(
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Z_LN,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Z_LN,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Z_LN, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));


	//6DOF wide range 
	
	public static final ChannelDetails channel_Euler_Yaw_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.EULER_6DOF_YAW_WR, //ObjectClusterName
			ObjectClusterSensorName.EULER_6DOF_YAW_WR, //GUI friendly text to display
			ObjectClusterSensorName.EULER_6DOF_YAW_WR, //database name
			CHANNEL_UNITS.DEGREES,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channel_Euler_Pitch_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.EULER_6DOF_PITCH_WR, //ObjectClusterName
			ObjectClusterSensorName.EULER_6DOF_PITCH_WR, //GUI friendly text to display
			ObjectClusterSensorName.EULER_6DOF_PITCH_WR, //database name
			CHANNEL_UNITS.DEGREES,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channel_Euler_Roll_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.EULER_6DOF_ROLL_WR, //ObjectClusterName
			ObjectClusterSensorName.EULER_6DOF_ROLL_WR, //GUI friendly text to display
			ObjectClusterSensorName.EULER_6DOF_ROLL_WR, //database name
			CHANNEL_UNITS.DEGREES,
			Arrays.asList(CHANNEL_TYPE.CAL));
	
	public static final ChannelDetails channelAngleA_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_A_WR, //ObjectClusterName
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_A_WR, //GUI friendly text to display
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_A_WR, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelAngleX_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_X_WR, //ObjectClusterName
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_X_WR, //GUI friendly text to display
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_X_WR, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelAngleY_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Y_WR, //ObjectClusterName
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Y_WR, //GUI friendly text to display
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Y_WR, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelAngleZ_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Z_WR, //ObjectClusterName
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Z_WR, //GUI friendly text to display
			ObjectClusterSensorName.AXIS_ANGLE_6DOF_Z_WR, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelQuatW_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.QUAT_MADGE_6DOF_W_WR,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_W_WR,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_W_WR, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelQuatX_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.QUAT_MADGE_6DOF_X_WR,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_X_WR,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_X_WR, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelQuatY_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Y_WR,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Y_WR,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Y_WR, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	public static final ChannelDetails channelQuatZ_6DOF_WR = new ChannelDetails(
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Z_WR,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Z_WR,
			ObjectClusterSensorName.QUAT_MADGE_6DOF_Z_WR, //database name
			CHANNEL_UNITS.NO_UNITS,
			Arrays.asList(CHANNEL_TYPE.CAL));

	//6DOF channel groups
	public static List<ChannelDetails> listChannelsQuat6DOF_WR = Arrays.asList(
			channelQuatW_6DOF_WR, channelQuatX_6DOF_WR, channelQuatY_6DOF_WR, channelQuatZ_6DOF_WR);

	public static List<ChannelDetails> listChannelsAxisAngle6DOF_LN = Arrays.asList(
			channelAngleA_6DOF_LN, channelAngleX_6DOF_LN, channelAngleY_6DOF_LN, channelAngleZ_6DOF_LN);

	public static List<ChannelDetails> listChannelsQuat6DOF_LN = Arrays.asList(
			channelQuatW_6DOF_LN, channelQuatX_6DOF_LN, channelQuatY_6DOF_LN, channelQuatZ_6DOF_LN);

	public static List<ChannelDetails> listChannelsAxisAngle6DOF_WR = Arrays.asList(
			channelAngleA_6DOF_WR, channelAngleX_6DOF_WR, channelAngleY_6DOF_WR, channelAngleZ_6DOF_WR);
	
	public static List<ChannelDetails> listChannelsEuler6DOF_LN = Arrays.asList(
			channel_Euler_Yaw_6DOF_LN, channel_Euler_Pitch_6DOF_LN, channel_Euler_Roll_6DOF_LN);
	
	public static List<ChannelDetails> listChannelsEuler6DOF_WR = Arrays.asList(
			channel_Euler_Yaw_6DOF_WR, channel_Euler_Pitch_6DOF_WR, channel_Euler_Roll_6DOF_WR);

	//--------- Channel maps end --------------

	//--------- Algorithm maps start --------------
	public static final AlgorithmDetails algo6DoFOrientation_LN_Acc = new AlgorithmDetails(
			AlgorithmName.ORIENTATION_6DOF_LN, 
			OrientationModule.GuiLabelConfig.ORIENTATAION_LN, 
			Arrays.asList(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_X,
					Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y,
					Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z,
					Configuration.Shimmer3.ObjectClusterSensorName.GYRO_X,
					Configuration.Shimmer3.ObjectClusterSensorName.GYRO_Y,
					Configuration.Shimmer3.ObjectClusterSensorName.GYRO_Z),
			(DerivedSensorsBitMask.ORIENTATION_6DOF_LN_QUAT|DerivedSensorsBitMask.ORIENTATION_6DOF_LN_EULER), 
			Arrays.asList(Configuration.Shimmer3.SENSOR_ID.SHIMMER_ANALOG_ACCEL,
					Configuration.Shimmer3.SENSOR_ID.SHIMMER_MPU9X50_GYRO,
					Configuration.Shimmer3.SENSOR_ID.SHIMMER_LSM6DSV_ACCEL_LN,
					Configuration.Shimmer3.SENSOR_ID.SHIMMER_LSM6DSV_GYRO),
					CHANNEL_UNITS.NO_UNITS,
					SENSOR_CHECK_METHOD.ANY,
					listChannelsEuler6DOF_LN);

	public static final AlgorithmDetails algo6DoFOrientation_WR_Acc = new AlgorithmDetails(
			AlgorithmName.ORIENTATION_6DOF_WR, 
			OrientationModule.GuiLabelConfig.ORIENTATAION_WR, 
			Arrays.asList(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_X,
					Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y,
					Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z,
					Configuration.Shimmer3.ObjectClusterSensorName.GYRO_X,
					Configuration.Shimmer3.ObjectClusterSensorName.GYRO_Y,
					Configuration.Shimmer3.ObjectClusterSensorName.GYRO_Z),
			(DerivedSensorsBitMask.ORIENTATION_6DOF_WR_QUAT|DerivedSensorsBitMask.ORIENTATION_6DOF_WR_EULER), 
			Arrays.asList(Configuration.Shimmer3.SENSOR_ID.SHIMMER_LSM303_ACCEL,
					Configuration.Shimmer3.SENSOR_ID.SHIMMER_MPU9X50_GYRO,
					Configuration.Shimmer3.SENSOR_ID.SHIMMER_LIS2DW12_ACCEL_WR,
					Configuration.Shimmer3.SENSOR_ID.SHIMMER_LSM6DSV_GYRO),
					CHANNEL_UNITS.NO_UNITS,
					SENSOR_CHECK_METHOD.ANY,
					listChannelsQuat6DOF_WR);

	public static final Map<String, AlgorithmDetails> mAlgorithmMapRef;
	static {
		Map<String, AlgorithmDetails> aMap = new LinkedHashMap<String, AlgorithmDetails>();
		aMap.put(algo6DoFOrientation_LN_Acc.mAlgorithmName, algo6DoFOrientation_LN_Acc);
		aMap.put(algo6DoFOrientation_WR_Acc.mAlgorithmName, algo6DoFOrientation_WR_Acc);
		mAlgorithmMapRef = Collections.unmodifiableMap(aMap);
	}
	//--------- Algorithm maps end --------------

	// ------------------- Algorithms grouping map start -----------------------
	public static final SensorGroupingDetails sGD6Dof = new SensorGroupingDetails(
			Configuration.Shimmer3.GuiLabelAlgorithmGrouping.ORIENTATION_6DOF.getTileText(), 
			Arrays.asList(OrientationModule6DOF.algo6DoFOrientation_LN_Acc,
					OrientationModule6DOF.algo6DoFOrientation_WR_Acc),
					Arrays.asList(GuiLabelConfig.QUATERNION_OUTPUT_6DOF,
							GuiLabelConfig.EULER_OUTPUT_6DOF),
							0);
	// ------------------- Algorithms grouping map end -----------------------

//	{
//		//TODO move to generateConfigOptionsMap()
//		mConfigOptionsMap.put(GuiLabelConfig.QUATERNION_OUTPUT_6DOF, configOptionQuatOutput);
//		mConfigOptionsMap.put(GuiLabelConfig.EULER_OUTPUT_6DOF, configOptionEulerOutput);
//
//		//TODO move to generateAlgorithmGroupingMap()
//		mMapOfAlgorithmGrouping.put(Configuration.Shimmer3.GuiLabelAlgorithmGrouping.ORIENTATION_6DOF.ordinal(), sGD6Dof);
//	}

    //--------- Constructors for this class start --------------
	
	public OrientationModule6DOF(ShimmerDevice shimmerDevice, AlgorithmDetails algorithmDetails, double samplingRate) {
		super(shimmerDevice, algorithmDetails);
		setupAlgorithm();

		setShimmerSamplingRate(samplingRate);
	}

    //--------- Constructors for this class end --------------

	@Override
	public void setupAlgorithm() {
		super.setupAlgorithm();
		
		mAlgorithmType = ALGORITHM_TYPE.ALGORITHM_TYPE_CONTINUOUS;
		mAlgorithmResultType = ALGORITHM_RESULT_TYPE.ALGORITHM_RESULT_TYPE_SINGLE_OBJECT_CLUSTER;

		try {
			initialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setGeneralAlgorithmName() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setFilteringOption() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setMinSamplingRateForAlgorithm() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setSupportedVerInfo() {
		mListOfCompatibleSVO.add(svoSh3Module);
	}


	@Override
	public void generateConfigOptionsMap() {
		mConfigOptionsMap.put(GuiLabelConfig.QUATERNION_OUTPUT_6DOF, configOptionQuatOutput);
		mConfigOptionsMap.put(GuiLabelConfig.EULER_OUTPUT_6DOF, configOptionEulerOutput);
	}


	@Override
	public void generateAlgorithmGroupingMap() {
		mMapOfAlgorithmGrouping.put(Configuration.Shimmer3.GuiLabelAlgorithmGrouping.ORIENTATION_6DOF.ordinal(), sGD6Dof);
	}
	
	@Override
	public void initialize() throws Exception {

		double samplingPeriod = 1/getShimmerSamplingRate();

		if(mAlgorithmName.equals(AlgorithmName.ORIENTATION_6DOF_LN)
				||mAlgorithmName.equals(AlgorithmName.ORIENTATION_6DOF_WR)){
			orientationType = ORIENTATION_TYPE.SIX_DOF;

			if(orientationAlgorithm==null){
				orientationAlgorithm = new GradDes3DOrientation(samplingPeriod);
			} else {
				orientationAlgorithm.setSamplingPeriod(samplingPeriod);
			}
		}

		if(mAlgorithmName.equals(AlgorithmName.ORIENTATION_6DOF_LN)){
			setAccelerometer(OrientationModule.GuiLabelConfig.ORIENTATAION_LN);
		}
		else{
			setAccelerometer(OrientationModule.GuiLabelConfig.ORIENTATAION_WR);
		}
	}


	public Orientation3DObject applyOrientationAlgorithm(){

		Orientation3DObject quaternion;

		quaternion = ((GradDes3DOrientation) orientationAlgorithm).update(
				accValues.x, accValues.y, accValues.z, 
				gyroValues.x, gyroValues.y, gyroValues.z);

		return quaternion;
	}


	@Override
	public List<ChannelDetails> getChannelDetails(boolean showDisabledChannels) {

		List<ChannelDetails> listOfChannelDetails = new ArrayList<ChannelDetails>();

		if(mAlgorithmDetails.mAlgorithmName.equals(AlgorithmName.ORIENTATION_6DOF_WR)){
			if(showDisabledChannels || isQuaternionOutput()){
				listOfChannelDetails.addAll(listChannelsQuat6DOF_WR);
			}
			if(showDisabledChannels || isEulerOutput()){
				listOfChannelDetails.addAll(listChannelsEuler6DOF_WR);
			}
			if(showDisabledChannels || isAxisAngleOutput()){
				listOfChannelDetails.addAll(listChannelsAxisAngle6DOF_WR);
			}
		}
		else if(mAlgorithmDetails.mAlgorithmName.equals(AlgorithmName.ORIENTATION_6DOF_LN)){
			if(showDisabledChannels || isQuaternionOutput()){
				listOfChannelDetails.addAll(listChannelsQuat6DOF_LN);
			}
			if(showDisabledChannels || isEulerOutput()){
				listOfChannelDetails.addAll(listChannelsEuler6DOF_LN);
			}
			if(showDisabledChannels || isAxisAngleOutput()){
				listOfChannelDetails.addAll(listChannelsAxisAngle6DOF_LN);
			}
		}

		//		return super.getChannelDetails();
		return listOfChannelDetails;
	}

	public void setAccelerometer(String accelerometerName){
		this.accelerometerSensor = accelerometerName;
	}

	@Override
	public Object getSettings(String componentName) {
		Object returnValue = null;
		switch(componentName){

		case(GuiLabelConfigCommon.SHIMMER_SAMPLING_RATE):
			returnValue = getShimmerSamplingRate();
		break;
//		case(GuiLabelConfig.ACCELEROMETER):
//			returnValue = getAccelerometer();
//		break;
		case(GuiLabelConfig.QUATERNION_OUTPUT_6DOF):
			returnValue = isQuaternionOutput();
		break;
		case(GuiLabelConfig.EULER_OUTPUT_6DOF):
			returnValue = isEulerOutput();
		break;
		}
		return returnValue;
	}


	@Override
	public Object getDefaultSettings(String componentName) {
		Object returnValue = null;
		switch(componentName){
		case(GuiLabelConfigCommon.SHIMMER_SAMPLING_RATE):
			returnValue = 512;
		break;
//		case(GuiLabelConfig.ACCELEROMETER):
//			returnValue = Shimmer3.LABEL_SENSOR_TILE.LOW_NOISE_ACCEL;
//			break;
		case(GuiLabelConfig.QUATERNION_OUTPUT_6DOF):
			returnValue = true;
		break;
		case(GuiLabelConfig.EULER_OUTPUT_6DOF):
			returnValue = false;
		break;
		}
		return returnValue;
	}

	@Override
	public void setSettings(String componentName, Object valueToSet){

		switch(componentName){
		case(GuiLabelConfigCommon.SHIMMER_SAMPLING_RATE):
			if(valueToSet instanceof String){
				if(!((String) valueToSet).isEmpty()){
					setShimmerSamplingRate(Double.parseDouble((String) valueToSet));
				}
			}
			else if(valueToSet instanceof Double){
				setShimmerSamplingRate((Double) valueToSet);
			}
		break;
//		case(GuiLabelConfig.ACCELEROMETER):
//			setAccelerometer((String) valueToSet);
//			break;
		case(GuiLabelConfig.QUATERNION_OUTPUT_6DOF):
			if(valueToSet instanceof Boolean){
				setQuaternionOutput((boolean) valueToSet);
			}
			else if(valueToSet instanceof Integer){
				setQuaternionOutput(((Integer) valueToSet)>0? true:false);
			}
		break;
		case(GuiLabelConfig.EULER_OUTPUT_6DOF):
			if(valueToSet instanceof Boolean){
				setEulerOutput((boolean) valueToSet);
			}
			else if(valueToSet instanceof Integer){
				setEulerOutput(((Integer) valueToSet)>0? true:false);
			}
		break;
		}
	}
	
	@Override
	public void algorithmMapUpdateFromEnabledSensorsVars(long derivedSensorBitmapID) {
		setQuaternionOutput(false);
		setEulerOutput(false);
		setAxisAngleOutput(false);
		setIsEnabled(false);
		
		// This is necessary because all 6DOF modules need to have synced
		// booleans for enabling Quaternion/Euler outputs for the Consensys implementation. 
		if(((derivedSensorBitmapID&DerivedSensorsBitMask.ORIENTATION_6DOF_LN_QUAT)>0)
			||(derivedSensorBitmapID&DerivedSensorsBitMask.ORIENTATION_6DOF_WR_QUAT)>0){
			setQuaternionOutput(true);
		}
		if(((derivedSensorBitmapID&DerivedSensorsBitMask.ORIENTATION_6DOF_LN_EULER)>0)
			||(derivedSensorBitmapID&DerivedSensorsBitMask.ORIENTATION_6DOF_WR_EULER)>0){
			setEulerOutput(true);
		}
		
		
		if(mAlgorithmDetails.mAlgorithmName.equals(AlgorithmName.ORIENTATION_6DOF_LN)){
			if(((derivedSensorBitmapID&DerivedSensorsBitMask.ORIENTATION_6DOF_LN_QUAT)>0)
				||(derivedSensorBitmapID&DerivedSensorsBitMask.ORIENTATION_6DOF_LN_EULER)>0){
				setIsEnabled(true);
			}
		}
		else if(mAlgorithmDetails.mAlgorithmName.equals(AlgorithmName.ORIENTATION_6DOF_WR)){
			if(((derivedSensorBitmapID&DerivedSensorsBitMask.ORIENTATION_6DOF_WR_QUAT)>0)
				||(derivedSensorBitmapID&DerivedSensorsBitMask.ORIENTATION_6DOF_WR_EULER)>0){
				setIsEnabled(true);
			}
		}
	}

	@Override
	public LinkedHashMap<String, Object> generateConfigMap() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void parseConfigMapFromDb(LinkedHashMap<String, Object> mapOfConfigPerShimmer) {
		// TODO Auto-generated method stub
		
	}

	public ObjectCluster addQuaternionToObjectCluster(Orientation3DObject quaternion, ObjectCluster objectCluster){
		
		if(mAlgorithmName.equals(AlgorithmName.ORIENTATION_6DOF_LN)){
			if(isEulerOutput()){
				objectCluster.addData(ObjectClusterSensorName.EULER_6DOF_YAW_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getYaw());
				objectCluster.addData(ObjectClusterSensorName.EULER_6DOF_PITCH_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getPitch());
				objectCluster.addData(ObjectClusterSensorName.EULER_6DOF_ROLL_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getRoll());
			}
			if(isQuaternionOutput()){
				objectCluster.addData(ObjectClusterSensorName.QUAT_MADGE_6DOF_W_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getQuaternionW());
				objectCluster.addData(ObjectClusterSensorName.QUAT_MADGE_6DOF_X_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getQuaternionX());
				objectCluster.addData(ObjectClusterSensorName.QUAT_MADGE_6DOF_Y_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getQuaternionY());
				objectCluster.addData(ObjectClusterSensorName.QUAT_MADGE_6DOF_Z_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getQuaternionZ());
			}
			if(isAxisAngleOutput()){
				objectCluster.addData(ObjectClusterSensorName.AXIS_ANGLE_6DOF_A_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getTheta());
				objectCluster.addData(ObjectClusterSensorName.AXIS_ANGLE_6DOF_X_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getAngleX());
				objectCluster.addData(ObjectClusterSensorName.AXIS_ANGLE_6DOF_Y_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getAngleY());
				objectCluster.addData(ObjectClusterSensorName.AXIS_ANGLE_6DOF_Z_LN,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getAngleZ());
			}
			
		}
		else if(mAlgorithmName.equals(AlgorithmName.ORIENTATION_6DOF_WR)){
			if(isEulerOutput()){
				objectCluster.addData(ObjectClusterSensorName.EULER_6DOF_YAW_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getYaw());
				objectCluster.addData(ObjectClusterSensorName.EULER_6DOF_PITCH_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getPitch());
				objectCluster.addData(ObjectClusterSensorName.EULER_6DOF_ROLL_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getRoll());
			}
			if(isQuaternionOutput()){
				objectCluster.addData(ObjectClusterSensorName.QUAT_MADGE_6DOF_W_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getQuaternionW());
				objectCluster.addData(ObjectClusterSensorName.QUAT_MADGE_6DOF_X_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getQuaternionX());
				objectCluster.addData(ObjectClusterSensorName.QUAT_MADGE_6DOF_Y_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getQuaternionY());
				objectCluster.addData(ObjectClusterSensorName.QUAT_MADGE_6DOF_Z_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getQuaternionZ());
			}
			if(isAxisAngleOutput()){
				objectCluster.addData(ObjectClusterSensorName.AXIS_ANGLE_6DOF_A_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getTheta());
				objectCluster.addData(ObjectClusterSensorName.AXIS_ANGLE_6DOF_X_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getAngleX());
				objectCluster.addData(ObjectClusterSensorName.AXIS_ANGLE_6DOF_Y_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getAngleY());
				objectCluster.addData(ObjectClusterSensorName.AXIS_ANGLE_6DOF_Z_WR,CHANNEL_TYPE.CAL,CHANNEL_UNITS.NO_UNITS,quaternion.getAngleZ());
			}
			
		}

		return objectCluster;
	}

	
}


