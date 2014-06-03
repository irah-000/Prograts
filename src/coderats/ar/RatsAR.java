package coderats.ar;

import java.util.concurrent.ConcurrentHashMap;

import coderats.ar.gl.Globals;
import coderats.ar.objects.ARCard;
import coderats.ar.videoproc.WebcamImageProcessor;

public class RatsAR {
	
	static OpenGLTableAugment table;
	static WebcamImageProcessor input;
	
	public static void main(String[] args) {

		if(Globals.FAKE_AR){
			table = new OpenGLTableAugment(new ConcurrentHashMap<Integer, ARCard>());
		} else {
			input = new WebcamImageProcessor();
			table = new OpenGLTableAugment(input.getKnownCards());
			input.addListener(table);
		}
		
		table.startGL();
		
	}

	public static void requestShutdown() {
		if(!Globals.FAKE_AR) input.shutdown();
		table.requestClose();
	}

}
