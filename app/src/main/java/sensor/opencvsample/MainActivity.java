package sensor.opencvsample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.samples.imagemanipulations.R;
import org.opencv.utils.Converters;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    public static final int      VIEW_MODE_RGBA         = 0;
    public static final int      VIEW_MODE_HIST         = 1;
    public static final int      VIEW_MODE_STATIC_ROI   = 2;
    public static final int      VIEW_MODE_CONTOURS     = 3;
    public static final int      VIEW_MODE_MASK         = 4;
    public static final int      VIEW_MODE_BLOBDETECT   = 5;
    public static final int      VIEW_MODE_REGISTER     = 6;

    private MenuItem             mItemPreviewRGBA;
    private MenuItem             mItemPreviewHist;
    private MenuItem             mItemPreviewStatic;
    private MenuItem             mItemPreviewContours;
    private MenuItem             mItemPreviewMask;
    private MenuItem             mItemPreviewBlobDetect;
    private MenuItem             mItemPreviewRegister;

    private Toolbar toolbar;

    private CameraBridgeViewBase mOpenCvCameraView;

    private Size                 mSize0;

    private Mat                  mIntermediateMat;
    private Mat                  mMat0;
    private MatOfInt             mChannels[];
    private MatOfInt             mHistSize;
    private int                  mHistSizeNum = 25;
    private MatOfFloat           mRanges;
    private Scalar               mColorsRGB[];
    private Scalar               mColorsHue[];
    private Scalar               mWhilte;
    private Point                mP1;
    private Point                mP2;
    private float                mBuff[];
    private Mat                  mSepiaKernel;


    // Initialize Matricies.
    private Mat                  zoomCorner;
    private Mat                  mZoomWindow;
    private Mat                  rgbROI;
    private Mat                  rgbBlob;
    private List<MatOfPoint> mContours;
    private Size                 wsize;



    //blob detector variables
    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private Scalar               DOT_COLOR;


    public static int           viewMode = VIEW_MODE_HIST;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        toolbar = (Toolbar)findViewById(R.id.my_Toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //super.onCreateOptionsMenu(menu);
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.menusample, menu);
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA  = menu.add("Preview RGBA");
        mItemPreviewHist  = menu.add("Histograms");
        mItemPreviewStatic  = menu.add("Histogram (Static)");
        mItemPreviewContours = menu.add("Contours");
        mItemPreviewMask = menu.add("Check Mask");
        mItemPreviewBlobDetect = menu.add("Blob Detector");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemPreviewRGBA)
            viewMode = VIEW_MODE_RGBA;
        if (item == mItemPreviewHist)
            viewMode = VIEW_MODE_HIST;
        else if (item == mItemPreviewContours)
            viewMode = VIEW_MODE_CONTOURS;
        else if (item == mItemPreviewMask)
            viewMode = VIEW_MODE_MASK;
        else if (item == mItemPreviewBlobDetect)
            viewMode = VIEW_MODE_BLOBDETECT;
        else if (item == mItemPreviewStatic)
            viewMode = VIEW_MODE_STATIC_ROI;
        else if (item == mItemPreviewRegister)
            viewMode = VIEW_MODE_REGISTER;


        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
        mSize0 = new Size();
        mChannels = new MatOfInt[] { new MatOfInt(0), new MatOfInt(1), new MatOfInt(2) };
        mBuff = new float[mHistSizeNum];
        mHistSize = new MatOfInt(mHistSizeNum);
        mRanges = new MatOfFloat(0f, 256f);
        mMat0  = new Mat();
        mColorsRGB = new Scalar[] { new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255) };
        mColorsHue = new Scalar[] {
                new Scalar(255, 0, 0, 255),   new Scalar(255, 60, 0, 255),  new Scalar(255, 120, 0, 255), new Scalar(255, 180, 0, 255), new Scalar(255, 240, 0, 255),
                new Scalar(215, 213, 0, 255), new Scalar(150, 255, 0, 255), new Scalar(85, 255, 0, 255),  new Scalar(20, 255, 0, 255),  new Scalar(0, 255, 30, 255),
                new Scalar(0, 255, 85, 255),  new Scalar(0, 255, 150, 255), new Scalar(0, 255, 215, 255), new Scalar(0, 234, 255, 255), new Scalar(0, 170, 255, 255),
                new Scalar(0, 120, 255, 255), new Scalar(0, 60, 255, 255),  new Scalar(0, 0, 255, 255),   new Scalar(64, 0, 255, 255),  new Scalar(120, 0, 255, 255),
                new Scalar(180, 0, 255, 255), new Scalar(255, 0, 255, 255), new Scalar(255, 0, 215, 255), new Scalar(255, 0, 85, 255),  new Scalar(255, 0, 0, 255)
        };
        // BLOB DETECTOR
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(100, 0, 0, 255);
        DOT_COLOR = new Scalar(255,0,0);
        // END
        mWhilte = Scalar.all(255);
        mP1 = new Point();
        mP2 = new Point();

        // Fill sepia kernel
        mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
        mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
        mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
        mRgba.release();

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Size sizeRgba = rgba.size();
        Mat mRoi = new Mat();

        Mat rgbaInnerWindow;
        Mat mPyrDownMat = new Mat();//temp for blob
        Mat mHsvMat = new Mat();//temp for blob



        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = cols / 8;
        int top = rows / 8;

        int width = cols * 3 / 4;
        int height = rows * 3 / 4;

        //Declare base dimensions for the submatrix (full screen).
        int x1 = 0;
        int x2 = cols;
        int y1 = 0;
        int y2 = rows;
        rgbROI = rgba.submat(y1, y2, x1, x2); //create submatrix rgbROI that is the region of interest for finding hue etc...

        int num = rows/16;
        int xStart = rows/16;


        switch (MainActivity.viewMode) {
            case MainActivity.VIEW_MODE_RGBA:
                break;

            case MainActivity.VIEW_MODE_HIST:
                Mat hist = new Mat();
                int thikness = (int) (sizeRgba.width / (mHistSizeNum + 10) / 5);
                if(thikness > 5) thikness = 5;
                int offset = (int) ((sizeRgba.width - (5*mHistSizeNum + 4*10)*thikness)/2);
                // RGB
                for(int c=0; c<3; c++) {
                    Imgproc.calcHist(Arrays.asList(rgba), mChannels[c], mMat0, hist, mHistSize, mRanges);
                    Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
                    hist.get(0, 0, mBuff);
                    for(int h=0; h<mHistSizeNum; h++) {
                        mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
                        mP1.y = sizeRgba.height-1;
                        mP2.y = mP1.y - 2 - (int)mBuff[h];
                        Imgproc.line(rgba, mP1, mP2, mColorsRGB[c], thikness);
                    }
                }
                // Value and Hue
                Imgproc.cvtColor(rgba, mIntermediateMat, Imgproc.COLOR_RGB2HSV_FULL);
                // Value
                Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[2], mMat0, hist, mHistSize, mRanges);
                Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
                hist.get(0, 0, mBuff);
                for(int h=0; h<mHistSizeNum; h++) {
                    mP1.x = mP2.x = offset + (3 * (mHistSizeNum + 10) + h) * thikness;
                    mP1.y = sizeRgba.height-1;
                    mP2.y = mP1.y - 2 - (int)mBuff[h];
                    Imgproc.line(rgba, mP1, mP2, mWhilte, thikness);
                }
                // Hue
                Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[0], mMat0, hist, mHistSize, mRanges);
                Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
                hist.get(0, 0, mBuff);
                for(int h=0; h<mHistSizeNum; h++) {
                    mP1.x = mP2.x = offset + (4 * (mHistSizeNum + 10) + h) * thikness;
                    mP1.y = sizeRgba.height-1;
                    mP2.y = mP1.y - 2 - (int)mBuff[h];
                    Imgproc.line(rgba, mP1, mP2, mColorsHue[h], thikness);
                }
                break;
            case MainActivity.VIEW_MODE_STATIC_ROI:

                x1 = cols / 2 - 9 * cols / 100;
                x2 = cols / 2 + 9 * cols / 100;
                y1 = rows / 2 - 9 * rows / 100;
                y2 = rows / 2 + 9 * rows / 100;
                rgbROI = rgba.submat(y1, y2, x1, x2);
                Analysis.findHist(rgba, rgbROI, true, true, true, 1);

                break;

            //Display Histogram for the 4 boxes and a white balance area on a Whatman pH strip.
            case MainActivity.VIEW_MODE_CONTOURS:
                //Blob detection Hue
                mRgba = inputFrame.rgba();

                FindContours mContour = new FindContours();
                mContour.detect(mRgba);
                List<MatOfPoint> contours = mContour.getContours();
                Log.e(TAG, "Contours count: " + contours.size());
                Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

                zoomCorner = rgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);
                mZoomWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
                mRoi = Mask.getMask(mZoomWindow, true);
                Imgproc.resize(mRoi, zoomCorner, zoomCorner.size());
                wsize = mZoomWindow.size();
                zoomCorner.release();
                mZoomWindow.release();

                break;

            //Empty view mode.
            case MainActivity.VIEW_MODE_MASK:
                x1 = cols / 2 - 9 * cols / 100;
                x2 = cols / 2 + 9 * cols / 100;
                y1 = rows / 2 - 9 * rows / 100;
                y2 = rows / 2 + 9 * rows / 100;
                rgbROI = rgba.submat(y1, y2, x1, x2);
                Analysis.findHist(rgba, rgbROI, true, true, true, 1);

                mRoi = Mask.getMask(rgbROI, true);

                zoomCorner = rgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);
                mZoomWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
                mRoi = Mask.getMask(mZoomWindow, true);
                Imgproc.resize(mRoi, zoomCorner, zoomCorner.size());
                wsize = mZoomWindow.size();
                zoomCorner.release();
                mZoomWindow.release();

                break;

            //Test view mode.
            case MainActivity.VIEW_MODE_BLOBDETECT:

                mRgba = inputFrame.rgba();
                Mat                  hsvROI = new Mat(); //create hsv matrix.
                Mat                  maskROI = new Mat(); //Create mask matrix.
                Mat                  maskGreyROI = new Mat(); //Create inverse mask matrix?
                Mat                  colorROI = new Mat(); //Create masked matrix (only showing colored regions).

                Imgproc.cvtColor(mRgba, hsvROI, Imgproc.COLOR_RGB2HSV_FULL); //Go from rgb to hsv of submatrix, and output an intermediate matrix.


                //////////////
                //Blue
                //////////////
                // Set lower and upper limits for mask.
                Scalar BlueCircleLower_lim = new Scalar(156,20,0);
                Scalar BlueCircleUpper_lim = new Scalar(184,255,255);
                //Set mask matrix based on lower/upper limits using hsv matrix.
                Core.inRange(hsvROI,BlueCircleLower_lim, BlueCircleUpper_lim, maskROI);

                FindContours mShape = new FindContours();

                List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
                contours = new ArrayList<>();
                MatOfPoint maxContour = new MatOfPoint();
                Mat mHierarchy = new Mat();

                Imgproc.findContours(maskROI, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

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

                //maskImg = Mask.blue(mRgba);
                //List<MatOfPoint> cont3 = mShape.maskDetect(maskImg);

                //instead of drawing contours, draw a circle with Imgproc.circle()
                Point blueCenter = new Point();
//                Moments newMoments =new Moments(maxContour);
//                center.x = newMoments.get_m10() / newMoments.get_m00();
//                center.y = newMoments.get_m01() / newMoments.get_m00();
                Imgproc.drawContours(mRgba, mContours, -1, CONTOUR_COLOR);

                //bounding rectangle to find center point
                Rect rectangle = Imgproc.boundingRect(maxContour);
                blueCenter.x = rectangle.x+rectangle.width/2;
                blueCenter.y = rectangle.y+rectangle.height/2;
                Imgproc.circle(mRgba, blueCenter, 5, DOT_COLOR);


                //////////////
                //Yellow
                //////////////
                // Set lower and upper limits for mask.
                Scalar YellowCircleLower_lim = new Scalar(28,80,0);
                Scalar YellowCircleUpper_lim = new Scalar(57,255,255);
                //Set mask matrix based on lower/upper limits using hsv matrix.
                Core.inRange(hsvROI,YellowCircleLower_lim, YellowCircleUpper_lim, maskROI);

                Imgproc.findContours(maskROI, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                // Find max contour area
                maxArea = 0;
                each = contours.iterator();
                while (each.hasNext()) {
                    MatOfPoint wrapper = each.next();
                    double area = Imgproc.contourArea(wrapper);
                    if (area > maxArea) {
                        maxArea = area;
                        maxContour = wrapper;
                    }
                }
                mContours.add(maxContour);

                //maskImg = Mask.blue(mRgba);
                //List<MatOfPoint> cont3 = mShape.maskDetect(maskImg);

                //bounding rectangle to find center point
                Point yellowCenter = new Point();
                rectangle = Imgproc.boundingRect(maxContour);
                yellowCenter.x = rectangle.x+rectangle.width/2;
                yellowCenter.y = rectangle.y+rectangle.height/2;
                Imgproc.circle(mRgba, yellowCenter, 5, DOT_COLOR);

                //instead of drawing contours, draw a circle with Imgproc.circle()
                //Imgproc.circle(mRgba, mShape.findCoord(mContours),5, DOT_COLOR);
                Imgproc.drawContours(mRgba, mContours, -1, CONTOUR_COLOR);


                //////////////
                //Green
                //////////////
                // Set lower and upper limits for mask.
                Scalar GreenCircleLower_lim = new Scalar(57,20,0);
                Scalar GreenCircleUpper_lim = new Scalar(85,255,255);
                //Set mask matrix based on lower/upper limits using hsv matrix.
                Core.inRange(hsvROI,GreenCircleLower_lim, GreenCircleUpper_lim, maskROI);

                Imgproc.findContours(maskROI, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                // Find max contour area
                maxArea = 0;
                each = contours.iterator();
                while (each.hasNext()) {
                    MatOfPoint wrapper = each.next();
                    double area = Imgproc.contourArea(wrapper);
                    if (area > maxArea) {
                        maxArea = area;
                        maxContour = wrapper;
                    }
                }
                mContours.add(maxContour);

                //maskImg = Mask.blue(mRgba);
                //List<MatOfPoint> cont3 = mShape.maskDetect(maskImg);

                //bounding rectangle to find center point
                Point greenCenter = new Point();
                rectangle = Imgproc.boundingRect(maxContour);
                greenCenter.x = rectangle.x+rectangle.width/2;
                greenCenter.y = rectangle.y+rectangle.height/2;
                Imgproc.circle(mRgba, greenCenter, 5, DOT_COLOR);

                //instead of drawing contours, draw a circle with Imgproc.circle()
                //Imgproc.circle(mRgba, mShape.findCoord(mContours),5, DOT_COLOR);
                Imgproc.drawContours(mRgba, mContours, -1, CONTOUR_COLOR);


                //////////////
                //Cyan
                //////////////
                // Set lower and upper limits for mask.
                Scalar CyanCircleLower_lim = new Scalar(123,60,0);
                Scalar CyanCircleUpper_lim = new Scalar(152,255,255);
                //Set mask matrix based on lower/upper limits using hsv matrix.
                Core.inRange(hsvROI,CyanCircleLower_lim, CyanCircleUpper_lim, maskROI);

                Imgproc.findContours(maskROI, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                // Find max contour area
                maxArea = 0;
                each = contours.iterator();
                while (each.hasNext()) {
                    MatOfPoint wrapper = each.next();
                    double area = Imgproc.contourArea(wrapper);
                    if (area > maxArea) {
                        maxArea = area;
                        maxContour = wrapper;
                    }
                }
                mContours.add(maxContour);

                //maskImg = Mask.blue(mRgba);
                //List<MatOfPoint> cont3 = mShape.maskDetect(maskImg);

                //bounding rectangle to find center point
                Point cyanCenter = new Point();
                rectangle = Imgproc.boundingRect(maxContour);
                cyanCenter.x = rectangle.x+rectangle.width/2;
                cyanCenter.y = rectangle.y+rectangle.height/2;
                Imgproc.circle(mRgba, cyanCenter, 5, DOT_COLOR);

                //instead of drawing contours, draw a circle with Imgproc.circle()
                //Imgproc.circle(mRgba, mShape.findCoord(mContours),5, DOT_COLOR);
                Imgproc.drawContours(mRgba, mContours, -1, CONTOUR_COLOR);


                //Image Registration and processing
                ArrayList<Point> ptsFound = new ArrayList<>();
                ptsFound.add(yellowCenter);
                ptsFound.add(greenCenter);
                ptsFound.add(cyanCenter);
                ptsFound.add(blueCenter);


                ArrayList<Point> ptsAssigned = new ArrayList<>();
                ptsAssigned.add(new Point(0,0));
                ptsAssigned.add(new Point(850,0));
                ptsAssigned.add(new Point(850,400));
                ptsAssigned.add(new Point(0,400));

                Mat src = Converters.vector_Point2f_to_Mat(ptsFound);
                Mat dst = Converters.vector_Point2f_to_Mat(ptsAssigned);

                if (yellowCenter != null && greenCenter != null && cyanCenter != null && blueCenter != null) {
                    Mat warp = Imgproc.getPerspectiveTransform(src, dst);

                    //warp image to fit to smaller window on frame
                    zoomCorner = rgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);
                    mZoomWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
                    wsize = mZoomWindow.size();

                    //warp the image
                    Imgproc.warpPerspective(mRgba, zoomCorner, warp, wsize, Imgproc.INTER_CUBIC);

                    mRoi = Mask.getMask(mRoi, true);
                    Imgproc.resize(mRoi, zoomCorner, zoomCorner.size());
                    wsize = mZoomWindow.size();
                    zoomCorner.release();
                    mZoomWindow.release();
                }


//                maskImg = Mask.yellow(inputFrame.rgba());
//                List<MatOfPoint> cont4 = mShape.maskDetect(maskImg);
//                Imgproc.drawContours(mRgba, cont4, -1, CONTOUR_COLOR);

//                maskImg = Mask.cyan(inputFrame.rgba());
//                List<MatOfPoint> cont1 = mShape.maskDetect(maskImg);
//                Imgproc.drawContours(mRgba, cont1, -1, CONTOUR_COLOR);
//
//                maskImg = Mask.magenta(inputFrame.rgba());
//                List<MatOfPoint> cont2 = mShape.maskDetect(maskImg);
//                Imgproc.drawContours(mRgba, cont2, -1, CONTOUR_COLOR);


//////////////////IMAGE REFACTORING
//                Mat dst = new Mat(4,1,CvType.CV_32FC2);
//                dst.put(0,0,blueCenter.x,blueCenter.y,yellowCenter.x,yellowCenter.y, greenCenter.x,greenCenter.y, cyanCenter.x, cyanCenter.y);
//                Mat warp = Imgproc.getPerspectiveTransform(mRgba,dst);
//                dst = mRgba.clone();
//                Imgproc.warpPerspective(mRgba,dst,warp,new Size(600,400));
//
//                //warp image to fit to smaller window on frame
//                zoomCorner = rgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);//zoomed corner
//                mZoomWindow = rgba.submat((int)yellowCenter.x,(int) yellowCenter.y,(int)cyanCenter.x,(int)cyanCenter.y);//what is zoomed in on
//
//                mRoi = Mask.getMask(mZoomWindow, true);
//                Imgproc.resize(mRoi, zoomCorner, zoomCorner.size());
//                wsize = mZoomWindow.size();
//                zoomCorner.release();
//                mZoomWindow.release();
//
//                Imgproc.warpAffine(mRgba,dst,warp,new Size(80,45));
//                Imgproc.resize(warp, zoomCorner, zoomCorner.size());

                break;

            //register method set lower and upper limits and call centers draw red dots on screen
            case MainActivity.VIEW_MODE_REGISTER:

                Core YellowCircleMask;

            //END

        }


        return rgba;
    }
}
