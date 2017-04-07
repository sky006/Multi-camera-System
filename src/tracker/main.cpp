#include <opencv2/core/utility.hpp>
#include <opencv2/tracking.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/highgui.hpp>
#include <iostream>
#include <cstring>
#include<bits/stdc++.h>
#include<mysql/mysql.h>
#include<stdio.h>

#include"cqk_Result.h"
using namespace std;
using namespace cv;
int resID=0,recID=0;//ID of the result--one for a tracker, ID of the record--unique
const string prePath="/home/sky/Desktop/asura";
string resIDStr="";
string int2Str(int n)
{
	stringstream ss;
	string s;
	ss<<n;
	ss>>s;
	return s;
}
struct Date {
	long long year, month, day, hour, minute, second;
	long long getNum() {
		return ((((year * 12 + month) * 30 + day) * 24 + hour * 60)
				+ minute * 60) + second;
	}
	void showDate()
	{
		printf("%lld-%lld-%lld_%lld:%lld:%lld\n",year, month, day, hour, minute, second);
	}
};
//////////////////////////////
//int resID=0;
vector<string> resVideoPath;
string resPicPath;
//////////////////////////////
class mytrack {
private:
	double fRate;
	vector<Point> points;
	Mat lastFrame;
	string outVideoPath;
	string outVideoPathmp4;
	void tracking(string videoName, double fRate, int x, int y, int hei,
			int wid) {
		// declares all required variables
		Rect2d roi;
		Mat frame;
		// create a tracker object
		Ptr<Tracker> tracker = Tracker::create("MIL");
		//set input video
		string video = videoName;
		cout<<"video: "<<video<<endl;
		VideoCapture cap(video);
		//get the total num of frames
		int totalFrame = cap.get(CAP_PROP_FRAME_COUNT);
		cout<<"getFnum:"<<endl;
		int fNum = totalFrame * fRate;
		cout<<"fNUm: "<<fNum<<endl;
		// get bounding box
		cap >> frame;
		cout<<"test000"<<endl;
		//imshow("test000",frame);
		//destroyWindow("test000");
		lastFrame = frame;
		//set frame to selected one
		while (fNum--)
			cap >> frame;
		cout<<"test006"<<endl;
		//imshow("test",frame);
		//set the rec of selected object
		//roi=selectROI("tracker",frame);
		roi.x = x;
		roi.y = y;
		roi.height = hei;
		roi.width = wid;
		//quit if ROI was not selected
		if (roi.width == 0 || roi.height == 0)
			return;
		// initialize the tracker
		tracker->init(frame, roi);
		////////////////////////////////
		//set output
		VideoWriter outputVideo;
		string NAME = outVideoPath;
		cout<<"NAME:"<<NAME<<endl;
		//set the code type of video
		int ex = static_cast<int>(CV_FOURCC('D', 'I', 'V', '3'));
		Size S = Size((int) cap.get(CV_CAP_PROP_FRAME_WIDTH), // Acquire input size
		(int) cap.get(CV_CAP_PROP_FRAME_HEIGHT));
		outputVideo.open(NAME, ex, cap.get(CV_CAP_PROP_FPS), S, true);
		if (!outputVideo.isOpened()) {
			cout << "Could not open the output video for write: " << endl;
		}
		/////////////////////////////////////////
		////////////////////////////////////////////
		// perform the tracking process
		printf("Start the tracking process, press ESC to quit.\n");
		int ptk=0;
		for (;;) {
			/////////////////////
			//set break;
			ptk++;
			if(ptk>100) break;
			/////////////////////
			// get frame from the video
			cap >> frame;
			// stop the program if no more images
			if (frame.rows == 0 || frame.cols == 0)
				break;
			// update the tracking result
			int preX = roi.x;
			if (!tracker->update(frame, roi))
				break;
			if (abs(roi.x - preX) > 20)
				break;
			Point tp;
			tp.x = roi.x + roi.height / 2;
			tp.y = roi.y + roi.width / 2;
			points.push_back(tp);
			cout << roi.x + roi.height / 2 << " " << roi.y + roi.width / 2
					<< endl;
			// draw the tracked object
			rectangle(frame, roi, Scalar(255, 0, 0), 2, 1);
			// show image with the tracked object
			//imshow("tracker", frame);
			outputVideo << frame;
			//quit on ESC button
			if (waitKey(1) == 27)
				break;
		}
		//////////////////////
		//destroyWindow("test");
		//destroyWindow("tracker");
		// use ffmpeg to transfer .avi to .mp4
		outVideoPathmp4=outVideoPath;
		int len=outVideoPathmp4.size();
		outVideoPathmp4[len-3]='m';
		outVideoPathmp4[len-2]='p';
		outVideoPathmp4[len-1]='4';
		string ffmpeg="ffmpeg -i "+outVideoPath+" "+outVideoPathmp4;
		char *s =(char*) ffmpeg.data();
		system(s);
		cout<<ffmpeg<<endl;
		cout<<"transfer Ready!!!"<<endl;
		//////////////////////
	}
	bool reidentification(Mat object) {
		//this part hasn't finshed yet
		//i need to study in the next semester
		return false;
	}
	void drawPathOnBackg() {
		for (int i = 1; i < (int)points.size(); i++) {
			Point pt1, pt2;
			pt1.x = points[i - 1].x;
			pt1.y = points[i - 1].y;
			pt2.x = points[i].x;
			pt2.y = points[i].y;
			rectangle(lastFrame, pt1, pt2, Scalar(0, 255, 127), 2, 1);
		}
		//imshow("resultBack", lastFrame);
		////////////////////
		//save the backpathPIC
		IplImage resImg;
		resImg = IplImage(lastFrame);

		string resultBackPath=prePath+"data/respic"+resIDStr+"resultBack.png";
		cout<<"resImgPath: "<<resultBackPath<<endl;
		char * resImgPath=(char*)resultBackPath.data();
		cvSaveImage(resImgPath, &resImg);
		//destroyWindow("resultBack");
		////////////////////
	}
	void drawPathOnPlane() {
		Mat plane = imread(prePath+"/data/plane.png");
		double rateY = 2.0 / 3.0 * plane.rows / lastFrame.rows * 1.5;
		double rateX = 1.0 / 5.0 * plane.cols / lastFrame.cols;
		Point p00;
		p00.x = 3.4 / 5.0 * plane.cols;
		p00.y = 0;
		Point p1, p2;
		p1.x = p00.x + 2, p1.y = p00.y + 2;
		p2.x = p00.x + 5, p2.y = p00.y + 5;
		//cout<<"p1x:"<<p1.x<<"  p1y: "<<p1.y<<endl;
		//cout<<"p2x:"<<p2.x<<"  p2y: "<<p2.y<<endl;
		rectangle(plane, p1, p2, Scalar(255, 0, 0), 2, 2);
		for (int i = 1; i < (int)points.size(); i++) {
			Point pt1, pt2;
			pt1.x = p00.x + (rateX * points[i - 1].x);
			pt1.y = p00.y + (rateY * points[i - 1].y);
			pt2.x = p00.x + (rateX * points[i].x);
			pt2.y = p00.y + (rateY * points[i].y);
			//cout<<"pt1.x: "<<pt1.x<<endl;
			//cout<<"pt1.y: "<<pt1.y<<endl;
			//cout<<"pt2.x: "<<pt2.x<<endl;
			//cout<<"pt2.y: "<<pt2.y<<endl;
			rectangle(plane, pt1, pt2, Scalar(0, 255, 127), 2, 1);
			//imshow("resultPlane", plane);
		}
		////////////////////
		//save the pathplanePIC
		IplImage resImg;
		resImg = IplImage(plane);
		resPicPath=prePath+"/data/respic/"+resIDStr+"resultPlane.png";
		cout<<"resPicPath: "<<resPicPath<<endl;
		char * resPlaneImg=(char*)resPicPath.data();
		cvSaveImage(resPlaneImg, &resImg);
		////////////////////
//		while (1) {
//			if (waitKey(1) == 27)
//				break;
//		}
		//cout<<"png close"<<endl;
	}
public:
	void init(string videoName, double fRate, int x, int y, int hei, int wid) {
		points.clear();
		outVideoPath=prePath+"/data/resvideo/"+resIDStr+"out.avi";
		this->tracking(videoName, fRate, x, y, hei, wid);
		Mat temp;
		while (reidentification(temp)) {
			//tracking(videoName, fRate, x, y, hei,wid);
			//drawPathOnBackg();
			//drawPathOnPlane()
		}
		drawPathOnBackg();
		drawPathOnPlane();
		//resPicPath = "./data/respic/resultPlane.png";
		resVideoPath.push_back(outVideoPathmp4);
		resVideoPath.push_back(outVideoPathmp4);
		cout<<"Size: "<<resVideoPath.size()<<endl;
	}
};
////////////////////////////////
//transfer jstring to string
char* jstringTostring(JNIEnv* env, jstring jstr) {
	char* rtn = NULL;
	jclass clsstring = env->FindClass("java/lang/String");
	jstring strencode = env->NewStringUTF("utf-8");
	jmethodID mid = env->GetMethodID(clsstring, "getBytes",
			"(Ljava/lang/String;)[B");
	jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
	jsize alen = env->GetArrayLength(barr);
	jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
	if (alen > 0) {
		rtn = (char*) malloc(alen + 1);
		memcpy(rtn, ba, alen);
		rtn[alen] = 0;
	}
	env->ReleaseByteArrayElements(barr, ba, 0);
	return rtn;
}
////////////////////////////////
//transfer the webPath into the path that can be used to track
string getRightPath(string webPath) {
	webPath=prePath+webPath;
	return webPath;
}
////////////////////////////////
Date getDate(string sDate) {
	//format: 2016-12-12 18:44:52
	Date vDate;
	string yearStr, monthStr, dayStr, hourStr, minuteStr, secStr;
	yearStr.insert(0, sDate, 0, 4);
	monthStr.insert(0, sDate, 5, 2);
	dayStr.insert(0, sDate, 8, 2);
	hourStr.insert(0, sDate, 11, 2);
	minuteStr.insert(0, sDate, 14, 2);
	secStr.insert(0, sDate, 17, 2);
	char *trans = (char*)yearStr.data();
	vDate.year = atoi(trans);
	trans = (char*)monthStr.data();
	vDate.month = atoi(trans);
	trans = (char*)dayStr.data();
	vDate.day = atoi(trans);
	trans = (char*)hourStr.data();
	vDate.hour = atoi(trans);
	trans = (char*)minuteStr.data();
	vDate.minute = atoi(trans);
	trans = (char*)secStr.data();
	vDate.second = atoi(trans);
	return vDate;
}
////////////////////////////////
Point getPoint(string sPoint) {
	//format: (x,y)
	string xStr, yStr;
	sPoint.erase(sPoint.begin());
	while (sPoint[0] != ',') {
		xStr.push_back(sPoint[0]);
		sPoint.erase(sPoint.begin());
	}
	sPoint.erase(sPoint.begin());
	while (sPoint[0] != ')') {
		yStr.push_back(sPoint[0]);
		sPoint.erase(sPoint.begin());
	}
	char* trans = (char*)xStr.data();
	Point vPoint;
	vPoint.x = atoi(trans)*2;
	trans = (char*)yStr.data();
	vPoint.y = atoi(trans)*2;
	return vPoint;
}
////////////////////////////////
void insertToDB() {
	cout<<"Start Insert!!!"<<endl;
	MYSQL mysql;
	mysql_init(&mysql);
	cout<<"Init Ready!!!"<<endl;
	if (!mysql_real_connect(&mysql, "localhost", "root", "root", "cv", 0, NULL,
			0)) {
		printf("Error connecting to database:%s\n", mysql_error(&mysql));
	}
	else
		printf("Connected........\n");
	cout<<"Insert!!!"<<endl;
	cout<<"resPIC: !!!"<<resPicPath<<endl;
	while(1)
	{
		cout<<resPicPath[0]<<endl;
		if(resPicPath[0]=='/'&&resPicPath[1]=='a'&&resPicPath[2]=='s') break;
		resPicPath.erase(resPicPath.begin());
	}
	char *picPath = (char*)resPicPath.data();
	cout<<"picPath: "<<picPath<<endl;
	while(1)
	{
		if(resVideoPath[0][0]=='/'&&resVideoPath[0][1]=='a'&&resVideoPath[0][2]=='s') break;
		resVideoPath[0].erase(resVideoPath[0].begin());
	}
	char *vPath = (char*)resVideoPath[0].data();
	cout<<"vPath: "<<vPath<<endl;
	char m_buf[1000];
	int t = 0;
	cout<<resVideoPath.size()<<endl;
	srand(time(NULL));
	for (int i = 0; i < (int)resVideoPath.size(); i++) {
		cout<<"i: "<<i<<endl;
		recID=rand()%100000000;
		sprintf(m_buf, "INSERT INTO resultTable values(%d, %d,'%s',%d,'%s');",recID, resID,picPath, i, vPath);
		cout<<"hello"<<endl;
		cout<<"m_buf: "<<m_buf<<endl;
		t = mysql_query(&mysql, m_buf);
		if(t!=0) cout<<"Wrong_query"<<endl;
	}
	string str2="test.mp4";
	char *vPath2=(char*)
	resVideoPath.clear();
	cout<<"INSERT Ready"<<endl;
}
////////////////////////////////
//Math(cNum,startTime,nowTime,leftT,rightB);
int callTrack(string vPath, string startTime, string nowTime, string leftT,
		string rightB) {
	//set the resID
	srand(time(NULL));
	resID=rand()%100000000;
	resIDStr=int2Str(resID);
	cout<<"callTRack"<<endl;
	mytrack mytracker;
	string videoName = getRightPath(vPath);
	cout<<"videoName: "<<videoName<<endl;
	//videoName="test.mp4";
	Date dstart, dnow,dend;
	////////////////////////
	//get stratTime and endTime
	//string endTime;
	int len=startTime.size();
	string endTime="";
	for(int i=len-1;i>=0;i--)
	{
		if(startTime[i]=='/')
		{
			//startTime.pop_back();
			startTime.erase(startTime.begin()+(startTime.size()-1));
			break;
		}
		endTime.insert(endTime.begin(),startTime[i]);
		//startTime.pop_back();
		startTime.erase(startTime.begin()+(startTime.size()-1));
	}
	endTime.insert(0,startTime,0,11);
	////////////////////////////
	dstart = getDate(startTime);
	dstart.showDate();

	dend=getDate(endTime);
	dend.showDate();

	dnow = getDate(nowTime);
	dnow.showDate();
	//double fRate = 1.0 * dnow.getNum()/(dend.getNum()-dstart.getNum()) ;
	//
	double fRate = 1.0 * (dnow.getNum()-dstart.getNum())/(dend.getNum()-dstart.getNum()) ;
	//
	cout<<"fRate: "<<fRate<<endl;
	Point pl, pr;
	pl = getPoint(leftT);
	pr = getPoint(rightB);
	int x, y, height, width;
	x = pl.x;
	y = pl.y;
	height = pr.y - pl.y;
	width = pr.x - pl.x;
	mytracker.init(videoName, fRate, x, y, height, width);
	cout<<"trackFiNished!!!"<<endl;
	insertToDB();
	return resID;
}
//////////////////////////////
JNIEXPORT jint JNICALL Java_cqk_Result_Math(JNIEnv * env, jobject,
		jstring ivPath, jstring istartTime, jstring inowTime, jstring ileftT,
		jstring irightB) {
	string vPath, startTime, nowTime, leftT, rightB;
	//transfer jstring to string
	cout<<"call JNI"<<endl;
	//freopen("out","w",stdout);
	vPath = jstringTostring(env, ivPath);
	startTime = jstringTostring(env, istartTime);
	nowTime = jstringTostring(env, inowTime);
	leftT = jstringTostring(env, ileftT);
	rightB = jstringTostring(env, irightB);
	cout<<"vPath: "<<vPath<<endl;
	cout<<"startTime: "<<startTime<<endl;
	cout<<"nowTime: "<<nowTime<<endl;
	cout<<"leftT: "<<leftT<<endl;
	cout<<"rightB: "<<rightB<<endl;
	//return 100;
	cout<<"callTrack"<<endl;
	return callTrack(vPath, startTime, nowTime, leftT, rightB);
}
//////////////////////////////
/*int main()
{
	freopen("in","r",stdin);
	string path,startT,nowT,leftT,rightB;
	cin>>path>>startT>>nowT>>leftT>>rightB;
	cout<<path<<endl;
	cout<<"track"<<endl;
	callTrack(path,startT,nowT,leftT,rightB);
	return 0;
}*/
