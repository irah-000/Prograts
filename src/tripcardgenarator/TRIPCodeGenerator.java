package tripcardgenarator;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

public class TRIPCodeGenerator {
	
	public static void drawTRIPcode(double xOffset, double yOffset, double radius, TRIPCodesContainer tripCodesWrapper, Graphics2D g) {

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final double sectorWidth = radius * 0.15d;
		final double sectorGap = sectorWidth * 0.58d;
		
		final AffineTransform oldTransform = g.getTransform();
		g.translate(xOffset+radius+(sectorWidth/2), yOffset+radius+(sectorWidth/2));
		
		g.setStroke(new BasicStroke((float) sectorWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
		
		/*
		 * draw outer ring
		 */

		Arc2D.Double ring = new Arc2D.Double();
		ring.setArcByCenter(0, 0, radius, 0, 360, Arc2D.CHORD);
		g.draw(ring);
		
		/*
		 * draw bits
		 */
		
		final int totalBits = tripCodesWrapper.getRingCode1().length();
		final double oneBitAngle = (360d/totalBits);
		
		drawBitsAsConcatenatedArcs(
				tripCodesWrapper.getRingCode2(), g, 1,
				oneBitAngle, totalBits,
				radius, sectorWidth, sectorGap);
		
		drawBitsAsConcatenatedArcs(
				tripCodesWrapper.getRingCode1(), g, 2,
				oneBitAngle, totalBits,
				radius, sectorWidth, sectorGap);

		
		/*
		 * draw inner ring
		 */
		
		ring.setArcByCenter(0, 0, radius-sectorWidth*3-sectorGap*3, 0, 360, Arc2D.CHORD);
		g.draw(ring);
		
		/*
		 * draw innermost ellipse
		 */
		
		Ellipse2D.Double ellipse = new Ellipse2D.Double();
		ellipse.setFrameFromCenter(0, 0, sectorWidth/2, sectorWidth/2);
		g.fill(ellipse);
		

		g.setTransform(oldTransform);
		
	}  // method
	
	
	private static void drawBitsAsConcatenatedArcs(String ringCode, Graphics2D g, int sectorNum, double oneBitAngle, int totalBits, double radius, double sectorWidth, double sectorGap) {
		
		final ArrayList<Arc2D.Double> bitsArcList = new ArrayList<Arc2D.Double>();
		double bitsAngleStart = 0;
		double bitsAngleExtent = 0;
		
		for ( int i=0; i<totalBits; i++ ) {
			if (ringCode.charAt(i) == '1') {
				/*
				 * continue concatenated bits angle by one bit angle
				 */
				bitsAngleExtent += oneBitAngle;
			}
			else {
				/*
				 * add concatenated bits -arc to list,
				 * add start angle by concatenated angle and reset extent
				 */
				if ( bitsAngleExtent != 0 ) {
					Arc2D.Double bitsArc = new Arc2D.Double();
					bitsArc.setArcByCenter(0, 0, radius-(sectorWidth*sectorNum)-(sectorGap*sectorNum), bitsAngleStart, bitsAngleExtent, Arc2D.OPEN);

					bitsAngleStart += bitsAngleExtent;
					bitsAngleExtent = 0;
					bitsArcList.add(bitsArc);
				}
				
				bitsAngleStart += oneBitAngle;
				bitsAngleExtent = 0;
			}
		} // for
		
		/*
		 * draw concatenated bit arcs
		 */
		for (Arc2D.Double bitsAngleArc : bitsArcList) {
			g.draw(bitsAngleArc);
//			System.out.println("from "+bitsAngleArc.start+" extent "+bitsAngleArc.extent);
		}
		
	} // method


	public static TRIPCodesContainer encodeToTRIPcode( long num, long minBitsOnSector ) {
		
		if (num > 10460353202l) return null;
		
		int numOnes = 0;
		int numTwos = 0;
	    StringBuilder ternaryCode = new StringBuilder();
//	    int ternaryDigits = 0;
	    StringBuilder ringCode1 = new StringBuilder();;
	    StringBuilder ringCode2= new StringBuilder();;
	    
	    while (num > 0) {
	    	int rest = (int) (num % 3);
	    	ternaryCode.insert(0, rest);
	    	
	    	if (rest == 1) {
	    		numOnes++;
	    		ringCode1.insert(0, "1");
	    		ringCode2.insert(0, "0");
	    	} else if (rest == 2) {
	    		numTwos++;
	    		ringCode1.insert(0, "0");
	    		ringCode2.insert(0, "1");
	    	} else {
	    		ringCode1.insert(0, "0");
	    		ringCode2.insert(0, "0");
	    	}
	    	
	    	num /= 3;
//	    	ternaryDigits++;
	    }
	    
	    final String formatCode = "%"+(minBitsOnSector-3)+"s"; 
	    ternaryCode = new StringBuilder( String.format(formatCode, ternaryCode).replace(' ', '0') );
	    ringCode1 = new StringBuilder( String.format(formatCode, ringCode1).replace(' ', '0') );
	    ringCode2 = new StringBuilder( String.format(formatCode, ringCode2).replace(' ', '0') );
//	    ternaryDigits = 21;
	    
	    if (numTwos%2 != 0) {
	        ternaryCode.insert(0, "2");
	    	ringCode2.insert(0, "1");
	    } else {
	    	ternaryCode.insert(0, "0");
	    	ringCode2.insert(0, "0");
	    }
    	ringCode1.insert(0, "0");
	    
    	if (numOnes%2 != 0) {
    		ternaryCode.insert(0, "1");
    		ringCode1.insert(0, "1");
	    } else {
	    	ternaryCode.insert(0, "0");
	    	ringCode1.insert(0, "0");
	    }
    	ringCode2.insert(0, "0");
    	
    	ringCode1.insert(0, "1");
    	ringCode2.insert(0, "1");
	    ternaryCode.insert(0, "1");
	    
//	    System.out.println(
//	    		ternaryCode+"\n" +
//	    		ringCode1+"\n" +
//	    		ringCode2
//	    		);
	    
	    return new TRIPCodesContainer(ternaryCode, ringCode1, ringCode2, ternaryCode.length());
	} // method

} // class
