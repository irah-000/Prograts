import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class TripCircle {

	private final int BUFFER_SIZE = 5;
	private final int PROXIMITY = 50;
	private final int TIMEOUT = 250;
	private final int SAMPLE_FRAME_SIZE = 64;
	private final int THRESHOLD = 127;
	private final float ROUGH_STEP = 0.35f;
	private final float FINE_STEP = 0.005f;

	private int last_index;

	private int lastX = 0;
	private int lastY = 0;
	private boolean xRecalc = true;
	private boolean yRecalc = true;

	private float[] x;
	private float[] y;
	private float[] r;

	private float a;
	
	private long timestamp;

	private BufferedImage processedTripcode;
	private String code;

	public TripCircle() {
		x = new float[BUFFER_SIZE];
		y = new float[BUFFER_SIZE];
		r = new float[BUFFER_SIZE];
		code = "";
		processedTripcode = new BufferedImage(SAMPLE_FRAME_SIZE, SAMPLE_FRAME_SIZE, BufferedImage.TYPE_3BYTE_BGR);
	}

	public TripCircle(double[] coords, Mat m) {
		this();
		for(int i=0; i < BUFFER_SIZE; i++){
			x[i] = (float) coords[0];
			y[i] = (float) coords[1];
			r[i] = (float) coords[2];
		}
		updateImage(m);
		timestamp = System.currentTimeMillis();
	}

	private void updateImage(Mat m){

		if (m.channels() != 1){
			System.err.println("Wrong format matrix given!");
			return;
		}

		int yLow = (int) (getY()-SAMPLE_FRAME_SIZE/2);
		int yHigh = (int) (getY()+SAMPLE_FRAME_SIZE/2);
		int xLow = (int) (getX()-SAMPLE_FRAME_SIZE/2);
		int xHigh = (int) (getX()+SAMPLE_FRAME_SIZE/2);

		if(yLow < 0) {
			yHigh = SAMPLE_FRAME_SIZE;
			yLow = 0;
		}
		if(xLow < 0){
			xHigh = SAMPLE_FRAME_SIZE;
			xLow = 0;
		}
		if(yHigh > m.height()) {
			yHigh = m.height()-1;
			yLow = m.height() - SAMPLE_FRAME_SIZE;
		}
		if(xHigh > m.width()) {
			xHigh = m.width()-1;
			xLow = m.width() - SAMPLE_FRAME_SIZE;
		}

		Mat newFrame = m.submat(yLow, yHigh, xLow, xHigh).clone();

		processNewFrame(newFrame);

	}

	//	private void transformNewFrame(Mat frame){
	//		if(lastProcessedFrame == null){
	//			lastProcessedFrame = frame;
	//		} else {
	//			try {
	//				Mat transform = Video.estimateRigidTransform(lastProcessedFrame,frame,false);
	//				if(transform.empty()){
	//					lastProcessedFrame = frame;
	//				} else {
	//					Imgproc.warpAffine(frame,lastProcessedFrame,transform,lastProcessedFrame.size(),Imgproc.INTER_NEAREST|Imgproc.WARP_INVERSE_MAP);
	//				}
	//			} catch (Exception e){
	//				S.eprintf(e.getMessage());
	//			}
	//		}
	//	}

	private float findAngleByMaximum(float from, float to, float step, Mat m){
		int lim = 0;
		float target = Integer.MAX_VALUE;
		float angle = from;
		
		//Rough find the starting segment
		for(float i=from; i < to; i += step){

			float xi = (float) (Math.sin(i));
			float yi = (float) (Math.cos(i));
			lim = 0;

			for(int d = 5; d < 25; d++){
				int x = (int)(xi*d + SAMPLE_FRAME_SIZE/2.0f);				
				int y = (int)(yi*d + SAMPLE_FRAME_SIZE/2.0f);
				lim += m.get(y, x)[0];
			}
			
			if(lim < target){
				target = lim;
				angle = i;
			}
			
		}
		return angle;
	}
	
	private float findAngleBySweep(float from, float step, int iterations, Mat m){
	
		float angle = from;
		float value;
		int interrupt = 0;
		
		do {
			
			float xi = (float) (Math.sin(angle));
			float yi = (float) (Math.cos(angle));
			
			int x = (int)(xi*12 + SAMPLE_FRAME_SIZE/2.0f);				
			int y = (int)(yi*12 + SAMPLE_FRAME_SIZE/2.0f);
			
			value = (float) m.get(y, x)[0];
			
			angle -= step;
			
			interrupt ++;
		
		} while (value < THRESHOLD && interrupt < 50);
		
		return angle;
	}
	
	private void processNewFrame(Mat frame){
		
		boolean findXMin = true, findXMax = true, findYMin = true, findYMax = true;

		float xShift = 0;
		float yShift = 0;		
		int outerTop = 0;
		int outerBottom = 0;
		int outerLeft = 0;
		int outerRight = 0;
		
		int innerTop = 0;
		int innerBottom = 0;
		int innerLeft = 0;
		int innerRight = 0;
		
		//Test where the center is. Inside out.
		for(int i=1; i < 8; i++){
			if(findXMax && frame.get(SAMPLE_FRAME_SIZE/2, SAMPLE_FRAME_SIZE/2 + i)[0] > THRESHOLD) innerLeft = i; else findXMax = false;
			if(findXMin && frame.get(SAMPLE_FRAME_SIZE/2, SAMPLE_FRAME_SIZE/2 - i)[0] > THRESHOLD) innerRight = i; else findXMin = false;
			if(findYMax && frame.get(SAMPLE_FRAME_SIZE/2 + i, SAMPLE_FRAME_SIZE/2)[0] > THRESHOLD) innerTop = i; else findYMax = false;
			if(findYMin && frame.get(SAMPLE_FRAME_SIZE/2 - i, SAMPLE_FRAME_SIZE/2)[0] > THRESHOLD) innerBottom = i; else findYMin = false;
		}

		findXMin = true;
		findXMax = true;
		findYMin = true;
		findYMax = true;
		
		//Test the outsides. Outside in.
		for(int i=1; i < SAMPLE_FRAME_SIZE*0.35f; i++){
			if(findXMax && frame.get(SAMPLE_FRAME_SIZE/2, i)[0] > THRESHOLD) outerLeft = i; else findXMax = false;
			if(findXMin && frame.get(SAMPLE_FRAME_SIZE/2, SAMPLE_FRAME_SIZE - 1 - i)[0] > THRESHOLD) outerRight = i; else findXMin = false;
			if(findYMax && frame.get(i, SAMPLE_FRAME_SIZE/2)[0] > THRESHOLD) outerTop = i; else findYMax = false;
			if(findYMin && frame.get(SAMPLE_FRAME_SIZE - 1 - i, SAMPLE_FRAME_SIZE/2)[0] > THRESHOLD) outerBottom = i; else findYMin = false;
		}
		
		//System.out.println(innerLeft + " " + innerRight + " " + innerTop + " " + innerBottom);
		
		//Correct shift
		xShift = ((innerRight-innerLeft)+(outerRight-outerLeft))/4.0f;
		yShift = ((innerBottom-innerTop)+(outerBottom-outerTop))/4.0f;
		
		//Transform the matrix
		Mat M = Mat.zeros(2, 3, CvType.CV_32F);
		M.put(0, 0, new float[] {1,0,xShift,0,1,yShift});
		Imgproc.warpAffine(frame, frame, M, new Size(SAMPLE_FRAME_SIZE, SAMPLE_FRAME_SIZE));
		//frame.convertTo(frame, -1, 2, -50);
		Imgproc.threshold(frame, frame, 90, 255, Imgproc.THRESH_BINARY);

		float roughAngle = 0;
		float refinedAngle = 0;

		roughAngle = findAngleByMaximum(0, (float) (2*Math.PI), ROUGH_STEP, frame);
		
		refinedAngle = findAngleBySweep(roughAngle-ROUGH_STEP*0.5f, FINE_STEP, 5, frame);
		
		updateAngle(refinedAngle);
				
		Imgproc.warpAffine(frame, frame, Imgproc.getRotationMatrix2D(new Point(SAMPLE_FRAME_SIZE/2, SAMPLE_FRAME_SIZE/2), Math.toDegrees(-refinedAngle), 1), new Size(SAMPLE_FRAME_SIZE, SAMPLE_FRAME_SIZE));

		processedTripcode = OpenCVUtils.matToBufferedImage(frame);
		Graphics2D g =  (Graphics2D) processedTripcode.getGraphics();
		
		code = "";
		
		for(float i=(float) (Math.PI*0.15f); i < Math.PI*2; i += Math.PI*0.25f){
		
			float xi = (float) (Math.sin(i));
			float yi = (float) (Math.cos(i));
	
			int x = (int)(xi*25 + SAMPLE_FRAME_SIZE/2.0f);				
			int y = (int)(yi*25 + SAMPLE_FRAME_SIZE/2.0f);
			
			g.setColor(Color.white);
			g.drawLine(SAMPLE_FRAME_SIZE/2, SAMPLE_FRAME_SIZE/2, x, y);
			
			x = (int)(xi*14 + SAMPLE_FRAME_SIZE/2.0f);				
			y = (int)(yi*14 + SAMPLE_FRAME_SIZE/2.0f);

			float value1 = (float) ((frame.get(y, x)[0] + frame.get(y, x+1)[0] + frame.get(y+1, x)[0] + frame.get(y+1, x+1)[0])/4);

			x = (int)(xi*8 + SAMPLE_FRAME_SIZE/2.0f);				
			y = (int)(yi*8 + SAMPLE_FRAME_SIZE/2.0f);
			
			float value2 = (float) ((frame.get(y, x)[0] + frame.get(y, x+1)[0] + frame.get(y+1, x)[0] + frame.get(y+1, x+1)[0])/4);
			
			if(value1 < THRESHOLD && value2 > THRESHOLD){
				code = code + "X";
			} else if(value1 > THRESHOLD && value2 < THRESHOLD){
				code = code + "0";
			} else if(value1 < THRESHOLD && value2 < THRESHOLD){
				code = code + ".";
			} else {
				code = code + " ";
			}
			
		}

	}
	
	public float getAngle(){
		return a;
	}

	private void updateAngle(float b){
		a = (a+b)/2;
	}
	
	public boolean isDead(){
		if(System.currentTimeMillis() < timestamp+TIMEOUT) return false;
		return true;
	}

	public void processNewCoordinates(double[] coordinates, Mat m){
		last_index++;
		if(last_index >= BUFFER_SIZE) last_index = 0;
		x[last_index] = (float) coordinates[0];
		y[last_index] = (float) coordinates[1];
		r[last_index] = (float) coordinates[2];
		xRecalc = true;
		yRecalc = true;
		updateImage(m);
		timestamp = System.currentTimeMillis();
	}

	public boolean isCloseTo(double[] coordinates){
		if(Math.abs(x[0] - coordinates[0]) <= PROXIMITY && Math.abs(y[0] - coordinates[1]) <= PROXIMITY) return true;
		return false;
	}

	public int getX(){
		return (int) x[last_index];
	}

	public int getY(){
		return (int) y[last_index];
	}

	public int getSmoothX() {
		if(xRecalc){
			lastX = 0;
			for(int i=0; i < BUFFER_SIZE; i++){
				lastX += x[i];
			}
			lastX /= BUFFER_SIZE;
			xRecalc = false;
		}
		return lastX;
	}

	public int getSmoothY() {
		if(yRecalc){
			lastY = 0;
			for(int i=0; i < BUFFER_SIZE; i++){
				lastY += y[i];
			}
			lastY /= BUFFER_SIZE;
			yRecalc = false;
		}
		return lastY;
	}

	public int getRadius() {
		int tr = 0;
		for(int i=0; i < BUFFER_SIZE; i++){
			tr += r[i];
		}
		return tr/BUFFER_SIZE;
	}

	public BufferedImage getTripcode() {
		Graphics2D g = (Graphics2D) processedTripcode.getGraphics();
		g.setFont(new Font("Serif", Font.PLAIN, 7));
		g.setColor(Color.white);
		g.fillRect(0, 0, 64, 8);
		g.setColor(Color.blue);
		g.drawString("  "+code, 2, 8);
		return processedTripcode;
	}

}