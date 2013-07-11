package com.grazz.pebblereactor;

public class ReactorConstants {

	public static final String REACTOR_UUIDPREFIX = "1941e614-9163-49bd-ba01-6d7fa7";

	public static final int REACTOR_COMMAND = 810401000; // uint8
	public static final int REACTOR_PHONEBATTERYLEVEL = 810401001; // uint8
	public static final int REACTOR_PHONERADIOSTATE = 810401002; // uint8
	public static final int REACTOR_PHONENETWORKTYPE = 810401003; // uint8
	public static final int REACTOR_PHONESIGNALLEVEL = 810401004; // uint8
	public static final int REACTOR_NUMBERMISSEDCALLS = 810401005; // uint8
	public static final int REACTOR_NUMBERUNREADSMS = 810401006; // uint8
	public static final int REACTOR_NUMBERCELLRX = 810401007; // uint32
	public static final int REACTOR_NUMBERCELLTX = 810401008; // uint32
	public static final int REACTOR_GPSLATITUDE = 810401009; // uint32
	public static final int REACTOR_GPSLONGITUDE = 810401010; // uint32

	public static final int REACTOR_COMMAND_REFRESH = 0; // uint8
	public static final int REACTOR_COMMAND_QUERYGPS = 1; // uint8

}
