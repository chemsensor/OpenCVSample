package sensor.opencvsample;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by sensor on 3/22/2016.
 */

public class FindContours {

    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
    private double mMinContourArea = 0.1;

    public void detect(Mat rgbImage){
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        MatOfPoint maxContour = new MatOfPoint();
        Mat mMask = Mask.getMask(rgbImage,false);
        Mat mHierarchy = new Mat();

        Imgproc.findContours(mMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
                maxContour = wrapper;
        }
        mContours.add(maxContour);
/*
        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Core.multiply(contour, new Scalar(1, 1), contour);
                mContours.add(contour);
            }
        }
*/
    }
    public List<MatOfPoint> getContours() {
        return mContours;
    }

    public List<MatOfPoint> maskDetect(Mat maskOfImg){
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        MatOfPoint maxContour = new MatOfPoint();
        Mat mHierarchy = new Mat();

        Imgproc.findContours(maskOfImg, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea) {
                maxArea = area;
                maxContour = wrapper;
            }
        }
        mContours.add(maxContour);

        return mContours;
    }

    public Point findCoord(MatOfPoint contour){

        //changed List<MatOfPoint> to List<Point>

        Point center = new Point();
//        MatOfPoint mop = new MatOfPoint();
//        List<Point> newContour = new ArrayList<Point>();
//
//        mop.fromList(newContour);

        Moments centerMoment = new Moments();
        centerMoment = Imgproc.moments(contour);

        Point centroid = new Point();

        centroid.x = centerMoment.get_m10() / centerMoment.get_m00();
        centroid.y = centerMoment.get_m01() / centerMoment.get_m00();

        center.x = centroid.x;
        center.y = centroid.y;
        //put red dot on centers on screen
        return center;
    }
}
